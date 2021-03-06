package app.actors

import scala.concurrent.duration.DurationInt

import com.typesafe.conductr.bundlelib.scala.LocationService

import akka.actor.{ Actor, ActorLogging, Props }
import akka.pattern.pipe
import akka.util.Timeout
import play.api.Play.current
import play.api.libs.ws.WS

object Delivery {
  def props(): Props = Props(new Delivery)
  sealed abstract trait Message
  case class DeliveryMsg(msg: String) extends Message
  case object Ack extends Message
  case object Nack extends Message
}

class Delivery extends Actor with ActorLogging {
  import Delivery._
  implicit val timeout = Timeout(5.seconds)
  implicit val executionContext = context.dispatcher
  val ferry = LocationService.getLookupUrl("/ferry", "http://127.0.0.1:9666/ferry")

  def receive = {
    case delivery: DeliveryMsg =>
      val msg = delivery.msg
      WS.url(s"$ferry/transport/$msg").withFollowRedirects(follow = true).put(Map("msg" -> Seq(msg)))
        .map { response =>
          response.status match {
            case 200 => Ack
            case _   => Nack
          }
        }
        .pipeTo(sender())
  }

}

