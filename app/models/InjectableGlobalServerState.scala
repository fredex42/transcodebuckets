package models

import com.amazonaws.services.elastictranscoder.model.Pipeline

trait InjectableGlobalServerState {
  var pipeline:Option[Pipeline]
  def updatePipeline(p: Pipeline)
}
