package com.datacybernetics.qml2017.actors

import akka.actor.{Actor, ActorRef}
import com.datacybernetics.qml2017.util._
import com.datacybernetics.qml2017.actors.HelloWorldManager.{ReturnTheThing, SayTheThing}

import scala.collection.mutable

/**
  * The HelloWorldManager is the "brain" of the computation. It orchestrates calling sub-actors
  * and aggregating the results together
  * It saves its state internally.
  *
  * Of course this is totally stupid to do it this way. This is only a showcase of language features.
  */
class HelloWorldManager extends Actor {

  /**
    * This maps a character used in "HELLO WORLD" to the actual positions.
    */
  val helloWorldMapping = Map(
    'h' -> Array(0),
    'e' -> Array(1),
    'l' -> Array(2,3,9),
    'o' -> Array(4,7),
    ' ' -> Array(5),
    'w' -> Array(6),
    'r' -> Array(8),
    'd' -> Array(10)
  )

  /**
    * This is also a very interesting concept. We defined the LetterFetcher in Main, it is in essence a
    * pool of actors of the type LetterFetcher. We are referencing the pool manager singleton
    */
  val letterFetcher = context.system.actorSelection("/user/LetterFetcher")

  /**
    * This is the internal state of the actor regarding the answer
    */
  val jobs = mutable.Map[String,Array[Char]]()

  /**
    * This is the internal state of the actor regarding who to send the result
    */
  val jobSenders = mutable.Map[String, ActorRef]()

  /**
    * This is the main functionality of any actor: the receive function.
    * It pattern-matches messages that were dequeued by the actorsystem.
    * If a message does not match, it gets discarded.
    * @return
    */
  override def receive: Receive = {
    case SayTheThing() =>

      /*
      Totally pedestrian way to create a random id

      It shows how to create a sequence and alter it using methods reminiscent of functional programming:
      the map method is used like a pipe operator.
       */
      Console.err.print(s"Creating a correlation ID...")
      val id = (0 to 10)
        .map(i => math.random*100)
        .map(_.toInt)
        .map(_% (90-65) + 65)
        .map(_.toChar)
        .mkString
      Console.err.print(s"$id.")

      jobs.put(id, new Array[Char](11))
      jobSenders.put(id, sender())

      /*
      Now this is also interesting. The normal way to communicate is the "ask" method, also
      defined by the '!' sign. Scala allows to define any UTF-8 character as operator.

      Unfortunately we don't have nice keyboards with greek symbols and other nifty things
      so we need to take care not to get too fancy with the definition, or have ascii names as
      alternatives!

      Okay, so here we request _in_parallel_ the needed letters of "HELLO WORLD". Each "ask" call is swiftly executed
      by just sending the according message to the mailbox of the actor.

      Now we have a round robin pool of 5, that means that the first messages
      will get executed at the same time.

      The other messages will have to wait until their turn.
       */
      Console.err.println(s"[$id] Sending requests to the LetterFetcher...")
      letterFetcher ! LetterFetcher.RequestLetter(id, LetterH())
      letterFetcher ! LetterFetcher.RequestLetter(id, LetterE())
      letterFetcher ! LetterFetcher.RequestLetter(id, LetterL())
      letterFetcher ! LetterFetcher.RequestLetter(id, LetterO())
      letterFetcher ! LetterFetcher.RequestLetter(id, LetterSpace())
      letterFetcher ! LetterFetcher.RequestLetter(id, LetterW())
      letterFetcher ! LetterFetcher.RequestLetter(id, LetterR())
      letterFetcher ! LetterFetcher.RequestLetter(id, LetterD())

    /*
    This is also a very interesting concept. We have saved the internal state by the correlation id
    and are awaiting the requested letters. We receive them asynchronously in any order, but we know
    what to do and when the computation is actually done.

    Attention: something bad might happen. In this case it will never end. This is a potential bug!!
    => Think of a mechanism to catch a failing...!
     */
    case LetterFetcher.ReturnLetter(id, letter) =>
      Console.err.println(s"[$id] Recevied letter '$letter'.")

      helloWorldMapping(letter.toLower)
        .foreach( index => jobs(id)(index) = letter)

      /*
      End condition is when all necessary letters have been placed and there are no more \0 left in
      the array.
      If that happens we return to the sender actor the according message, and remove the state.
       */
      if (!jobs(id).contains(0)) {
        Console.err.println(s"[$id] All letters have been gathered. We are ready to say the THING!")
        Console.err.flush()
        jobSenders(id) ! ReturnTheThing(jobs(id).mkString)
        jobSenders.remove(id)
        jobs.remove(id)
      }
  }

}

/*
 A companion object is something like a static class. It has all
 static methods of 'HttpFetcher' in it as well as case classes.
 */

/**
  * These are the static classes that we use as messages.
  * I personally like it this way. You can kind of explore
  * with autocomplete and javadoc the published "methods" realized by
  * messages (which are case classes)
  */
object HelloWorldManager {

  /**
    * This message is a request to get the popular string
    */
  case class SayTheThing()

  /**
    * This message is the return class that has the thing as string in it
    * @param thing This is "HELLO WORLD"
    */
  case class ReturnTheThing(thing: String)
}