package net.ivoah.music

import net.ivoah.vial.*
import java.nio.file.Paths
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.typesafe.config.ConfigFactory

class Endpoints {
  private val conf = ConfigFactory.load()
  given Database = Database(conf.getString("db"))
  def router: Router = Router {
    case ("GET" , "/", _) =>
      val song = sql"""
        SELECT * FROM (
          SELECT
          data #>> '{"item", "name"}' title,
          data #>> '{"item", "external_urls", "spotify"}' link,
          data #>> '{"item", "album", "images", 0, "url"}' artwork,
          (NOW() - time) < INTERVAL '90 seconds' AND data['is_playing']::boolean active
          FROM history
          ORDER BY time DESC
          LIMIT 1
        ) t WHERE active
      """.query(r => Song(r.getString("title"), r.getString("link"), r.getString("artwork"))).headOption
      Response(Templates.root(song))
    case ("GET", "/history.json", _) =>
      val rows = sql"""
        SELECT
          time,
          data #>> '{"item", "name"}' song,
          data['progress_ms']::integer progress
        FROM history
        WHERE data['is_playing']::boolean and jsonb_typeof(data->'item') != 'null'
        ORDER BY time ASC
      """.query(r => Snapshot(r.getTimestamp("time").toLocalDateTime(), r.getString("song"), r.getInt("progress")))

      val sessions = rows.foldLeft(Seq[Session]()) { case (sessions, s) =>
        val startOfDay = s.time.toLocalDate().atStartOfDay()
        val newSession = Session(s.time.toLocalDate(), s.time, s.time, Seq(s))
        val lastSession = sessions.lastOption.getOrElse(newSession)

        if (ChronoUnit.MINUTES.between(lastSession.end, s.time) > 5) {
          sessions :+ newSession
        } else {
          val newSong = if (s.song != lastSession.songs.last.song || s.progress < lastSession.songs.last.progress) Some(s)
                        else None
          sessions.dropRight(1) :+ Session(lastSession.date, lastSession.start, s.time, lastSession.songs ++ newSong)
        }
      }
      
      Response(sessions.map(_.toJson).mkString("[\n    ", ",\n    ", "\n]"), headers = Map("Content-Type" -> Seq("application/json")))
    case ("GET", s"/static/$file", _) => Response.forFile(Paths.get("static"), Paths.get(file))
  }
}
