package com.linagora.tmail.james.jmap.model

import cats.implicits.toTraverseOps
import eu.timepit.refined.refineV
import org.apache.james.jmap.core.Id.{Id, IdConstraint}
import org.apache.james.jmap.core.SetError.SetErrorDescription
import org.apache.james.jmap.core.{AccountId, Id, Properties, SetError, UTCDate, UuidState}
import org.apache.james.jmap.mail.Email.Size
import org.apache.james.jmap.mail.{BlobId, DestroyIds, EmailCreationRequest, EmailCreationResponse, EmailSet, EmailSetRequest, EmailSubmissionId, Envelope, ThreadId, UnparsedMessageId}
import org.apache.james.jmap.method.WithAccountId
import org.apache.james.mailbox.MessageManager.AppendCommand
import org.apache.james.mailbox.model.MessageId
import org.apache.james.mime4j.dom.Message
import play.api.libs.json.{JsObject, JsPath, JsonValidationError}

import java.time.ZonedDateTime
import java.util.{Date, UUID}
import javax.mail.Flags

object EmailCreationRequest {
  def asAppendCommand(request: EmailCreationRequest, message: Message): AppendCommand =
    AppendCommand.builder()
      .recent()
      .withFlags(request.keywords.map(_.asFlags).getOrElse(new Flags()))
      .withInternalDate(Date.from(request.receivedAt.getOrElse(UTCDate(ZonedDateTime.now())).asUTC.toInstant))
      .build(message)
}

case class EmailSendCreationId(id: Id)

case class EmailSendRequest(accountId: AccountId,
                            create: Map[EmailSendCreationId, JsObject],
                            onSuccessUpdateEmail: Option[Map[EmailSendCreationId, JsObject]],
                            onSuccessDestroyEmail: Option[List[EmailSendCreationId]]) extends WithAccountId {

  def validate: Either[IllegalArgumentException, EmailSendRequest] = {
    val supportedCreationIds: List[EmailSendCreationId] = create.keys.toList

    validateOnSuccessUpdateEmail(supportedCreationIds)
      .flatMap(_ => validateOnSuccessDestroyEmail(supportedCreationIds))
  }

  private def validateOnSuccessUpdateEmail(supportedCreationIds: List[EmailSendCreationId]): Either[IllegalArgumentException, EmailSendRequest] =
    onSuccessUpdateEmail.getOrElse(Map())
      .keys
      .toList
      .map(id => validate(id, supportedCreationIds))
      .sequence
      .map(_ => this)

  private def validateOnSuccessDestroyEmail(supportedCreationIds: List[EmailSendCreationId]): Either[IllegalArgumentException, EmailSendRequest] =
    onSuccessDestroyEmail.getOrElse(List())
      .map(id => validate(id, supportedCreationIds))
      .sequence
      .map(_ => this)

  private def validate(creationId: EmailSendCreationId, supportedCreationIds: List[EmailSendCreationId]): Either[IllegalArgumentException, EmailSendCreationId] = {
    if (creationId.id.value.startsWith("#")) {
      val realId: String = creationId.id.value.substring(1)
      val validatedId: Either[String, Id] = refineV[IdConstraint](realId)
      validatedId
        .left.map(s => new IllegalArgumentException(s))
        .flatMap(id => if (supportedCreationIds.contains(EmailSendCreationId(id))) {
          scala.Right(EmailSendCreationId(id))
        } else {
          Left(new IllegalArgumentException(s"${creationId.id} cannot be referenced in current method call"))
        })
    } else {
      Left(new IllegalArgumentException(s"${creationId.id} cannot be retrieved as storage for EmailSubmission is not yet implemented"))
    }
  }

  def implicitEmailSetRequest(messageIdResolver: EmailSendCreationId => Either[IllegalArgumentException, MessageId]) : Either[IllegalArgumentException, Option[EmailSetRequest]] =
    for {
      update <- resolveOnSuccessUpdateEmail(messageIdResolver)
      destroy <- resolveOnSuccessDestroyEmail(messageIdResolver)
    } yield {
      if (update.isEmpty && destroy.isEmpty) {
        None
      } else {
        Some(EmailSetRequest(
          accountId = accountId,
          create = None,
          update = update,
          destroy = destroy.map(DestroyIds(_))))
      }
    }

  def resolveOnSuccessUpdateEmail(messageIdResolver: EmailSendCreationId => Either[IllegalArgumentException, MessageId]): Either[IllegalArgumentException, Option[Map[UnparsedMessageId, JsObject]]]=
    onSuccessUpdateEmail.map(map => map.toList
      .map {
        case (creationId, json) => messageIdResolver.apply(creationId).map(messageId => (EmailSet.asUnparsed(messageId), json))
      }
      .sequence
      .map(list => list.toMap))
      .sequence

  def resolveOnSuccessDestroyEmail(messageIdResolver: EmailSendCreationId => Either[IllegalArgumentException, MessageId]): Either[IllegalArgumentException, Option[List[UnparsedMessageId]]]=
    onSuccessDestroyEmail.map(list => list
      .map(creationId => messageIdResolver.apply(creationId).map(messageId => EmailSet.asUnparsed(messageId)))
      .sequence)
      .sequence

}

