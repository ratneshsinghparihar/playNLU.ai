package services

import java.util.Properties

import edu.stanford.nlp.ie.util.RelationTriple
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.ling.CoreAnnotations.{NamedEntityTagAnnotation, PartOfSpeechAnnotation, TextAnnotation, TokensAnnotation}
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import edu.stanford.nlp.simple.Document
import edu.stanford.nlp.util.Quadruple
import models.{openIECollection, openIEOuputInner, openIEOutput}
import org.apache.spark.sql.Row
import services.Sentiment.Sentiment
import services.rsNLPStanfordWraper.OpenIE

import scala.collection.convert.wrapAll._
//import edu.stanford.nlp.co
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.simple;

/**
  * Created by ratnesh on 01-12-2016.
  */
object rsNLPOpenIEService {



  var pipeline: StanfordCoreNLP=null ;

  def init(): Unit = {
    val props = new Properties()
    props.put("annotators", "tokenize, ssplit, pos, lemma,ner,parse,depparse,sentiment,natlog,openie")
    props.put("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz")
    props.put("depparse.model", "edu/stanford/nlp/models/parser/nndep/english_SD.gz")
    pipeline = new StanfordCoreNLP(props)
  }


/*  def extractRelations(text: String):openIEOutput = {
    val annotation: Annotation = pipeline.process(text)
    val sentences = annotation.get(classOf[CoreAnnotations.SentencesAnnotation])
    return openIEOutput(
      sentences.map(sentence =>
                      openIECollection(
                          sentence.get(classOf[NaturalLogicAnnotations.RelationTriplesAnnotation]).map(triple =>
                            (openIEOuputInner(triple.confidence,triple.subjectLemmaGloss(),triple.relationLemmaGloss(),triple.objectLemmaGloss()))
                                ).toList
                      )
                  ).toList
    )
  }*/

/*
  def extractRelations2(text: String):openIEOutput = {

    var doc = new Document(text)
    return openIEOutput(
      doc.sentences().map(sentence =>

        (rsBaseSparkSetup.executeFunctionInBatch(rsNLPStanfordWraper.openie,sentence.toString).asInstanceOf[OpenIE])
      ).toList)
  }
*/

  def extractRelations3(text: String):openIEOutput = {

    var batchprocessObj=rsBaseSparkSetup.executeFunctionInBatch(rsNLPStanfordWraper.openie,text).asInstanceOf[Seq[Row]]

    return openIEOutput(


      batchprocessObj
          .map(row=> OpenIE(row.getString(0),row.getString(1),row.getString(2),row.getDouble(3)))
    )

  }

}


