package models

import java.util.UUID

import com.gu.scanamo._
import com.gu.scanamo.syntax._

case class JobRecord(sourceUrl:String, jobId:UUID, destUrl:String, status:String, dummyHash: String)
