package nyub.expekt.diff

/**
 * Test [Myers algorithm](http://www.xmailserver.org/diff2.pdf) implementation that explores the edit graph
 * breadth-first. Should yield the same results as an actual Myers implementation, with degraded performances
 *
 * @see Myers
 */
class BFSMyers<Element>(private val equality: (Element, Element) -> Boolean) : SequenceDiff<Element> {
    override fun diff(left: List<Element>, right: List<Element>): Patch<Element> {
        val q = mutableListOf(SearchItem(0, 0, emptyList()))
        val best = Array(left.size + 1) { IntArray(right.size + 1) { Int.MAX_VALUE } }
        while (q.isNotEmpty()) {
            val item = q.removeFirst()
            if (best[item.leftIndex][item.rightIndex] <= item.path.size) continue
            best[item.leftIndex][item.rightIndex] = item.path.size
            if (item.leftIndex == left.size && item.rightIndex == right.size) return item.path
            if (item.leftIndex < left.size) {
                q.add(SearchItem(item.leftIndex + 1, item.rightIndex, item.path + SequenceDiff.Delete))
            }
            if (item.rightIndex < right.size) {
                q.add(
                    SearchItem(
                        item.leftIndex,
                        item.rightIndex + 1,
                        item.path + SequenceDiff.Add(right[item.rightIndex]),
                    )
                )
            }
            if (
                item.rightIndex < right.size &&
                    item.leftIndex < left.size &&
                    left[item.leftIndex] == right[item.rightIndex]
            ) {
                q.add(SearchItem(item.leftIndex + 1, item.rightIndex + 1, item.path + SequenceDiff.Keep))
            }
        }
        throw IllegalStateException("Unreachable")
    }

    private inner class SearchItem(
        val leftIndex: Int,
        val rightIndex: Int,
        val path: List<SequenceDiff.PatchItem<Element>>,
    )

    private infix fun Element.eq(other: Element) = equality(this, other)
}
