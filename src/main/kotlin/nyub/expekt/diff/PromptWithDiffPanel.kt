package nyub.expekt.diff

import java.awt.Dimension
import java.awt.Frame
import java.awt.Toolkit
import javax.swing.JDialog
import nyub.expekt.PromotionTrigger

/** If expected/actual contents differ, spawn a diff view and ask user to accept or reject the promotion */
object PromptWithDiffPanel : PromotionTrigger {
    override fun invoke(expected: String, actual: String): Boolean {
        if (expected == actual) return false
        val diff = DiffPanel(actual.split("\n"), expected.split("\n"))
        val dialog =
            JDialog(null as Frame?, true).apply {
                contentPane = diff
                size = portionOfScreen()
                setLocationRelativeTo(null) // center on screen
            }

        var userApproved = false
        diff.onApprove = {
            userApproved = true
            dialog.isVisible = false
        }
        diff.onReject = {
            userApproved = false
            dialog.isVisible = false
        }

        dialog.isVisible = true // blocking until dialog is not visible
        dialog.dispose()
        return userApproved
    }

    private fun portionOfScreen(): Dimension {
        val screen = Toolkit.getDefaultToolkit().screenSize
        return Dimension(3 * screen.width / 5, 3 * screen.height / 5)
    }
}
