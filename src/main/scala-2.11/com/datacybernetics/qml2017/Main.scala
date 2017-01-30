package com.datacybernetics.qml2017

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.datacybernetics.qml2017.actors.{HelloWorldManager, HttpFetcher, LetterFetcher}
import com.datacybernetics.qml2017.util.TraitShowcase

import scala.util.{Failure, Success}

/**
  * This is a little showcase of scala+akka
  * This combination I use for designing and running productive 24/7 systems.
  *
  * I would like you to follow my comments in this sequence:
  * 1) Main
  * 2) HelloWorldManager
  * 3) LetterFetcher
  * 4) HttpFetcher
  * 5) the util package
  *
  * In this way the comments are done such that I build upon them
  */
object Main {
  /**
    * This is the main entry to the application
    * we initialize the actor system and instantiate
    * the actors that are used.
    * @param args input arguments. Not used now.
    */
  def main(args: Array[String]): Unit = {

    /*
    An actorsystem needs to be initialized, it handles the messaging and has a
    threadpool called the 'dispatcher' to allow execution in parallel, one of the
    main advantages of scala.
     */

    val system = ActorSystem("qml2017-system")

    /*
    I personally find it good practice to define all used actors during system initialization

    Here we find an interesting feature. The code below creates a Pool actor that distributes
    messages according to the round robin scheme. There are others of course

    Without any specification you create a single actor, as we have done with the HelloWorldManager
     */

    val httpFetcher = system.actorOf(
      Props(classOf[HttpFetcher]).withRouter(RoundRobinPool(5)),
      "HttpFetcher")

    val letterFetcher = system.actorOf(
      Props(classOf[LetterFetcher]).withRouter(RoundRobinPool(5)),
      "LetterFetcher")

    val helloWorldManager = system.actorOf(
      Props(classOf[HelloWorldManager]),
      "HelloWorldManager")

    try {
      /*
      The ask pattern is used to query an async "process" (here an actor)
      There are two basic situations that can come out of such an async process:
       - A result: Success or Failure
       - A timeout

       We need a thread executor, this is the actorsystem's dispatcher
       There are other ways. Without an actorsystem it is Alternative 1).

       As the ask pattern can timeout you need to set a timeout. This is
       implicitly defined to make for a nicer code. Explicitly this would be Alternative 2)

       It is important to note, that the ask pattern implicitly creates a temporary actor
       that lives as long as the computation takes, or the timeout occurs.
       */

      // Alternative 1)
      //import scala.concurrent.ExecutionContext.Implicits.global
      import system.dispatcher

      import scala.concurrent.duration._
      implicit val timeout = Timeout(20 seconds)

      // Alternative 2)
      //helloWorldManager.?(HelloWorldManager.SayTheThing())(Timeout(20 seconds)) onComplete {
      (helloWorldManager ? HelloWorldManager.SayTheThing()) onComplete {

        /*
        This is a pattern matcher. We go through and check if that specific
        case class is found. Then the corresponding code will be executed

        There are also alternative ways doing this, but they aren't the scala
        way to do it...
         */

        case Success(HelloWorldManager.ReturnTheThing(thing)) =>
          println("We are done! I am saying:")
          println(thing)

          /*
          This is another showcase, I will discuss this at the very end!
          I apologize for not integrating this into the akka showcase.
           */
          println()
          println("Another showcase with traits")
          val traitShowcase = new TraitShowcase
          println(traitShowcase.sayHiWithUser("carsten"))
          println(traitShowcase.sayHiWithUserAge("carsten", 36))

          sys.exit()
        case Failure(exception) =>
          sys.error(s"Unfortunately there was an error! ${exception.getMessage}.")
        case x =>
          Console.err.println(s"Ups!!! This is an unexpected response: ${x.getClass}.")
          sys.exit()
      }
    } catch {
      case exception: Exception =>
        sys.error(s"Unfortunately there was an error! ${exception.getMessage}.")
    }

  }
}
