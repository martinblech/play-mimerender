import play.api._
import play.api.http.Status._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.Jsonp
import play.api.libs.json._
import play.api.libs.json.Json._
import mimerender.DSL._

object Global extends GlobalSettings {
  def jsonTransform(ex: Throwable) = toJson(
    Map("class" -> toJson(ex.getClass.getName),
        "message" -> toJson(ex.getMessage))
  )

  def jsonpTransform(ex: Throwable, r: RequestHeader) = {
    val callback = r.queryString.getOrElse("callback", Nil).headOption
      .getOrElse("callback")
    Jsonp(callback, jsonTransform(ex))
  }

  def xmlTransform(ex: Throwable) =
    <error>
      <class>{ex.getClass.getName}</class>
      <message>{ex.getMessage}</message>
    </error>
  
  def txtTransform(ex: Throwable) = ex.getClass.getName + " - " + ex.getMessage

  val m = mapping(
    "text/html" -> { ex: Throwable => views.html.error(ex) },
    "application/xml" -> xmlTransform _,
    "application/json" -> jsonTransform _,
    "application/javascript" -> jsonpTransform _,
    "text/plain" -> txtTransform _
  ) queryStringOverride "format"

  override def onError(request: RequestHeader, ex: Throwable) = {
    implicit val req = request
    ex match {
      case pe: PlayException =>
        pe.cause.get match {
          case e: IllegalArgumentException => m.status(BAD_REQUEST)(e)
          case e: AssertionError => m.status(INTERNAL_SERVER_ERROR)(e)
          case _ => super.onError(request, ex)
        }
      case _ => super.onError(request, ex)
    }
  }
}
