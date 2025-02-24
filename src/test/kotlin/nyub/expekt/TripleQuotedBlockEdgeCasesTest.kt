package nyub.expekt

import org.assertj.core.api.AbstractThrowableAssert
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class TripleQuotedBlockEdgeCasesTest {
    @Test
    fun `when the expected string cannot be found, raise an error hinting toward missing triple-quotes`() =
        ExpectTests(promote = true).expectTest {
            print("Not within triple quotes")
            assertThatThrownBy { expect("Not within triple quotes") }.isTripleQuotedBlockError()
        }

    @Test
    fun `two consecutive expect calls, the first one missing triple quotes`() =
        ExpectTests(promote = true).expectTest {
            assertThatThrownBy {
                    // The next two statements are on consecutive lines
                    expect("Not within triple quotes")
                    expect("""""")
                }
                .isTripleQuotedBlockError()
        }

    @Test
    fun `immediately closed string block`() =
        ExpectTests(promote = true).expectTest { assertThatThrownBy { expect("""""") }.isTripleQuotedBlockError() }

    @Test
    fun `standalone string block after erroneous call to expect`() =
        ExpectTests(promote = true).expectTest {
            assertThatThrownBy {
                    // The next two statements are on consecutive lines
                    expect("Not within triple quotes")
                    """
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

    private fun AbstractThrowableAssert<*, out Throwable>.isTripleQuotedBlockError(): AbstractThrowableAssert<*, *>? =
        this.isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Could not find expected string")
            .hasMessageContaining("${TripleQuotedBlockEdgeCasesTest::class.simpleName}.kt")
            .hasMessageContaining("triple-quoted block")
}
