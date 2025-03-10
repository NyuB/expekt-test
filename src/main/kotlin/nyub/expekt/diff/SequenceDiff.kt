package nyub.expekt.diff

typealias Patch<Element> = List<SequenceDiff.PatchItem<Element>>

interface SequenceDiff<Element> : Diff<List<Element>, Patch<Element>> {
    sealed interface PatchItem<out T>

    data class Add<T>(val element: T) : PatchItem<T>

    data object Keep : PatchItem<Nothing>

    data object Delete : PatchItem<Nothing>

    override fun patch(left: List<Element>, patch: Patch<Element>): List<Element> = buildList {
        var index = 0
        patch.forEach {
            when (it) {
                is Keep -> {
                    add(left[index])
                    index++
                }
                is Delete -> {
                    index++
                }
                is Add -> add(it.element)
            }
        }
    }
}
