package net.ivoah.music

import java.time.{LocalDate, LocalDateTime}
import java.time.temporal.ChronoUnit

extension (s: String) {
  def escape: String = s.replaceAllLiterally("\"", "\\\"")
  def quote: String = s"\"$s\""
}

case class Song(title: String, link: String, artwork: String)

case class Snapshot(time: LocalDateTime, song: String, progress: Int)

case class Listen(song: String, start: LocalDateTime, end: LocalDateTime, progress: Int)

case class Session(date: LocalDate, start: LocalDateTime, end: LocalDateTime, songs: Seq[Snapshot]) {
  def toJson: String = {
    val startOfDay = date.atStartOfDay()
    val spanJson = s"[${ChronoUnit.MINUTES.between(startOfDay, start)}, ${ChronoUnit.MINUTES.between(startOfDay, end)}]"
    val songsJson = songs.map(_.song.escape.quote).mkString("[", ", ", "]")
    s"""{"date": "$date", "span": $spanJson, "songs": $songsJson}"""
  }
}
