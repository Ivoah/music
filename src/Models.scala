package net.ivoah.music

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

extension (s: String) {
  def escape: String = s.replace("\"", "\\\"")
  def quote: String = s"\"$s\""
}

case class Song(title: String, link: String, artwork: String)

case class Snapshot(time: LocalDateTime, song: String, progress: Int)

case class Listen(song: String, start: LocalDateTime, end: LocalDateTime, progress: Int)

case class Session(date: LocalDate, start: LocalDateTime, end: LocalDateTime, songs: Seq[Snapshot]) {
  private val dateFormat = DateTimeFormatter.ofPattern("EEEE MMMM d, uuuu")
  private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

  lazy val startMinute: Int = ChronoUnit.MINUTES.between(date.atStartOfDay(), start).toInt
  lazy val endMinute: Int = ChronoUnit.MINUTES.between(date.atStartOfDay(), end).toInt

  lazy val hoverText: String =
    s"""${dateFormat.format(date)}
        |start: ${timeFormat.format(start)}
        |stop: ${timeFormat.format(end)}
        |${ChronoUnit.MINUTES.between(start, end)} minutes total
        |songs:
        |${songs.map(s => s"* ${s.song}").mkString("\n")}""".stripMargin

  lazy val toJson: String = {
    val startOfDay = date.atStartOfDay()
    val spanJson = s"[${ChronoUnit.MINUTES.between(startOfDay, start)}, ${ChronoUnit.MINUTES.between(startOfDay, end)}]"
    val songsJson = songs.map(_.song.escape.quote).mkString("[", ", ", "]")
    s"""{"date": "$date", "span": $spanJson, "songs": $songsJson}"""
  }
}
