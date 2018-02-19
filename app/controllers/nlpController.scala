package controllers

import models._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc.{Action, BodyParsers, Controller}
import services.rsNLPStanfordWraper.OpenIE
import services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
/**
  * Created by ratnesh on 02-12-2016.
  */
class nlpController extends Controller {


  object JsonExtensions {
    def withDefaultCreatedAt[T](base: Format[T]) = new Format[T]{
      def reads(json: JsValue): JsResult[T] = base.compose(JsonExtensions.withDefault("createdAt", DateTime.now())).reads(json)
      def writes(o: T): JsValue = base.writes(o)
    }
    def withDefault[A](key:String, default:A)(implicit writes:Writes[A]) =
      __.json.update((__ \ key).json.copyFrom((__ \ key).json.pick orElse Reads.pure(Json.toJson(default))))
  }

  implicit val nerInputWrites = Json.writes[nerInput]
  implicit val nerInputReads = Json.reads[nerInput]

  implicit val nlpInputWrites = Json.writes[nlpInput]
  implicit val nlpInputReads = Json.reads[nlpInput]


  implicit val sentimentInputWrites = Json.writes[sentimentInput]
  implicit val sentimentInputReads = Json.reads[sentimentInput]

  implicit val topicModeInputWrites = Json.writes[topicModelInput]
  implicit val topicModelFmt = JsonExtensions.withDefaultCreatedAt(Json.format[topicModelInput])
  implicit val topicModelInputReads = Json.reads[topicModelInput]

  implicit val termModelInputWrites = Json.writes[termModel]
  implicit val termModelInputReads = Json.reads[termModel]



  implicit val termModelClassifyInputWrites = Json.writes[termModelClassify]
  implicit val termModelClassifyInputReads = Json.reads[termModelClassify]




  implicit val topicModelOutputWrites = Json.writes[topicModelOutput]
  implicit val topicModelOutputReads = Json.reads[topicModelOutput]

  implicit val topicModelfinalOutputWrites = Json.writes[topicModelfinalOutput]
  implicit val topicModelfinalOutputReads = Json.reads[topicModelfinalOutput]


  implicit val topicModelClassifyAllWrites = Json.writes[topicModelClassifyAll]
  implicit val topicModelClassifyAllReads = Json.reads[topicModelClassifyAll]



  //topicClassifyModelInput

  implicit val topicClassifyContentModelInputWrites = Json.writes[topicClassifyContentModelInput]
  implicit val topicClassifyContentModelInputReads = Json.reads[topicClassifyContentModelInput]

  implicit val topicClassifyModelInputWrites = Json.writes[topicClassifyModelInput]
  implicit val topicClassifyModelInputReads = Json.reads[topicClassifyModelInput]

  implicit val OpenIEWrites = Json.writes[OpenIE]
  implicit val OpenIEReads = Json.reads[OpenIE]

  implicit val openIEOuputInnerWrites = Json.writes[openIEOuputInner]
  implicit val openIEOuputInnerReads = Json.reads[openIEOuputInner]

  implicit val openIEOutputWrites = Json.writes[openIEOutput]
  implicit val openIEOutputReads = Json.reads[openIEOutput]

  implicit val openIECollectionWrites = Json.writes[openIECollection]
  implicit val openIECollectionReads = Json.reads[openIECollection]

  implicit val nlpOutputInnerWrites = Json.writes[nlpOutputInner]
  implicit val nlpOutputInnerReads = Json.reads[nlpOutputInner]

  implicit val nlpOutputWrites = Json.writes[nlpOutput]
  implicit val nlpOutputReads = Json.reads[nlpOutput]





  implicit val nlpOutputSentimentWrites = Json.writes[nlpOutputSentiment]
  implicit val nlpOutputSentimentReads = Json.reads[nlpOutputSentiment]

  implicit val nlpOutputPOSWrites = Json.writes[nlpOutputPOS]
  implicit val nlpOutputPOSReads = Json.reads[nlpOutputPOS]

  implicit val nlpOutputNERWrites = Json.writes[nlpOutputNER]
  implicit val nlpOutputNERReads = Json.reads[nlpOutputNER]

  implicit val nlpOutputOneWrites = Json.writes[nlpOutputOne]
  implicit val nlpOutputOneReads = Json.reads[nlpOutputOne]


  implicit val topicModelClassifyInternalModelWrites = Json.writes[topicModelClassifyInternalModel]
  implicit val topicModelClassifyInternalModelReads = Json.reads[topicModelClassifyInternalModel]

  implicit val topicModelClassifyAllWithModelWrites = Json.writes[topicModelClassifyAllWithModel]
  implicit val topicModelClassifyAllWithModelReads = Json.reads[topicModelClassifyAllWithModel]






