package nyub.expekt.promote

import nyub.nyub.expekt.promote.ExpectTests
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

internal class ExpectTestsTest {
    private val e = ExpectTests(promote = System.getProperty("nyub.expekt.promote", "false") == "true")

    @Test
    fun `happy path`() =
        e.expectTest {
            println("Just print as usual")
            println("   along your test")
            print("and then ... ")
            println("expect")

            expect(
                """
            Just print as usual
               along your test
            and then ... expect
            
        """
                    .trimIndent()
            )
        }

    @Test
    fun `raises when output is not equal to expected string`() =
        ExpectTests(promote = false).expectTest {
            println("Demain dès l'aube")
            println("Je mangerai un croissant")
            assertThatThrownBy {
                    expect(
                        """
                Demain, dès l'aube
                A l'heure où blanchit la campagne
            """
                            .trimIndent()
                    )
                }
                .isInstanceOf(AssertionError::class.java)
        }

    @Test
    fun `raises if not all output is consumed by assertions`() {
        assertThatThrownBy { e.expectTest { println("Not consumed") } }.isInstanceOf(AssertionError::class.java)
    }
}
