package nyub.expekt.diff

/**
 * From [the original paper](http://www.xmailserver.org/diff2.pdf) and
 * [James Coglan blog implementation](https://blog.jcoglan.com/2017/02/12/the-myers-diff-algorithm-part-1/)
 */
class Myers<Element>(private val equality: (Element, Element) -> Boolean) : SequenceDiff<Element> {
    override fun diff(left: List<Element>, right: List<Element>): Patch<Element> {
        if (left.isEmpty()) return right.map { SequenceDiff.Add(it) }
        if (right.isEmpty()) return left.map { SequenceDiff.Delete }
        val m = left.size
        val n = right.size
        val maxD = m + n
        val v = EndpointArray(IntArray(2 * maxD + 1), maxD)
        v[1] = 0
        val backtracking = mutableListOf<EndpointArray>()
        for (d in 0..maxD) {
            backtracking.add(v.clone())
            for (k in -d..d by 2) {
                var x =
                    if (k == -d || k != d && v[k - 1] < v[k + 1]) {
                        v[k + 1]
                    } else {
                        v[k - 1] + 1
                    }
                var y = x - k
                while (x < m && y < n && left[x] eq right[y]) {
                    x++
                    y++
                }
                v[k] = x
                if (x >= left.size && y >= right.size) {
                    return backtrack(right, backtracking, x, y)
                }
            }
        }
        throw IllegalStateException("unreachable")
    }

    private tailrec fun backtrack(
        right: List<Element>,
        backtracking: List<EndpointArray>,
        x: Int,
        y: Int,
        acc: MutableList<SequenceDiff.PatchItem<Element>> = mutableListOf(),
    ): List<SequenceDiff.PatchItem<Element>> {
        if (x == 0 && y == 0) return acc.asReversed()
        val k = x - y
        val d = backtracking.size - 1
        val v = backtracking.last()
        val prevK =
            if (k == -d || k != d && v[k - 1] < v[k + 1]) {
                k + 1
            } else {
                k - 1
            }
        val prevX = v[prevK]
        val prevY = prevX - prevK

        var x = x
        var y = y
        while (x > prevX && y > prevY) {
            acc.add(SequenceDiff.Keep)
            x--
            y--
        }
        if (x <= 0 && y <= 0) return acc.asReversed()
        if (x != prevX) {
            acc.add(SequenceDiff.Delete)
        } else {
            acc.add(SequenceDiff.Add(right[prevY]))
        }
        return backtrack(right, backtracking.subList(0, backtracking.size - 1), prevX, prevY, acc)
    }

    private class EndpointArray(private val endpoints: IntArray, val d: Int) {
        operator fun get(k: Int): Int = endpoints[k + d]

        operator fun set(k: Int, value: Int) {
            endpoints[k + d] = value
        }

        fun clone(): EndpointArray = EndpointArray(endpoints.clone(), d)
    }

    private infix fun IntRange.by(step: Int): IntProgression {
        return IntProgression.fromClosedRange(this.first, this.last, step)
    }

    private infix fun Element.eq(other: Element): Boolean = equality(this, other)
}