  def getNERClassification = Action(BodyParsers.parse.json) { request =>
    val b = request.body.validate[nerInput]
    b.fold(
      errors => {
        BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toFlatJson(errors)))
      },
      input => {
        val result=rsNERService.getNERClassification(input.content)
        Ok(result)
      }
    )
  }

  def getSentiment = Action(BodyParsers.parse.json) { request =>
    val b = request.body.validate[sentimentInput]
    b.fold(
      errors => {
        BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toFlatJson(errors)))
      },
      input => {
        val result=rsNLPService.getSentimentFromBatch(input.content)
        Ok(result.toString())
      }
    )
  }

  def getPOSFromBatch = Action(BodyParsers.parse.json) { request =>
    val b = request.body.validate[sentimentInput]
    b.fold(
      errors => {
        BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toFlatJson(errors)))
      },
      input => {
        val result=rsNLPService.getPOSFromBatch(input.content)
        val data = Json.obj(
          "result" -> result
        )
        Ok(data)
      }
    )
  }

  def getTextAnalysis= Action(BodyParsers.parse.json) { request =>

    val b = request.body.validate[sentimentInput]
    b.fold(
      errors => {
        BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toFlatJson(errors)))
      },
      input => {

        val result=rsNLPService.getNLPFromBatch(input.content)
        val data = Json.obj(
          "content" -> result.content,
          "Sentiment" -> result.sentiment,
          "pos" -> result.pos,
          "ner"-> result.ner,
          "token" -> result.tokens
        )
        Ok(data)
      }
    )
  }

  def getTextAnalysisALL= Action(BodyParsers.parse.json) { request =>

    val b = request.body.validate[nlpInput]
    b.fold(
      errors => {
        BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toFlatJson(errors)))
      },
      input => {

        val result=rsNLPService.getAllNLPFromBatch(input)
        val data = Json.obj(
          "result" -> result
        )
        Ok(data)
      }
    )
  }

  def getAllNLPFromBatch= Action(BodyParsers.parse.json) { request =>

    val b = request.body.validate[Array[nlpInput]]
    b.fold(
      errors => {
        BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toFlatJson(errors)))
      },
      input => {

        val result=rsNLPService.getAllNLPFromBatch(input)
        val data = Json.obj(
          "result" -> result
        )
        Ok(data)
      }
    )
  }


  def getTopics = Action(BodyParsers.parse.json) { request =>
    val b = request.body.validate[topicModelInput]
    b.fold(
      errors => {
        BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toFlatJson(errors)))
      },
      input => {
        val result=rsTopicModelingService.getTopics(input)
        Ok(Json.toJson(result))
      }
    )
  }

  def searchTopics = Action(BodyParsers.parse.json) { request =>
    val b = request.body.validate[topicClassifyModelInput]
    b.fold(
      errors => {
        BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toFlatJson(errors)))
      },
      input => {
        val result=rsTopicModelingService.classifyTopics(input)
        //println(Json.toJson(result))
        Ok(Json.toJson(result))
      }
    )
  }

  def classifyTopics = Action(BodyParsers.parse.json) { request =>
    val b = request.body.validate[topicClassifyModelInput]
    b.fold(
      errors => {
        BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toFlatJson(errors)))
      },
      input => {
        val result=rsTopicModelingService.classifyTopics(input)
        //println(Json.toJson(result))
        Ok(Json.toJson(result))
      }
    )
  }


  def classifyTopicsWithModel = Action(BodyParsers.parse.json) { request =>
    val b = request.body.validate[topicClassifyModelInput]
    b.fold(
      errors => {
        BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toFlatJson(errors)))
      },
      input => {
        val result=rsTopicModelingService.classifyTopicsUsingModel(input)
        //println(Json.toJson(result))
        Ok(Json.toJson(result))
      }
    )
  }


  def getOpenIEInformation = Action(BodyParsers.parse.json) { request =>
    val b = request.body.validate[nerInput]
    b.fold(
      errors => {
        BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toFlatJson(errors)))
      },
      input => {
        val result=rsNLPOpenIEService.extractRelations3(input.content)
        Ok(Json.toJson(result))
      }
    )
  }

  def getPersonalityFromContent = Action.async(BodyParsers.parse.json) { request =>
    val b = request.body.validate[String]
    b.fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" -> "OK", "message" -> JsError.toFlatJson(errors))))
      },
      input => {
        val result=ReceptivitiService.getPersonalityFromContent(input)
        result.map( res => Ok(Json.toJson(res)))
      }
    )
  }
}
