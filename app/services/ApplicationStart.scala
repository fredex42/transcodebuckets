package services
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import helpers.TranscoderSetup

import scala.concurrent.Future
import javax.inject._
import models.{InjectableGlobalServerState, SNSMessage}
import play.api.{Configuration, Logger}
import play.api.inject.ApplicationLifecycle

import scala.util.{Failure, Success}

@Singleton
class ApplicationStart @Inject()(lifecycle: ApplicationLifecycle, config:Configuration,
                                 processor:MessageProcessor[SNSMessage, Unit], serverState: InjectableGlobalServerState)
                                (implicit actorSystem: ActorSystem) {
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

      MessageHook.connect[SNSMessage](topicArn, processor).onComplete({
        case Success(hook)=>
          logger.debug(s"Success, subscriptionArn is ${hook.getSubscriptionArn}")
          lifecycle.addStopHook(()=> {
            hook.disconnect()
            Future.successful()
          })
          val pipelineName = "transcodebuckets-test"
          implicit val scheduler = actorSystem.scheduler

          val pipelinePromise = TranscoderSetup.createPipeline(
            config.get[String]("transcoder.inputBucket"),
            config.get[String]("transcoder.outputBucket"),
            topicArn,
            config.get[String]("transcoder.role"),
            pipelineName
          )
          pipelinePromise.future.onComplete({
            case Success(p)=>
              logger.info(s"Pipeline ${p.getId} is ready to process, updating server state")
              serverState.updatePipeline(p)
            case Failure(err)=>
              logger.error(s"Could not set up pipeline:", err)
              lifecycle.stop()
          })
        case Failure(err)=>
          logger.error("Could not connect to notification topic", err)
          lifecycle.stop()
      })
  }

}
