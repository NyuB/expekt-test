package nyub.expekt.demos

import nyub.expekt.ExpectTests
import org.junit.jupiter.api.Test

class HistogramTest {
    @Test
    fun `histogram demo`() =
        ExpectTests(promote = true).expectTest {
            val values = listOf(7, 3, 9, 4, 5, 7, 3, 8, 4, 2)
            printVerticalHistogram(values)
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

            printVerticalHistogram(values.reversed())
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

    private fun ExpectTests.ExpectTest.printVerticalHistogram(values: List<Int>) {
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
