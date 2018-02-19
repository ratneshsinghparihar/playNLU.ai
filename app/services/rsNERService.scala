package services

import edu.stanford.nlp.ie.crf.CRFClassifier
import play.api.libs.json.{JsArray, JsString, JsValue, _}

/**
  * Created by ratnesh on 01-12-2016.
  */
object rsNERService {

  var serializedClassifier:String ="edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz"

  var classifier= CRFClassifier.getClassifierNoExceptions(serializedClassifier)

  def getNERClassification(contentText:String):String=
  {
   // var jsonObj:JsonNode =Json.parse("{\"PERSON\":\"[]\"},{\"ORGANIZATION\":\"[]\"},{\"LOCATION\":\"[]\"},{\"O\":\"[]\"}");

    var personJsArray:JsArray=new JsArray()
    var organizationJsArray:JsArray=new JsArray()
    var locationJsArray:JsArray=new JsArray()


    var xmlOutputStr= classifier.classifyToString("<root>"+contentText+"</root>","xml",true)

    var xmlObj=scala.xml.XML.loadString(xmlOutputStr)

    xmlObj.child.foreach( (child)=> {
      var attribute=child.attribute("entity")
      if(attribute.isDefined) {
        if (attribute.get.toString() == "PERSON") {
          personJsArray= personJsArray.append(JsString(child.text))
        }
        if (attribute.get.toString() == "ORGANIZATION") {
          organizationJsArray= organizationJsArray.append(JsString(child.text))
        }
        if (attribute.get.toString() == "LOCATION") {
          locationJsArray= locationJsArray.append(JsString(child.text))
        }
      }
    } )

    var jsonReturn:JsValue=Json.obj("persons" -> personJsArray,"organizations" -> organizationJsArray,"locations" -> locationJsArray )

    return jsonReturn.toString
  }


}
