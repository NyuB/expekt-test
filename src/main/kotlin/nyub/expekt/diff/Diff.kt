package nyub.expekt.diff

interface Diff<Element, Patch> {
    fun diff(left: Element, right: Element): Patch

    fun patch(left: Element, patch: Patch): Element
}
