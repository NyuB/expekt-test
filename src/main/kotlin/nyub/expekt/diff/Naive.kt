package nyub.expekt.diff

class Naive<Element> : SequenceDiff<Element> {
    override fun diff(left: List<Element>, right: List<Element>): Patch<Element> {
        return right.flatMapIndexed { index, element ->
            if (index < left.size && element == left[index]) listOf(SequenceDiff.Keep)
            else listOf(SequenceDiff.Delete, SequenceDiff.Add(element))
        }
    }
}
