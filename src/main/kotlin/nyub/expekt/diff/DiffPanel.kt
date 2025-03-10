package nyub.expekt.diff

import java.awt.*
import java.awt.GridBagConstraints.BOTH
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.text.DefaultHighlighter

/**
 * ```
 * |----X---|--------|---V----|
 * |        |        |        |
 * |        |        |        |
 * | Apprvd |  Diff  | Recvd  |
 * |        |        |        |
 * |--------|--------|--------|
 * ```
 *
 * All icons are from [icon8](https://icons8.com)
 */
class DiffPanel(received: List<String>, approvedLines: List<String>) : JPanel(GridBagLayout()) {
    private val diff = Myers(String::equals).diff(approvedLines, received)

    private val receivedText = JTextArea(received.joinToString(separator = "\n"))
    private val approvedText = JTextArea(approvedLines.joinToString(separator = "\n"))
    private val diffText = DiffHighlightPanel(approvedLines, diff)
    private val rejectButton = JButton()
    private val approveButton = JButton()

    var onApprove: () -> Unit = {}
    var onReject: () -> Unit = {}

    init {
        val rejectButtonPosition =
            GridBagConstraints().apply {
                buttonPosition()
                gridx = 0
            }

        val approveButtonPosition =
            GridBagConstraints().apply {
                buttonPosition()
                gridx = 2
            }

        val receivedTextPosition =
            GridBagConstraints().apply {
                textPanePosition()
                gridx = 0
            }

        val diffTextPosition =
            GridBagConstraints().apply {
                textPanePosition()
                gridx = 1
            }

        val approvedTextPosition =
            GridBagConstraints().apply {
                textPanePosition()
                gridx = 2
            }

        add(receivedText, receivedTextPosition)
        add(diffText, diffTextPosition)
        add(approvedText, approvedTextPosition)
        add(rejectButton, rejectButtonPosition)
        add(approveButton, approveButtonPosition)

        refreshDiffOnTextChange()

        approveButton.addActionListener { onApprove() }
        rejectButton.addActionListener { onReject() }
        rejectButton.icon = ImageIcon(ImageIO.read(javaClass.getResource("/icons8-cross-48.png")))
        approveButton.icon = ImageIcon(ImageIO.read(javaClass.getResource("/icons8-checkmark-48.png")))
    }

    private fun GridBagConstraints.textPanePosition() {
        gridwidth = 1
        gridheight = 1
        gridy = 1
        weightx = 33.0 // 1/3rd each
        weighty = 95.0 // Almost full height
        fill = BOTH
    }

    private fun GridBagConstraints.buttonPosition() {
        gridy = 0
        weightx = 33.0
        weighty = 5.0
        gridheight = 1
        gridwidth = 1
        fill = BOTH
    }

    private fun refreshDiffOnTextChange() {
        val refreshDiffListener =
            object : KeyListener {
                override fun keyTyped(e: KeyEvent) {
                    val approved = approvedText.text.split("\n")
                    val diff = Myers(String::equals).diff(approved, receivedText.text.split("\n"))
                    diffText.update(approved, diff)
                    println("fired")
                }

                override fun keyPressed(e: KeyEvent) = Unit

                override fun keyReleased(e: KeyEvent) = Unit
            }
        receivedText.isEditable = true
        approvedText.isEditable = true
        receivedText.addKeyListener(refreshDiffListener)
        approvedText.addKeyListener(refreshDiffListener)
    }
}

private class DiffHighlightPanel(approvedLines: List<String>, diff: List<SequenceDiff.PatchItem<String>>) :
    JTextArea() {
    init {
        update(approvedLines, diff)
    }

    fun update(approvedLines: List<String>, diff: List<SequenceDiff.PatchItem<String>>) {
        val deletePainter = DefaultHighlighter.DefaultHighlightPainter(Color.RED.withAlpha(0.25f))
        val addPainter = DefaultHighlighter.DefaultHighlightPainter(Color.GREEN.withAlpha(0.25f))
        val lines = buildList {
            var leftIndex = 0
            diff.forEach {
                when (it) {
                    is SequenceDiff.Delete -> {
                        add("- ${approvedLines[leftIndex]}")
                        leftIndex++
                    }

                    is SequenceDiff.Add -> {
                        add("+ ${it.element}")
                    }
                    SequenceDiff.Keep -> {
                        add(approvedLines[leftIndex])
                        leftIndex++
                    }
                }
            }
        }
        this.text = lines.joinToString(separator = "\n")
        diff.forEachIndexed { index, element ->
            val line = index
            val start = this.getLineStartOffset(line)
            val end = this.getLineEndOffset(line)
            when (element) {
                is SequenceDiff.Delete -> {
                    this.highlighter.addHighlight(start, end, deletePainter)
                }

                is SequenceDiff.Add -> {
                    this.highlighter.addHighlight(start, end, addPainter)
                }
                SequenceDiff.Keep -> {}
            }
        }
    }

    private fun Color.withAlpha(alpha: Float): Color {
        return Color(this.red.toFloat() / 255, this.green.toFloat() / 255, this.blue.toFloat() / 255, alpha)
    }
}
