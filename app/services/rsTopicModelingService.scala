package services

/**
  * Created by ratnesh on 06-12-2016.
  */


import edu.stanford.nlp.process.Morphology
import edu.stanford.nlp.simple.Document
import models._

import scala.collection.JavaConversions._
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.{CountVectorizer, CountVectorizerModel, RegexTokenizer, StopWordsRemover}
import org.apache.spark.ml.linalg.{Vector => MLVector}
import org.apache.spark.mllib.clustering.{LocalLDAModel, _}
import org.apache.spark.mllib.linalg.{DenseVector, Vector, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, SparkSession}

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import services.rsBaseSparkSetup

object rsTopicModelingService {


  def getTopics(input: topicModelInput): topicModelfinalOutput = {
    if (input.input.isEmpty)
      return topicModelfinalOutput(List[topicModelOutput](),"",0,0,0,0,0)
    return run(input)
  }

  def classifyTopics(inputModel: topicClassifyModelInput): Array[topicModelClassifyAll] = {
    inputModel.input.map(topicInput => topicModelClassifyAll(topicInput.id, classifyTopics(topicInput, inputModel.trainedModel))).toList.toArray
  }

  def classifyTopicsUsingModel(inputModel: topicClassifyModelInput): Array[topicModelClassifyAllWithModel] = {
    var sc: SparkContext = rsBaseSparkSetup.sc
    if (sc == null) {
      rsBaseSparkSetup.close()
      rsBaseSparkSetup.init()
      sc = rsBaseSparkSetup.sc
    }
    //load model

    var samelocalModel:LocalLDAModel=null
    try {    samelocalModel=DistributedLDAModel.load(sc, inputModel.trainedModel.trainingModelId).toLocal}catch {case e:Exception=>{ println(s"Not a distributed model :" + inputModel.trainedModel.trainingModelId)}}
    if(samelocalModel==null) {
      samelocalModel = LocalLDAModel.load(sc, inputModel.trainedModel.trainingModelId)
    }
    val finalOutput=inputModel.input.map(topicInput =>
      topicModelClassifyAllWithModel(topicInput.id,classifyTopicsUsingModel(sc,samelocalModel ,topicInput, inputModel.trainedModel))
    ).toList.toArray
    samelocalModel=null
    finalOutput
  }

  private def classifyTopicsUsingModel(sc: SparkContext,samelocalModel:LocalLDAModel,inputModel: topicClassifyContentModelInput,
                                       trainedModel: topicModelfinalOutput) : List[topicModelClassifyInternalModel]= {
    val allClassifiedTerms = ListBuffer[termModelClassify]()
    val inputs:ArrayBuffer[String]= new ArrayBuffer[String]()
    val returnOutput=new ListBuffer[topicModelClassifyInternalModel]
    //validation for input
    if(inputModel!=null && samelocalModel!=null) {

      val preprocessStart = System.nanoTime()
      inputs.append(inputModel.input)

      val (corpus, vocabArray, actualNumTokens) =
        preprocess(sc, inputs.toList, samelocalModel.vocabSize,"stopWords.txt","" )
      corpus.cache()
      val actualCorpusSize = corpus.count()
      val actualVocabSize = vocabArray.length
      val preprocessElapsed = (System.nanoTime() - preprocessStart) / 1e9


      //build

      println()
      println(s"Corpus summary:")
      println(s"\t Training set size: $actualCorpusSize documents")
      println(s"\t Vocabulary size: $actualVocabSize terms")
      println(s"\t Training set size: $actualNumTokens tokens")
      println(s"\t Preprocessing time: $preprocessElapsed sec")
      println()
      //predict using topicDistributions
      val actualPredictions =samelocalModel.topicDistributions(corpus).collect()

      if(actualPredictions!=null && actualPredictions(0)!=null )
      {
        val topicDistribution=actualPredictions(0)
        val output =actualPredictions(0)._2.asInstanceOf[DenseVector].values.zipWithIndex.map
              {case(value,index)=>
                                  trainedModel.topicsCollected.map(topicmodel =>
                                                                                {
                                                                                  if(topicmodel.index==index)
                                                                                    { returnOutput.append(topicModelClassifyInternalModel(topicmodel,value)) }
                                                                                } ) }

      }



      //tranform the ouput of topic distrubution using map
      //return trained model


    }
    return returnOutput.toList

  }

