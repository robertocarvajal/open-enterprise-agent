package io.iohk.atala.agent.server.http

import zio._
import zio.http.{Header => _, *}
import io.iohk.atala.mercury._

object ZioHttpClient {
  val layer = ZLayer.succeed(new ZioHttpClient())
}

class ZioHttpClient extends HttpClient {

  override def get(url: String): Task[HttpResponse] =
    zio.http.Client
      .request(Request(url = URL(Path(url))))
      .provideSomeLayer(zio.http.Client.default)
      .provideSomeLayer(zio.Scope.default)
      .flatMap { response =>
        response.headers.toSeq.map(e => e)
        response.body.asString
          .map(body =>
            HttpResponse(
              response.status.code,
              response.headers.map(h => Header(h.headerName, h.renderedValue)).toSeq,
              body
            )
          )
      }

  def postDIDComm(url: String, data: String): Task[HttpResponse] =
    for {
      url <- ZIO.succeed(URL.decode(url).getOrElse(URL(path = Path(url))))
      _ <- ZIO.logInfo(s"URL => $url")
      response <- zio.http.Client
        .request(
          Request(
            url = url, // TODO make ERROR type
            method = Method.POST,
            headers = Headers("content-type" -> "application/didcomm-encrypted+json"),
            // headers = Headers("content-type" -> MediaTypes.contentTypeEncrypted),
            body = Body.fromChunk(Chunk.fromArray(data.getBytes))
            // ssl = ClientSSLOptions.DefaultSSL,)
          )
        )
        .provideSomeLayer(zio.http.Client.default)
        .provideSomeLayer(zio.Scope.default)
        .flatMap { response =>
          response.headers.toSeq.map(e => e)
          response.body.asString
            .map(body =>
              HttpResponse(
                response.status.code,
                response.headers.map(h => Header(h.headerName, h.renderedValue)).toSeq,
                body
              )
            )
        }
    } yield response
}
