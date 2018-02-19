import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

trait BaseSparkSetupForTest extends FunSuite with BeforeAndAfterAll {
  @transient var sc: SparkContext = _
  @transient var sqlContext: SQLContext = _

  override def beforeAll(): Unit = {
   /* sc = SparkContext.getOrCreate(
      new SparkConf()
        .setMaster("local[2]")
        .setAppName(this.getClass.getSimpleName)
    )*/

    sc = SparkContext.getOrCreate(
      new SparkConf().setAppName("RSNLPAnalysis")
        .setMaster("local[2]").set("spark.executor.memory","1g")
        //.setMaster("spark://tsipl-arch1:7077").set("spark.executor.memory","2g")
        .set("spark.driver.memory","1g")
        .set("spark.sql.warehouse.dir", "D:/sparkCoreNLP/spark-corenlp/spark-warehouse")
        .setSparkHome("SPARK_HOME")
    )


    sqlContext = SQLContext.getOrCreate(sc)
  }

  override def afterAll(): Unit = {
    sc.stop()
    sc = null
    sqlContext = null
  }
}
