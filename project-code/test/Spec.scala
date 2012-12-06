package test

import play.api.test._
import play.api.test.Helpers._
import play.api.mvc.Result
import play.api.http.Writeable
import play.api.libs.json._
import org.specs2.mutable._
import scala.xml._
import mimerender._

class MappingSpec extends Specification {
  "a json mapping" should {
    val mapping = Mapping { s: String => new JsObject("value" -> JsString(s) :: Nil) }
    "have 'application/json' among the typeStrings" in {
      mapping.typeStrings must contain("application/json")
    }
    "take an empty accept header and produce a json response" in {
      implicit val request = FakeRequest()
      val result = mapping.ok("hello")
      contentType(result) must beSome("application/json")
      val parsedContent = Json.parse(contentAsString(result))
      (parsedContent \ "value").as[String] must be_==("hello")
    }
    "take 'application/json' and produce a json response" in {
      implicit val request = FakeRequest(
        "GET", "/", FakeHeaders(Seq(
          "Accept" -> Seq("application/json"))), "")
      val result = mapping.ok("hello")
      contentType(result) must beSome("application/json")
    }
    "take '*/*' and produce a json response" in {
      implicit val request = FakeRequest(
        "GET", "/", FakeHeaders(Seq(
          "Accept" -> Seq("*/*"))), "")
      val result = mapping.ok("hello")
      contentType(result) must beSome("application/json")
    }
    "fail with 'text/plain'" in {
      implicit val request = FakeRequest(
        "GET", "/", FakeHeaders(Seq(
          "Accept" -> Seq("text/plain"))), "")
      val result = mapping.ok("hello")
      status(result) must be_==(NOT_ACCEPTABLE)
    }
  }
  "a xml mapping" should {
    val mapping = Mapping { s: String => <root><value>{s}</value></root> }
    "have 'text/xml' among the typeStrings" in {
      mapping.typeStrings must contain("text/xml")
    }
    "take an empty accept header and produce a xml response" in {
      implicit val request = FakeRequest()
      val result = mapping.ok("hello")
      contentType(result) must beSome("text/xml")
      val parsedContent = XML.loadString(contentAsString(result))
      (parsedContent \ "value").text must be_==("hello")
    }
  }
  "a composite json/xml mapping" should {
    val mapping = new CompositeMapping(
      Mapping { s: String => Json.obj("value" -> s) },
      Mapping { s: String => <root><value>{s}</value></root> }
    )
    "take the first option (json) when accept header is empty" in {
      implicit val request = FakeRequest()
      val result = mapping.ok("hello")
      contentType(result) must beSome("application/json")
    }
    "take 'application/json' and produce a json response" in {
      implicit val request = FakeRequest(
        "GET", "/", FakeHeaders(Seq(
          "Accept" -> Seq("application/json"))), "")
      val result = mapping.ok("hello")
      contentType(result) must beSome("application/json")
    }
    "take 'text/xml' and produce a xml response" in {
      implicit val request = FakeRequest(
        "GET", "/", FakeHeaders(Seq(
          "Accept" -> Seq("text/xml"))), "")
      val result = mapping.ok("hello")
      contentType(result) must beSome("text/xml")
    }
  }
}