  private def classifyTopics(inputModel: topicClassifyContentModelInput, trainedModel: topicModelfinalOutput): List[termModelClassify] = {

    //log the initTime

    //pick model from modelkey
    val allTerms = trainedModel.getAllTerms()
    val allClassifiedTerms = ListBuffer[termModelClassify]()

    //clean the content

    //preprocessing te content

    //create lda instance using model and cleared content

    //call lda.classify

    allTerms.foreach((term: termModelClassify) => {
      if (inputModel.input.contains(term.termName)) {
        allClassifiedTerms.append(term)
      }
    })

    //log the completion time
    /*for (elem <- allClassifiedTerms) {
      println(elem.confidenceScore.toString + "" + elem.termName  )
    }*/
    return allClassifiedTerms.toList
  }


  private def createSparkContext(): SparkContext = {
    val conf: SparkConf = new SparkConf().setAppName("RSNLPAnalysis")
      .setMaster("local[2]").set("spark.executor.memory", "1g")
      //.setMaster("spark://tsipl-arch1:7077").set("spark.executor.memory","2g")
      .set("spark.driver.memory", "1g")
      .setSparkHome("SPARK_HOME")
    return new SparkContext(conf)
  }

  private def run(params: topicModelInput): topicModelfinalOutput = {

    var sc: SparkContext = rsBaseSparkSetup.sc
    if (sc == null) {
      rsBaseSparkSetup.close()
      rsBaseSparkSetup.init()
      sc = rsBaseSparkSetup.sc
    }


    //Logger.getRootLogger.setLevel(Level.WARN)

    // Load documents, and prepare them for LDA.
    val preprocessStart = System.nanoTime()
    val (corpus, vocabArray, actualNumTokens) =
      preprocess(sc, params.input, params.vocabSize.get, params.stopwordFile.get,params.stopWords.get)
    corpus.cache()
    val actualCorpusSize = corpus.count()
    val actualVocabSize = vocabArray.length
    val preprocessElapsed = (System.nanoTime() - preprocessStart) / 1e9

    println()
    println(s"Corpus summary:")
    println(s"\t Training set size: $actualCorpusSize documents")
    println(s"\t Vocabulary size: $actualVocabSize terms")
    println(s"\t Training set size: $actualNumTokens tokens")
    println(s"\t Preprocessing time: $preprocessElapsed sec")
    println()

    // Run LDA.
    val lda = new LDA()

    var miniBatchFraction=0.05 + 1.0 / actualCorpusSize
    if(miniBatchFraction >1){miniBatchFraction=1 }
    val optimizer = params.algorithm.get.toLowerCase match {
      case "em" => new EMLDAOptimizer
      // add (1.0 / actualCorpusSize) to MiniBatchFraction be more robust on tiny datasets.
      case "online" => new OnlineLDAOptimizer().setMiniBatchFraction(miniBatchFraction)
      case _ => throw new IllegalArgumentException(
        s"Only em, online are supported but got ${params.algorithm}.")
    }

    lda.setOptimizer(optimizer)
      .setK(params.noOfTopicsTobeDiscovered.get)
      .setMaxIterations(params.maxIterations.get)
      .setDocConcentration(params.docConcentration.get)
      .setTopicConcentration(params.topicConcentration.get)
    //.setCheckpointInterval(params.checkpointInterval.get)
    /*if (params.checkpointDir.get.nonEmpty) {
      sc.setCheckpointDir(params.checkpointDir.get)
    }*/
    val startTime = System.nanoTime()
    val ldaModel = lda.run(corpus)
    val guidForNewModel=java.util.UUID.randomUUID().toString();

    ldaModel.save(sc,guidForNewModel)
    val elapsed = (System.nanoTime() - startTime) / 1e9

    println(s"Finished training LDA model.  Summary:")
    println(s"\t Training time: $elapsed sec")
    var avgLogLikelihood:Double=0
    if (ldaModel.isInstanceOf[DistributedLDAModel]) {
      val distLDAModel = ldaModel.asInstanceOf[DistributedLDAModel]
       avgLogLikelihood = distLDAModel.logLikelihood / actualCorpusSize.toDouble
      println(s"\t Training data average log likelihood: $avgLogLikelihood")
      println()
    }



    // Print the topics, showing the top-weighted terms for each topic.
    val topicIndices = ldaModel.describeTopics(maxTermsPerTopic = params.maxTermsPerTopic.get)
    val topics = topicIndices.map { case (terms, termWeights) =>
      terms.zip(termWeights).map { case (term, weight) => (vocabArray(term.toInt), weight) }
    }

    val topicModelOutputs: ListBuffer[topicModelOutput] = ListBuffer[topicModelOutput]()

    println(s"${params.noOfTopicsTobeDiscovered} topics:")
    topics.zipWithIndex.foreach { case (topic, i) =>
      val cutermList: ListBuffer[termModel] = ListBuffer[termModel]()

      println(s"TOPIC $i")
      topic.foreach { case (term, weight) =>
        println(s"$term\t$weight")
        var curTermModel = termModel(termName = term, confidenceScore = weight)
        cutermList.append(curTermModel)
      }
      println()
      val curtopicModelOutput: topicModelOutput = topicModelOutput("TOPIC $i", cutermList.toList, "",i)
      topicModelOutputs.append(curtopicModelOutput)
    }
    //    sc.stop()
    //    sc=null
    return topicModelfinalOutput(topicModelOutputs.toList,guidForNewModel,
      actualCorpusSize,actualVocabSize,actualNumTokens,preprocessElapsed,avgLogLikelihood)
  }


