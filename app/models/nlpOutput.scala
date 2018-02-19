package models

import scala.collection.mutable.ArrayBuffer

/**
  * Created by ratnesh on 01-12-2016.
  */
case class nlpOutput(content:String, sentiment:String,pos:Array[String],
                     ner:Array[String],tokens:Array[String])

case class nlpOutputOne(content:String,id:String, sentiment:nlpOutputSentiment,pos:Array[nlpOutputPOS],
                     ner:Array[nlpOutputNER])

case class nlpOutputSentiment(sentiment:String,score:Int)

case class nlpOutputPOS(pos:String,terms:ArrayBuffer[String])

case class nlpOutputNER(ner:String,terms:ArrayBuffer[String])

case class nlpOutputInner(pos:Array[String])
//relations
//quotes
//topics
//entity mentions
//opne information