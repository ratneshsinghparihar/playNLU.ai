package services

import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by ratnesh on 1/23/2017.
  */
object ReceptivitiService {

  def getPersonalityFromContent(content: String): Future[JsObject] = {

    var wsClient = NingWSClient()
    wsClient
      .url("https://app.receptiviti.com/v2/api/person/59108c71dc038c05b770f7d0/contents")
      //.withQueryString("some_parameter" -> "some_value", "some_other_parameter" -> "some_other_value")
      .withHeaders("X-API-SECRET-KEY" -> "jaTXnKJmpH82NVsGeJrdkVOA0nsp3rsaUSXQ0XU9eLI",
      "X-API-KEY" -> "59108aacfb488d0595282b38")
      .post(Json.obj(
        "language" -> "english",
        "content_source" -> 4,
        "language_content" -> content
      ))
      .map { wsResponse =>
        if (!(200 to 299).contains(wsResponse.status)) {
          sys.error(s"Received unexpected status ${wsResponse.status} : ${wsResponse.body}")
        }
        println(s"OK, received ${wsResponse.body}")
        println(s"The response header Content-Length was ${wsResponse.header("Content-Length")}")
        Json.obj("result" -> Json.parse((wsResponse.body)))
      }
  }

}
