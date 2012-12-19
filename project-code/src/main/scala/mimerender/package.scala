/** RESTful HTTP Content Negotiation for the Play! Framework. */
package object mimerender{
  val SHORT_MIME_MAP = Map(
    "xml"     -> Seq("application/xml", "text/xml", "application/x-xml"),
    "json"    -> Seq("application/json"),
    "jsonp"   -> Seq("application/javascript", "text/javascript"),
    "bson"    -> Seq("application/bson"),
    "msgpack" -> Seq("application/x-msgpack"),
    "yaml"    -> Seq("application/x-yaml", "text/x-yaml"),
    "xhtml"   -> Seq("application/xhtml+xml"),
    "html"    -> Seq("text/html"),
    "txt"     -> Seq("text/plain"),
    "csv"     -> Seq("text/csv"),
    "tsv"     -> Seq("text/tab-separated-values"),
    "rss"     -> Seq("application/rss+xml"),
    "rdf"     -> Seq("application/rdf+xml"),
    "atom"    -> Seq("application/atom+xml"),
    "ical"    -> Seq("text/calendar"),
    "vcard"   -> Seq("text/vcard"),
    "binary"  -> Seq("application/octet-stream")
  )
}
