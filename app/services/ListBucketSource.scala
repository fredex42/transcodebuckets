package services

import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.{AbstractOutHandler, GraphStage, GraphStageLogic}
import com.amazonaws.services.s3.AmazonS3
import javax.inject.Inject
import models.S3Location
import play.api.Logger

class ListBucketSource (bucketScanner: BucketScanner, bucketName:String)(implicit s3Client:AmazonS3) extends GraphStage[SourceShape[S3Location]]{
  private val logger = Logger(getClass)
  final val out:Outlet[S3Location] = Outlet.create("ListBucketSource.out")
  private final val sShape = SourceShape.of(out)
  private var continuationToken:Option[String] = None

  override def shape: SourceShape[S3Location] = sShape

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    setHandler(out, new AbstractOutHandler {
      override def onPull(): Unit = {
        bucketScanner.simpleScanBucket(bucketName,1, continuationToken).headOption match {
          case Some(location)=>
            logger.info(s"Got location $location")
            continuationToken = location.continuationToken
            push(out, location)
          case None=>
            complete(out)
            logger.warn(s"No more results returned from $bucketName")
        }
      }
    })
  }
}
