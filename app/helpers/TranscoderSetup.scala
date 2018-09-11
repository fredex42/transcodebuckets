package helpers

import akka.actor.{Cancellable, Scheduler}
import com.amazonaws.services.elastictranscoder.AmazonElasticTranscoderClientBuilder
import com.amazonaws.services.elastictranscoder.model._
import play.api.Logger

import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

trait TranscoderSetupT {
  val logger = Logger(getClass)

  /**
    * get an ETS client. Supplied in this way for easy mocking in tests.
    * @return
    */
  protected def getClient = AmazonElasticTranscoderClientBuilder.defaultClient()

  /**
    * creates a pipeline
    * @param inputBucket String of the name of the bucket to use as input
    * @param outputBucket String of the name of the bucket to use as output
    * @param notificationTopic String of the ARN of a topic to use for messages
    * @param role String of the ARN of a role to run the pipeline under
    * @param name String of the name to use for the pipeline
    * @param scheduler Implicitly provided akka scheduler, get this from an injected ActorSystem
    * @return a Promise, containing the Pipeline. The promise will not resolve until the pipeline is ready to run.
    */
  def createPipeline(inputBucket: String, outputBucket: String, notificationTopic: String, role: String, name:String)(implicit scheduler:Scheduler) = {
    val client = getClient

    val notifications = new Notifications()
      .withCompleted(notificationTopic)
      .withError(notificationTopic)
      .withProgressing(notificationTopic)
      .withWarning(notificationTopic)

    val outputConfig = new PipelineOutputConfig()
      .withBucket(outputBucket)
      .withStorageClass("REDUCED_REDUNDANCY")

    val rq = new CreatePipelineRequest()
        .withInputBucket(inputBucket)
        .withOutputBucket(outputBucket)
        .withNotifications(notifications)
        .withRole(role)
        .withContentConfig(outputConfig)
        .withName(name)

    val p = Promise[Pipeline]

    val result = client.createPipeline(rq)

    val sched:Cancellable = scheduler.schedule(1.second, 5.seconds){
      logger.debug(s"Creating pipeline ${result.getPipeline.getId}, status is ${result.getPipeline.getStatus}")
      if(result.getPipeline.getStatus=="READY"){
        p.complete(Success(result.getPipeline))
      }
      if(result.getPipeline.getStatus=="FAILED"){
        p.complete(Failure(new RuntimeException("Pipeline creation failed")))
      }
    }
    p.future.onComplete(tried=>sched.cancel())
    p
  }

  def deletePipeline(pipeline: Pipeline)(implicit scheduler: Scheduler):Promise[Unit] = {
    val client = getClient
    val rq = new DeletePipelineRequest().withId(pipeline.getId)

    val result = client.deletePipeline(rq)
    val p = Promise[Unit]

    val sched:Cancellable = scheduler.schedule(1.second, 5.seconds){
      logger.debug(s"Deleting pipeline ${pipeline.getId}, status is ${pipeline.getStatus}")
      if(pipeline.getStatus=="DELETED"){
        p.complete(Success())
      }
      if(pipeline.getStatus=="FAILED"){
        p.complete(Failure(new RuntimeException("Pipeline deletion failed")))
      }
    }
    p.future.onComplete(tried=>sched.cancel())
    p
  }
}

object TranscoderSetup extends TranscoderSetupT {

}