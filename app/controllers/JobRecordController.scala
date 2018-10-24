package controllers

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import helpers.DataAccess
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}
import io.circe.generic.auto._, io.circe.syntax._

@Singleton
class JobRecordController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem, dataAccess:DataAccess)(implicit exec: ExecutionContext) extends AbstractController(cc) {
  def all(limit:Int) = Action.async {
    implicit val ddbClient:AmazonDynamoDB = AmazonDynamoDBClientBuilder.defaultClient()
    dataAccess.scanAllRecords(limit).map({
      case Left(dynamoReadError)=>
        InternalServerError(Json.obj("status"->"error", "detail"->dynamoReadError.toString))
      case Right(records)=>
        Ok(records.asJson.toString)
    })
  }
}