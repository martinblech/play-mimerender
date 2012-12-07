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
  "the SimpleMapping constructor" should {
    "use the implicit Writeable's typeString by default" in {
      val mapping = new SimpleMapping(None, { _: Any => <root/> })
      mapping.typeStrings must be_==(Seq("text/xml"))
    }
    "override the Writeable's typeString when needed" in {
      val mapping = new SimpleMapping(Some(Seq("application/xml")),
        { _: Any => <root/> })
      mapping.typeStrings must be_==(Seq("application/xml"))
    }
    "fail when the Writeable has no typeString" in {
      new SimpleMapping(None, { _: Any => Array[Byte]() })(
        new Writeable(identity, None)
      ) must throwA[IllegalArgumentException]
    }
    "fail when the explicit typeStrings is empty" in {
      new SimpleMapping(Some(Nil),
        { _: Any => <root/> }) must throwA[IllegalArgumentException]
    }
  }
  // request helpers
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
    "take an empty accept and produce application/xml (first option)" in {
      implicit val request = emptyRequest
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/xml")
      val parsedContent = XML.loadString(contentAsString(result))
      (parsedContent \ "value").text must be_==("hello")
    }
    "take */* and produce application/xml (first option)" in {
      implicit val request = requestWithAccept("*/*")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/xml")
    }
    "take text/* and produce text/xml" in {
      implicit val request = requestWithAccept("text/*")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("text/xml")
    }
    "take application/xml and produce application/xml" in {
      implicit val request = requestWithAccept("application/xml")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/xml")
    }
    "fail with 'application/octet-stream'" in {
      implicit val request = requestWithAccept("application/octet-stream")
      val result = mapping.status(200)("hello")
      status(result) must be_==(NOT_ACCEPTABLE)
    }
  }

  // TXT Mapping with default typeString (provided by the implicit writeable)
  val txtMapping = new SimpleMapping(None, identity[String])
  "a txt mapping" should {
    val mapping = txtMapping
    "have 'text/plain' among the typeStrings" in {
      mapping.typeStrings must contain("text/plain")
    }
    "take an empty accept header and produce an text/plain response" in {
      implicit val request = emptyRequest
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("text/plain")
      contentAsString(result) must be_==("hello")
    }
    "take text/plain and produce an text/plain response" in {
      implicit val request = requestWithAccept("text/plain")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("text/plain")
    }
    "fail with 'application/octet-stream'" in {
      implicit val request = requestWithAccept("application/octet-stream")
      val result = mapping.status(200)("hello")
      status(result) must be_==(NOT_ACCEPTABLE)
    }
  }

  // HTML Mapping with default typeString (provided by the implicit writeable)
  val htmlMapping = new SimpleMapping(None, { s: String =>
    new play.api.templates.Html("<html><body>" + s + "</body></html>")
  })
  "a html mapping" should {
    val mapping = htmlMapping
    "have 'text/html' among the typeStrings" in {
      mapping.typeStrings must contain("text/html")
    }
    "take an empty accept header and produce an text/html response" in {
      implicit val request = emptyRequest
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("text/html")
      contentAsString(result) must be_==("<html><body>hello</body></html>")
    }
    "take text/plain and produce an text/html response" in {
      implicit val request = requestWithAccept("text/html")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("text/html")
    }
    "fail with 'application/octet-stream'" in {
      implicit val request = requestWithAccept("application/octet-stream")
      val result = mapping.status(200)("hello")
      status(result) must be_==(NOT_ACCEPTABLE)
    }
  }

  // composite JSON/XML/TXT/HTML mapping
  val compositeMapping = new CompositeMapping(
    jsonMapping, xmlMapping, txtMapping, htmlMapping)
  "a composite json/xml mapping" should {
    val mapping = compositeMapping
    "choose the first option (json) when accept header is empty" in {
      implicit val request = emptyRequest
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/json")
    }
    "choose the first option (json) when given '*/*'" in {
      implicit val request = requestWithAccept("*/*")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/json")
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
    "take '*/plain' and produce text/plain" in {
      implicit val request = requestWithAccept("*/plain")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("text/plain")
    }
    "take '*/html' and produce text/html" in {
      implicit val request = requestWithAccept("*/html")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("text/html")
    }
    "fail with 'application/octet-stream'" in {
      implicit val request = requestWithAccept("application/octet-stream")
      val result = mapping.status(200)("hello")
      status(result) must be_==(NOT_ACCEPTABLE)
    }
  }
}
