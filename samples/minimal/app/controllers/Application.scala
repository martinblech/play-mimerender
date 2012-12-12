package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Json._
import mimerender._

object Application extends Controller {
  val m = mapping(
    "text/html" -> { s: String => views.html.index(s) },
    "application/xml" -> { s: String => <message>{s}</message> },
    "application/json" -> { s: String => toJson(Map("message" -> toJson(s))) },
    "text/plain" -> identity[String]_
  )
  
  def index = Action { implicit request =>
    m.status(200)("Hello, world!")
  }
  
}
