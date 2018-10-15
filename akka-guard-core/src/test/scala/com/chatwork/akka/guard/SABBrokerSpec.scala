package com.chatwork.akka.guard

import akka.actor.{ ActorPath, ActorRef, ActorSelection, ActorSystem, Props }
import akka.pattern.ask
import akka.testkit.TestKit
import akka.util.Timeout
import org.scalacheck.Gen
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks
import org.scalatest.time.{ Millis, Seconds, Span }

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Try }

class SABBrokerSpec
    extends TestKit(ActorSystem("SABBrokerSpec"))
    with FeatureSpecLike
    with BeforeAndAfterAll
    with GivenWhenThen
    with PropertyChecks
    with Matchers
    with ScalaFutures {

  val BoundaryLength           = 50
  val genShortStr: Gen[String] = Gen.asciiStr.suchThat(_.length < BoundaryLength)
  val genLongStr: Gen[String]  = Gen.asciiStr.suchThat(_.length >= BoundaryLength)

  val failedMessage               = "failed!!"
  val errorMessage                = "error!!"
  val successMessage              = "success!!"
  val failedResponse: Try[String] = Failure(new Exception(failedMessage))
  val isFailed: String => Boolean = _ => false

  feature("SABBrokerSpec") {

    scenario("Success") {

      Given("broker pattern 1")
      implicit val timeout: Timeout = Timeout(5.seconds)
      val sabBrokerName1: String    = "broker-1"
      val messageId: String         = "id-1"
      val config: SABBrokerConfig = SABBrokerConfig(
        maxFailures = 9,
        failureTimeout = 10.seconds,
        resetTimeout = 1.hour
      )
      val handler: String => Future[String] = {
        case request if request.length < BoundaryLength  => Future.failed(new Exception(errorMessage))
        case request if request.length >= BoundaryLength => Future.successful(successMessage)
      }
      val sabBroker: ActorRef        = system.actorOf(Props(new SABBroker(config, failedResponse, isFailed)), sabBrokerName1)
      val messagePath: ActorPath     = system / sabBrokerName1 / SABActor.name(messageId)
      val messageRef: ActorSelection = system.actorSelection(messagePath)

      When("Long input")
      Then("return success message")
      forAll(genLongStr) { value =>
        val message = SABMessage(messageId, value, handler)
        (sabBroker ? message).mapTo[String].futureValue shouldBe successMessage
      }

      And("Status Closed")
      (messageRef ? SABActor.GetStatus)
        .mapTo[SABStatus].futureValue shouldBe SABStatus.Closed

      When("Short input")
      Then("return error message")
      forAll(genShortStr) { value =>
        val message = SABMessage(messageId, value, handler)
        (sabBroker ? message).mapTo[String].failed.futureValue.getMessage shouldBe errorMessage
      }

      When("Short input")
      Then("return failed message")
      forAll(genShortStr) { value =>
        val message = SABMessage(messageId, value, handler)
        (sabBroker ? message).mapTo[String].failed.futureValue.getMessage shouldBe failedMessage
      }

      And("Status Open")
      (messageRef ? SABActor.GetStatus)
        .mapTo[SABStatus].futureValue shouldBe SABStatus.Open
    }

    scenario("Future is slow") {

      Given("broker pattern 2")
      import system.dispatcher
      implicit val timeout: Timeout = Timeout(5.seconds)
      val sabBrokerName2: String    = "broker-2"
      val messageId: String         = "id-2"
      val config: SABBrokerConfig = SABBrokerConfig(
        maxFailures = 9,
        failureTimeout = 500.milliseconds,
        resetTimeout = 1.hour
      )
      val handler: String => Future[String] = _ =>
        Future {
          Thread.sleep(1000L)
          successMessage
      }
      val sabBroker: ActorRef = system.actorOf(Props(new SABBroker(config, failedResponse, isFailed)), sabBrokerName2)

      When("input slow handler")
      val message = SABMessage(messageId, "???", handler)
      (sabBroker ? message).mapTo[String].futureValue shouldBe successMessage
    }

  }

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  override protected def afterAll(): Unit = {
    shutdown()
  }
}
