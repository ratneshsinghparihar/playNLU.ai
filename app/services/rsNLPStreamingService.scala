package services

import java.util

import models._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.spark.sql.SparkSession
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
  * Created by ratnesh on 1/17/2017.
  */
object rsNLPStreamingService {

  var producer: KafkaProducer[String, String] = null

  implicit val nlpInputWrites = Json.writes[nlpInput]
  implicit val nlpInputReads = Json.reads[nlpInput]

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

  def init(): Unit = {
    println("-----------------NLP stream producer Initializing -------------------")

    try {
      val props = new util.HashMap[String, Object]()

      val config = play.Play.application.configuration

      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getString("kafka.bootstrap.servers"))
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringSerializer")
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringSerializer")

      producer = new KafkaProducer[String, String](props)
    }
    catch{
      case ex:Exception =>
        println("NLP stream producer Initialization Failed" + ex.toString)
    }

    println("-----------------NLP stream producer Initialization completed-------------------")
  }

  private def runProducer(inputMessage: String) = {

    if (producer == null) {
      init()
    }

    val config=play.Play.application.configuration
    val message = new ProducerRecord[String, String](config.getString("produce"), null, inputMessage)
    producer.send(message)

  }


  def close() {}

  def processAndProduceStream(input: String): String = {
    var nlpOutputString: String = ""
    try {
      println("in nlpStream consumer for " + input)
      val inputJson: JsValue = Json.parse(input)
      val nlpinput: nlpInput = inputJson.as[nlpInput]
      val nlpoutput: nlpOutputOne = rsNLPService.getAllNLPFromBatch(nlpinput)
      val nlpoutputJson = Json.toJson(nlpoutput)
      nlpOutputString = Json.stringify(nlpoutputJson)
      runProducer(nlpOutputString)
    }
    catch {
      case ex: Exception =>
        nlpOutputString = ex.getMessage
    }
    return nlpOutputString
  }


}
