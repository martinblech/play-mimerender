package mimerender

import play.api.http.{Writeable, ContentTypeOf}

object DSL {
  /** Build a mapping (main entry point for users). */
  def mapping[A](m: Mapping[A]*) = m match {
    case Seq(x) => x
    case m => new CompositeMapping(m)
  }

  /** Implicit conversion, from a A => B to a SimpleMapping[A, B]. */
  implicit def transformToMapping[A, B](transform: A => B)
      (implicit writeable: Writeable[B], contentTypeOf: ContentTypeOf[B]) =
    new SimpleMapping(None, transform)(writeable, contentTypeOf)

  /** Implicit conversion for custom typeString. */
  implicit def stringMappingPairToMapping[A, B](
      pair: (String, A => B))(
      implicit conv: (A => B) => SimpleMapping[A, B]) = {
    val (typeString, mapping) = pair
    mapping withCustomTypeString typeString
  }

  /** Implicit conversion for custom typeStrings. */
  implicit def stringSeqMappingPairToMapping[A, B](
      pair: (Seq[String], A => B))(
      implicit conv: (A => B) => SimpleMapping[A, B]) = {
    val (typeStrings, mapping) = pair
    mapping withCustomTypeStrings typeStrings
  }
}
