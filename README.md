play-mimerender
===============

[![Build Status](https://secure.travis-ci.org/martinblech/play-mimerender.png)](http://travis-ci.org/martinblech/play-mimerender)

Play module for RESTful HTTP Content Negotiation. Lets you define a mapping from
your domain objects to different representations:

```scala
def jsonTransform(s: String) = toJson(Map("message" -> toJson(s)))
def jsonpTransform(s: String, r: Request[Any]) = {
  val callback = r.queryString.getOrElse("callback", Nil).headOption
    .getOrElse("callback")
  Jsonp(callback, jsonTransform(s))
}
val m = mapping(
  "text/html" -> { s: String => views.html.index(s) },
  "application/xml" -> { s: String => <message>{s}</message> },
  "application/json" -> jsonTransform _,
  "text/javascript" -> jsonpTransform _,
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

$ # text/javascript produces jsonp with a user-specified callback
$ curl -iH "Accept: text/javascript" "localhost:9000/?callback=callMe"
HTTP/1.1 200 OK
Content-Type: text/javascript
Vary: Accept
Content-Length: 36

callMe({"message":"Hello, world!"});
```

There's really not much more to it, except looking at the `samples` directory.

Installation
------------

Just add the dependency and the resolver in your `Build.scala`:

```scala
val appDependencies = Seq(
  "mimerender" %% "mimerender" % "0.1.1"
)

val main = PlayProject(appName, appVersion, appDependencies).settings(
  resolvers +=
    Resolver.url("mimerender github repo",
      url("http://martinblech.github.com/play-mimerender/releases")
    )(Resolver.ivyStylePatterns)
)
```

History
-------

`play-mimerender` is a loose Play! 2 port of the Python
[mimerender](http://github.com/martinblech/play-mimerender) module.

License
-------

See `LICENSE`
