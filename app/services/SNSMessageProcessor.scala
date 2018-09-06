package services

import models.SNSMessage
import play.api.{Configuration, Logger}
import javax.inject._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SNSMessageProcessor @Inject() (config: Configuration) extends MessageProcessor[SNSMessage, Unit] {
  private val logger = Logger(getClass)
  override def process(msg: SNSMessage): Future[Unit] = Future {
    logger.info(s"Got message body ${msg.Message}")
  }
}
