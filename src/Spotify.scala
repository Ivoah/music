package net.ivoah.music

import play.api.libs.json.*

import java.net.*
import java.time.LocalDateTime
import java.util.Base64

case class Spotify(private val client_id: String, private val client_secret: String, scope: String = "")(using Database) {
  private given Format[AuthResponse] = Json.format[AuthResponse]
  private case class AuthResponse(access_token: String, token_type: String, scope: String, expires_in: Int, refresh_token: Option[String])

  private def access_token: Option[String] = {
    sql"SELECT access_token, refresh_token, expires FROM token"
      .query(r => (r.getString("access_token"), r.getString("refresh_token"), r.getTimestamp("expires").toLocalDateTime))
      .headOption.map { case (access_token, refresh_token, expires) =>
        if (LocalDateTime.now().isBefore(expires)) access_token
        else {
          val response = refresh(refresh_token)
          sql"""
            DELETE FROM token;
            INSERT INTO token VALUES (
              ${response.access_token},
              ${response.refresh_token.getOrElse(refresh_token)},
              ${LocalDateTime.now().plusSeconds(response.expires_in)}
            );
          """.execute()
          response.access_token
        }
      }
  }

  private def refresh(refresh_token: String): AuthResponse = {
    val post = requests.post(
      "https://accounts.spotify.com/api/token",
      data = Map(
        "grant_type" -> "refresh_token",
        "refresh_token" -> refresh_token,
      ),
      headers = Map(
        "Authorization" -> s"Basic ${Base64.getEncoder.encodeToString(s"$client_id:$client_secret".getBytes)}"
      )
    )
    Json.parse(post.text()).as[AuthResponse]
  }

  val redirect: String = s"https://accounts.spotify.com/authorize?${Map(
    "client_id" -> client_id,
    "response_type" -> "code",
    "redirect_uri" -> "https://music.ivoah.net/callback",
    "scope" -> scope
  ).map{case (key, value) => s"$key=${URLEncoder.encode(value, "UTF-8")}"}.mkString("&")}"

  def save_token(code: String): Unit = {
    val post = requests.post(
      "https://accounts.spotify.com/api/token",
      data = Map(
        "client_id" -> client_id,
        "client_secret" -> client_secret,
        "grant_type" -> "authorization_code",
        "code" -> code,
        "redirect_uri" -> "https://music.ivoah.net/callback",
      )
    )
    val response = Json.parse(post.text()).as[AuthResponse]
    sql"""
      DELETE FROM token;
      INSERT INTO token VALUES (
        ${response.access_token},
        ${response.refresh_token.get},
        ${LocalDateTime.now().plusSeconds(response.expires_in)}
      );
    """.execute()
  }

  def nowPlaying(): Option[String] = access_token.map(access_token =>
    val headers = Map("Authorization" -> s"Bearer ${access_token}")
    val req = requests.get("https://api.spotify.com/v1/me/player", headers = headers)
    req.text()
  )
}
