package services
import models.SNSMessage
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MessageHookSpec extends Specification {
  "MessageHook.processMessageBody" should {
    "parse a valid Json document" in {
      val mh = new MessageHook[SNSMessage](processor = None) {
        def testProcessMessageBody(str:String) = processMessageBody(str)
      }

      val testValidJson =
        """{
          |  "Type" : "Notification",
          |  "MessageId" : "22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324",
          |  "TopicArn" : "arn:aws:sns:us-west-2:123456789012:MyTopic",
          |  "Subject" : "My First Message",
          |  "Message" : "Hello world!",
          |  "Timestamp" : "2012-05-02T00:54:06.655Z",
          |  "SignatureVersion" : "1",
          |  "Signature" : "EXAMPLEw6JRNwm1LFQL4ICB0bnXrdB8ClRMTQFGBqwLpGbM78tJ4etTwC5zU7O3tS6tGpey3ejedNdOJ+1fkIp9F2/LmNVKb5aFlYq+9rk9ZiPph5YlLmWsDcyC5T+Sy9/umic5S0UQc2PEtgdpVBahwNOdMW4JPwk0kAJJztnc=",
          |  "SigningCertURL" : "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem",
          |  "UnsubscribeURL" : "https://sns.us-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-west-2:123456789012:MyTopic:c9135db0-26c4-47ec-8998-413945fb5a96"
          |}
        """.stripMargin
      val result = mh.testProcessMessageBody(testValidJson)
      result must beSuccessfulTry
    }
  }

  "return failure for a malformatted json document" in {
    val mh = new MessageHook[SNSMessage](processor = None) {
      def testProcessMessageBody(str:String) = processMessageBody(str)
    }

    val testValidJson =
      """{
        |  "Type" : "Notification",
        |  "MessageId" : "22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324",
        |  "TopicArn" : "arn:aws:sns:us-west-2:123456789012:MyTopic",
        |  "Subject" : "My First Message",
        |  "Timestamp" : "2012-05-02T00:54:06.655Z",
        |  "SignatureVersion" : "1",
        |  "Signature" : "EXAMPLEw6JRNwm1LFQL4ICB0bnXrdB8ClRMTQFGBqwLpGbM78tJ4etTwC5zU7O3tS6tGpey3ejedNdOJ+1fkIp9F2/LmNVKb5aFlYq+9rk9ZiPph5YlLmWsDcyC5T+Sy9/umic5S0UQc2PEtgdpVBahwNOdMW4JPwk0kAJJztnc=",
        |  "SigningCertURL" : "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem",
        |  "UnsubscribeURL" : "https://sns.us-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-west-2:123456789012:MyTopic:c9135db0-26c4-47ec-8998-413945fb5a96"
        |}
      """.stripMargin
    val result = mh.testProcessMessageBody(testValidJson)
    result must beFailedTry
  }

  "return failure for invalid json" in {
    val mh = new MessageHook[SNSMessage](processor = None) {
      def testProcessMessageBody(str:String) = processMessageBody(str)
    }

    val testValidJson =
      """
        |  "Type" : "Notification",
        |  "MessageId" : "22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324",
        |  "TopicArn" : "arn:aws:sns:us-west-2:123456789012:MyTopic",
        |  "Subject" : "My First Message",
        |  "Message" : "Hello world!",
        |  "Timestamp" : "2012-05-02T00:54:06.655Z",
        |  "SignatureVersion" : "1",
        |  "Signature" : "EXAMPLEw6JRNwm1LFQL4ICB0bnXrdB8ClRMTQFGBqwLpGbM78tJ4etTwC5zU7O3tS6tGpey3ejedNdOJ+1fkIp9F2/LmNVKb5aFlYq+9rk9ZiPph5YlLmWsDcyC5T+Sy9/umic5S0UQc2PEtgdpVBahwNOdMW4JPwk0kAJJztnc=",
        |  "SigningCertURL" : "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem",
        |  "UnsubscribeURL" : "https://sns.us-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-west-2:123456789012:MyTopic:c9135db0-26c4-47ec-8998-413945fb5a96"
        |}
      """.stripMargin
    val result = mh.testProcessMessageBody(testValidJson)
    result must beFailedTry
  }
}
