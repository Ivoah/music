package net.ivoah.music

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*

import java.io.*
import java.net.*
import java.security.MessageDigest
import java.time.Instant
import com.typesafe.config.ConfigFactory

case class Spotify(private val client_id: String, private val client_secret: String, scope: String = "")(using Database) {
  private implicit val tokenReads: Reads[Token] = (
    (JsPath \ "access_token").read[String] and
      (JsPath \ "token_type").read[String] and
      (JsPath \ "scope").read[String] and
      (JsPath \ "expires_in").read[Int] and
      (JsPath \ "refresh_token").read[String] and
      (JsPath \ "expires_on").readNullable[Long]
    ) { (access_token: String, token_type: String, scope: String, expires_in: Int, refresh_token: String, expires_on: Option[Long]) =>
    expires_on match {
      case Some(timestamp) => Token(access_token, token_type, scope, expires_in, refresh_token, timestamp)
      case None => Token(access_token, token_type, scope, expires_in, refresh_token)
    }
  }
  private implicit val tokenWrites: Writes[Token] = Json.writes[Token]
  private object Token {
    def apply(access_token: String, token_type: String, scope: String, expires_in: Int, refresh_token: String): Token = {
      Token(access_token, token_type, scope, expires_in, refresh_token, Instant.now.getEpochSecond + expires_in)
    }
  }
  private case class Token(access_token: String, token_type: String, scope: String, expires_in: Int, refresh_token: String, val expires_on: Long) {
    override def toString: String = access_token
  }

  private def token: Option[Token] = {
    sql"SELECT token FROM token".query(r => Json.parse(r.getString("token")).as[Token]).headOption map { token =>
      if (Instant.now.getEpochSecond < token.expires_on) token
      else {
        val new_token = refresh_token(token)
        sql"DELETE FROM token; INSERT INTO token VALUES (${Json.prettyPrint(Json.toJson(new_token))}::jsonb);".execute()
        new_token
      }
    }
  }

  private def refresh_token(token: Token): Token = {
    try {
      val post = requests.post(
        "https://accounts.spotify.com/api/token",
        data = Map(
          "grant_type" -> "refresh_token",
          "refresh_token" -> token.refresh_token,
          "client_id" -> client_id
        )
      )
      Json.parse(post.text()).as[Token]
    } catch {
      case e: requests.RequestFailedException if e.response.statusCode == 400 =>
        println("Error refreshing token")
        throw e
    }
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
    val new_token = Json.parse(post.text()).as[Token]
    sql"DELETE FROM token; INSERT INTO token VALUES (${Json.prettyPrint(Json.toJson(new_token))}::jsonb)".execute()
  }

  def nowPlaying(): Option[String] = token.map(token =>
    val headers = Map("Authorization" -> s"Bearer ${token.access_token}")
    val req = requests.get("https://api.spotify.com/v1/me/player", headers = headers)
    req.text()
  )
}
