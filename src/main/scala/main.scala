package net.ivoah.bookmarks

import net.ivoah.vial.*
import org.rogach.scallop.*
import scalatags.Text.all.*
import scalatags.Text.tags2.title

import upickle.default.*

case class Bookmark(title: String, url: String, children: Seq[Bookmark] = Seq()) derives ReadWriter {
  def render(): Frag = frag(
    s"$title: ", a(href:=url, url), br(),
    ul(
      for (bookmark <- children) yield li(bookmark.render())
    )
  )
}

def router(bookmarks: Seq[Bookmark]) = Router {
  case _ => Response("<!DOCTYPE html>\n" + html(
    head(
      title("Noah's bookmarks"),
      link(rel:="stylesheet", href:="https://ivoah.net/common.css")
    ),
    body(
      h1("Noah's bookmarks"),
      hr(),
      div(textAlign:="left",
        ul(
          for (bookmark <- bookmarks) yield li(bookmark.render())
        )
      )
    )
  ))
}

@main
def main(args: String*): Unit = {
  class Conf(args: Seq[String]) extends ScallopConf(args) {
    val host: ScallopOption[String] = opt[String](default = Some("127.0.0.1"))
    val port: ScallopOption[Int] = opt[Int](default = Some(8080))
    val socket: ScallopOption[String] = opt[String]()
    val verbose: ScallopOption[Boolean] = opt[Boolean](default = Some(false))

    conflicts(socket, List(host, port))
    verify()
  }

  val conf = Conf(args)
  implicit val logger: String => Unit = if (conf.verbose()) println else (msg: String) => ()

//  val bookmarks = Seq(
//    Bookmark("Foo", "https://example.com")
//  )

  println("loading bookmarks")
  val bookmarks = read[Seq[Bookmark]](os.read(os.pwd / "bookmarks.json"))
  println(s"loaded bookmarks: $bookmarks")

  val server = if (conf.socket.isDefined) {
    println(s"Using unix socket: ${conf.socket()}")
    Server(router(bookmarks), socket = conf.socket.toOption)
  } else {
    println(s"Using host/port: ${conf.host()}:${conf.port()}")
    Server(router(bookmarks), conf.host(), conf.port())
  }
  server.serve()
}
