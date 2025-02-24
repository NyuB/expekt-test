package nyub.expekt

import org.assertj.core.api.AbstractThrowableAssert
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class ExpectTestsTest {
    private fun expectTest(test: ExpectTests.ExpectTest.() -> Unit) =
        ExpectTests(promote = System.getProperty("nyub.expekt.promote", "false") == "true").expectTest(test)

    @Test
    fun `do not remove non leading or trailing blank lines`() = expectTest {
        println("Start")
        newLine()
        newLine()
        print("End")
        expect(
            """
            Start
            
            
            End
        """
                .trimIndent()
        )
    }

    @Test
    fun `handle cases where there is more newline in the expected string than actual lines in the string blocks`() =
        ExpectTests(promote = true).expectTest {
            print("Start")
            newLine()
            print("End")
            expect(
                """
            Start
            End
        """
                    .trimIndent()
            )
        }

    @Nested
    inner class TripleQuotesSearchEdgeCasesTest {
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

        private fun AbstractThrowableAssert<*, out Throwable>.isTripleQuotedBlockError():
            AbstractThrowableAssert<*, *>? =
            this.isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("Could not find expected string")
                .hasMessageContaining("${ExpectTestsTest::class.simpleName}.kt")
                .hasMessageContaining("triple-quoted block")
    }
}
