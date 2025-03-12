package nyub.expekt.demos

import nyub.expekt.ExpectTests
import nyub.expekt.PromotionTrigger

val expectTests = ExpectTests(promote = PromotionTrigger.BySystemProperty("nyub.expekt.promote"))

fun expectTest(test: ExpectTests.ExpectTest.() -> Unit) = expectTests.expectTest(test)