object EmailSendCreationRequestInvalidException {
  def parse(errors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]): EmailSendCreationRequestInvalidException = ???
}
case class EmailSendCreationRequestInvalidException(error: SetError) extends Exception

object EmailSendCreationRequest {
  private val assignableProperties: Set[String] = ???

  def validateProperties(jsObject: JsObject): Either[EmailSendCreationRequestInvalidException, JsObject] =
    jsObject.keys.diff(assignableProperties) match {
      case unknownProperties if unknownProperties.nonEmpty =>
        Left(EmailSendCreationRequestInvalidException(SetError.invalidArguments(
          SetErrorDescription("Some unknown properties were specified"),
          Some(Properties.toProperties(unknownProperties.toSet)))))
      case _ => scala.Right(jsObject)
    }

}
case class EmailSendCreationRequest(emailCreate: EmailCreationRequest,
                                    emailSubmissionSet: EmailSubmissionCreationRequest)

case class EmailSubmissionCreationRequest(identityId: Option[Id],
                                          envelope: Option[Envelope])

object EmailSendId {
  def generate: EmailSendId = EmailSendId(Id.validate(UUID.randomUUID().toString).toOption.get)
}

case class EmailSendId(value: Id)

case class EmailSendCreationResponse(id: EmailSendId,
                                     emailSubmissionId: EmailSubmissionId,
                                     messageId: MessageId,
                                     blobId: Option[BlobId],
                                     threadId: ThreadId,
                                     size: Size)

trait EmailSetCreationResult

case class EmailSetCreationSuccess(clientId: EmailSendCreationId, response: EmailCreationResponse) extends EmailSetCreationResult

case class EmailSetCreationFailure(clientId: EmailSendCreationId, error: Throwable) extends EmailSetCreationResult


object EmailSendResults {
  def empty(): EmailSendResults = ???

  def created(emailSendCreationId: EmailSendCreationId, emailSendCreationResponse: EmailSendCreationResponse): EmailSendResults=
    EmailSendResults(Some(Map(emailSendCreationId -> emailSendCreationResponse)), None)

  def notCreated(emailSendCreationId: EmailSendCreationId,throwable: Throwable ): EmailSendResults = ???

  def merge(result1: EmailSendResults, result2: EmailSendResults): EmailSendResults = ???
}



case class EmailSendResults(created: Option[Map[EmailSendCreationId, EmailSendCreationResponse]],
                            notCreated: Option[Map[EmailSendCreationId, SetError]]) {
  def asResponse(accountId: AccountId, newState: UuidState): EmailSendResponse =EmailSendResponse(
    accountId = accountId,
    newState = newState,
    created = created,
    notCreated = notCreated)

  def resolveMessageId(creationId: EmailSendCreationId): Either[IllegalArgumentException, MessageId] = ???
}

case class EmailSendResponse(accountId: AccountId,
                             newState: UuidState,
                             created: Option[Map[EmailSendCreationId, EmailSendCreationResponse]],
                             notCreated: Option[Map[EmailSendCreationId, SetError]])