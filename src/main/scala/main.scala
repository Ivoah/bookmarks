package net.ivoah.bookmarks

import net.ivoah.vial.*
import org.rogach.scallop.*

val router = Router {
  case _ => Response("Hello!")
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
  val server = if (conf.socket.isDefined) {
    println(s"Using unix socket: ${conf.socket()}")
    Server(router, socket = conf.socket.toOption)
  } else {
    println(s"Using host/port: ${conf.host()}:${conf.port()}")
    Server(router, conf.host(), conf.port())
  }
  server.serve()
}
