package nyub.expekt.demos

import nyub.expekt.ExpectTests
import nyub.expekt.demos.RegexesTest.Match.Companion.findAll
import org.junit.jupiter.api.Test

class RegexesTest {
    private fun expectTest(block: (ExpectTests.ExpectTest).() -> Unit) = ExpectTests().expectTest(block)

    @Test
    fun `multiple matches`() = expectTest {
        val snakeIdRegex = Regex("([a-zA-Z][_a-zA-Z0-9]+)_([_a-zA-Z0-9]+)")
        val multipleMatches = "This snake_id is in snake_case"
        val matches = multipleMatches.findAll(snakeIdRegex)
        matches.representation().forEach(::println)
        expect(
            """
            This  snake _ id  is in  snake _ case
                 └─────┘ └──┘       └─────┘ └────┘
                 └──────────┘       └────────────┘
        """
                .trimIndent()
        )
    }

    @Test
    fun `email matching`() = expectTest {
        val emailRegex = Regex("(([a-zA-Z0-9_-]+)(?:[.]([a-zA-Z0-9_-]+))*)@([a-zA-Z0-9_-]+)[.]([a-zA-Z0-9_-]+)")
        listOf(
                "salto.arriere@cirquedusoleil.fr",
                "sasukedu42@konoha.jp",
                "definitely not an email",
                "justin.bieber",
                "@.",
            )
            .forEach {
                it.findAll(emailRegex).representation(ifNoMatch = "(no match)").forEach(::println)
                newLine()
            }
        ".firstDotNotIncluded@a.b".findAll(emailRegex).representation(onlyMainGroup = true).forEach(::println)

        expect(
            """
             salto . arriere @ cirquedusoleil . fr
            └─────┘ └───────┘
            └───────────────┘ └──────────────┘ └──┘
            └─────────────────────────────────────┘
            
             sasukedu42 @ konoha . jp
            └──────────┘
            └──────────┘ └──────┘ └──┘
            └────────────────────────┘
            
            definitely not an email
            (no match)
            
            justin.bieber
            (no match)
            
            @.
            (no match)
            
            . firstDotNotIncluded@a.b
             └───────────────────────┘
            """
                .trimIndent()
        )
    }

    private class Match(private val string: String, private val matches: List<MatchResult>) {
        companion object {
            fun String.findAll(regex: Regex): Match = Match(this, regex.findAll(this).toList())
        }

        fun representation(ifNoMatch: String? = null, onlyMainGroup: Boolean = false): List<String> {
            if (matches.isEmpty() && ifNoMatch != null) return listOf(string, ifNoMatch)

            val ranges =
                matches
                    .flatMap {
                        buildList {
                            if (onlyMainGroup) add(it.groups.first()!!.range)
                            else {
                                it.groups.forEach { it?.range?.let(::add) }
                            }
                        }
                    }
                    .let(::unOverlapMatchedGroups)
            return StringMultiRangesUnderline(string, ranges).lines()
        }

        private fun unOverlapMatchedGroups(ranges: List<IntRange>): List<List<IntRange>> {
            val res = mutableListOf<MutableList<IntRange>>(mutableListOf())
            ranges.forEach outer@{ r ->
                res.forEach { previousRanges ->
                    if (previousRanges.none { it.overlap(r) }) {
                        previousRanges.add(r)
                        return@outer
                    }
                }
                res.add(mutableListOf(r))
            }
            return res
        }

        private fun IntRange.overlap(other: IntRange): Boolean {
            return (other.first in first..last) || (first in other.first..other.last)
        }
    }

    private class StringMultiRangesUnderline(private val reference: String, private val ranges: List<List<IntRange>>) {
        fun lines(): List<String> {
            val referenceLine = StringBuilder()
            val underlines = ranges.asReversed().map { it to StringBuilder() }

            reference.indices.forEach { i ->
                val start = ranges.any { it.any { it.first == i } }
                val end = ranges.any { it.any { it.last == i } }
                if (start) referenceLine.append(' ')
                referenceLine.append(reference[i])
                if (end) referenceLine.append(' ')
                underlines.forEach { (ranges, sb) ->
                    val contains = ranges.any { it.contains(i) }

                    if (ranges.any { it.first == i }) sb.append('└')
                    else if (start && contains) sb.append('─') else if (start) sb.append(' ')

                    if (contains) sb.append('─') else sb.append(' ')

                    if (ranges.any { it.last == i }) sb.append('┘')
                    else if (end && contains) sb.append('─') else if (end) sb.append(' ')
                }
            }

            return buildList {
                add(referenceLine.toString())
                addAll(underlines.map { it.second.toString() })
            }
        }
    }
}
