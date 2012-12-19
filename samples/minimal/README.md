Minimal sample application for mimerender
=========================================

Run and try with cURL:

```sh
$ curl -i "localhost:9000"
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

$ curl -iH "Accept: application/json;q=1.0,text/plain;q=0.9" "localhost:9000"
HTTP/1.1 200 OK
Content-Type: application/json
Vary: Accept
Content-Length: 27

{"message":"Hello, world!"}

$ curl -i "localhost:9000/?format=txt"
HTTP/1.1 200 OK
Content-Type: text/plain
Vary: Accept
Content-Length: 13

Hello, world!

$ curl -iH "Accept: application/javascript" "localhost:9000"
HTTP/1.1 200 OK
Content-Type: application/javascript
Vary: Accept
Content-Length: 38

callback({"message":"Hello, world!"});

$ curl -i "localhost:9000/?format=jsonp&callback=myFunction"
HTTP/1.1 200 OK
Content-Type: application/javascript
Vary: Accept
Content-Length: 40

myFunction({"message":"Hello, world!"});
```
