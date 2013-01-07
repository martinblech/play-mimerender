package controllers

import play.api._
import play.api.mvc._
import play.api.libs.Jsonp
import play.api.libs.json._
import play.api.libs.json.Json._
import mimerender.DSL._

object Application extends Controller {
  def jsonTransform(s: String) = toJson(Map("message" -> toJson(s)))
  def jsonpTransform(s: String, r: RequestHeader) = {
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
  
  private val startsWithNumber = "(^\\d.*)".r
  def index(name: String) = Action { implicit request =>
    assume(name != "John", "I hate John!")
    require(!startsWithNumber.pattern.matcher(name).matches,
      "names cannot start with numbers")
    m.status(200)("Hello, " + name + "!")
  }
  
}
