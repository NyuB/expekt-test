package nyub.expekt

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class TripleQuotedBlockEdgeCasesTest {
    @Test
    fun `when the expected string cannot be found, raise an error hinting toward missing triple-quotes`() =
        ExpectTests(promote = true).expectTest {
            print("Not within triple quotes")
            assertThatThrownBy { expect("Not within triple quotes") }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("Could not find expected string")
                .hasMessageContaining("${TripleQuotedBlockEdgeCasesTest::class.simpleName}.kt")
                .hasMessageContaining("triple-quoted block")

            print("Do not confuse this block with the missing one above")
            expect(
                """
                Do not confuse this block with the missing one above
            """
                    .trimIndent()
            )
        }

    @Test
    fun `two consecutive expect calls, the first one missing triple quotes`() =
        ExpectTests(promote = true).expectTest {
            assertThatThrownBy {
                    // The next two statements are on consecutive lines
                    expect("Not within triple quotes")
                    expect("""""")
                }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("Could not find expected string")
                .hasMessageContaining("${TripleQuotedBlockEdgeCasesTest::class.simpleName}.kt")
                .hasMessageContaining("triple-quoted block")
        }

    @Test
    fun `immediately closed string block`() =
        ExpectTests(promote = true).expectTest {
            assertThatThrownBy { expect("""""") }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("Could not find expected string")
                .hasMessageContaining("${TripleQuotedBlockEdgeCasesTest::class.simpleName}.kt")
                .hasMessageContaining("triple-quoted block")
        }
}
