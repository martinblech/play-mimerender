package mimerender

import play.api.http.{Writeable, ContentTypeOf}
import play.api.mvc.Request

object DSL {
  /** Build a mapping (main entry point for users). */
  def mapping[A](m: Mapping[A]*) = m match {
    case Seq(x) => x
    case m => new CompositeMapping(m)
  }

  /** Implicit conversion, for functions that don't care about Request */
  implicit def expandTransform[A, B](transform: A => B) =
    { (value: A, _: Request[Any]) => transform(value) }

  /** Implicit conversion, from a A => B to a SimpleMapping[A, B]. */
  implicit def transformToMapping[A, B, C[A, B]](transform: C[A, B])
      (implicit writeable: Writeable[B],
                contentTypeOf: ContentTypeOf[B],
                conv: C[A, B] => ((A, Request[Any]) => B)) =
    new SimpleMapping(None, transform)(writeable, contentTypeOf)

  /** Implicit conversion for custom typeString. */
  implicit def stringMappingPairToMapping[A, B, C[A, B]](
      pair: (String, C[A, B]))(
      implicit conv: C[A, B] => SimpleMapping[A, B]) = {
    val (typeString, mapping) = pair
    mapping withCustomTypeString typeString
  }

  /** Implicit conversion for custom typeStrings. */
  implicit def stringSeqMappingPairToMapping[A, B, C[A, B]](
      pair: (Seq[String], C[A, B]))(
      implicit conv: C[A, B] => SimpleMapping[A, B]) = {
    val (typeStrings, mapping) = pair
    mapping withCustomTypeStrings typeStrings
  }
}
