package services

import java.util.Properties

import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation
import edu.stanford.nlp.hcoref.CorefCoreAnnotations.CorefMentionsAnnotation
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations.RelationMentionsAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.{NamedEntityTagAnnotation, PartOfSpeechAnnotation, TextAnnotation, TokensAnnotation}
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations
import org.apache.spark.sql.Row
import services.Sentiment.Sentiment
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import edu.stanford.nlp.ie.util.RelationTriple
import edu.stanford.nlp.simple.Document
import models._
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema

import scala.collection.convert.wrapAll._
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
//import edu.stanford.nlp.co

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ratnesh on 01-12-2016.
  */
object rsNLPService {



  var pipeline: StanfordCoreNLP=null ;

  def init(): Unit = {
    val props = new Properties()
    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, depparse,sentiment,,mention,coref")
    pipeline = new StanfordCoreNLP(props)
  }
  /**
    * Extracts the main sentiment for a given input
    */
  def mainSentiment(input: String): Sentiment = Option(input) match {
    case Some(text) if text.nonEmpty => extractSentiment(text)
    case _ => throw new IllegalArgumentException("input can't be null or empty")
  }


  def getOpenInformation(input:String):Any={
    // Create a CoreNLP document
    var doc:Document = new Document(input);

    return doc.sentences().map(sentence => (
          sentence.openieTriples().map(triple => (triple.confidence,triple.subjectLemmaGloss(),triple.relationLemmaGloss(),triple.objectLemmaGloss()  ))
      )).toList

  }

  /**
    * Extracts a list of sentiments for a given input
    */
  def sentiment(input: String): List[(String, Sentiment,Any)] = Option(input) match {
    case Some(text) if text.nonEmpty => extractSentiments(text)
    case _ => throw new IllegalArgumentException("input can't be null or empty")
  }

  private def extractSentiment(text: String): Sentiment = {
    val (_, sentiment,token) = extractSentiments(text)
      .maxBy { case (sentence, sentiment,token) => sentence.length }
    sentiment
  }

