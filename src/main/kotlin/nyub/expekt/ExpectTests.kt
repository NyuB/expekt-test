package nyub.expekt

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.assertj.core.api.Assertions.assertThat

/**
 * In-source snapshot testing library
 *
 * @property resolveClassesFrom indicates where to search for the actual source file when running an expect test
 * @property promote when `true` updates all source files with the current output, otherwise performs equality
 *   assertions
 * @see ExpectTest
 * @see <a href="https://github.com/NyuB/expekt-test">Documentation on GitHub</a>
 * @see <a href="https://blog.janestreet.com/the-joy-of-expect-tests/">Main inspiration for this library</a>
 * @see <a href="https://ianthehenry.com/posts/my-kind-of-repl/">Detailed article on inline snapshot testing</a>
 */
data class ExpectTests(
    private val resolveClassesFrom: Path = Paths.get("src/test/kotlin"),
    private val promote: Boolean = false,
) {
    /**
     * All printed outputs within the provided scope are stored and can be asserted with [ExpectTest.expect] A call to
     * [ExpectTest.expect] clears the output. When leaving expectTest, an AssertionError is raised if there is any
     * remaining output.
     *
     * ```kotlin
     * @Test
     * fun myTest() = expectTest() {
     *     print("A line")
     *     expect("""
     *     A line
     *     """.trimIndent())
     * }
     * ```
     */
    fun expectTest(test: ExpectTest.() -> Unit) {
        val expectTest = ExpectTest(this)
        expectTest.test()
        expectTest.end()
    }

    fun expectTest(): ExpectTest = ExpectTest(this)

    /** Test scope. Maintains an output buffer with the printed content and provides assertions on its content */
    class ExpectTest internal constructor(private val creator: ExpectTests) {
        /**
         * Current printed content. [print] calls fill this output, [expect] calls clear it, [end] calls ensure it is
         * empty.
         *
         * @see print
         * @see expect
         * @see end
         */
        private val output = StringBuilder()

        /** `true` when [output] is empty */
        val isEmpty: Boolean
            get() = output.isEmpty()

        /** Add [content] to the output buffer */
        fun print(content: Any) {
            output.append(content)
        }

        /**
         * Equivalent to
         *
         * ```kotlin
         * print(content)
         * print("\n")
         * ```
         *
         * @see print
         */
        fun println(content: Any) {
            output.append(content).append("\n")
        }

        /**
         * Equivalent to
         *
         * ```kotlin
         * print("\n")
         * ```
         *
         * @see print
         */
        fun newLine() = print("\n")

        /**
         * Equivalent to
         *
         * ```kotlin
         * print(String.format(formatString, *format))
         * ```
         *
         * @see print
         */
        fun printf(formatString: String, vararg format: Any) {
            print(String.format(formatString, *format))
        }

        /**
         * Asserts that the current [output] matches the [expected] content, or update the [expected] content in place
         * if [creator] is in promote mode. Clears the [output] buffer.
         *
         * @throws AssertionError if the current output does not match [expected]
         */
        fun expect(expected: String) =
            try {
                val lines = output.toString().nonEmptyLines()
                creator.expect(expected, lines.joinToString(separator = "\n"))
            } finally {
                output.clear()
            }

        /**
         * Asserts that [actual].toString() matches the [expected] content, or update the [expected] content in place if
         * [creator] is in promote mode.
         */
        fun expect(actual: Any, expected: String) {
            val lines = actual.toString().nonEmptyLines()
            creator.expect(expected, lines.joinToString(separator = "\n"))
        }

        /**
         * @throws AssertionError if there is still any unhandled output in [output]
         * @see expect
         */
        fun end() {
            if (output.isNotEmpty()) throw AssertionError("Unhandled output remaining after expect test: '$output'")
        }

        /** Clears the current output buffer */
        fun clear() {
            output.clear()
        }

        private fun String.nonEmptyLines() = this.split("\n").map { it.trimEnd() }.filter { it.isNotEmpty() }
    }

    private fun expect(expected: String, actual: String) {
        if (promote) {
            promote(expected, actual)
        } else {
            assertThat(actual).isEqualTo(expected.trim { it.isWhitespace() || it == '\n' })
        }
    }

    private fun promote(expected: String, actual: String) {
        val callSite = callSite()
        val callSiteFile =
            resolveClassesFrom.resolve(callSite.className.replace(".", "/")).parent.resolve(callSite.fileName!!)
        val callSiteLines = Files.readString(callSiteFile).split("\n")
        val expectedLines = if (expected.isEmpty()) emptyList() else expected.split("\n").dropLastWhile { it.isEmpty() }

        val lineNumber = offsets.getAdjustedLine(callSiteFile, callSite.lineNumber)
        val stringStartIndex =
            findTripleQuotedStringStart(callSiteLines, lineNumber - 1)
                ?: throw RuntimeException(
                    "Could not find expected string at $callSiteFile:$lineNumber, maybe it is not within a triple-quoted block?"
                )

        val before = callSiteLines.subList(0, stringStartIndex + 1)
        val between = callSiteLines.subList(stringStartIndex + 1, stringStartIndex + 1 + expectedLines.size)
        val after = callSiteLines.subList(stringStartIndex + 1 + expectedLines.size, callSiteLines.size)

        val actualLines = actual.split("\n")
        Files.writeString(callSiteFile, ExpectedLinesReplacement(before, between, after).replaceWith(actualLines))
        offsets.record(callSiteFile, callSite.lineNumber, actualLines.size - between.size)
    }

    private fun findTripleQuotedStringStart(lines: List<String>, startIndex: Int): Int? {
        var index = startIndex
        while (index < lines.size && !lines[index].contains("\"\"\"")) index++
        return if (index == lines.size) null else index
    }

    private class ExpectedLinesReplacement(val before: List<String>, between: List<String>, val after: List<String>) {
        fun replaceWith(actualLines: List<String>) =
            (before + actualLines.map { commonPrefix + it } + after).joinToString(separator = "\n")

        private val commonPrefix: String = sharedIndentation(between)

        private fun sharedIndentation(between: List<String>): String {
            if (between.isEmpty()) return ""
            var minPrefix: String? = null
            between.forEach {
                if (minPrefix == null || !it.startsWith(minPrefix!!)) {
                    minPrefix = spacePrefix(it)
                }
            }
            return minPrefix!!
        }

        private fun spacePrefix(s: String): String {
            var space = 0
            while (space < s.length && s[space].isWhitespace()) space++
            return s.substring(0, space)
        }
    }

    private fun callSite(): StackTraceElement {
        val currentStack = Thread.currentThread().stackTrace
        val (thisMethodIndex, _) =
            currentStack.withIndex().first { (_, e) ->
                e.methodName == "expect" && e.className == ExpectTest::class.java.name
            }
        return currentStack[thisMethodIndex + 1]
    }
}

