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

    class ExpectTest internal constructor(private val creator: ExpectTests) {
        private val actual = StringBuilder()

        fun print(s: String) {
            actual.append(s)
        }

        fun println(s: String) {
            actual.append(s).append("\n")
        }

        fun expect(expected: String) =
            try {
                creator.expect(expected, actual.toString().trimEnd { it.isWhitespace() || it == '\n' })
            } finally {
                actual.clear()
            }

        fun end() {
            if (actual.isNotEmpty()) throw AssertionError("Unhandled output remaining after expect test: '$actual'")
        }
    }

    private fun expect(expected: String, actual: String) {
        if (promote) {
            promote(expected, actual)
        } else {
            assertThat(actual).isEqualTo(expected)
        }
    }

    private fun promote(expected: String, actual: String) {
        val callSite = callSite()
        val callSiteFile =
            resolveClassesFrom.resolve(callSite.className.replace(".", "/")).parent.resolve(callSite.fileName!!)
        val callSiteLines = Files.readString(callSiteFile).split("\n")
        val expectedLines = if (expected.isEmpty()) emptyList() else expected.split("\n").dropLastWhile { it.isEmpty() }
        var stringStartIndex = callSite.lineNumber - 1
        while (!callSiteLines[stringStartIndex].contains("\"\"\"")) stringStartIndex++

        val before = callSiteLines.subList(0, stringStartIndex + 1)
        val after = callSiteLines.subList(stringStartIndex + 1 + expectedLines.size, callSiteLines.size)
        val between = callSiteLines.subList(stringStartIndex + 1, stringStartIndex + 1 + expectedLines.size)

        val actualLines = actual.split("\n")
        Files.writeString(callSiteFile, ExpectedLinesReplacement(before, between, after).replaceWith(actualLines))
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
