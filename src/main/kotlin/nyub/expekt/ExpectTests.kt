package nyub.expekt

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.assertj.core.api.Assertions.assertThat

/**
 * @property resolveClassesFrom indicates where to search for the actual source file when running and expect test
 * @property promote when `true` updates all source files with the current output, otherwise perform usual equality
 *   assertions
 */
class ExpectTests(
    private val resolveClassesFrom: Path = Paths.get("src/test/kotlin"),
    private val promote: Boolean = false,
) {
    /**
     * All prints output within the provided scopes are stored and can be asserted with [ExpectTest.expect] A call to
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

    /** Test scope. Maintains an output buffer with the printed content and provides assertions on its content */
    class ExpectTest internal constructor(private val creator: ExpectTests) {
        private val actual = StringBuilder()
        internal val output: String
            get() = actual.toString()

        /** Add [s] to the output buffer */
        fun print(s: String) {
            actual.append(s)
        }

        /**
         * Equivalent to
         *
         * ```kotlin
         * print(s)
         * print("\n")
         * ```
         *
         * @see print
         */
        fun println(s: String) {
            actual.append(s).append("\n")
        }

        /**
         * Asserts that the current output matches the [expected] content, or update the [expected] content in place if
         * [creator] is in promote mode. Clears the output buffer.
         *
         * @throws AssertionError if the current output does not match [expected]
         */
        fun expect(expected: String) =
            try {
                val lines = actual.toString().split("\n").map { it.trimEnd() }.filter { it.isNotEmpty() }
                creator.expect(expected, lines.joinToString(separator = "\n"))
            } finally {
                actual.clear()
            }

        /**
         * @throws AssertionError if there is still any unhandled output
         * @see expect
         */
        fun end() {
            if (actual.isNotEmpty()) throw AssertionError("Unhandled output remaining after expect test: '$actual'")
        }

        /** Clears the current output buffer */
        fun clear() {
            actual.clear()
        }
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
        val stringStartIndex =
            findTripleQuotedStringStart(callSiteLines, callSite.lineNumber - 1)
                ?: throw RuntimeException(
                    "Could not find expected string at $callSiteFile:${callSite.lineNumber}, maybe it is not within a triple-quoted block?"
                )

        val before = callSiteLines.subList(0, stringStartIndex + 1)
        val after = callSiteLines.subList(stringStartIndex + 1 + expectedLines.size, callSiteLines.size)
        val between = callSiteLines.subList(stringStartIndex + 1, stringStartIndex + 1 + expectedLines.size)

        val actualLines = actual.split("\n")
        Files.writeString(callSiteFile, ExpectedLinesReplacement(before, between, after).replaceWith(actualLines))
    }

    private fun findTripleQuotedStringStart(lines: List<String>, startIndex: Int): Int? {
        var index = startIndex
        while (index < lines.size && !lines[index].contains("\"\"\"")) index++
        return if (index == lines.size) null else index
    }

    private data class ExpectedLinesReplacement(
        val before: List<String>,
        val between: List<String>,
        val after: List<String>,
    ) {
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
