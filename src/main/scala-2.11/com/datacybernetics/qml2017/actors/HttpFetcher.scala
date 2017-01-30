package com.datacybernetics.qml2017.actors

import akka.actor.{Actor, ActorRef}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, Materializer}
import com.datacybernetics.qml2017.actors.HttpFetcher.{RequestText, ReturnText}

import scala.util.{Failure, Success}

/**
  * This actor loads from the internet the QML2017 location/travel page and renders it to
  * a String, then returning it.
  *
  * The interesting topic in this actor is the akka-streaming library which
  * no longer buffers data in memory but streams it, so it can in principal
  * load petabytes of data.
  */
class HttpFetcher extends Actor {

  override def receive: Receive = {

    case RequestText(id, returnTo, state) =>
      val originalSender = returnTo.getOrElse(sender())
      Console.err.println(s"[$id] Received a request for the HTML.")

      val uri = Uri("http://www.quantummachinelearning.org/location--travel1.html")
      Console.err.println(s"[$id] Use GET @$uri.")

      /**
        * Okay now, for this to work, we need an Materialzer
        */
      import context.dispatcher
      val materializer: Materializer = ActorMaterializer.create(context.system)

      /*
      The standard way to do this is by using the async functionality of scala
      so using a Future. Again, there are two kinds of outcomes:
      - Success/Failure
      - Timeout
      It is a single http request. Once done (getting a response from the server) we try to get the data.
       */
      Http(context.system).singleRequest(HttpRequest(uri = uri))(materializer) onComplete {
        case Success(response) =>

          /*
          The idea behind akka-streaming is that you go through the 'lines' of your data
          that got into the tcp sockets buffer in order to do meaningful operations on it.

          It is modelled as a flow from a source to a sink, using an arbitrary number of
          intermediate steps, also called flows.

          The mechanisms is very reminiscent of the map&reduce algorithm presented by Google
          which now forms the bases of the Hadoop-familiy of big data technologies.

          We are doing exactly this:
          map bytes to strings
          reduce them by concatenating.

          If we have defined our Flow, we run it, which will again be a Future

         Upon completion we return the text to the sender. If anything goes wrong we send a failure back, so that
         the calling actor is informed.
           */
          val source = response.entity.dataBytes
          val sink = Sink.reduce[String]{ (a,b) => a+b }

          val bodyFuture = source
            .map(_.decodeString("utf-8"))
            .runWith(sink)(materializer)
          Console.err.println(s"[$id] Waiting for retrieval.")

          bodyFuture onComplete {
            case Success(body) =>
              originalSender ! ReturnText(id, body, state)
            case Failure(exception) => originalSender ! Failure(exception)
          }

        case Failure(exception) =>
          Console.err.println(s"[$id] Observed an error $exception. We will retry.")
          Thread.sleep(1000)
          self ! RequestText(id, Some(originalSender))
      }
  }
}

object HttpFetcher {
  case class RequestText(correlationId: String, returnTo : Option[ActorRef] = None, state: Option[Any] = None)
  case class ReturnText(correlationId: String, text: String, state: Option[Any] = None)
}
