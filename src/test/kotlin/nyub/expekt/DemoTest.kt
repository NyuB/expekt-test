package nyub.expekt

import org.junit.jupiter.api.Test

class DemoTest {
    private val e = ExpectTests(promote = System.getProperty("nyub.expekt.promote", "false") == "true")

    @Test
    fun `histogram demo`() =
        e.expectTest {
            val values = listOf(1, 2, 5, 4, 9, 7, 0)
            printHistogram(values)
            expect(
                """
                9 |         #
                8 |         #
                7 |         # #
                6 |         # #
                5 |     #   # #
                4 |     # # # #
                3 |     # # # #
                2 |   # # # # #
                1 | # # # # # #
            """
                    .trimIndent()
            )

            printHistogram(values.reversed())
            expect(
                """
                9 |     #
                8 |     #
                7 |   # #
                6 |   # #
                5 |   # #   #
                4 |   # # # #
                3 |   # # # #
                2 |   # # # # #
                1 |   # # # # # #
            """
                    .trimIndent()
            )
        }

    private fun ExpectTests.ExpectTest.printHistogram(values: List<Int>) {
        val maxi = values.max()
        val mutableValues = values.toMutableList()
        for (i in (1..maxi).reversed()) {
            print("$i | ")
            for (j in 0..<mutableValues.size) {
                if (mutableValues[j] >= i) print("#") else print(" ")
                if (j != mutableValues.size - 1) print(" ")
            }
            if (i != 1) println("")
        }
    }
}
