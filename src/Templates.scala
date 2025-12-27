package net.ivoah.music

import scalatags.Text.all.*
import scalatags.Text.tags2.title
import scalatags.Text.svgTags.svg

object Templates {
  def root(song: Option[Song], history: Seq[Session]) = doctype("html")(html(
    head(
      title(s"Noah is listening to ${song.map(_.title).getOrElse("nothing")}"),

      link(rel:="stylesheet", href:="/static/style.css"),
      link(rel:="shortcut icon", `type`:="image/png", href:="/static/favicon.png"),
      meta(httpEquiv:="refresh", content:=60),

      script(src:="https://code.jquery.com/jquery-3.6.0.min.js", crossorigin:="anonymous"),
    ),
    body(
      song.map(song => frag(
        img(cls:="center", style:="filter: blur(100px);", src:=song.artwork),
        img(cls:="center hidden artwork", src:=song.artwork)
      )).getOrElse(frag()),
      div(cls:="center textbox",
        "Noah is listening to ",
        song match {
          case Some(song) => b(a(href:=song.link, attr("onmouseenter"):="$('.artwork').toggleClass('hidden', false)", attr("onmouseleave"):="$('.artwork').toggleClass('hidden', true)", song.title))
          case None => "nothing"
        },
        " right now"
      ),
      div(cls:="bottom", Charts.musicChart(history)),
      script(raw("document.querySelectorAll('div[style=\"overflow-x: scroll;\"]').forEach(e => e.scrollLeft = e.scrollWidth)"))
    )
  )).render
}
