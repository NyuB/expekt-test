package nyub.expekt.diff

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Group
import nyub.expekt.ExpectTests
import nyub.expekt.demos.expectTest
import org.junit.jupiter.api.Test

class MyersTest {
    val charDiff = Myers(Char::equals)
    val linesDiff = Myers(String::equals)

    @Test
    fun `paper example`() = expectTest {
        val left = listOf('A', 'B', 'C', 'A', 'B', 'B', 'A')
        val right = listOf('C', 'B', 'A', 'B', 'A', 'C')
        charDiff.printDiff(left, right, ::println)
        expect(
            promote@ """
        - A
        - B
        C
        + B
        A
        B
        - B
        A
        + C
        """
                .trimIndent()
        )
    }

    @Test
    fun `first two lines differ, last line is the same`() =
        ExpectTests().expectTest {
            linesDiff.printDiff(
                listOf("Demain dès l'aube,", "A l'heure où blanchit la campagne,", "Je partirai."),
                listOf("Demain matin,", "Vers 10h30,", "Je partirai."),
                ::println,
            )
            expect(
                """
            - Demain dès l'aube,
            - A l'heure où blanchit la campagne,
            + Demain matin,
            + Vers 10h30,
            Je partirai.
            """
                    .trimIndent()
            )
        }

    @Group
    inner class CharTest : DiffTest<List<Char>, List<SequenceDiff.PatchItem<Char>>>() {
        override val diff: Diff<List<Char>, List<SequenceDiff.PatchItem<Char>>>
            get() = charDiff

        override fun arbitraryElements(): Arbitrary<List<Char>> {
            return Arbitraries.chars().alpha().map { it.lowercaseChar() }.list().ofMaxSize(20)
        }
    }

    @Group
    inner class StringTest : DiffTest<List<String>, List<SequenceDiff.PatchItem<String>>>() {
        override val diff: Diff<List<String>, List<SequenceDiff.PatchItem<String>>>
            get() = linesDiff

        override fun arbitraryElements(): Arbitrary<List<String>> {
            return Arbitraries.strings().ascii().map { it.lowercase() }.list()
        }
    }
}
