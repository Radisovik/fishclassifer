import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.StorageOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities


object MainFrame : JFrame("Fish classifier") {
    val classes = ArrayList<Classified>()
    val center =  ImagePanel()
    val south = JPanel()
    init {
        contentPane.layout = BorderLayout()
        contentPane.add(center, BorderLayout.CENTER)
        contentPane.add(south, BorderLayout.SOUTH)
        south.layout = FlowLayout()
        this.setSize(1280,720)
        this.setLocationRelativeTo(null)
        setFocusable(true)

        classes.add(Classified("Salmon"))
        classes.add(Classified("Debris"))
        classes.add(Classified("Beaver"))
        classes.add(Classified("Otter"))
        classes.add(Classified("Other fish"))
        classes.add(Classified("Shadows"))
        classes.forEach { south.add(it.button) }
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_LEFT ->
                        center.prev()

                    KeyEvent.VK_RIGHT ->
                        center.next()
                }
               // println("You pressed ${e.keyCode}")
            }
        })

    }

}

class Classified(name:String) : JPanel() {
    val button: JButton = JButton()
    init {
        button.text = name
    }
}

class ImagePanel : JPanel() {
    private var idx =0
    private val images = ArrayList<BufferedImage>()
    init {
    }
    override fun paint(g: Graphics) {
        super.paint(g)
        if(images.size ==0) {
            return
        }
        g.drawImage(images[idx], 0,0, size.width, size.height, null)
    }

    fun images(images:ArrayList<BufferedImage>) {
        idx=0
        this.images.clear()
        this.images.addAll(images)
        println("Clip has ${this.images.size} frames")
    }

    fun next() {
        idx++
        if (idx >= images.size) {
            idx--
        }
        SwingUtilities.invokeLater { this.repaint() }
    }

    fun prev() {
        idx--
        if (idx <0) {
            idx=0
        }
        SwingUtilities.invokeLater { this.repaint() }
    }

}


fun main(args: Array<String>) {
    val options: FirebaseOptions = FirebaseOptions.builder()
        .setDatabaseUrl("https://fishspotter-8057.firebaseio.com/")
        .setCredentials(getGoogleCreds())
        .setReadTimeout(1000 * 15)
        .setConnectTimeout(1000 * 5)
        .build()

    val creds = getGoogleCreds()
    val storage = StorageOptions.newBuilder()
        .setCredentials(creds)
        .setProjectId("fishspotter-8057").build().service

    val app = FirebaseApp.initializeApp(options)
    val db = FirestoreClient.getFirestore()

    val videoFilesWithClips = db.collection("seasons")
        .document("s2122")
        .collection("files")
        .whereGreaterThan("classifications", 0)
        .limit(1)
        .get().get()

    val classes = ArrayList<String>()
    classes.add("Beaver")
    classes.add("Salmon")

    val converter = Java2DFrameConverter()
    videoFilesWithClips.documents.forEach{ it ->
        println("seasons/s2122/files/"+it.id+"/suspects")
        val suspects = db.collection("seasons/s2122/files/" + it.id + "/suspects")
            .whereIn("classification", classes)
            .limit(1)
            .get().get().toObjects(Suspect::class.java)
        suspects.forEach{
            val url = URL(it.mediaLink)
            val stream = url.openStream()
            val fg = FFmpegFrameGrabber(stream)
            fg.start()
            val imageList = ArrayList<BufferedImage>()
            var grab = fg.grab()
            var c =0
            while (grab!=null) {
                val clone = converter.convert(grab)
                val cropped = clone.getSubimage(0,0, grab.imageWidth, grab.imageHeight/2)
                val rtn = BufferedImage(cropped.getWidth(), cropped.getHeight(), BufferedImage.TYPE_3BYTE_BGR)
                rtn.graphics.drawImage(cropped, 0,0,null)
                imageList.add(rtn)
            //    ImageIO.write(element, "jpg", File("test-$c.jpg"))
                c++
                grab = fg.grab()
            }
            fg.stop()
            MainFrame.center.images(imageList)
        }
    }
    MainFrame.isVisible = true
}


data class Suspect(val condition:Boolean?=false, val mediaLink:String?=null, val classification:String?=null) {
}

@Throws(IOException::class)
fun getGoogleCreds(): GoogleCredentials {
    val serviceAccount: InputStream = FileInputStream("fishspotter-8057-a1098b747c9e.json")
        //GoogleCredentials::class.java.getResourceAsStream("/fishspotter-8057-a1098b747c9e.json")
    return GoogleCredentials.fromStream(serviceAccount)
}
