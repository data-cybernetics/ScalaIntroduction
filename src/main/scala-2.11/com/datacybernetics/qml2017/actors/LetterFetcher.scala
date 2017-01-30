package com.datacybernetics.qml2017.actors

import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import com.datacybernetics.qml2017.actors.LetterFetcher.{RequestLetter, ReturnLetter}
import com.datacybernetics.qml2017.util.Letter

/**
  * The LetterFetcher is an actor that requests (completely unnecessarily) a html document
  * and searches through the text to find one letter.
  */
class LetterFetcher extends Actor {

  /**
    * This is the reference to the HttpFetcher roud robin pool
    */
  val httpFetcher = context.system.actorSelection("/user/HttpFetcher")

  override def receive: Receive = {
    case RequestLetter(correlationId, letter, returnTo) =>
      /*
      Here we ask the HttpFetcher to send us the text **everytime** we are asked to
      return a letter. This is utter BS, but bear with me: it is a showcase of concurrency!

      This time, I don't want to internally save the state in the actor.
      I simply encode the state into my message! Of course you get some
      non-local behavior form an architectural point of view. In some cases this is OK.

      Here it is actually necessary, since the LetterFetcher is in a pool of actors,
      and saving the state in one actor is not propagated through the other actors of the pool.
       */
      Console.err.println(s"[$correlationId] Sending request for HTML.")
      httpFetcher ! HttpFetcher.RequestText(correlationId, state = Some( (sender, letter) ))

    case HttpFetcher.ReturnText(correlationId, text, Some(state)) =>
      /*
      As I said, we return the complete state that we have encoded from above when the
      HttpFetcher returns its html text.

      This is very nice, since we don't know when a certain message will arrive. Remember, it is all
      very concurrent. Saving states can be dangerous, especially if you are using pools, as we are
      in the case of the LetterFetcher!!!

      Then we go through the text, again very scala-like, reminiscent of functional programming:
      we filter, then take the first element.

      **Attention** of course the element might not be in the text. Then there would be an exception.
      Figure out a way to do this...
       */
      Console.err.println(s"[$correlationId] Received the HTML. Thank you!")
      /*
      Another nice feature of scala borrowed from python... we return a tuple
      and by the pattern matching we can dissect them and declare separate variables
       */
      val (returnTo, letter) = state.asInstanceOf[(ActorRef, Letter)]
      val resultCharacter = text.filter(_ == letter.char).head
      returnTo ! ReturnLetter(correlationId, resultCharacter)
  }
}

object LetterFetcher {
  case class RequestLetter(correlationId: String, letter: Letter, returnTo: Option[ActorRef] = None)
  case class ReturnLetter(correlationId: String, letter: Char)
}
