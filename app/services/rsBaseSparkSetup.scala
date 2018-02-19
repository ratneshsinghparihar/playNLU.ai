package services





import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions._
import org.apache.spark.{SparkConf, SparkContext}

import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.SparkSession


import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.reflect.runtime.universe.TypeTag
import scala.concurrent.ExecutionContext.Implicits.global

object rsBaseSparkSetup {
  @transient var sc: SparkContext = _
  @transient var sqlContext: SQLContext = _

  def init(): Unit = {

    val config=play.Play.application.configuration

    val spark = SparkSession
      .builder
      .appName(config.getString("sparkAppName"))
      .config("spark.master", config.getString("spark.master"))
      .config("spark.executor.memory", config.getString("spark.executor.memory"))
      .config("spark.driver.memory", config.getString("spark.driver.memory"))
      .config("spark.sql.warehouse.dir", config.getString("spark.sql.warehouse.dir"))
      .config("spark.home", config.getString("spark.home"))
      .getOrCreate()


    sc = spark.sparkContext

    sqlContext = SQLContext.getOrCreate(sc)


    val futures = new ListBuffer[Future[Boolean]]
    val f: Future[Boolean] = runStreamWatcher(spark)
    futures.append(f)

    Future.sequence(futures)


  }


  def runStreamWatcher(spark: SparkSession): Future[Boolean] = {
    Future(runConsumer(spark))
  }


  private def runConsumer(spark: SparkSession): Boolean = {
    println("-----------------NLP stream consumer Initializing -------------------")
    try {
      import spark.implicits._
      val config = play.Play.application.configuration
      val streams = spark
        .readStream
        .format("kafka")
        .option("kafka.bootstrap.servers", config.getString("kafka.bootstrap.servers"))
        .option("subscribe", config.getString("subscribe"))
        .option("startingOffsets", config.getString("startingOffsets"))
        .load()
        .selectExpr("CAST(value AS STRING)")
        .as[String]


      val nlpOutput = streams.map(input => processAndProduceStream(input).asInstanceOf[String]
      )


      val query = nlpOutput.writeStream
        .outputMode("append")
        .format("console")
        .start()

      println("-----------------NLP stream consumer initialized  waiting streams-------------------")

      query.awaitTermination()
    }
    catch {
      case ex:Exception =>
        println("nlp stream consumer failed to initalize" + ex.toString)
    }

    return true
  }

  private def processAndProduceStream(input:String):String ={
  rsNLPStreamingService.processAndProduceStream(input)
}

  def close(): Unit = {
    sc.stop()
    sc = null
    sqlContext = null
  }

  def executeFunctionInBatch[T: TypeTag](function: UserDefinedFunction, input: T): Any = {
    val df = sqlContext.createDataFrame(Seq((0, input))).toDF("id", "input")
    val returnobj= df.select(function(col("input"))).first().get(0)
    return returnobj
  }

  def executeFunctionsInBatch[T: TypeTag](function1: UserDefinedFunction,
                                            function2: UserDefinedFunction,
                                            function3: UserDefinedFunction,
                                            function4: UserDefinedFunction,
                                            input: T): GenericRowWithSchema = {
    val df = sqlContext.createDataFrame(Seq((0, input))).toDF("id", "input")
    val curObj=df.select(function1(col("input")),function2(col("input")),function3(col("input")),function4(col("input"))).first()

     curObj.asInstanceOf[GenericRowWithSchema]
  }

}
