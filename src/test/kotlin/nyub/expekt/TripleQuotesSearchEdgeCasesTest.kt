package nyub.expekt

import org.assertj.core.api.AbstractThrowableAssert
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

internal class TripleQuotesSearchEdgeCasesTest {
    @Test
    fun `when the expected string cannot be found, raise an error hinting toward missing triple-quotes`() =
        ExpectTests(promote = true).expectTest {
            assertThatThrownBy { expect("<CONTENT>") }.isTripleQuotedBlockError()
        }

    @Test
    fun `immediately closed string block`() =
        ExpectTests(promote = true).expectTest {
            assertThatThrownBy { expect("""<CONTENT>""") }.isTripleQuotedBlockError()
        }

    @Test
    fun `triple quotes not on the next line after expect call`() =
        ExpectTests(promote = true).expectTest {
            assertThatThrownBy {
                expect( // Keep string block on the next line
                    // String block should be here
                    """
                                    <CONTENT>
                                     """
                )
            }
                .isTripleQuotedBlockError()
        }

    @Test
    fun `standalone string block after erroneous call to expect`() =
        ExpectTests(promote = true).expectTest {
            assertThatThrownBy {
                // The next two statements are on consecutive lines
                expect("<CONTENT>")
                """
                        Just a string block
                    """
                    .let(::println)
            }
                .isTripleQuotedBlockError()
        }

    @Test
    fun `comment after expect call`() =
        ExpectTests(promote = true).expectTest {
            expect( // Comment
                """
                    
                """
            )
        }

    @Test
    fun `aliased expect`() =
        ExpectTests(promote = true).expectTest {
            assertThatThrownBy {
                fun alias(s: String) = expect(s)
                alias(
                    """
                                <CONTENT>
                           """
                )
            }
                .isTripleQuotedBlockError()
        }

    @Test
    fun `expect( in string block`() {
        ExpectTests(promote = true).expectTest {
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
                .isTripleQuotedBlockError()
        }
    }

    private fun AbstractThrowableAssert<*, out Throwable>.isTripleQuotedBlockError():
        AbstractThrowableAssert<*, *>? =
        this.isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Could not find expected string")
            .hasMessageContaining("${this@TripleQuotesSearchEdgeCasesTest::class.simpleName}.kt")
            .hasMessageContaining("triple-quoted block")
}