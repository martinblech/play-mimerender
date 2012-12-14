package mimerender

import scala.collection.JavaConversions._

import play.api.http.{Writeable, ContentTypeOf}
import play.api.mvc.{PlainResult, Results, Request}

import mimeparse.Matcher

/** Abstract mapping superclass. Takes care of extracting the Accept header
 * from the HTTP request and selecting the most appropriate  representation 
 * among the supported typeStrings. Delegates the actual transformation from
 * A to the best content type to its subclasses.
 */
trait Mapping[A] {

  /** Get the supported types for this mapping */
  def typeStrings: Seq[String]

  /** Get the default type that will be used when the Accept header is absent
   */
  lazy val defaultTypeString = typeStrings.head

  /** Create a Result with the given HTTP status, where the body contains
   * the best available representation for the A value. Returns a '406
   * Not Acceptable' when the Accept header does not match any of the
   * available types. */
  def status(status: Int)(value: A)(implicit request: Request[Any]) = {
    val acceptHeader = request.headers.get("Accept")
    val typeString: Option[String] = acceptHeader match {
      case Some(acceptHeader) => bestMatch(acceptHeader)
      case None => Some(defaultTypeString)
    }
    typeString.map(getResult(status, _)(value))
      .getOrElse(Results.NotAcceptable(buildNotAcceptableBody(
        acceptHeader.get)))
      .withHeaders("Vary" -> "Accept")
  }

  /** Actual result creation, implemented by subclasses. */
  def getResult(status: Int, typeString: String)(value: A): PlainResult

  private lazy val mimeMatcher = new Matcher(typeStrings)

  /** Find the best match among the supported type strings for the given
   * accept header. */
  def bestMatch(acceptHeader: String): Option[String] =
    mimeMatcher.bestMatch(acceptHeader)

  /** Construct the text/plain body for a 406 result. */
  def buildNotAcceptableBody(acceptHeader: String): String =
    "None of the supported types (" + typeStrings.mkString(", ") + 
    ") is acceptable for the Acccept header '" + acceptHeader + "'"

  /** Get a new mapping that falls back to the default type instead of
   * faililng with 406. */
  def notAcceptableFallback: Mapping[A] =
    new NotAcceptableFallbackWrapper(this)

  /** Get a new mapping that is able to build a custom body for 406 results.
   * The build function takes two arguments, the Accept string and the list
   * of supported types for this mapping, and must return a String with the
   * error body. */
  def notAcceptableBody(build: (String, Seq[String]) => String): Mapping[A] =
    new NotAcceptableBodyWrapper(this, build)
}

/** Wraps a mapping instance and delegates method calls to it. */
private class MappingWrapper[A](wrapped: Mapping[A]) extends Mapping[A] {
  override def typeStrings = wrapped.typeStrings
  override def getResult(status: Int, typeString: String)(value: A) =
    wrapped.getResult(status, typeString)(value)
}

/** Wraps a mapping and falls back to the default type instead of failing
 * with 406. */
private class NotAcceptableFallbackWrapper[A](wrapped: Mapping[A])
    extends MappingWrapper[A](wrapped) {

  /** Gets the wrapped mapping's best match, or else the default type string
   * (but never None). */
  override def bestMatch(acceptHeader: String) =
    wrapped.bestMatch(acceptHeader).orElse(Some(defaultTypeString))
}

/** Wraps a mapping and uses the given build function to create a custom
 * body for 406 errors. */
private class NotAcceptableBodyWrapper[A](wrapped: Mapping[A],
    build: (String, Seq[String]) => String)
    extends MappingWrapper[A](wrapped) {

  /** Build the 406 body using the build function. */
  override def buildNotAcceptableBody(acceptHeader: String) =
    build(acceptHeader, typeStrings)
}

/** Mapping that is only able to create a single representation type. The
 * transform function converts an A to a B, which in turn ends up being
 * converted to an Array[Byte] by the implicit writeable. customTypeStrings
 * is an optional list of typeStrings, useful for cases where there are
 * several equivalent strings (e.g. text/xml and application/xml). */
class SimpleMapping[A, B](
    customTypeStrings: Option[Seq[String]],
    transform: (A => B))
    (implicit writeable: Writeable[B],
              contentTypeOf: ContentTypeOf[B]) extends Mapping[A] {

  /** Either the customTypeStrings or else the writeable's typeString. */
  override val typeStrings = customTypeStrings.getOrElse(
    contentTypeOf.mimeType.map(_.split(';').head).toSeq)

  /** Get a result where the body is value transformed by the transform
   * function and the content type is the given typeString. */
  override def getResult(status: Int, typeString: String)(value: A) = 
    Results.Status(status)(transform(value))(writeable, contentTypeOf).as(typeString)

  /** Create a new SimpleMapping that has the given typeString as its sole
   * customTypeString. */
  def withCustomTypeString(typeString: String) =
    new SimpleMapping(Some(Seq(typeString)), transform)(writeable,
      contentTypeOf)

  /** Create a new SimpleMapping that has the given typeStrings as
   * customTypeStrings. */
  def withCustomTypeStrings(typeStrings: Seq[String]) =
    new SimpleMapping(Some(typeStrings), transform)(writeable, contentTypeOf)
}

/** Mapping that is able to delegate to the appropriate sub-mapping for a
 * given typeString. */
class CompositeMapping[A](mappings: Seq[Mapping[A]]) extends Mapping[A] {
  require(!mappings.isEmpty, "need at least one mapping")
  private val typeStringMappingPairs = (for {
    mapping <- mappings
    typeString <- mapping.typeStrings
  } yield (typeString -> mapping))

  private val mappingsByTypeString = typeStringMappingPairs.toMap

  /** All the sub-mappings' typeStrings concatenated */
  override val typeStrings = typeStringMappingPairs.map(_._1)

  /** Find the sub-mapping for the given typeString and delegate. */
  override def getResult(status: Int, typeString: String)(value: A) =
    mappingsByTypeString(typeString).getResult(status, typeString)(value)
}
