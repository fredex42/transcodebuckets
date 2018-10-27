package models

case class S3Location(bucket:String, path:String, continuationToken: Option[String])
