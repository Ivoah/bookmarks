package net.ivoah.bookmarks

import net.ivoah.vial.*
import org.rogach.scallop.*
import scalatags.Text.all.*
import scalatags.Text.tags2.title

import upickle.default as json

import scala.util.Using
import scala.io.Source

case class Bookmark(title: String, url: String = "", children: Seq[Bookmark] = Seq()) derives json.ReadWriter {
  def withBookmark(path: Seq[Int], bookmark: Bookmark): Bookmark = {
    path match {
      case Seq(i) => Bookmark(title, url, children.patch(i, Seq(bookmark), 0))
      case Seq(i, tail*) => Bookmark(title, url, children.patch(i, Seq(children(i).withBookmark(tail, bookmark)), 1))
    }
  }

  def render(path: Seq[Int]): Frag = {
    frag(
      div(`class`:="bookmark", s"$title: ", a(href:=url, url)),
      ul(
        for ((bookmark, i) <- children.zipWithIndex) yield li(bookmark.render(path :+ i)),
        li(`class`:="form", display:="none", form(
          action:="add_bookmark",
          method:="post",
          input(`type`:="hidden", name:="path", value:=(path :+ children.length).mkString(".")),
          input(name:="title"), ": ", input(name:="url"), input(`type`:="submit", value:="+")
        )),
      )
    )
  }
}

def router(credentials: (String, String)) = Router {
  case ("GET", "/", _) =>
    val bookmarks = json.read[Seq[Bookmark]](os.read(os.pwd / "bookmarks.json"))
    Response("<!DOCTYPE html>\n" + html(
      head(
        title("Noah's bookmarks"),
        link(rel:="stylesheet", href:="https://ivoah.net/common.css"),
        script(src:="https://code.jquery.com/jquery-3.7.1.min.js"),
        script(raw("""
          $(function() {
            $(".bookmark").click(function() {
              $(this).siblings().children("li.form").slideToggle();
            })
          })
        """))
      ),
      body(
        h1("Noah's bookmarks"),
        hr(),
        div(textAlign:="left",
          ul(
            for ((bookmark, i) <- bookmarks.zipWithIndex) yield li(bookmark.render(Seq(i)))
          )
        )
      )
    ))
  case ("POST", "/add_bookmark", request) if request.auth.contains(credentials) =>
    val path = request.form("path").split(raw"\.").map(_.toInt).toSeq
    val title = request.form("title")
    val url = request.form("url")
    val bookmarks = json.read[Seq[Bookmark]](os.read(os.pwd / "bookmarks.json"))
    println(s"New bookmark: $path, $title, $url")
    os.write.over(os.pwd / "bookmarks.json", json.write(bookmarks.patch(path.head, Seq(bookmarks(path.head).withBookmark(path.tail, Bookmark(title, url))), 1), indent = 2))
    Response.Redirect("/")
  case ("POST", "/add_bookmark", _) => Response.Unauthorized()
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

  val credentials = Using.resource(Source.fromResource("credentials.txt"))(_.getLines().mkString("\n")) match {
    case s"$user:$password" => (user, password)
    case _ =>
      System.err.println("Could not read credentials")
      System.exit(1)
      ("", "")
  }

  val server = if (conf.socket.isDefined) {
    println(s"Using unix socket: ${conf.socket()}")
    Server(router(credentials), socket = conf.socket.toOption)
  } else {
    println(s"Using host/port: ${conf.host()}:${conf.port()}")
    Server(router(credentials), conf.host(), conf.port())
  }
  server.serve()
}
