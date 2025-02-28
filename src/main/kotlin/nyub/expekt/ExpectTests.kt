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
                val actual = output.toString().trimEmptyLines()
                creator.expect(expected, actual)
            } finally {
                output.clear()
            }

        /**
         * Asserts that `this`.toString() matches the [expected] content, or update the [expected] content in place if
         * [creator] is in promote mode.
         */
        fun Any.expect(expected: String) {
            val actualString = toString().trimEmptyLines()
            creator.expect(expected, actualString)
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

        private fun String.trimEmptyLines() =
            this.split("\n")
                .map { it.trimEnd() }
                .dropWhile { it.isEmpty() }
                .dropLastWhile { it.isEmpty() }
                .joinToString(separator = "\n")
    }

    private fun expect(expected: String, actual: String) {
        if (promote) {
            promote(actual)
        } else {
            assertThat(actual).isEqualTo(expected.trim { it.isWhitespace() || it == '\n' })
        }
    }

    private fun promote(actual: String) =
        synchronized(ExpectTests::class.java) {
            val callSite = callSite()
            val callSiteFile =
                resolveClassesFrom.resolve(callSite.className.replace(".", "/")).parent.resolve(callSite.fileName!!)
            val callSiteLines = Files.readString(callSiteFile).split("\n")

            val lineNumber = offsets.getAdjustedLine(callSiteFile, callSite.lineNumber)
            val (stringStartIndex, stringEndIndex) =
                findTripleQuotedStringStart(callSiteLines, lineNumber - 1).getOrElse { e ->
                    throw RuntimeException(
                        "Could not find expected triple-quoted string block at $callSiteFile:$lineNumber: ${e.message}"
                    )
                }

            val before = callSiteLines.subList(0, stringStartIndex + 1)
            val between = callSiteLines.subList(stringStartIndex + 1, stringEndIndex)
            val after = callSiteLines.subList(stringEndIndex, callSiteLines.size)

            val actualLines = actual.split("\n")
            Files.writeString(callSiteFile, ExpectedLinesReplacement(before, between, after).replaceWith(actualLines))
            offsets.record(callSiteFile, callSite.lineNumber, actualLines.size - between.size)
        }

    /**
     * Searches for
     *
     * ```kotlin
     * expect("""
     * <CONTENT>
     * """)
     * ```
     *
     * @return the line index of the opening triple-quote and the line index of the closing triple-quote, searching from
     *   [expectLine] line index, or fails if one of the two markers could not be found or the string block is invalid
     */
    private fun findTripleQuotedStringStart(lines: List<String>, expectLine: Int): Result<Pair<Int, Int>> {
        val expectColumn =
            locateExpectCallColumn(lines, expectLine).getOrElse {
                return fail(it)
            }

        val scanner = ExpectContentScanner(lines, expectLine, expectColumn)
        scanner.identifier("expect(").getOrElse {
            return fail(it)
        }
        scanner.skipNonSignificantCharacters()
        val startIndex = scanner.position.line
        scanner.identifier(tripleQuotes).getOrElse {
            return fail("could not find opening quotes")
        }
        scanner.stringBlockContent().getOrElse {
            return fail(it)
        }
        val endIndex = scanner.position.line
        scanner.identifier(tripleQuotes).getOrElse {
            return fail("could not find closing quotes")
        }
        if (endIndex <= startIndex) return fail("closing quotes must be on a different line than opening ones")
        return Result.success(startIndex to endIndex)
    }

    private fun locateExpectCallColumn(lines: List<String>, expectCallSite: Int): Result<Int> {
        if (expectCallSite >= lines.size) return fail("provided call site line is greater than file's lines count")
        val callLine = lines[expectCallSite]

        val containsExpectAt = callLine.indexOf(expectCall)
        if (containsExpectAt == -1) return fail("could not find 'expect(' call")
        val containsAnotherExpect = callLine.indexOf(expectCall, startIndex = containsExpectAt + 1)
        if (containsAnotherExpect != -1) return fail("found two 'expect(' sequences on the same line")
        return Result.success(containsExpectAt)
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

private const val tripleQuotes = "\"\"\""
private const val expectCall = "expect("

private fun fail(message: String): Result<Nothing> = Result.failure(IllegalStateException(message))

private fun fail(throwable: Throwable): Result<Nothing> = Result.failure(throwable)

/**
 * Globally records line additions or removal to keep line counts up-to-date if there are multiple promotions to the
 * same file
 */
private val offsets = FileOffsets()

private class FileOffsets {
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

private class LineOffsets {
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

private class ExpectContentScanner(val lines: List<String>, startLine: Int, startColumn: Int) {
    class Position(val line: Int, val column: Int) {
        fun incrementLine(): Position = Position(this.line + 1, 0)

        fun incrementColumn(): Position = Position(this.line, this.column + 1)

        override fun toString(): String {
            return "${line + 1}:${column + 1}"
        }
    }

    var position: Position = Position(startLine, startColumn)
    val currentLine: String
        get() = lines[position.line]

    val currentCharacter: Char
        get() = currentLine[position.column]

    fun nextCharacter(ahead: Int): Char? =
        if (position.column + ahead >= currentLine.length) null else currentLine[position.column + ahead]

    fun identifier(id: String): Result<Unit> {
        if (position.line >= lines.size) return fail("Reached EOF")
        if (currentLine.length - position.column < id.length) return fail("Expected to find $id at $position")
        var index = 0
        while (index < id.length) {
            if (currentCharacter != id[index]) return fail("Expected to find $id at $position")
            position = position.incrementColumn()
            index++
        }
        return Result.success(Unit)
    }

    fun stringBlockContent(): Result<Unit> {
        while (position.line < lines.size) {
            while (position.column < currentLine.length) {
                if (currentCharacter == '"' && nextCharacter(1) == '"' && nextCharacter(2) == '"')
                    return Result.success(Unit)
                if (currentCharacter == '$' && nextCharacter(1).isInterpolatedCharacter())
                    return fail("string interpolation is not allowed within expected string block")
                position = position.incrementColumn()
            }
            position = position.incrementLine()
        }
        return fail("unterminated string content")
    }

    fun skipNonSignificantCharacters() {
        while (position.line < lines.size) {
            while (position.column < currentLine.length) {
                if (currentCharacter == '/' && nextCharacter(1) == '/') {
                    break
                }
                if (!currentCharacter.isWhitespace()) return
                position = position.incrementColumn()
            }
            position = position.incrementLine()
        }
    }

    private fun Char?.isInterpolatedCharacter(): Boolean {
        if (this == null) return false
        return this.isLetter() || this == '_' || this == '`' || this == '{'
    }
}

private class ExpectedLinesReplacement(
    private val before: List<String>,
    between: List<String>,
    private val after: List<String>,
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
