package nyub.expekt.demos

import nyub.expekt.ExpectTests
import org.junit.jupiter.api.Test

class HistogramTest {
    @Test
    fun `histogram demo`() = expectTest {
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

    @Test
    fun `horizontal histogram`() = expectTest {
        val values = listOf("Cats" to 7, "Chihuahua" to 4, "Aligator" to 13, "Donkey" to 5)
        printHorizontalHistogram(values)
        expect(
            """
         Cats     |■■■■■■■ 7
         Chihuahua|■■■■ 4
         Aligator |■■■■■■■■■■■■■ 13
         Donkey   |■■■■■ 5
        """
                .trimIndent()
        )

        data class VoteBallot(val name: String, val amount: Int)
        fun String.amount(n: Int) = VoteBallot(this, n)
        val ballots = listOf("BOB".amount(2), "bob".amount(6), "Alice".amount(4), "AlIcE".amount(5), "Stuart".amount(3))

        println("Results:")
        printHorizontalHistogram(ballots.toHistogram(VoteBallot::name, VoteBallot::amount))
        newLine()
        println("Corrected Results:")
        printHorizontalHistogram(ballots.toHistogram({ it.name.lowercase() }, VoteBallot::amount))
        expect(
            """
        Results:
        BOB   |■■ 2
        bob   |■■■■■■ 6
        Alice |■■■■ 4
        AlIcE |■■■■■ 5
        Stuart|■■■ 3
        
        Corrected Results:
        bob   |■■■■■■■■ 8
        alice |■■■■■■■■■ 9
        stuart|■■■ 3
        """
                .trimIndent()
        )
    }

    private fun <T : Any> Iterable<T>.toHistogram(
        labeling: (T) -> String = Any::toString,
        counting: (T) -> Int = { 1 },
    ): List<Pair<String, Int>> {
        val map = mutableMapOf<String, Int>()
        this.forEach { map.compute(labeling(it)) { _, count -> (counting(it)) + (count ?: 0) } }
        return map.toList()
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

    private fun ExpectTests.ExpectTest.printHorizontalHistogram(values: List<Pair<String, Int>>) {
        val maxLabelLength = values.maxOf { it.first.length }
        values.forEach { (label, value) ->
            printf("%-${maxLabelLength}s|", label)
            repeat(value) { print("■") }
            print(" $value")
            newLine()
        }
    }
}
