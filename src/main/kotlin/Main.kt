import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel


object MainFrame : JFrame("Fish classifier") {
    val classes = ArrayList<Classified>()
    val center = JPanel()
    val south = JPanel()
    init {
        contentPane.layout = BorderLayout()
        contentPane.add(center, BorderLayout.CENTER)
        contentPane.add(south, BorderLayout.SOUTH)
        south.layout = FlowLayout()
        this.setSize(1280,720)
        this.setLocationRelativeTo(null)
        classes.add(Classified("Salmon"))
        classes.add(Classified("Debris"))
        classes.add(Classified("Beaver"))
        classes.add(Classified("Otter"))
        classes.add(Classified("Other fish"))
        classes.add(Classified("Shadows"))
        classes.forEach { south.add(it.button) }
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    }

}

class Classified(name:String) : JPanel() {
    val button: JButton = JButton()
    init {
        button.text = name
    }



}

fun main(args: Array<String>) {
    MainFrame.isVisible = true

}