  def extractSentiments(text: String): List[(String, Sentiment,Any)] = {
    if(pipeline==null){
      init()
    }
    val annotation: Annotation = pipeline.process(text)
    val sentences = annotation.get(classOf[CoreAnnotations.SentencesAnnotation])
    sentences
      .map(sentence => (sentence,
                        sentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree]),
                        sentence.get(classOf[TokensAnnotation]).map(token => (
                                                                            //  token.get(classOf[TextAnnotation]),
                                                                              token.get(classOf[PartOfSpeechAnnotation])
                                                                            //  token.get(classOf[NamedEntityTagAnnotation]),
                                                                            //  token.get(classOf[CorefChainAnnotation]),
                                                                            //  token.get(classOf[CorefMentionsAnnotation]),
                                                                            //  token.get(classOf[RelationMentionsAnnotation])
                                                                              )
                                                                    )
                        )
          )
      .map { case (sentence, tree,token ) =>
                            (sentence.toString, Sentiment.toSentiment(RNNCoreAnnotations.getPredictedClass(tree)),token) }
      .toList
  }

  def getSentimentFromBatch(text:String):Sentiment={
    var batchprocessObj=rsBaseSparkSetup.executeFunctionInBatch(rsNLPStanfordWraper.sentiment,text).asInstanceOf[Int]
    Sentiment.toSentiment(batchprocessObj)
  }

  def getPOSFromBatch(text:String):Array[String]={
    var batchprocessObj=rsBaseSparkSetup.executeFunctionInBatch(rsNLPStanfordWraper.pos,text).asInstanceOf[Seq[String]]
    return batchprocessObj.toArray
  }

  def getNERFromBatch(text:String):Array[String]={
    var batchprocessObj=rsBaseSparkSetup.executeFunctionInBatch(rsNLPStanfordWraper.ner,text).asInstanceOf[Seq[String]]
    return batchprocessObj.toArray
  }

  def getTokenFromBatch(text:String):Array[String]={
    var batchprocessObj=rsBaseSparkSetup.executeFunctionInBatch(rsNLPStanfordWraper.tokenize,text).asInstanceOf[Seq[String]]
    return batchprocessObj.toArray
  }

  def getNLPFromBatch(text:String):nlpOutput={
    nlpOutput(text,getSentimentFromBatch(text).toString,
      getPOSFromBatch(text),
      getNERFromBatch(text),
      getTokenFromBatch(text))
  }

  var counter:Integer=0

  private def getNLPOutput(input:nlpInput,sentiment: Int,pos:Array[String],ner:Array[String],tokens:Array[String]): nlpOutputOne ={

    val curSentiment:nlpOutputSentiment=nlpOutputSentiment(Sentiment.toSentiment(sentiment).toString,sentiment)
    val curPOS:ListBuffer[nlpOutputPOS]=ListBuffer[nlpOutputPOS]()
    val curNER:ListBuffer[nlpOutputNER]=ListBuffer[nlpOutputNER]()

    pos.toList.filter (_.length > 0).sortWith (_ > _).distinct.foreach( uniquePos=> curPOS.append(nlpOutputPOS(uniquePos,ArrayBuffer[String]())) )

    ner.toList.filter (_.length > 0).sortWith (_ > _).distinct.foreach( uniqueNer=> curNER.append(nlpOutputNER(uniqueNer,ArrayBuffer[String]())) )

    var currentTokenIndex=0;

    tokens.foreach(token => {
      val posFOrCurIndex=pos(currentTokenIndex)
      val posFOrNERIndex=ner(currentTokenIndex)

      curPOS.filter( _.pos == posFOrCurIndex )(0).terms.append(token)

      curNER.filter( _.ner == posFOrNERIndex )(0).terms.append(token)

      currentTokenIndex=currentTokenIndex+1 })

    nlpOutputOne(input.content,input.id,curSentiment,curPOS.toArray,curNER.toArray)
  }

  def getAllNLPFromBatch(input:nlpInput):nlpOutputOne={
    counter=counter+1
    val localId=counter
    println("in getAllNLPFromBatch" + localId.toString())

    val returnObj:GenericRowWithSchema=rsBaseSparkSetup.executeFunctionsInBatch(rsNLPStanfordWraper.sentiment,
                                                          rsNLPStanfordWraper.pos,
                                                          rsNLPStanfordWraper.ner,
      rsNLPStanfordWraper.tokenize,input.content)

    println("out getAllNLPFromBatch" + localId.toString())



    getNLPOutput(input, returnObj.get(0).asInstanceOf[Int] ,
      returnObj.get(1).asInstanceOf[Seq[String]].toArray,
      returnObj.get(2).asInstanceOf[Seq[String]].toArray,
      returnObj.get(3).asInstanceOf[Seq[String]].toArray)

  }

  def getAllNLPFromBatch(text:Array[nlpInput]):Array[nlpOutputOne]= {
  val finalResult:ListBuffer[nlpOutputOne]=ListBuffer[nlpOutputOne]()
    val futures = new ListBuffer[Future[nlpOutputOne]]
    text.foreach(input => {
      val f:Future[nlpOutputOne]=getAllNLPFromBatchAsyn(input)
      futures.append(f)
      f onSuccess{ case  output:nlpOutputOne => finalResult.append(output) }
    })

    Await.ready(Future.sequence(futures), Duration.Inf)

    return finalResult.toArray
  }


  private def getAllNLPFromBatchAsyn(input:nlpInput):Future[nlpOutputOne]={
    return Future(getAllNLPFromBatch(input))
  }

}



object Sentiment extends Enumeration {
  type Sentiment = Value
  val POSITIVE, NEGATIVE, NEUTRAL = Value

  def toSentiment(sentiment: Int): Sentiment = sentiment match {
    case x if x == 0 || x == 1 => Sentiment.NEGATIVE
    case 2 => Sentiment.NEUTRAL
    case x if x == 3 || x == 4 => Sentiment.POSITIVE
  }

}
