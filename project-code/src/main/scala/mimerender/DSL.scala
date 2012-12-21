package mimerender

import play.api.http.{Writeable, ContentTypeOf}
import play.api.mvc.RequestHeader

object DSL {
  /** Build a mapping (main entry point for users). */
  def mapping[A](m: Mapping[A]*) = m match {
    case Seq(x) => x
    case m => new CompositeMapping(m)
  }

  implicit def f1ToMapping[A, B: Writeable: ContentTypeOf](f1: A => B):
      SimpleMapping[A, B] =
    new SimpleMapping(None, (v: A, _: RequestHeader) => f1(v))

  implicit def f2ToMapping[A, B: Writeable: ContentTypeOf](
    f2: (A, RequestHeader) => B) = new SimpleMapping(None, f2)

  implicit def stringf1ToMapping[A, B](pair: (String, A => B))(
      implicit ev: (A => B) => SimpleMapping[A, B]) = {
    val (contentType, mapping) = pair
    mapping withCustomTypeStrings Seq(contentType)
  }

  implicit def stringf2ToMapping[A, B](pair: (String, (A, RequestHeader) => B))(
      implicit ev: ((A, RequestHeader) => B) => SimpleMapping[A, B]) = {
    val (contentType, mapping) = pair
    mapping withCustomTypeStrings Seq(contentType)
  }

  implicit def stringsf1ToMapping[A, B](pair: (Seq[String], A => B))(
      implicit ev: (A => B) => SimpleMapping[A, B]) = {
    val (contentTypes, mapping) = pair
    mapping withCustomTypeStrings contentTypes
  }

  implicit def stringsf2ToMapping[A, B](
      pair: (Seq[String], (A, RequestHeader) => B))(
      implicit ev: ((A, RequestHeader) => B) => SimpleMapping[A, B]) = {
    val (contentTypes, mapping) = pair
    mapping withCustomTypeStrings contentTypes
  }
}
