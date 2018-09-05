package services
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.amazonaws.services.sns.{AmazonSNSAsyncClientBuilder, AmazonSNS, AmazonSNSClientBuilder}
import com.amazonaws.services.sns.model.SubscribeResult
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClientBuilder}
import com.amazonaws.services.sqs.model.CreateQueueResult
import javax.inject.Inject
import play.api.Logger

import scala.compat.java8.FutureConverters
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.matching.Regex
import collection.JavaConverters._

object MessageHook {
  val logger = Logger(getClass)

  /**
    * Assuming we're in EC2, look up the instance's internal IP address by contacting the metadata service
    * @param actorSystem
    * @param materializer
    * @return
    */
  def getMyIp(implicit actorSystem:ActorSystem, materializer: ActorMaterializer):Future[String] = {
    implicit val executionContext = actorSystem.dispatcher

    Http().singleRequest(HttpRequest(uri = "http://169.254.169.254/latest/meta-data/local-ipv4"))
      .flatMap(_.entity.dataBytes.runFold(ByteString(""))(_ ++ _))
      .map(_.utf8String)
  }

  protected def createSqsQueue(implicit actorSystem:ActorSystem, materializer: ActorMaterializer, sqsClient: AmazonSQS) = {
    val replacer = new Regex("\\.")
    val queueNameFuture = getMyIp.map(internalIpAddress=>{
      s"transcodebuckets-${replacer.replaceAllIn(internalIpAddress, "-")}"
    }).recoverWith({
      case err:Throwable=>
        logger.warn("Could not resolve internal ip address:", err)
        Future(s"transcodebuckets-dev-system-test")
    })

    queueNameFuture.map(queueName=> {
      logger.debug(s"MessageHook::createSqsQueue - queue name will be $queueName")
      sqsClient.createQueue(queueName)
    })
  }

  protected def subscribeSqsQueue(topicArn:String, qData: CreateQueueResult)(implicit sqsClient:AmazonSQS, snsClient:AmazonSNS) = Future {
    val attrs = sqsClient.getQueueAttributes(qData.getQueueUrl, Seq("QueueArn").asJava)
    val queueArn =  attrs.getAttributes.get("QueueArn")
    logger.debug(s"subscribeSqsQueue - created queue ARN is $queueArn")
    snsClient.subscribe(topicArn, "sqs", queueArn)
  }

  /**
    * connect to the given topic ARN with our internal endpoint.
    * @param topicArn topic to connect to
    * @return a Future containing the ARN of the completed subscription. Future will fail if there was an error, pick
    *         this up with .onComplete or .recover
    */
  def connect(topicArn: String)(implicit actorSystem:ActorSystem, materializer: ActorMaterializer):Future[MessageHook] = {
    implicit val sqsClient:com.amazonaws.services.sqs.AmazonSQS = AmazonSQSClientBuilder.standard().build()
    implicit val snsClient = AmazonSNSClientBuilder.defaultClient()

    createSqsQueue.flatMap(qData=>{
      subscribeSqsQueue(topicArn, qData).map(subscribeResult=>{
        new MessageHook(Some(qData), Some(subscribeResult.getSubscriptionArn))
      })
    })
    /*    logger.debug(s"MessageHook::connect - $topicArn")
        getMyIp.map(internalIpAddress=>{
          val endpointUri = s"http://$internalIpAddress/transcoder-message"
          logger.info(s"Subscribing to $topicArn with endpoint $endpointUri")
          client.subscribe(topicArn, "http", endpointUri)
        }).map(_.getSubscriptionArn)*/
  }
}

class MessageHook (sqsQueue:Option[CreateQueueResult] = None, subscriptionArn:Option[String]=None)  {
  val logger = Logger(getClass)

  def getSubscriptionArn = subscriptionArn

  protected def deleteSqsQueue(qData: CreateQueueResult)(implicit sqsClient: AmazonSQS) = {
    sqsClient.deleteQueue(qData.getQueueUrl)
  }

  /**
    * cancel the provided subscription and delete the SQS queue for marshalling
    */
  def disconnect():Unit = {
    implicit val snsClient = AmazonSNSClientBuilder.defaultClient()
    implicit val sqsClient = AmazonSQSClientBuilder.defaultClient()

    subscriptionArn match {
      case Some(realSubArn)=>
        logger.info(s"Disconnecting subscription with arn $realSubArn")
        snsClient.unsubscribe(realSubArn)
      case None=>
        logger.error(s"No active SNS subscription to disconnect")
    }
    sqsQueue match {
      case Some(realSQSQ)=>
        logger.info(s"Deleting created SQS queue at ${realSQSQ.getQueueUrl}")
        deleteSqsQueue(realSQSQ)
      case None=>
        logger.error("No created SQS queue to clean up")
    }
  }
}
