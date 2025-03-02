package nyub.expekt

import org.assertj.core.api.AbstractThrowableAssert
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

internal class ExpectCallConstraintsTest {
    @Test
    fun `single quoted string`() = ExpectTests(promote = true).expectTest {
        assertThatThrownBy { expect("<CONTENT>") }
            .isExpectCallConstraintError()
            .hasMessageContaining("could not find opening quotes")
    }

    @Test
    fun `immediately closed string block`() = ExpectTests(promote = true).expectTest {
        assertThatThrownBy { expect("""<CONTENT>""") }
            .isExpectCallConstraintError()
            .hasMessageContaining("closing quotes must be on a different line than opening ones")
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
    fun `standalone string block after erroneous call to expect`() = ExpectTests(promote = true).expectTest {
        assertThatThrownBy {
            // The next two statements are on consecutive lines
            expect("<CONTENT>")
            """
                        Just a string block
                    """
                .let(::println)
        }
            .isExpectCallConstraintError()
            .hasMessageContaining("could not find opening quotes")
    }

    @Test
    fun `aliased expect`() = ExpectTests(promote = true).expectTest {
        assertThatThrownBy {
            fun alias(s: String) = expect(s)
            alias(
                """
                                <CONTENT>
                           """
            )
        }
            .isExpectCallConstraintError()
            .hasMessageContaining("could not find opening quotes")
    }

    @Test
    fun `expect( in string block`() = ExpectTests(promote = true).expectTest {
        assertThatThrownBy {
            print("<CONTENT>")
            expect("""expect(
                       """
            )
            // Before the fix, string block below was confused and interleaved with the end of the above block
            """
                    OOPS
                """
                .trimIndent()
        }
            .isExpectCallConstraintError()
            .hasMessageContaining("found two 'expect(' sequences on the same line")
    }

    @Test
    fun `interpolation in string block`() = ExpectTests(promote = true).expectTest {
        val content = "$<CONTENT>"
        print(content)
        assertThatThrownBy {
            expect(
                """
                        $content
                       """
            )
        }
            .isExpectCallConstraintError()
            .hasMessageContaining("string interpolation is not allowed within expected string block")
        assertThatThrownBy {
            expect(
                """
                        ${content}
                       """
            )
        }
            .isExpectCallConstraintError()
            .hasMessageContaining("string interpolation is not allowed within expected string block")
    }

    @Test
    fun `space between expect and opening parenthesis`() = ExpectTests().expectTest {
        //                ↓
        "<CONTENT>".expect ("""
            <CONTENT>
        """.trimIndent())
    }

    private fun AbstractThrowableAssert<*, out Throwable>.isExpectCallConstraintError():
        AbstractThrowableAssert<*, out Throwable> =
        this.isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Could not find expected triple-quoted string block")
            .hasMessageContaining("${this@ExpectCallConstraintsTest::class.simpleName}.kt")
}