package nyub.expekt.diff

import net.jqwik.api.*
import org.assertj.core.api.Assertions.assertThat

abstract class DiffTest<Element, Patch> {
    @Property
    fun `forall diff of two elements, applying the patch to the left element yields the second element`(
        @ForAll("arbitraryElements") left: Element,
        @ForAll("arbitraryElements") right: Element,
    ) {
        diff.patch(left, diff.diff(left, right)) `is equal to` right
    }

    abstract val diff: Diff<Element, Patch>

    @Provide abstract fun arbitraryElements(): Arbitrary<Element>

    private infix fun Element.`is equal to`(other: Element) = assertThat(this).isEqualTo(other)
}

fun <Element> SequenceDiff<Element>.printDiff(left: List<Element>, right: List<Element>, println: (String) -> Unit) {
    val diff = diff(left, right)
    var leftIndex = 0
    var rightIndex = 0
    diff.forEach {
        when (it) {
            is SequenceDiff.Delete -> {
                println("- ${left[leftIndex]}")
                leftIndex++
            }
            is SequenceDiff.Add<*> -> {
                println("+ ${right[rightIndex]}")
                rightIndex++
            }
            SequenceDiff.Keep -> {
                println(left[leftIndex].toString())
                leftIndex++
                rightIndex++
            }
        }
    }
}
