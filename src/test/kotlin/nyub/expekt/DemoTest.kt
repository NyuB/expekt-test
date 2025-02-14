package nyub.expekt

import org.junit.jupiter.api.Test

class DemoTest {
    @Test
    fun `histogram demo`() =
        ExpectTests(promote = true).expectTest {
            val values = listOf(7, 3, 9, 4, 5, 7, 3, 8, 4, 2)
            printHistogram(values)
            expect(
                """
                9 |     □
                8 |     □         □
                7 | □   □     □   □
                6 | □   □     □   □
                5 | □   □   □ □   □
                4 | □   □ □ □ □   □ □
                3 | □ □ □ □ □ □ □ □ □
                2 | □ □ □ □ □ □ □ □ □ □
                1 | □ □ □ □ □ □ □ □ □ □
            """
                    .trimIndent()
            )

            printHistogram(values.reversed())
            expect(
                """
                9 |               □
                8 |     □         □
                7 |     □   □     □   □
                6 |     □   □     □   □
                5 |     □   □ □   □   □
                4 |   □ □   □ □ □ □   □
                3 |   □ □ □ □ □ □ □ □ □
                2 | □ □ □ □ □ □ □ □ □ □
                1 | □ □ □ □ □ □ □ □ □ □
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
                if (mutableValues[j] >= i) print("□") else print(" ")
                if (j != mutableValues.size - 1) print(" ")
            }
            if (i != 1) println("")
        }
    }
}
