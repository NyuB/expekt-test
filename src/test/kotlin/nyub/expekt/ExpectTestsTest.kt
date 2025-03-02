package nyub.expekt

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

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

    /**
     * To reproduce the setup that triggered the failure, remove the content of the expected blocks Before the
     * synchronization fixes, one thread was reading the file while the other was writing to it
     */
    @Test
    fun `multi-threading test`() {
        val tas =
            List(25) {
                Thread {
                    ExpectTests(promote = true).expectTest {
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
                    ExpectTests(promote = true).expectTest {
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
}
