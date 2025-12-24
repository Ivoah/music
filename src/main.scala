package net.ivoah.music

import org.rogach.scallop.*
import net.ivoah.vial.*
import java.util.{Timer, TimerTask}
import com.typesafe.config.ConfigFactory

@main
def main(rawArgs: String*): Unit = {
  class Args(args: Seq[String]) extends ScallopConf(args) {
    val host: ScallopOption[String] = opt[String](default = Some("0.0.0.0"))
    val port: ScallopOption[Int] = opt[Int](default = Some(1337))
    val socket: ScallopOption[String] = opt[String]()
    val verbose: ScallopOption[Boolean] = opt[Boolean](default = Some(false))

    conflicts(socket, List(host, port))
    verify()
  }

  val conf = ConfigFactory.load()
  given Database = Database(conf.getString("db"))
  val spotify = Spotify(conf.getString("client_id"), conf.getString("client_secret"), "user-read-playback-state")

  Timer().scheduleAtFixedRate(new TimerTask {
    def run() = spotify.nowPlaying() match {
      case Some(data) => if (data.nonEmpty) sql"INSERT INTO history VALUES (NOW(), $data::jsonb)".update()
      case None =>
        val bot_token = conf.getString("bot_token")
        val chat_id = conf.getString("chat_id")
        requests.post(s"https://api.telegram.org/bot$bot_token/sendMessage", data = Map(
          "chat_id" -> chat_id,
          "parse_mode" -> "MarkdownV2",
          "text" -> "music\\.ivoah\\.net: Could not get spotify activity\\! Try [logging in](https://music.ivoah.net/login)"
        ))
        println("Could not get spotify activity")
    }
  }, 0, 60*1000)

  val args = Args(rawArgs)
  implicit val logger: String => Unit = if (args.verbose()) println else (msg: String) => ()
  val endpoints = Endpoints(spotify)
  val server = args.socket.toOption match {
    case Some(path) =>
      println(s"Using unix socket: $path")
      Server(endpoints.router, path)
    case None =>
      println(s"Using host/port: ${args.host()}:${args.port()}")
      Server(endpoints.router, (args.host(), args.port()))
  }
  server.serve()
}
