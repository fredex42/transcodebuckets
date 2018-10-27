package services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{RunnableGraph, Sink, Source}
import com.amazonaws.services.s3.AmazonS3
import models.S3Location
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.duration._

class ListBucketSourceSpec extends Specification with Mockito {

  "ListBucketSource" should {
    "provide an akka source for a list of S3Location" in {
      val system = ActorSystem.create("TestListBucketSource")
      implicit val mat = ActorMaterializer.create(system)
      import system.dispatcher
      import akka.pattern.pipe

      implicit val mockedClient = mock[AmazonS3]
      val mockedScanner = mock[BucketScanner]
      mockedScanner.simpleScanBucket(anyString,anyInt,any)(any) returns (
        Seq(S3Location("testbucket","key1",Some("a"))),
        Seq(S3Location("testbucket","key2",Some("b"))),
        Seq(S3Location("testbucket","key3",None)),
        Seq()
      )


      val source = Source
        .fromGraph(new ListBucketSource(mockedScanner,"testbucket"))

      val future = source.runWith(Sink.seq)
      val result = Await.result(future, 3.seconds)
      result mustEqual Seq(
        S3Location("testbucket","key1",Some("a")),
        S3Location("testbucket","key2",Some("b")),
        S3Location("testbucket","key3",None)
      )
    }
  }


}
