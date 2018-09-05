package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import javax.inject.Inject
import play.api.mvc.{AbstractController, BodyParsers, ControllerComponents, PlayBodyParsers}

import scala.concurrent.ExecutionContext
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax
import models.{SNSMessage, SNSMessageSerializer}
import play.api.Logger

class TranscoderMessageController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem)(implicit exec: ExecutionContext) extends AbstractController(cc) with SNSMessageSerializer {
  val logger = Logger(getClass)

  def transcoderMessage = Action(parse.json) { request: Request[JsValue]=>
    val maybeResult = request.body.validate[SNSMessage]

    maybeResult.fold(
      errors => {
        logger.error(s"Could not parse incoming json: $errors")
        BadRequest(Json.obj("status" -> "error", "message" -> JsError.toJson(errors)))
      },
      message =>
        logger.debug(s"Got SNS message ${message.toString}")
    )

    Ok(Json.obj("status"->"ok"))
  }
}