/**
 * Globally records line additions or removal to keep line counts up-to-date if there are multiple promotions to the
 * same file
 */
private val offsets = FileOffsets()

class FileOffsets {
    private val offsets = mutableMapOf<String, LineOffsets>()

    /** Records a number of lines added (if [offset] is positive) or removed (if [offset] is negative) from a [file] */
    fun record(file: Path, originalLine: Int, offset: Int) {
        val current = offsets.getOrPut(file.toString()) { LineOffsets() }
        current.insert(originalLine, offset)
    }

    /**
     * @return the line corresponding to [originalLine] within a [file], which may differ if the file was written to
     *   during a promotion
     */
    fun getAdjustedLine(file: Path, originalLine: Int): Int {
        return offsets[file.toString()]?.getAdjustedLine(originalLine) ?: originalLine
    }
}

class LineOffsets {
    private val offsets = mutableListOf<Offset>()

    /** Records an offset from [originalLine] */
    fun insert(originalLine: Int, offset: Int) {
        var index = 0
        while (index < offsets.size && offsets[index].originalLine < originalLine) index++
        offsets.add(index, Offset(originalLine, offset))
    }

    /** @return the line corresponding to [originalLine] after taking these [offsets] into account */
    fun getAdjustedLine(originalLine: Int): Int {
        var res = originalLine
        offsets.forEach { if (it.originalLine < originalLine) res += it.offset }
        return res
    }

    private class Offset(val originalLine: Int, val offset: Int)
}
