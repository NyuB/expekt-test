package nyub.expekt

import org.assertj.core.api.AbstractThrowableAssert
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

internal class ExpectCallConstraintsTest {
    @Test
    fun `single quoted string`() = constraintNotRespected("could not find opening quotes") {
        expect("<CONTENT>")
    }

    @Test
    fun `immediately closed string block`() =
        constraintNotRespected("closing quotes must be on a different line than opening ones") {
            expect("""<CONTENT>""")
        }

    @Test
    fun `content before closing triple-quotes`() =
        constraintNotRespected("closing quotes must be on a different line than expected content") {
            expect("""
                <CONTENT>""".trimIndent())
        }

    @Test
    fun `standalone string block after erroneous call to expect`() =
        constraintNotRespected("could not find opening quotes") {
            // The next two statements are on consecutive lines
            expect("<CONTENT>")
            """
            Just a string block
            """.trimIndent()
        }

    @Test
    fun `aliased expect`() = constraintNotRespected("could not find opening quotes") {
        fun alias(s: String) = expect(s)
        alias(
        """
           <CONTENT>
           """
        )
    }

    @Test
    fun `expect( in string block`() =
        constraintNotRespected("found two 'expect(' sequences on the same line") {
            expect("""expect(
            """)
            // Before the fix, string block below was confused and interleaved with the end of the above block
            """
            OOPS
            """.trimIndent()
    }

    @Test
    fun `interpolation in string block (no braces)`() =
        constraintNotRespected("string interpolation is not allowed within expected string block") {
            val content = "$<CONTENT>"
            expect(
                """
                $content
               """
            )
        }

    @Test
    fun `interpolation in string block (braces)`() =
        constraintNotRespected("string interpolation is not allowed within expected string block") {
            val content = "$<CONTENT>"
            expect(
                """
                    ${content}
                   """
            )
        }

    @Test
    fun `label other than promote@`() =
        constraintNotRespected("could not find opening quotes") {
            "<CONTENT>".expect(
                notPromote@ """
                <CONTENT>
            """.trimIndent()
            )
        }

    @Test
    fun `opening triple quotes not on the next line after expect call`() = ExpectTests(promote = true).expectTest {
        print("<CONTENT>")
        expect( // Keep string block on the next line
            // Another line
            """
            <CONTENT>
             """
        )
    }

    @Test
    fun `space between expect and opening parenthesis`() = ExpectTests().expectTest {
        //                ↓
        "<CONTENT>".expect ("""
            <CONTENT>
        """.trimIndent())
    }

    private fun constraintNotRespected(constraintErrorMessage: String, test: ExpectTests.ExpectTest.() -> Unit) =
        ExpectTests(promote = true).expectTest {
            assertThatThrownBy {
                test()
            }.isExpectCallConstraintError().hasMessageContaining(constraintErrorMessage)
        }

    private fun AbstractThrowableAssert<*, out Throwable>.isExpectCallConstraintError():
        AbstractThrowableAssert<*, out Throwable> =
        this.isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Could not find expected triple-quoted string block")
            .hasMessageContaining("${this@ExpectCallConstraintsTest::class.simpleName}.kt")
}