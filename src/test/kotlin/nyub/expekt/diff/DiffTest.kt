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
