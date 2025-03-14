package nyub.expekt.demos

import kotlin.math.max
import org.junit.jupiter.api.Test

typealias CSV = List<Pair<*, List<*>>>

class CSVTest {

    @Test
    fun `csv demo`() = expectTest {
        val data =
            mapOf(
                "ID" to listOf("Task A", "Execution B", "Workaround C"),
                "Time (s)" to listOf(5, 14, 7),
                "CPU (%)" to listOf(12.34, 5.6, 7),
            )
        printCsv(data.toCSV(), ::print)
        expect(
            """
        ┌────────────┬────────┬───────┐
        │ID          │Time (s)│CPU (%)│
        ├────────────│────────│───────┤
        │Task A      │       5│  12.34│
        ├────────────│────────│───────┤
        │Execution B │      14│    5.6│
        ├────────────│────────│───────┤
        │Workaround C│       7│      7│
        └────────────┴────────┴───────┘
        """
                .trimIndent()
        )
        printCsv(data.toCSV().swap(0), ::print)
        expect(
            """
        ┌────────┬──────┬───────────┬────────────┐
        │ID      │Task A│Execution B│Workaround C│
        ├────────│──────│───────────│────────────┤
        │Time (s)│     5│         14│           7│
        ├────────│──────│───────────│────────────┤
        │CPU (%) │ 12.34│        5.6│           7│
        └────────┴──────┴───────────┴────────────┘
        """
                .trimIndent()
        )
        printCsv(data.toCSV().swap(1), ::print)
        expect(
            """
        ┌────────┬──────┬───────────┬────────────┐
        │Time (s)│     5│         14│           7│
        ├────────│──────│───────────│────────────┤
        │ID      │Task A│Execution B│Workaround C│
        ├────────│──────│───────────│────────────┤
        │CPU (%) │ 12.34│        5.6│           7│
        └────────┴──────┴───────────┴────────────┘
        """
                .trimIndent()
        )
    }

    fun Map<String, List<*>>.toCSV(): CSV = toList()

    private fun CSV.swap(pivotColumnIndex: Int): CSV {
        val pivotColumn = this[pivotColumnIndex]
        val pivotHeader = pivotColumn.first
        val firstColumn = pivotHeader to this.withoutIndex(pivotColumnIndex).map { it.first }
        val otherColumns =
            pivotColumn.second.mapIndexed { i, it ->
                it to List(size - 1) { j -> this[j.withoutIndex(pivotColumnIndex)].second[i] }
            }
        return listOf(firstColumn) + otherColumns
    }

    private fun CSV.withoutIndex(i: Int): CSV {
        return this.subList(0, i) + this.subList(i + 1, this.size)
    }

    private fun Int.withoutIndex(i: Int): Int = if (i <= this) this + 1 else this

    fun printCsv(columns: CSV, print: (String) -> Unit) {
        if (columns.isEmpty()) return
        val rows = columns[0].second.size
        val widthPerColumn =
            columns.map {
                val maxValue = it.second.maxOf { it.toString().length }
                max(maxValue, it.first.toString().length)
            }

        fun newline() = print("\n")
        fun verticalDash() = print("│")
        fun startConnector() = print("├")
        fun endConnector() = print("┤")
        fun topConnector() = print("┬")
        fun bottomConnector() = print("┴")
        fun topLeftCorner() = print("┌")
        fun topRightCorner() = print("┐")
        fun bottomLeftCorner() = print("└")
        fun bottomRightCorner() = print("┘")
        fun separationLine(start: () -> Unit, end: () -> Unit, mid: () -> Unit) {
            repeat(columns.size) { j ->
                if (j == 0) start() else mid()
                print("─".repeat(widthPerColumn[j]))
            }
            end()
            newline()
        }
        fun headerLine() = separationLine(::topLeftCorner, ::topRightCorner, ::topConnector)
        fun footerLine() = separationLine(::bottomLeftCorner, ::bottomRightCorner, ::bottomConnector)
        fun midLine() = separationLine(::startConnector, ::endConnector, ::verticalDash)
        fun printCell(j: Int, c: Any?) {
            if (c is Number) // Pad numbers right
             print(String.format("%${widthPerColumn[j]}s", c))
            else print(String.format("%-${widthPerColumn[j]}s", c))
        }

        headerLine()
        columns.forEachIndexed { j, e ->
            verticalDash()
            printCell(j, e.first)
        }
        verticalDash()
        newline()
        repeat(rows) { i ->
            midLine()
            columns.forEachIndexed { j, e ->
                verticalDash()
                printCell(j, e.second[i])
            }
            verticalDash()
            newline()
        }
        footerLine()
    }
}
