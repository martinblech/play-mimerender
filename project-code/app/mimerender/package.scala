import scala.collection.JavaConversions._

import play.api.http.Writeable
import play.api.mvc.{Result, Results, Request}

package object mimerender {

  trait Mapping[A] {
    def typeStrings: Seq[String]
    def status(status: Int)(value: A)(implicit request: Request[Any]) = {
      val acceptHeader = request.headers.get("Accept")
      val typeString: Option[String] = acceptHeader match {
        case Some(s) => MIMEParse.bestMatch(typeStrings, s) match {
          case "" => None
          case s => Option(s)
        }
        case None => Some(typeStrings.head)
      }
      typeString.map(getResult(status, _)(value))
        .getOrElse(Results.NotAcceptable(""))
    }
    def getResult(status: Int, typeString: String): A => Result

    def ok(value: A)(implicit request: Request[Any]) = status(200)(value)
  }

  class SimpleMapping[A, B](val transform: (A => B),
      val writeable: Writeable[B]) extends Mapping[A] {

    override def typeStrings =
      writeable.contentType.map(_.split(';').head).toSeq

    override def getResult(status: Int, typeString: String) = { value: A =>
      Results.Status(status)(transform(value))(writeable)
    }
  }

  class CompositeMapping[A](val mappings: Mapping[A]*) extends Mapping[A] {
    val mappingsByTypeString = (for {
      mapping <- mappings
      typeString <- mapping.typeStrings
    } yield (typeString -> mapping)).toMap

    override def typeStrings = mappingsByTypeString.keys.toSeq

    override def getResult(status: Int, typeString: String) =
      mappingsByTypeString(typeString).getResult(status, typeString)
  }

  object Mapping {
    def apply[A, B](transform: (A => B))(implicit writeable: Writeable[B]) =
      new SimpleMapping(transform, writeable)
  }

}
