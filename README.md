play-mimerender
===============

[![Build Status](https://secure.travis-ci.org/martinblech/play-mimerender.png)](http://travis-ci.org/martinblech/play-mimerender)

Play module for RESTful HTTP Content Negotiation. Lets you define a mapping from
your domain objects to different representations:

```scala
val m = mapping(
  "text/html" -> { s: String => views.html.index(s) },
  "application/xml" -> { s: String => <message>{s}</message> },
  "application/json" -> { s: String => toJson(Map("message" -> toJson(s))) },
  "text/plain" -> identity[String]_
)
```

You can then reuse that mapping in your controllers without polluting the domain
logic:

```scala
object Application extends Controller {
  def index = Action { implicit request =>
    m.status(200)("Hello, world!")
  }
}
```

and let `mimerender` take care of the rest:

```sh
$ # no accept header, gets the first representation (text/html)
$ curl -i localhost:9000
HTTP/1.1 200 OK
Content-Type: text/html
Vary: Accept
Content-Length: 130

<!doctype html>
<html>
  <head>
    <title>Hello, world!</title>
  </head>
  <body>
    <h1>Hello, world!</h1>
  </body>
</html>

$ # a simple text/plain accept header resolves to text/plain
$ curl -iH "Accept: text/plain" localhost:9000
HTTP/1.1 200 OK
Content-Type: text/plain
Vary: Accept
Content-Length: 13

Hello, world!

$ # */json;q=0.5,*/xml;q=1.0,*/*;q=0.1 resolves to application/xml
$ curl -iH "Accept: */json;q=0.5,*/xml;q=1.0,*/*;q=0.1" localhost:9000
HTTP/1.1 200 OK
Content-Type: application/xml
Vary: Accept
Content-Length: 32

<message>Hello, world!</message>

$ # application/octet-stream is not supported and it fails like it's supposed to
$ # (but you can override this)
$ curl -iH "Accept: application/octet-stream" localhost:9000
HTTP/1.1 406 Not Acceptable
Content-Type: text/plain; charset=utf-8
Vary: Accept
Content-Length: 150

None of the supported types (text/html, application/xml, application/json,
text/plain) is acceptable for the Acccept header 'application/octet-stream'
```

There's really not much more to it, except looking at the `samples` directory.
