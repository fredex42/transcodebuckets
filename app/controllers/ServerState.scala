package controllers
import javax.inject._
import akka.actor.ActorSystem
import models.{GlobalServerState, InjectableGlobalServerState}
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

@Singleton
class ServerState @Inject() (cc:ControllerComponents, serverState:InjectableGlobalServerState) extends AbstractController (cc){
  def serverStatus = Action {
    Ok(Json.obj("status"->"ok", "pipeline"->serverState.pipeline.map(_.getId)))
  }
}
