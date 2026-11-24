package kotlinx.browser
class Node
class MediaQueryList(val matches: Boolean)
class Document { val body: Node = Node() }
class Window {
    val innerWidth: Int = 1280
    fun matchMedia(query: String): MediaQueryList = MediaQueryList(false)
}
val document: Document = Document()
val window: Window = Window()
