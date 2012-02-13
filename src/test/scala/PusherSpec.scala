import org.specs2.mutable._
import org.specs2.execute.Skipped

object PusherSpec extends Specification {
  import dispatch._
  import pusher._
  import dispatch.liftjson.Js._
  import net.liftweb.json._

  val authKey = "278d425bdf160c739803"
  val secretKey = "7ad3773142a6692b25b8"
  val appId = "3"
  val channelName = "test_channel"

  val params = Map[String,String]("name" -> "foo",
                                  "auth_key" -> authKey,
                                  "auth_timestamp" -> "1272044395",
                                  "auth_version" -> "1.0",
                                  "body_md5" -> "7b3d404f5cde4a0b9b8fb4789a0098cb")

  val body = """{"some":"data"}"""
  val eventName = "foo"
  val eventData = parse(body)

  val expectedStringToSign = "POST\n" +
                 "/apps/" + appId + "/channels/" + channelName + "/events\n" +
                 "auth_key=" + authKey + "&auth_timestamp=1272044395&auth_version=1.0&body_md5=7b3d404f5cde4a0b9b8fb4789a0098cb&name=" + eventName

  val expectedSignature = "309fc4be20f04e53e011b00744642d3fe66c2c7c5686f35ed6cd2af6f202e445"

  "Pusher" should {
    "create the right string to sign" in {
      Pusher.canonicalString("POST", "/apps/3/channels/test_channel/events", params) must be_==(expectedStringToSign)
    }
    "create the right HMAC SHA256 signature" in {
      Pusher.sign(secretKey, "POST", "/apps/3/channels/test_channel/events", params) must be_==(expectedSignature)
    }
    "create the right MD5 SUM of the body" in {
      Pusher.md5(body) must be_==("7b3d404f5cde4a0b9b8fb4789a0098cb")
    }
    "create an event when sent to Pusher" in {
      val conf = new java.io.File("pusher.test.properties")
      if (!conf.exists)
        Skipped("no pusher properties file found")

      val config = {
        val fis = new java.io.FileInputStream(conf)
        val props = new java.util.Properties()
        props.load(fis)
        fis.close()
        props
      }

      val key = config.getProperty("key")
      val id = config.getProperty("app_id")
      val secret = config.getProperty("secret")
      val channel = config.getProperty("channel_name")
      val client = new PusherClient(id, key, secret, channel)

      Http x (client.handle(Event.name(eventName).data(eventData))) {
        case (code, _, _, _) => code must be_==(202)
      }
    }
  }
}
