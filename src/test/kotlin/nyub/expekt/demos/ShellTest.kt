package nyub.expekt.demos

import org.junit.jupiter.api.Test

class ShellTest {
    @Test
    fun `shell demo`() = expectTest {
        val shell = Shell(::camelCase, ::print)
        shell("AlreadyCamelCase")
        shell("snake_case")
        shell("pascal-case")
        shell("just capitalize this")
        expect(
            """
        $ AlreadyCamelCase
        AlreadyCamelCase
        $ snake_case
        SnakeCase
        $ pascal-case
        PascalCase
        $ just capitalize this
        Just Capitalize This
        """
                .trimIndent()
        )
    }

    class Shell(private val handler: (String) -> String, private val printer: (String) -> Unit) {
        operator fun invoke(input: String) {
            printer("$ $input\n")
            val output = handler(input)
            printer("$output\n")
        }
    }

    private fun camelCase(s: String): String {
        val res = StringBuilder()
        var bump = true
        s.forEach {
            when {
                it.isWhitespace() -> {
                    bump = true
                    res.append(it)
                }
                it == '-' || it == '_' -> {
                    bump = true
                }
                it.isLetter() && bump -> {
                    res.append(it.uppercase())
                    bump = false
                }
                it.isLetter() -> res.append(it)
            }
        }
        return res.toString()
    }
}
