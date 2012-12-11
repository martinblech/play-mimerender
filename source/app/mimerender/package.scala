import scala.collection.JavaConversions._

import play.api.http.Writeable
import play.api.mvc.{Result, Results, Request}

package object mimerender {

  trait Mapping[A] {
    def typeStrings: Seq[String]
    private[mimerender] lazy val defaultTypeString = typeStrings.head
    def status(status: Int)(value: A)(implicit request: Request[Any]) = {
      val acceptHeader = request.headers.get("Accept")
      val typeString: Option[String] = acceptHeader match {
        case Some(acceptHeader) => bestMatch(acceptHeader)
        case None => Some(defaultTypeString)
      }
      typeString.map(getResult(status, _)(value))
        .getOrElse(Results.NotAcceptable(notAcceptableBody(acceptHeader.get)))
    }
    private[mimerender] def getResult(
      status: Int, typeString: String): A => Result
    private[mimerender] def bestMatch(acceptHeader: String): Option[String] =
      MIMEParse.bestMatch(typeStrings.reverse, acceptHeader) match {
        case "" => None
        case s => Option(s)
      }
    private[mimerender] def notAcceptableBody(acceptHeader: String): String =
      "None of the supported types (" + typeStrings mkString ", " + 
      ") is acceptable for the Acccept header '" + acceptHeader + "'"
    def notAcceptableFallback = new NotAcceptableFallbackWrapper(this)
    def notAcceptableBody(build: (String, Seq[String]) => String) =
      new NotAcceptableBodyWrapper(this, build)
  }

  class NotAcceptableFallbackWrapper[A](wrapped: Mapping[A])
      extends Mapping[A] {
    override def typeStrings = wrapped.typeStrings
    override def getResult(status: Int, typeString: String) =
      wrapped.getResult(status, typeString)
    override def bestMatch(acceptHeader: String) =
      wrapped.bestMatch(acceptHeader).orElse(Some(defaultTypeString))
  }

  class NotAcceptableBodyWrapper[A](wrapped: Mapping[A],
      build: (String, Seq[String]) => String)
      extends Mapping[A] {
    override def typeStrings = wrapped.typeStrings
    override def getResult(status: Int, typeString: String) =
      wrapped.getResult(status, typeString)
    override def bestMatch(acceptHeader: String) =
      wrapped.bestMatch(acceptHeader)
    override def notAcceptableBody(acceptHeader: String) =
      build(acceptHeader, typeStrings)
  }

  class SimpleMapping[A, B](
      customTypeStrings: Option[Seq[String]],
      transform: (A => B))
      (implicit writeable: Writeable[B]) extends Mapping[A] {

    override val typeStrings = customTypeStrings.getOrElse(
      writeable.contentType.map(_.split(';').head).toSeq)

    override def getResult(status: Int, typeString: String) = { value: A =>
      Results.Status(status)(transform(value))(writeable).as(typeString)
        .withHeaders("Vary" -> "Accept")
    }

    def -: (typeString: String) =
      new SimpleMapping(Some(Seq(typeString)), transform)(writeable)

    def -: (typeStrings: Seq[String]) =
      new SimpleMapping(Some(typeStrings), transform)(writeable)
  }

  class CompositeMapping[A](mappings: Seq[Mapping[A]]) extends Mapping[A] {
    private val typeStringMappingPairs = (for {
      mapping <- mappings
      typeString <- mapping.typeStrings
    } yield (typeString -> mapping))

    private val mappingsByTypeString = typeStringMappingPairs.toMap

    override val typeStrings = typeStringMappingPairs.map(_._1)

    override def getResult(status: Int, typeString: String) =
      mappingsByTypeString(typeString).getResult(status, typeString)
  }

  def mapping[A](m: Mapping[A]*) = m match {
    case Seq(x) => x
    case m => new CompositeMapping(m)
  }

  implicit def transformToMapping[A, B](transform: A => B)
      (implicit writeable: Writeable[B]) =
    new SimpleMapping(None, transform)(writeable)

  implicit def transformAndWritableToMapping[A, B](
      pair: (A => B, Writeable[B])) =
    new SimpleMapping(None, pair._1)(pair._2)

}
