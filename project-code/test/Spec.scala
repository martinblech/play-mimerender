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
  // helpers
  val emptyRequest = FakeRequest()
  def requestWithAccept(accept: String) = FakeRequest(
    "GET", "/", FakeHeaders(Seq("Accept" -> Seq(accept))), "")

  // JSON Mapping with default typeString (provided by the implicit writeable)
  val jsonMapping = new SimpleMapping(None, { s: String =>
    Json.obj("value" -> s)
  })
  "a json mapping" should {
    val mapping = jsonMapping
    "have 'application/json' among the typeStrings" in {
      mapping.typeStrings must contain("application/json")
    }
    "take an empty accept header and produce a json response" in {
      implicit val request = emptyRequest
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/json")
      val parsedContent = Json.parse(contentAsString(result))
      (parsedContent \ "value").as[String] must be_==("hello")
    }
    "take 'application/json' and produce a json response" in {
      implicit val request = requestWithAccept("application/json")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/json")
    }
    "take '*/*' and produce a json response" in {
      implicit val request = requestWithAccept("*/*")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/json")
    }
    "fail with 'text/plain'" in {
      implicit val request = requestWithAccept("text/plain")
      val result = mapping.status(200)("hello")
      status(result) must be_==(NOT_ACCEPTABLE)
    }
  }

  // XML Mapping with explicit typeStrings
  val xmlMapping = new SimpleMapping(Some(Seq("application/xml", "text/xml")),
    { s: String => <root><value>{s}</value></root> })
  "a xml mapping" should {
    val mapping = xmlMapping
    "have 'application/xml' and 'text/xml' among the typeStrings" in {
      mapping.typeStrings must contain("application/xml")
      mapping.typeStrings must contain("text/xml")
    }
    "take an empty accept header and produce an application/xml response" in {
      implicit val request = emptyRequest
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/xml")
      val parsedContent = XML.loadString(contentAsString(result))
      (parsedContent \ "value").text must be_==("hello")
    }
    "take application/xml and produce an application/xml response" in {
      implicit val request = requestWithAccept("application/xml")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/xml")
    }
  }

  // composite JSON/XML mapping
  val compositeMapping = new CompositeMapping(jsonMapping, xmlMapping)
  "a composite json/xml mapping" should {
    val mapping = compositeMapping
    "take the first option (json) when accept header is empty" in {
      implicit val request = emptyRequest
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/json")
    }
    "take '*/*' and produce something" in {
      implicit val request = requestWithAccept("*/*")
      val result = mapping.status(200)("hello")
      status(result) must not be_==(NOT_ACCEPTABLE)
      contentType(result) must beSome
    }
    "take 'application/json' and produce a json response" in {
      implicit val request = requestWithAccept("application/json")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/json")
    }
    "take 'text/xml' and produce a xml response" in {
      implicit val request = requestWithAccept("text/xml")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("text/xml")
    }
    "take 'application/xml' and produce a xml response" in {
      implicit val request = requestWithAccept("application/xml")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/xml")
    }
    "take 'text/*' and produce a xml response" in {
      implicit val request = requestWithAccept("text/*")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("text/xml")
    }
    "take '*/json' and produce a json response" in {
      implicit val request = requestWithAccept("*/json")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/json")
    }
    "fail with 'text/plain'" in {
      implicit val request = requestWithAccept("text/plain")
      val result = mapping.status(200)("hello")
      status(result) must be_==(NOT_ACCEPTABLE)
    }
  }
}
