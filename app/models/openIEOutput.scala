package models

import org.apache.spark.sql.Row
import services.rsNLPStanfordWraper.OpenIE

/**
  * Created by ratnesh on 01-12-2016.
  */
case class openIEOutput(ouput:Seq[OpenIE])

case class openIECollection(matrices:List[OpenIE])

case class openIEOuputInner(confidence:Double,subjectLemmaGloss:String,relationLemmaGloss:String,objectLemmaGloss:String)
//relations
//quotes
//topics
//entity mentions
//opne information