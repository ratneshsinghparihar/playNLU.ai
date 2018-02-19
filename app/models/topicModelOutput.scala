package models

import scala.collection.mutable.ListBuffer

/**
  * Created by ratnesh on 01-12-2016.
  */
case class topicModelOutput(topicName:String,terms:List[termModel],topicId:String,index:Int )

case class termModel(termName:String,confidenceScore:Double)

case class termModelClassify(termName:String,confidenceScore:Double,topic:String,topicId:String)

case class topicModelClassifyAll(id:String,result:List[termModelClassify])

case class topicModelClassifyAllWithModel(id:String,result:List[topicModelClassifyInternalModel])
case class topicModelClassifyInternalModel(topicModel:topicModelOutput,confidenceScore:Double)

case class topicModelfinalOutput(topicsCollected:List[topicModelOutput],trainingModelId:String,actualCorpusSize:Double,
                                 actualVocabSize:Double,actualNumTokens:Double,preprocessElapsed:Double,avgLogLikelihood:Double ){
  def getAllTerms(): List[termModelClassify] =
  {
    var allterms=ListBuffer[termModelClassify]()
    topicsCollected.foreach((topic) =>
    {
      allterms.appendAll(topic.terms.map( term => termModelClassify(term.termName,term.confidenceScore,topic.topicName,topic.topicId) ))
    })
      return allterms.toList
  }
}