package services
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo._
import com.gu.scanamo.syntax._
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model.{ListObjectsV2Request, S3ObjectSummary}
import helpers.DataAccess
import javax.inject.{Inject, Singleton}
import play.api.Logger

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class BucketScanner @Inject() (dataAccess:DataAccess) {
  private val logger = Logger(getClass)
  def scanBucketForInput(bucketName:String)(func: S3ObjectSummary=>Future[Unit])(implicit s3Client:AmazonS3):Unit = scanBucketForInput(bucketName, None, 1)(func)(s3Client)

  def scanBucketForInput(bucketName:String, continuationToken: Option[String], pageNumber: Int)(func: (S3ObjectSummary)=>Future[Unit])(implicit s3Client:AmazonS3):Unit = {
    val rq = continuationToken match {
      case Some(token)=>
        new ListObjectsV2Request().withBucketName(bucketName).withContinuationToken(token)
      case None=>
        new ListObjectsV2Request().withBucketName(bucketName)
    }

    val result = s3Client.listObjectsV2(bucketName)
    val futuresList = result.getObjectSummaries.asScala.map(summary=>func(summary))
    Future.sequence(futuresList).onComplete({
      case Success(u)=>
        if(result.getNextContinuationToken!=null) {
          logger.info(s"Successfully processed page $pageNumber of results, loading next...")
          scanBucketForInput(bucketName, Some(result.getNextContinuationToken), pageNumber + 1)(func)(s3Client)
        } else {
          logger.info(s"Successfully processed $pageNumber pages of results, finishing.")
        }
      case Failure(err)=>
        logger.error("Processing S3 files failed: ", err)
    })
  }

  def processS3Item(item: S3ObjectSummary)(implicit ddbClient:AmazonDynamoDB):Future[Unit] = {
    val sourceUrl = s"s3://${item.getBucketName}/${item.getKey}"
    logger.info(s"Processing $sourceUrl")

    dataAccess.processingRecordsForSource(sourceUrl).map(processingRecords=>{
      if(processingRecords.nonEmpty){
        logger.info(s"$sourceUrl has ${processingRecords.length} records processing, not starting any more")
        processingRecords.foreach(rec=>logger.debug(rec.toString))
      } else {
        logger.info(s"Requesting transcode run for $sourceUrl")
      }
    })
  }
}
