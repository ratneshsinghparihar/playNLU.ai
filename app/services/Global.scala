import play.api._
import services.{rsBaseSparkSetup, rsNLPOpenIEService, rsNLPService}

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application has started")

    //rsNLPService.init()
  //  rsNLPOpenIEService.init()

    rsBaseSparkSetup.init()

  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
    rsBaseSparkSetup.close()
  }

}