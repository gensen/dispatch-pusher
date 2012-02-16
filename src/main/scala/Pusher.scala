package dispatch.pusher

import dispatch._
import dispatch.liftjson.Js._
import net.liftweb.json._
import org.apache.http.util.EntityUtils
import org.apache.http.entity.BufferedHttpEntity

object Pusher {
  val Root = "api.pusherapp.com"
  val SHA256 = "HmacSHA256"
  val UTF_8 = "UTF-8"

  def canonicalString(method: String, path: String, params: Map[String, String]) = {
    val paramsString = (params.toList.sortWith(_._1.toLowerCase < _._1.toLowerCase).map { case (k,v) => "%s=%s".format(k,v) }).mkString("&")
    "%s\n%s\n%s".format(method, path, paramsString)
  }

  def sign(secretKey: String, stringToSign: String): String = {
    import javax.crypto

    val sig = {
      val mac = crypto.Mac.getInstance(SHA256)
      println("Mac authentication algorithm: %s".format(mac.getAlgorithm))
      val key = new crypto.spec.SecretKeySpec(secretKey.getBytes(), SHA256)
      mac.init(key)
      val sb = new StringBuffer()
      hex_digest(mac.doFinal(bytes(stringToSign)))
    }
    sig
  }

  def sign(secretKey: String, method: String, path: String, params: Map[String, String]): String = {
    sign(secretKey, canonicalString(method, path, params))
  }

  def md5(s: String): String = md5(bytes(s))
  def md5(bytes: Array[Byte]): String = {
    import java.security.MessageDigest

    val r = MessageDigest.getInstance("MD5")
    r.reset
    r.update(bytes)
    hex_digest(r.digest)
  }

  def hex_digest(bytes: Array[Byte]) = bytes.map( "%02x".format(_) ).mkString
  def bytes(s: String) = s.getBytes(UTF_8)


  implicit def Request2PusherSigner(r: Request) = new PusherRequestSigner(r)
  implicit def Request2PusherSigner(s: String) = new PusherRequestSigner(new Request(s))

  class PusherRequestSigner(r: Request) {
    def <@ (key: String, secret: String) = {
      val bodyMd5 = r.body.map(ent => md5(EntityUtils.toByteArray(new BufferedHttpEntity(ent))))
      val eventName = r.headers.filter {
        case (name, _) => name.toLowerCase == "event-name"
      }.headOption.map { case (_, value) => value }

      val method = r.method
      val path = r.path
      val params = Map[String,String]("auth_key" -> key,
                                      "auth_timestamp" -> (System.currentTimeMillis / 1000).toString,
                                      "auth_version" -> "1.0",
                                      "body_md5" -> bodyMd5.getOrElse(error("No event data being sent")),
                                      "name" -> eventName.getOrElse(error("No event name was given")))

      r <<? (params + ("auth_signature" -> sign(secret, method, path, params)))
    }
  }

}

abstract class Client extends ((Request => Request) => Request) {
  def hostname = "api.pusherapp.com"
  def host: Request
  def handle[T](method: Method[T]) =
    method.default_handler(apply(method))
}

class PusherClient(appId: String, key: String, secret: String, channelName: String) extends Client {
  import Pusher._
  def host = :/(hostname) / "apps" / appId / "channels" / channelName

  def apply(block: Request => Request): Request =
    block(host) <@ (key, secret)
}

trait MethodBuilder extends Builder[Request => Request] {
  final def product = setup andThen complete
  def setup = identity[Request] _
  def complete: Request => Request
}

trait Method[T] extends MethodBuilder {
  /** default handler used by Client#call. You can also apply the client
to a Method and define your own request handler. */
  def default_handler: Request => Handler[T]
}

trait NoReturnMethod extends Method[Unit] {
  def default_handler = _ >|
}

object Event extends EventBuilder(None, None)
private[pusher] class EventBuilder(name: Option[String], data: Option[JValue]) extends NoReturnMethod {
  def name(eventName: String): EventBuilder = new EventBuilder(Some(eventName), data)
  def data(newData: JValue) = new EventBuilder(name, Some(newData))

  def complete =
    _ / "events" <:< Map("event-name" -> name.getOrElse { error("event name was not specified") }) << (pretty(render(data.getOrElse { error("event data was not specified") })))
}
