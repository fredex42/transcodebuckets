package models
import com.amazonaws.services.elastictranscoder.model.Pipeline
import javax.inject.Singleton

@Singleton
class GlobalServerState extends InjectableGlobalServerState {
  override var pipeline:Option[Pipeline] = None

  override def updatePipeline(p: Pipeline): Unit = {
    pipeline = Some(p)
  }
}
