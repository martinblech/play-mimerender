package mimerender

import play.api.test._
import play.api.test.Helpers._
import play.api.mvc.Result
import play.api.http.Writeable
import play.api.libs.json._
import play.api.libs.json.Json._
import org.specs2.mutable._
import scala.xml._

import mimerender.DSL._

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
  }
  // request helpers
  val emptyRequest = FakeRequest()
  def requestWithAccept(accept: String) = FakeRequest(
    "GET", "/", FakeHeaders(Map("Accept" -> Seq(accept))), "")

  // JSON Mapping with default typeString (provided by the implicit writeable)
  val jsonMapping = new SimpleMapping(None, { s: String =>
    toJson(Map("message" -> toJson(s)))
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
      (parsedContent \ "message").as[String] must be_==("hello")
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
      header("Vary", result).get must contain("Accept")
    }
    "have Accept within the Vary header" in {
      implicit val request = requestWithAccept("*/*")
      val result = mapping.status(200)("hello")
      header("Vary", result).get must contain("Accept")
    }
  }

  "a json mapping with notAcceptableFallback" should {
    val mapping = jsonMapping.notAcceptableFallback

    "produce a json response even with a 'text/plain' accept" in {
      implicit val request = requestWithAccept("text/plain")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/json")
      val parsedContent = Json.parse(contentAsString(result))
      (parsedContent \ "message").asOpt[String] must beSome("hello")
    }
  }

  "a json mapping with a custom notAcceptableBody" should {
    val mapping = jsonMapping notAcceptableBody { (acceptString, typeStrings) =>
      "bad: " + acceptString + " supported: " + typeStrings.mkString(", ")
    }

    "produce a custom not acceptable body for a 'text/x-whatever' accept" in {
      implicit val request = requestWithAccept("text/x-whatever")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("text/plain")
      status(result) must be_==(NOT_ACCEPTABLE)
      contentAsString(result) must be_==(
        "bad: text/x-whatever supported: application/json")
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
  val compositeMapping = new CompositeMapping(Seq(
    jsonMapping, xmlMapping, txtMapping, htmlMapping))
  "a composite json/xml/txt/html mapping" should {
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
    "resolve 'text/html,application/xml;q=0.9,*/*;q=0.8' to text/html" in {
      implicit val request = requestWithAccept(
        "text/html,application/xml;q=0.9,*/*;q=0.8")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("text/html")
    }
    "resolve 'text/html;q=0.8,application/xml;q=0.9' to application/xml" in {
      implicit val request = requestWithAccept(
        "text/html;q=0.8,application/xml;q=0.9")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/xml")
    }
    "resolve 'x/y,z/w;q=0.9,*/*;q=0.8' to application/json" in {
      implicit val request = requestWithAccept("x/y,z/w;q=0.9,*/*;q=0.8")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/json")
    }
    "fail with 'application/octet-stream'" in {
      implicit val request = requestWithAccept("application/octet-stream")
      val result = mapping.status(200)("hello")
      status(result) must be_==(NOT_ACCEPTABLE)
    }
    "have Accept within the Vary header" in {
      implicit val request = requestWithAccept("*/*")
      val result = mapping.status(200)("hello")
      header("Vary", result).get must contain("Accept")
    }
  }

  "a composite json/xml/txt/html mapping with notAcceptableFallback" should {
    val mapping = compositeMapping.notAcceptableFallback

    "produce a json response even with a 'application/x-whatever' accept" in {
      implicit val request = requestWithAccept("application/x-whatever")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("application/json")
      val parsedContent = Json.parse(contentAsString(result))
      (parsedContent \ "message").as[String] must be_==("hello")
    }
  }

  "a composite mapping with a custom notAcceptableBody" should {
    val mapping = compositeMapping notAcceptableBody {
      (acceptString, typeStrings) =>
        "bad: " + acceptString + " supported: " + typeStrings.mkString(", ")
    }

    "produce a custom not acceptable body for a 'text/x-whatever' accept" in {
      implicit val request = requestWithAccept("text/x-whatever")
      val result = mapping.status(200)("hello")
      contentType(result) must beSome("text/plain")
      status(result) must be_==(NOT_ACCEPTABLE)
      contentAsString(result) must be_==(
        "bad: text/x-whatever supported: application/json, application/xml, " +
        "text/xml, text/plain, text/html")
    }
  }

  "the mapping constructor DSL" should {
    "fail with empty args" in {
      mapping() should throwA[IllegalArgumentException]
    }
    "construct a simple mapping" in {
      val m = mapping (
        {s: String => <root><message>{s}</message></root>}
      )
      m must beAnInstanceOf[SimpleMapping[String, scala.xml.NodeSeq]]
      m.typeStrings must contain("text/xml")
    }
    "construct a mapping with one custom typeString" in {
      val m = mapping (
        "application/xml" -> {s: String => <root><message>{s}</message></root>}
      )
      m must beAnInstanceOf[SimpleMapping[String, scala.xml.NodeSeq]]
      m.typeStrings must contain("application/xml")
    }
    "construct a mapping with custom typeStrings" in {
      val m = mapping (
        Seq("application/xml", "text/xml") ->
          {s: String => <root><message>{s}</message></root>}
      )
      m must beAnInstanceOf[SimpleMapping[String, scala.xml.NodeSeq]]
      m.typeStrings must contain("application/xml")
      m.typeStrings must contain("text/xml")
    }
    "construct a composite mapping" in {
      val m = mapping (
        "application/xml" -> {s: String => <root><message>{s}</message></root>},
        "application/json" -> {s: String => toJson(Map("message" -> toJson(s)))}
      )
      m must beAnInstanceOf[CompositeMapping[String]]
      m.typeStrings must contain("application/json")
      m.typeStrings must contain("application/xml")
    }
  }
}
