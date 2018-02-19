package models

/**
  * Created by ratnesh on 01-12-2016.
  */
case class topicModelInput(input:List[String],
                           noOfTopicsTobeDiscovered: Option[Int] = Some(5),
                           maxIterations: Option[Int] = Some(10),
                           docConcentration: Option[Double] = Some(-1),
                           topicConcentration: Option[Double] = Some(-1),
                           vocabSize: Option[Int] = Some(2900000),
                           stopwordFile: Option[String] = Some("stopWords.txt"),
                           algorithm: Option[String] = Some("em"),
                           checkpointDir: Option[String] = Some(""),
                           checkpointInterval: Option[Int] = Some(10),
                           maxTermsPerTopic:Option[Int]=Some(10),
                           stopWords: Option[String]=Some(""))

case class topicClassifyModelInput(input:Array[topicClassifyContentModelInput],trainedModel:topicModelfinalOutput)

case class topicClassifyContentModelInput(input:String,id:String)
