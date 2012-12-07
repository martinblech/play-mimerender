import scala.collection.JavaConversions._

import play.api.http.Writeable
import play.api.mvc.{Result, Results, Request}

package object mimerender {

  trait Mapping[A] {
    def typeStrings: Seq[String]
    private lazy val defaultTypeString = typeStrings.reverse.head
    def status(status: Int)(value: A)(implicit request: Request[Any]) = {
      val acceptHeader = request.headers.get("Accept")
      val typeString: Option[String] = acceptHeader match {
        case Some(s) => MIMEParse.bestMatch(typeStrings, s) match {
          case "" => None
          case s => Option(s)
        }
        case None => Some(defaultTypeString)
      }
      typeString.map(getResult(status, _)(value))
        .getOrElse(Results.NotAcceptable("")) // TODO: some options for this
    }
    private[mimerender] def getResult(
      status: Int, typeString: String): A => Result
  }

  class SimpleMapping[A, B](
      customTypeStrings: Option[Seq[String]],
      transform: (A => B))
      (implicit writeable: Writeable[B]) extends Mapping[A] {

    override val typeStrings = customTypeStrings.getOrElse(
      writeable.contentType.map(_.split(';').head).toSeq).reverse

    require(!typeStrings.isEmpty)

    override def getResult(status: Int, typeString: String) = { value: A =>
      Results.Status(status)(transform(value))(writeable).as(typeString)
    }
  }

  class CompositeMapping[A](mappings: Mapping[A]*) extends Mapping[A] {
    private val typeStringMappingPairs = (for {
      mapping <- mappings
      typeString <- mapping.typeStrings
    } yield (typeString -> mapping))

    private val mappingsByTypeString = typeStringMappingPairs.toMap

    override val typeStrings = typeStringMappingPairs.map(_._1).reverse

    override def getResult(status: Int, typeString: String) =
      mappingsByTypeString(typeString).getResult(status, typeString)
  }

}
