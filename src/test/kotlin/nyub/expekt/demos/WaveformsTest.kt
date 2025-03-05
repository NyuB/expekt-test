package nyub.expekt.demos

import nyub.expekt.ExpectTests
import nyub.expekt.demos.WaveformsTest.SignalState.*
import org.junit.jupiter.api.Test

typealias SignalStates = Map<String, List<WaveformsTest.SignalState>>

typealias GateStates = Map<String, List<Int>>

class WaveformsTest {
    @Test
    fun `waveforms demo`() =
        ExpectTests().expectTest {
            val clock = Clock()
            val incr = CyclicSignal(listOf(LOW, LOW, HIGH, HIGH, HIGH, LOW, LOW, HIGH, HIGH, LOW))
            val counter = Counter(clock, incr)
            val signals = mapOf("clock" to clock, "incr" to incr)
            val gates = mapOf("counter" to counter)

            val (signalStates, gateStates) = runSystem(iterations = 25, signals, gates)
            printWaveforms(signalStates, gateStates)
            expect(
                """
             __________
            |          |   ┌──┐  ┌──┐  ┌──┐  ┌──┐  ┌──┐  ┌──┐  ┌──┐  ┌──┐  ┌──┐  ┌──┐  ┌──┐  ┌──┐
            |clock     |   │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │
            |          |───┘  └──┘  └──┘  └──┘  └──┘  └──┘  └──┘  └──┘  └──┘  └──┘  └──┘  └──┘  └──
            |__________|
            |          |      ┌────────┐     ┌─────┐        ┌────────┐     ┌─────┐        ┌────────
            |incr      |      │        │     │     │        │        │     │     │        │
            |          |──────┘        └─────┘     └────────┘        └─────┘     └────────┘
            |__________|
            |          |┬────────┬─────┬───────────┬───────────┬─────┬───────────┬───────────┬─────
            |counter   |│0       │1    │2          │3          │4    │5          │6          │7
            |          |┴────────┴─────┴───────────┴───────────┴─────┴───────────┴───────────┴─────
            |__________|
            
        """
                    .trimIndent()
            )
        }

    private fun runSystem(
        iterations: Int,
        signals: Map<String, Signal>,
        gates: Map<String, Counter>,
    ): Pair<SignalStates, GateStates> {
        val signalStates: Map<String, MutableList<SignalState>> = signals.mapValues { mutableListOf() }
        val gateStates: Map<String, MutableList<Int>> = gates.mapValues { mutableListOf() }
        repeat(iterations) {
            gates.forEach { (l, g) ->
                gateStates[l]!!.add(g.dout)
                g.tick()
            }
            signals.forEach { (l, s) ->
                signalStates[l]!!.add(s.state)
                s.tick()
            }
        }
        return signalStates to gateStates
    }

    private val labelWidth = 10
    private val signalWidth = 2
    private val String.labelPad
        get() = String.format("%-${labelWidth}s", this)

    private val String.signalRepeat
        get() = this.repeat(signalWidth)

    private val Int.signalPad
        get() = String.format("%-${signalWidth}d", this)

    private val labelPad = "".labelPad
    private val signalPad = " ".signalRepeat

    private fun ExpectTests.ExpectTest.printWaveforms(signalStates: SignalStates, gateStates: GateStates) {
        printf(" %s %n", "_".repeat(10))
        signalStates.forEach { (l, s) ->
            printSignalHistory(l, s)
            printf("|%s|%n", "_".repeat(10))
        }
        gateStates.forEach { (l, g) ->
            printGateHistory(l, g)
            printf("|%s|%n", "_".repeat(10))
        }
    }

    private fun ExpectTests.ExpectTest.printSignalHistory(label: String, states: List<SignalState>) {
        print("|$labelPad|")
        states.forEachWithPrevious { prev, it ->
            if (it == HIGH && prev != LOW) print("─${"─".signalRepeat}")
            else if (it == HIGH) print("┌${"─".signalRepeat}")
            else if (it == LOW && prev == HIGH) print("┐$signalPad") else print(" $signalPad")
        }
        newLine()
        printf("|${label.labelPad}|")
        states.forEachWithPrevious { prev, s ->
            if (prev != null && prev != s) {
                print("│$signalPad")
            } else print(" $signalPad")
        }
        newLine()
        print("|$labelPad|")
        states.forEachWithPrevious { prev, it ->
            if (it == LOW && prev != HIGH) print("─${"─".signalRepeat}")
            else if (it == LOW) print("└${"─".signalRepeat}")
            else if (it == HIGH && prev == LOW) print("┘$signalPad") else print(" $signalPad")
        }
        newLine()
    }

    private fun ExpectTests.ExpectTest.printGateHistory(label: String, values: List<Int>) {
        printGateHistoryFrame(values, "┬")
        var prev: Int? = null
        newLine()
        print("|${label.labelPad}|")
        values.forEach {
            if (prev != it) print("│${it.signalPad}") else print(" $signalPad")
            prev = it
        }
        newLine()
        printGateHistoryFrame(values, "┴")
        newLine()
    }

    private fun ExpectTests.ExpectTest.printGateHistoryFrame(values: List<Int>, onChange: String) {
        var prev: Int? = null
        print("|$labelPad|")
        values.forEach {
            if (it != prev) print("$onChange${"─".signalRepeat}") else print("─${"─".signalRepeat}")
            prev = it
        }
    }

    enum class SignalState {
        HIGH,
        LOW;

        override fun toString(): String {
            return when (this) {
                HIGH -> "H"
                LOW -> "L"
            }
        }
    }

    interface Tick {
        fun tick()

        fun tick(n: Int) = repeat(n, ::tick)
    }

    interface Signal : Tick {
        val state: SignalState
    }

    class Clock : Signal {
        override var state: SignalState = LOW

        override fun tick() {
            state = if (state == LOW) HIGH else LOW
        }
    }

    class CyclicSignal(private val values: List<SignalState>) : Signal {
        private var index = 0
        override val state: SignalState
            get() = values[index]

        override fun tick() {
            index++
            index %= values.size
        }
    }

    class Counter(private val clock: Clock, private val incr: Signal) : Tick {
        var dout: Int = 0
            private set

        override fun tick() {
            if (clock.state == LOW && incr.state == HIGH) dout++
        }
    }

    private inline fun <T> Iterable<T>.forEachWithPrevious(f: (T?, T) -> Unit) {
        var prev: T? = null
        forEach {
            f(prev, it)
            prev = it
        }
    }
}
