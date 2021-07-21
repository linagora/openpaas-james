package com.linagora.tmail.james.common

import com.linagora.tmail.james.common.EncryptHelper.uploadPublicKey
import com.linagora.tmail.james.common.LinagoraEmailSendMethodContract.BOB_INBOX_PATH
import io.netty.handler.codec.http.HttpHeaderNames.ACCEPT
import io.restassured.RestAssured.{`given`, requestSpecification}
import io.restassured.http.ContentType.JSON
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.apache.http.HttpStatus
import org.apache.james.GuiceJamesServer
import org.apache.james.jmap.core.ResponseObject.SESSION_STATE
import org.apache.james.jmap.http.UserCredential
import org.apache.james.jmap.rfc8621.contract.Fixture.{ACCEPT_RFC8621_VERSION_HEADER, ACCOUNT_ID, BOB, BOB_PASSWORD, DOMAIN, authScheme, baseRequestSpecBuilder}
import org.apache.james.mailbox.model.{MailboxPath, MessageId}
import org.apache.james.mime4j.dom.Message
import org.apache.james.modules.MailboxProbeImpl
import org.apache.james.utils.DataProbeImpl
import org.junit.jupiter.api.{BeforeEach, Test}

import java.nio.charset.StandardCharsets

object LinagoraEmailSendMethodContract {
  val MESSAGE: Message = Message.Builder.of
    .setSubject("test")
    .setSender(BOB.asString)
    .setFrom(BOB.asString)
    .setTo(BOB.asString)
    .setBody("test mail", StandardCharsets.UTF_8)
    .build

  val MESSAGE_PREVIEW: String = "test mail"

  val BOB_INBOX_PATH: MailboxPath = MailboxPath.inbox(BOB)
}

trait LinagoraEmailSendMethodContract {

