package helpers

import akka.actor.ActorSystem
import com.amazonaws.services.elastictranscoder.AmazonElasticTranscoder
import com.amazonaws.services.elastictranscoder.model.{CreatePipelineRequest, CreatePipelineResult, Pipeline}
import javax.inject.Inject
import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import org.specs2.runner.JUnitRunner

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import org.specs2.specification.AfterAll

@RunWith(classOf[JUnitRunner])
class TranscoderSetupSpec extends TestKit(ActorSystem("TranscoderSetupSpec")) with SpecificationLike with AfterAll{
  implicit val scheduler = system.scheduler

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  "TranscoderSetup.createPipeline" should {
    "return a Promise that resolves when the pipeline is created" in {
      class TranscoderSetupTest extends TranscoderSetupT with Mockito{
        val mockPipeline = mock[Pipeline]
        mockPipeline.getStatus.returns("READY")
        override protected def getClient: AmazonElasticTranscoder = mock[AmazonElasticTranscoder]
          .createPipeline(any[CreatePipelineRequest]).responds(req=>new CreatePipelineResult().withPipeline(mockPipeline))
      }

      val ts = new TranscoderSetupTest
      val result = ts.createPipeline("input","ouput","notification","role","name")
      val finalResult = Await.result(result.future, 10 seconds)
      finalResult shouldEqual ts.mockPipeline
    }

    "return a Promise that fails when the pipeline does not create" in {
      class TranscoderSetupTest extends TranscoderSetupT with Mockito {
        val mockPipeline = mock[Pipeline].getStatus.returns("FAILED")

        override protected def getClient: AmazonElasticTranscoder = mock[AmazonElasticTranscoder]
          .createPipeline(any[CreatePipelineRequest]).responds(req => new CreatePipelineResult().withPipeline(mockPipeline))
      }

      val ts = new TranscoderSetupTest
      val result = ts.createPipeline("input", "ouput", "notification", "role", "name")

     { Await.result(result.future, 10 seconds) } must throwA(new RuntimeException("Pipeline creation failed"))
    }

    "not return while creation is in progress" in {
      class TranscoderSetupTest extends TranscoderSetupT with Mockito {
        val mockPipeline = mock[Pipeline].getStatus.returns("InProgress")

        override protected def getClient: AmazonElasticTranscoder = mock[AmazonElasticTranscoder]
          .createPipeline(any[CreatePipelineRequest]).responds(req => new CreatePipelineResult().withPipeline(mockPipeline))
      }

      val ts = new TranscoderSetupTest
      val result = ts.createPipeline("input", "ouput", "notification", "role", "name")

      { Await.result(result.future, 10 seconds) } must throwA[java.util.concurrent.TimeoutException]
    }
  }
}
