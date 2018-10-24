package helpers

import java.util.UUID

import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClient}
import com.gu.scanamo._
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import javax.inject.{Inject, Singleton}
import models.JobRecord
import play.api.{Configuration, Logger}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class DataAccess @Inject() (configuration:Configuration) {
  protected val logger = Logger(getClass)

  def processingRecordsForSource(sourceUrl: String)(implicit client:AmazonDynamoDB):Future[List[JobRecord]] = Future {
    val table = Table[JobRecord](configuration.get[String]("jobsTable"))

    val scanResult = Scanamo.exec(client)(table.query('status -> "Processing"))

    val errors = scanResult.collect({
      case Left(error)=>error
    })
    if(errors.nonEmpty){
      logger.error(s"Could not scan for processing records. Errors were: $errors")
      throw new RuntimeException("Could not scan for processing records. Consult logs for details.")
    }
    scanResult.collect({
      case Right(record)=>record
    })
  }

  def scanAllRecords(limit: Int)(implicit client:AmazonDynamoDB):Future[Either[DynamoReadError, List[JobRecord]]] = Future {
    val table = Table[JobRecord](configuration.get[String]("jobsTable"))
    val scanResult = Scanamo.scanWithLimit[JobRecord](client)(configuration.get[String]("jobsTable"),limit)

    val errors = scanResult.collect({
      case Left(err)=>err
    })
    if(errors.nonEmpty){
      Left(errors.head)
    } else {
      Right(scanResult.collect({
        case Right(record)=>record
      }))
    }
  }

  def recordForUuid(uuid:UUID)(implicit client:AmazonDynamoDB):Future[Option[Either[DynamoReadError, JobRecord]]] = Future {
    val table = Table[JobRecord](configuration.get[String]("jobsTable"))
    Scanamo.exec(client)(table.get('uuid->uuid))
  }
}
