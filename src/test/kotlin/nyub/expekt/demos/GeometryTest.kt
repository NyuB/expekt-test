package nyub.expekt.demos

import org.junit.jupiter.api.Test

class GeometryTest {
    @Test
    fun `drawing boxes`() = expectTest {
        val filled = Rectangle(1, 1, 10, 3)
        val stroke = Rectangle(6, 3, 10, 3)

        DrawingFrame(10, 20)
            .draw(filled.fill('F'))
            .draw(stroke.strike('D'))
            .expect(
                """
            ┌────────────────────┐
            │                    │
            │                    │
            │                    │
            │      DDDDDDDDDDD   │
            │      D         D   │
            │ FFFFFDFFFFF    D   │
            │ FFFFFDDDDDDDDDDD   │
            │ FFFFFFFFFFF        │
            │ FFFFFFFFFFF        │
            │                    │
            └────────────────────┘
            """
                    .trimIndent()
            )
    }

    /**
     * @property x bottom-left corner x coordinate
     * @property y bottom-left corner y coordinate
     */
    private data class Rectangle(val x: Double, val y: Double, val width: Double, val height: Double) {
        constructor(
            x: Number,
            y: Number,
            width: Number,
            height: Number,
        ) : this(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

        val xSpan = x..(x + width)
        val ySpan = y..(y + height)
    }

    private fun Rectangle.strike(c: Char) = Drawable { x, y ->
        if (x == xSpan.start || x == xSpan.endInclusive) {
            if (y in ySpan) c else null
        } else if (y == ySpan.start || y == ySpan.endInclusive) {
            if (x in xSpan) c else null
        } else null
    }

    private fun Rectangle.fill(c: Char) = Drawable { x, y -> if (x in xSpan && y in ySpan) c else null }

    private fun interface Drawable {
        fun draw(x: Double, y: Double): Char?
    }

    private data class DrawingFrame(
        val rows: Int,
        val cols: Int,
        val originX: Double = 0.0,
        val originY: Double = 0.0,
        val xStep: Double = 1.0,
        val yStep: Double = 1.0,
        val drawables: List<Drawable> = emptyList(),
    ) {
        fun draw(drawable: Drawable): DrawingFrame {
            return copy(drawables = drawables + drawable)
        }

        private fun cellAsCoordinates(i: Int, j: Int): Pair<Double, Double> {
            return (originX + j * xStep) to (originY + (rows - 1 - i) * yStep)
        }

        override fun toString(): String {
            return StringBuilder()
                .apply {
                    append("┌")
                    repeat(cols) { append('─') }
                    append("┐")
                    append('\n')
                    repeat(rows) { i ->
                        append('│')
                        repeat(cols) { j ->
                            val (x, y) = cellAsCoordinates(i, j)
                            var drawn = false
                            drawables.asReversed().forEach {
                                if (drawn) return@forEach
                                it.draw(x, y)?.let {
                                    drawn = true
                                    append(it)
                                }
                            }
                            if (!drawn) append(' ')
                        }
                        append('│')
                        append('\n')
                    }

                    append("└")
                    repeat(cols) { append('─') }
                    append("┘")
                    append('\n')
                }
                .toString()
        }
    }
}
