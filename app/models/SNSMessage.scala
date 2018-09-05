package models

import java.sql.Timestamp

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class SNSMessage (Type:String, MessageId: String, TopicArn: String, Subject: Option[String],
                       Message: String, Timestamp: String, SignatureVersion: String, Signature: String,
                       SigningCertURL: String, UnsubscribeURL: String)

trait SNSMessageSerializer  {
  /*https://www.playframework.com/documentation/2.5.x/ScalaJson*/
  implicit val projectEntryWrites:Writes[SNSMessage] = (
    (JsPath \ "Type").write[String]  and
      (JsPath \ "MessageId").write[String]  and
      (JsPath \ "TopicArn").write[String]  and
      (JsPath \ "Subject").writeNullable[String] and
      (JsPath \ "Message").write[String] and
      (JsPath \ "Timestamp").write[String] and
      (JsPath \ "SignatureVersion").write[String]  and
      (JsPath \ "Signature").write[String]  and
      (JsPath \ "SigningCertURL").write[String]  and
      (JsPath \ "UnsubscribeURL").write[String]
    )(unlift(SNSMessage.unapply))

  implicit val projectEntryReads:Reads[SNSMessage] = (
    (JsPath \ "Type").read[String]  and
      (JsPath \ "MessageId").read[String]  and
      (JsPath \ "TopicArn").read[String]  and
      (JsPath \ "Subject").readNullable[String] and
      (JsPath \ "Message").read[String] and
      (JsPath \ "Timestamp").read[String] and
      (JsPath \ "SignatureVersion").read[String]  and
      (JsPath \ "Signature").read[String]  and
      (JsPath \ "SigningCertURL").read[String]  and
      (JsPath \ "UnsubscribeURL").read[String]
    )(SNSMessage.apply _)
}