  /**
    * Load documents, tokenize them, create vocabulary, and prepare documents as term count vectors.
    *
    * @return (corpus, vocabulary as array, total token count in corpus)
    */
  private def preprocess(
                          sc: SparkContext,
                          content: List[String],
                          vocabSize: Int,
                          stopwordFile: String,stopWords:String): (RDD[(Long, Vector)], Array[String], Long) = {

    val spark = SparkSession
      .builder
      .getOrCreate()
    import spark.implicits._

    // Get dataset of document texts
    // One document per line in each text file. If the input consists of many small files,
    // this can result in a large number of small partitions, which can degrade performance.
    // In this case, consider using coalesce() to create fewer, larger partitions.

    val initialrdd = sc.parallelize(content, content.size)
    //val initialrdd = spark.sparkContext.wholeTextFiles("docs").map(_._2)
    initialrdd.cache()

    val rdd = initialrdd.mapPartitions { partition =>
      val morphology = new Morphology()
      partition.map { value =>
        LDAHelper.getLemmaText(value, morphology)
      }
    }.map(LDAHelper.filterSpecialCharacters)
    rdd.cache()
    initialrdd.unpersist()
    val df = rdd.toDF("docs")

    val stopWordText = sc.textFile(stopwordFile).collect()
    stopWordText.flatMap(_.stripMargin.split(","))

    val customizedStopWords: Array[String] = if (stopwordFile.isEmpty) {
      Array.empty[String]
    } else {
      val stopWordText = sc.textFile(stopwordFile).collect()
      stopWordText.flatMap(_.stripMargin.split(","))
    }

    val tokenizer = new RegexTokenizer()
      .setInputCol("docs")
      .setOutputCol("rawTokens")
    val stopWordsRemover = new StopWordsRemover()
      .setInputCol("rawTokens")
      .setOutputCol("tokens")
    stopWordsRemover.setStopWords(stopWordsRemover.getStopWords ++ customizedStopWords ++ stopWords.split(","))
    val countVectorizer = new CountVectorizer()
      .setVocabSize(vocabSize)
      .setInputCol("tokens")
      .setOutputCol("features")

    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, stopWordsRemover, countVectorizer))

    val model = pipeline.fit(df)
    val documents = model.transform(df)
      .select("features")
      .rdd
      .map { case Row(features: MLVector) => Vectors.fromML(features) }
      .zipWithIndex()
      .map(_.swap)

    (documents,
      model.stages(2).asInstanceOf[CountVectorizerModel].vocabulary, // vocabulary
      documents.map(_._2.numActives).sum().toLong) // total token count
  }




}


object LDAHelper {

  def filterSpecialCharacters(document: String) = document.replaceAll("""[! @ # $ % ^ & * ( ) _ + - − , " ' ; : . ` ? -- / \\]""", " ")

  def getStemmedText(document: String) = {
    val morphology = new Morphology()
    new Document(document).sentences().toList.flatMap(_.words().toList.map(morphology.stem)).mkString(" ")
  }

  def getLemmaText(document: String, morphology: Morphology) = {
    val string = new StringBuilder()
    val value = new Document(document).sentences().toList.flatMap { a =>
      val words = a.words().toList
      val tags = a.posTags().toList
      (words zip tags).toMap.map { a =>
        val newWord = morphology.lemma(a._1, a._2)
        val addedWoed = if (newWord.length > 3) {
          newWord
        } else {
          ""
        }
        string.append(addedWoed + " ")
      }
    }
    string.toString()
  }
}