package net.ivoah.music

import scalatags.Text.all.*
import scalatags.Text.tags2.title
import scalatags.Text.svgTags.svg

object Templates {
  def root(song: Option[Song]) = doctype("html")(html(
    head(
      title(s"Noah is listening to ${song.map(_.title).getOrElse("nothing")}"),

      link(rel:="stylesheet", href:="/static/style.css"),
      link(rel:="shortcut icon", `type`:="image/png", href:="/static/favicon.png"),

      script(src:="https://d3js.org/d3.v5.min.js"),
      script(src:="https://code.jquery.com/jquery-3.6.0.min.js", crossorigin:="anonymous"),
      script(src:="/static/music.js")
    ),
    body(
      song.map(song => frag(
        img(cls:="center", style:="filter: blur(100px);", src:=song.artwork),
        img(cls:="center hidden artwork", src:=song.artwork)
      )).getOrElse(frag()),
      div(cls:="center textbox",
        song match {
          case Some(song) => frag("Noah is listening to ", b(a(href:=song.link, attr("onmouseenter"):="showArtwork()", attr("onmouseleave"):="hideArtwork()", song.title)), " right now")
          case None => "Noah is listening to nothing right now"
        }
      ),
      div(cls:="bottom",
        div(cls:="flexbox",
          svg(id:="labels"),
          div(cls:="scrollbox", svg(id:="chart"))
        )
      )
    )
  )).render
}
