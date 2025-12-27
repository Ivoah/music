package net.ivoah.music

import scalatags.Text.all.*

import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoUnit, TemporalAdjusters}
import java.time.{DayOfWeek, LocalDate, LocalTime}

object Charts {
  // Put here to avoid ambiguity with scalatags.Text.all.*
  import scalatags.Text.svgAttrs.*
  import scalatags.Text.svgTags.*

  private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")
  private val dateFormat = DateTimeFormatter.ofPattern("M/d")

  def musicChart(history: Seq[Session]): Frag = {
    val minDate = history.map(_.date).min.minusDays(1)
    object margin {
      val top = 20
      val right = 0
      val bottom = 30
      val left = 55
    }
    val barSpacing = 10

    def xScale(date: LocalDate) = ChronoUnit.DAYS.between(minDate, date)*(barSpacing)

    val _width = xScale(LocalDate.now) + barSpacing
    val _height = 250

    val maxTime = history.map(_.endMinute).max
    def yScale(time: Int) = margin.top + (_height - margin.bottom - margin.top)/maxTime.toDouble*(maxTime - time)

    val firstDays = Iterator.iterate(history.map(_.date).min.`with`(TemporalAdjusters.previous(DayOfWeek.SUNDAY))) {
      _.plus(1, ChronoUnit.WEEKS)
    }.takeWhile(LocalDate.now.isAfter).toSeq

    div(scalatags.Text.styles.display:="flex",
      svg(flexShrink:=0, width:=margin.left, height:=_height,
        g(fill:="currentColor", stroke:="currentColor", textAnchor:="end", transform:=s"translate(${margin.left},0)",
          for (h <- 0 to 24 by 4) yield g(transform:=s"translate(0,${yScale(h*60)})",
            line(x2:= -6),
            text(stroke:="none", x:= -9, dy:="0.32em", timeFormat.format(LocalTime.of(h%24, 0)))
          )
        )
      ),
      div(overflowX:="scroll",
        svg(width:=_width, height:=_height,
          g(for (h <- 0 to 24 by 4) yield line(stroke:="currentColor", strokeOpacity:=0.2, x1:=0, y1:=yScale(h*60), x2:=(_width - margin.right), y2:=yScale(h*60))),
          g(for (d <- firstDays) yield g(fill:="currentColor", stroke:="currentColor", textAnchor:="middle", transform:=s"translate(${xScale(d)}, ${yScale(0)})",
            line(y2:=5),
            text(stroke:="none", y:=15, dateFormat.format(d))
          )),
          g(stroke:="lightblue", strokeWidth:="7", strokeLinecap:="round",
            for (s <- history) yield {
              line(x1:=xScale(s.date), y1:=yScale(s.startMinute), x2:=xScale(s.date), y2:=yScale(s.endMinute), onclick:="alert(this.children[0].textContent)",
                scalatags.Text.svgTags.tag("title")(s.hoverText)
              )
            }
          )
        )
      )
    )
  }
}