  @BeforeEach
  def setUp(server: GuiceJamesServer): Unit = {
    server.getProbe(classOf[DataProbeImpl])
      .fluent()
      .addDomain(DOMAIN.asString)
      .addUser(BOB.asString(), BOB_PASSWORD)

    requestSpecification = baseRequestSpecBuilder(server)
      .setAuth(authScheme(UserCredential(BOB, BOB_PASSWORD)))
      .addHeader(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .build()

    val mailboxProbe: MailboxProbeImpl = server.getProbe(classOf[MailboxProbeImpl])
    mailboxProbe.createMailbox(BOB_INBOX_PATH)

    uploadPublicKey(ACCOUNT_ID, requestSpecification)
  }

  def randomMessageId: MessageId

  @Test
  def emailSendShouldReturnSuccess(server: GuiceJamesServer): Unit = {

  }

  @Test
  def emailSendShouldSendMailSuccessfully(server: GuiceJamesServer): Unit = {

  }

  @Test
  def emailSendShouldSendMailSuccessfullyToSelf(server: GuiceJamesServer): Unit = {

  }

  @Test
  def emailSendShouldSendMailSuccessfullyToBothRecipients(server: GuiceJamesServer): Unit = {

  }

  @Test
  def envelopShouldBeOptional(server: GuiceJamesServer): Unit = {

  }

  @Test
  def envelopeFromShouldAcceptAliases(server: GuiceJamesServer): Unit = {

  }

  @Test
  def envelopeToShouldAcceptAliases(server: GuiceJamesServer): Unit = {

  }

  @Test
  def emailSubmissionSetShouldAcceptIdentityId(server: GuiceJamesServer): Unit = {

  }

  @Test
  def emailSendShouldRejectNoRecipients(server: GuiceJamesServer): Unit = {

  }

  @Test
  def emailSendShouldAddAnEmailInTargetMailbox(server: GuiceJamesServer) : Unit = {

  }

  @Test
  def emailSendShouldSupportAttachment(server: GuiceJamesServer) : Unit = {}

  //region emailSet create
  @Test
  def emailSetCreateShouldPositionSpecificHeaders(server: GuiceJamesServer): Unit = {

  }
  @Test
  def emailSetCreateSpecificHeaderShouldMatchSupportedType(server: GuiceJamesServer): Unit = {

  }

  @Test
  def emailSetCreateSpecificHeadersCannotOverrideConvenienceHeader(server: GuiceJamesServer): Unit = {

  }
  @Test
  def emailSetCreateShouldFailWhenBadJsonPayloadForSpecificHeader(server: GuiceJamesServer): Unit = {

  }

  @Test
  def emailSetCreateSpecificContentHeadersShouldBeRejected(server: GuiceJamesServer): Unit = {

  }
  @Test
  def emailSetCreateShouldFailWhenEmailContainsHeadersProperties(server: GuiceJamesServer): Unit = {

  }

  @Test
  def emailSetCreateShouldHandleAddressHeaders(server: GuiceJamesServer): Unit = {}

  @Test
  def emailSetCreateWithMultipleSenderShouldNotCrash(server: GuiceJamesServer): Unit = {}

  @Test
  def emailSetCreateShouldFailIfForbiddenMailboxes(server: GuiceJamesServer): Unit = {}

  @Test
  def emailSetCreateShouldRejectEmptyMailboxIds(server: GuiceJamesServer): Unit = {}

  @Test
  def emailSetCreateShouldRejectInvalidMailboxIds(server: GuiceJamesServer): Unit = {}

  @Test
  def emailSetCreateShouldRejectNoMailboxIds(server: GuiceJamesServer): Unit = {}

  @Test
  def emailSetCreateShouldSupportHtmlBody(server: GuiceJamesServer): Unit = {}

  @Test
  def emailSetCreateShouldSucceedWhenPartPropertiesOmitted(server: GuiceJamesServer): Unit = {}

  @Test
  def emailSetCreateShouldFailWhenMultipleBodyParts(server: GuiceJamesServer): Unit = {}
  //endregion


  //region onSuccessUpdateEmail & onSuccessDestroyEmail checking
  @Test
  def onSuccessUpdateEmailShouldTriggerAnImplicitEmailSetCall(server: GuiceJamesServer): Unit = {

  }

  @Test
  def emailSendShouldFailWhenOnSuccessUpdateEmailMissesTheCreationIdSharp(server: GuiceJamesServer): Unit = {

  }

  @Test
  def emailSendShouldFailWhenOnSuccessUpdateEmailDoesNotReferenceACreationWithinThisCall(server: GuiceJamesServer): Unit = {

  }

  @Test
  def onSuccessDestroyEmailShouldTriggerAnImplicitEmailSetCall(server: GuiceJamesServer): Unit = {

  }

  @Test
  def emailSendShouldFailWhenOnSuccessDestroyEmailMissesTheCreationIdSharp(server: GuiceJamesServer): Unit = {

  }

  @Test
  def emailSendShouldFailWhenOnSuccessDestroyEmailDoesNotReferenceACreationWithinThisCall(server: GuiceJamesServer): Unit = {

  }

  @Test
  def implicitSetShouldNotBeAttemptedWhenNotSpecified(server: GuiceJamesServer): Unit = {

  }
  //endregion


  //region basic valid request
  @Test
  def methodShouldReturnFailWhenMissingOneCapability(): Unit = {
    val request: String =
      s"""{
         |  "using": ["urn:ietf:params:jmap:core"],
         |  "methodCalls": [
         |    ["Email/send", {
         |      "accountId": "$ACCOUNT_ID",
         |      "create": {
         |        "K87": {
         |            "email/create": {
         |            },
         |            "emailSubmission/set": {
         |            }
         |        }
         |      }
         |    }, "c1"]
         |  ]
         |}""".stripMargin

    val response: String = `given`
      .body(request)
    .when()
      .post()
    .`then`
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .extract()
      .body()
      .asString()

    assertThatJson(response).isEqualTo(
      s"""{
         |  "sessionState":"${SESSION_STATE.value}",
         |  "methodResponses": [
         |    ["error", {
         |      "type": "unknownMethod",
         |      "description": "Missing capability(ies): urn:ietf:params:jmap:mail, urn:ietf:params:jmap:submission, com:linagora:params:jmap:pgp"
         |    },"c1"]
         |  ]
         |}""".stripMargin)
  }

  @Test
  def methodShouldReturnFailWhenMissingAllCapabilities(): Unit = {
    val request: String =
      s"""{
         |  "using": [],
         |  "methodCalls": [
         |    ["Email/send", {
         |      "accountId": "$ACCOUNT_ID",
         |      "create": {
         |        "K87": {
         |            "email/create": {
         |            },
         |            "emailSubmission/set": {
         |            }
         |        }
         |      }
         |    }, "c1"]
         |  ]
         |}""".stripMargin

    val response: String = `given`
      .body(request)
    .when()
      .post()
    .`then`
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .extract()
      .body()
      .asString()

    assertThatJson(response).isEqualTo(
      s"""{
         |  "sessionState":"${SESSION_STATE.value}",
         |  "methodResponses": [
         |    ["error", {
         |      "type": "unknownMethod",
         |      "description": "Missing capability(ies): urn:ietf:params:jmap:core, urn:ietf:params:jmap:mail, urn:ietf:params:jmap:submission, com:linagora:params:jmap:pgp"
         |    },"c1"]
         |  ]
         |}""".stripMargin)
  }

  @Test
  def methodShouldFailWhenWrongAccountId(): Unit = {
    val request: String =
      s"""{
         |  "using": ["urn:ietf:params:jmap:core",
         |            "urn:ietf:params:jmap:mail",
         |            "urn:ietf:params:jmap:submission",
         |            "com:linagora:params:jmap:pgp"],
         |  "methodCalls": [
         |    ["Email/send", {
         |      "accountId": "unknownAccountId",
         |      "create": {
         |        "K87": {
         |            "email/create": {
         |            },
         |            "emailSubmission/set": {
         |            }
         |        }
         |      }
         |    }, "c1"]
         |  ]
         |}""".stripMargin

    val response: String = `given`
      .body(request)
    .when()
      .post()
    .`then`
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .extract()
      .body()
      .asString()

    assertThatJson(response).isEqualTo(
      s"""{
         |    "sessionState": "${SESSION_STATE.value}",
         |    "methodResponses": [[
         |            "error",
         |            {
         |                "type": "accountNotFound"
         |            },
         |            "c1"
         |        ]]
         |}""".stripMargin)
  }

  @Test
  def methodShouldRejectOtherAccountIds(server: GuiceJamesServer) : Unit = {

  }

  @Test
  def methodShouldRejectExtraProperties(server: GuiceJamesServer) : Unit = {

  }
  //endregion
}
