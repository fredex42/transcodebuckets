package services
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.amazonaws.services.sns.{AmazonSNSAsyncClientBuilder, AmazonSNSClientBuilder}
import com.amazonaws.services.sns.model.SubscribeResult
import play.api.Logger

import scala.compat.java8.FutureConverters
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

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

  /**
    * connect to the given topic ARN with our internal endpoint.
    * @param topicArn topic to connect to
    * @return a Future containing the ARN of the completed subscription. Future will fail if there was an error, pick
    *         this up with .onComplete or .recover
    */
  def connect(topicArn: String)(implicit actorSystem:ActorSystem, materializer: ActorMaterializer):Future[String] = {
    val client = AmazonSNSClientBuilder.defaultClient()
    logger.debug(s"MessageHook::connect - $topicArn")
    getMyIp.map(internalIpAddress=>{
      val endpointUri = s"http://$internalIpAddress/transcoder-message"
      logger.info(s"Subscribing to $topicArn with endpoint $endpointUri")
      client.subscribe(topicArn, "https", endpointUri)
    }).map(_.getSubscriptionArn)
  }

  /**
    * cancel the provided subscription
    * @param subscriptionArn subscription ARN to cancel
    * @return a Future with the UnsubscribeResult object. Future will fail if there was an error, pick this up with .onComplete or .recover
    */
  def disconnect(subscriptionArn:String) = {
    val client = AmazonSNSClientBuilder.defaultClient()
    logger.info(s"Disconnecting subscription with arn $subscriptionArn")
    client.unsubscribe(subscriptionArn)
  }
}
