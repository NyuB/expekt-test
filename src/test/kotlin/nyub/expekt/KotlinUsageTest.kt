package nyub.expekt

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

internal class KotlinUsageTest {
    /** Shared configuration for expect tests */
    private val expectTests = ExpectTests(promote = System.getProperty("nyub.expekt.promote", "false") == "true")

    /** Alias to avoid typing expectTests.expectTest for the generic case */
    private fun expectTest(test: ExpectTests.ExpectTest.() -> Unit) = expectTests.expectTest(test)

    @Test
    fun `happy path`() = expectTest {
        println("Just print as usual")
        println("   along your test")
        print("and then ... ")
        print("expect")

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
    fun `direct expect call on any object`() = expectTest {
        class Person(val name: String, val surname: String, val nickname: String) {
            override fun toString() = "$name '$nickname' $surname"
        }

        val billy = Person("Billy", "McCarty", "The Kid")

        billy.expect(
            """
            Billy 'The Kid' McCarty
            """
                .trimIndent()
        )
    }

    @Test
    fun `multiple expect call in a single test`() = expectTest {
        print("One")
        expect(
            """
        One
        """
                .trimIndent()
        )

        print("Two")
        expect(
            """
        Two
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
        assertThatThrownBy { expectTest { println("Not consumed") } }.isInstanceOf(AssertionError::class.java)
    }

    @Test
    fun `override promote with label`() = expectTest {
        "<CONTENT>"
            .expect(
                promote@ """
            <CONTENT>
            """
                    .trimIndent()
            )
    }
}
