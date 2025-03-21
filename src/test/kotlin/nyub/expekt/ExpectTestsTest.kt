package nyub.expekt

import nyub.expekt.PromotionTrigger.BySystemProperty
import nyub.expekt.PromotionTrigger.Companion.NO
import nyub.expekt.PromotionTrigger.Companion.YES
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

internal class ExpectTestsTest {
    private fun expectTest(test: ExpectTests.ExpectTest.() -> Unit) =
        ExpectTests(promote = BySystemProperty("nyub.expekt.promote")).expectTest(test)

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
    fun `ignore leading and trailing newlines`() = expectTest {
        print("Start")
        expect(
            """
Start
"""
        )
    }

    @Test
    fun `keep leading white spaces`() = throwsAssertionError {
        ExpectTests(promote = NO).expectTest {
            print("<CONTENT>")
            expect(
                """
 <CONTENT>
"""
            )
        }
    }

    @Test
    fun `handle cases where there is more newline in the expected string than actual lines in the string blocks`() =
        ExpectTests(promote = YES).expectTest {
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

    /**
     * To reproduce the setup that triggered the failure, remove the content of the expected blocks Before the
     * synchronization fixes, one thread was reading the file while the other was writing to it
     */
    @Test
    fun `multi-threading test`() {
        val tas =
            List(25) {
                Thread {
                    ExpectTests(promote = YES).expectTest {
                        repeat(5) { println("A") }
                        expect(
                            """
                A
                A
                A
                A
                A
                """
                                .trimIndent()
                        )
                    }
                }
            }
        val tbs =
            List(25) {
                Thread {
                    ExpectTests(promote = YES).expectTest {
                        repeat(5) { println("B") }
                        expect(
                            """
                B
                B
                B
                B
                B
                """
                                .trimIndent()
                        )
                    }
                }
            }

        var threadFailed: Throwable? = null
        val failTestIfErrorInThread = Thread.UncaughtExceptionHandler { _, e -> threadFailed = e }
        tas.forEach { it.uncaughtExceptionHandler = failTestIfErrorInThread }
        tbs.forEach { it.uncaughtExceptionHandler = failTestIfErrorInThread }

        tas.zip(tbs).forEach { (a, b) ->
            a.start()
            b.start()
        }
        tas.zip(tbs).forEach { (a, b) ->
            a.join()
            b.join()
        }

        threadFailed?.let { fail(it) }
    }

    private fun <T> throwsAssertionError(test: () -> T) {
        assertThatThrownBy { test() }.isInstanceOf(AssertionError::class.java)
    }
}
