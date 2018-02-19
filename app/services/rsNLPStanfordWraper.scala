package services

import java.util.Properties

import edu.stanford.nlp.ling.{CoreAnnotations, CoreLabel}
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{CleanXmlAnnotator, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import edu.stanford.nlp.simple.{Document, Sentence}
import edu.stanford.nlp.util.Quadruple
import org.apache.spark.sql.functions.udf

import scala.collection.JavaConverters._


object rsNLPStanfordWraper {

  @transient private var sentimentPipeline: StanfordCoreNLP = _


  private def getOrCreateSentimentPipeline(): StanfordCoreNLP = {
    if (sentimentPipeline == null) {
      val props = new Properties()
      props.setProperty("annotators", "tokenize, ssplit, parse, sentiment")
      sentimentPipeline = new StanfordCoreNLP(props)
    }
    sentimentPipeline
  }

  case class OpenIE(subject: String, relation: String, target: String, confidence: Double) {
    def this(quadruple: Quadruple[String, String, String, java.lang.Double]) =
      this(quadruple.first, quadruple.second, quadruple.third, quadruple.fourth)
  }

  private case class CorefMention(sentNum: Int, startIndex: Int, mention: String)

  private case class CorefChain(representative: String, mentions: Seq[CorefMention])

  private case class SemanticGraphEdge(
    source: String,
    sourceIndex: Int,
    relation: String,
    target: String,
    targetIndex: Int,
    weight: Double)


  def cleanxml = udf { document: String =>
    val words = new Sentence(document).words().asScala
    val labels = words.map { w =>
      val label = new CoreLabel()
      label.setWord(w)
      label
    }
    val annotator = new CleanXmlAnnotator()
    annotator.process(labels.asJava).asScala.map(_.word()).mkString(" ")
  }


  def tokenize = udf { sentence: String =>

    new Sentence(sentence).words().asScala
  }

  def ssplit = udf { document: String =>
    new Document(document).sentences().asScala.map(_.text())
  }


  def pos = udf { sentence: String =>
    new Sentence(sentence).posTags().asScala
  }


  def lemma = udf { sentence: String =>
    new Sentence(sentence).lemmas().asScala
  }


  def ner = udf { sentence: String =>
    new Sentence(sentence).nerTags().asScala
  }

  def parse = udf { sentence: String =>
    new Sentence(sentence).parse().asScala
  }


  def depparse = udf { sentence: String =>
    new Sentence(sentence).dependencyGraph().edgeListSorted().asScala.map { edge =>
      SemanticGraphEdge(
        edge.getSource.word(),
        edge.getSource.index(),
        edge.getRelation.toString,
        edge.getTarget.word(),
        edge.getTarget.index(),
        edge.getWeight)
    }
  }


  def coref = udf { document: String =>
    new Document(document).coref().asScala.values.map { chain =>
      val rep = chain.getRepresentativeMention.mentionSpan
      val mentions = chain.getMentionsInTextualOrder.asScala.map { m =>
        CorefMention(m.sentNum, m.startIndex, m.mentionSpan)
      }
      CorefChain(rep, mentions)
    }.toSeq
  }


  def natlog = udf { sentence: String =>
    new Sentence(sentence).natlogPolarities().asScala
        .map(_.toString)
  }


  def openie = udf { sentence: String =>
    new Sentence(sentence).openie().asScala.map(q => new OpenIE(q)).toSeq
  }


  def sentiment = udf { sentence: String =>
    val pipeline = getOrCreateSentimentPipeline()
    val annotation = pipeline.process(sentence)
    val tree = annotation.get(classOf[CoreAnnotations.SentencesAnnotation])
      .asScala
      .head
      .get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree])
    RNNCoreAnnotations.getPredictedClass(tree)
  }
}
