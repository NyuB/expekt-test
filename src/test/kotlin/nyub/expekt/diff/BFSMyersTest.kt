package nyub.expekt.diff

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Group
import nyub.expekt.ExpectTests
import org.junit.jupiter.api.Test

class BFSMyersTest {

    val linesDiff = BFSMyers(String::equals)

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
    inner class Lines : DiffTest<List<String>, List<SequenceDiff.PatchItem<String>>>() {
        override val diff: Diff<List<String>, List<SequenceDiff.PatchItem<String>>>
            get() = BFSMyers(String::equals)

        override fun arbitraryElements(): Arbitrary<List<String>> = Arbitraries.strings().ascii().list().ofMaxSize(100)
    }
}
