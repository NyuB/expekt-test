package nyub.expekt.diff

/**
 * Test diff that compare left and right sequences pairwise, [SequenceDiff.Keep]ing if they are equal,
 * [SequenceDiff.Delete]ing and [SequenceDiff.Add]ing otherwise
 */
class Naive<Element> : SequenceDiff<Element> {
    override fun diff(left: List<Element>, right: List<Element>): Patch<Element> =
        right.flatMapIndexed { index, element ->
            if (index < left.size && element == left[index]) listOf(SequenceDiff.Keep)
            else if (index >= left.size) listOf(SequenceDiff.Add(element))
            else listOf(SequenceDiff.Delete, SequenceDiff.Add(element))
        }
}
