package nyub.expekt.demos

import nyub.expekt.ExpectTests

val expectTests = ExpectTests(promote = System.getProperty("nyub.expekt.promote", "false") == "true")

fun expectTest(test: ExpectTests.ExpectTest.() -> Unit) = expectTests.expectTest(test)
