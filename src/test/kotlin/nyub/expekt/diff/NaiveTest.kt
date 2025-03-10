package nyub.expekt.diff

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Group

class NaiveTest {
    @Group
    inner class Lines : DiffTest<List<String>, List<SequenceDiff.PatchItem<String>>>() {
        override val diff: Diff<List<String>, List<SequenceDiff.PatchItem<String>>> = Naive()

        override fun arbitraryElements(): Arbitrary<List<String>> = Arbitraries.strings().list()
    }

    @Group
    inner class Chars : DiffTest<List<Char>, List<SequenceDiff.PatchItem<Char>>>() {
        override val diff: Diff<List<Char>, List<SequenceDiff.PatchItem<Char>>> = Naive()

        override fun arbitraryElements(): Arbitrary<List<Char>> = Arbitraries.strings().map { it.toList() }
    }
}
