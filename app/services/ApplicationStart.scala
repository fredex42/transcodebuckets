package services
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import javax.inject._
import play.api.{Configuration, Logger}
import play.api.inject.ApplicationLifecycle

import scala.util.{Failure, Success}

@Singleton
class ApplicationStart @Inject()(lifecycle: ApplicationLifecycle, config:Configuration)(implicit actorSystem: ActorSystem){
  implicit val mat = ActorMaterializer()
  implicit val ec = actorSystem.dispatcher
  val logger = Logger(getClass)

  /* set up connection to message topic. If successful the register cleanup code*/
  config.getOptional[String]("transcoder.notificationTopic") match {
    case None=>
      logger.error("You must set transcoder.notificationTopic in the settings")
      lifecycle.stop()
    case Some(topicArn)=>
      logger.debug(s"topicArn is $topicArn")
      MessageHook.connect(topicArn).onComplete({
        case Success(subscriptionArn)=>
          logger.debug(s"Success, subscriptionArn is $subscriptionArn")
          lifecycle.addStopHook(()=> {
            MessageHook.disconnect(subscriptionArn)
            Future.successful()
          })

        case Failure(err)=>
          logger.error("Could not connect to notification topic", err)
          lifecycle.stop()
      })
  }

}
