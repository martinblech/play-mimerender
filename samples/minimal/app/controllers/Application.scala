package controllers

import play.api._
import play.api.mvc._
import play.api.libs.Jsonp
import play.api.libs.json._
import play.api.libs.json.Json._
import mimerender.DSL._

object Application extends Controller {
  def jsonTransform(s: String) = toJson(Map("message" -> toJson(s)))
  def jsonpTransform(s: String, r: Request[Any]) = {
    val callback = r.queryString.getOrElse("callback", Nil).headOption
      .getOrElse("callback")
    Jsonp(callback, jsonTransform(s))
  }
  val m = mapping(
    "text/html" -> { s: String => views.html.index(s) },
    "application/xml" -> { s: String => <message>{s}</message> },
    "application/json" -> jsonTransform _,
    "application/javascript" -> jsonpTransform _,
    "text/plain" -> identity[String]_
  ) queryStringOverride "format"
  
  def index = Action { implicit request =>
    m.status(200)("Hello, world!")
  }
  
}
