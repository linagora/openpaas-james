package com.linagora.tmail.james.jmap.json

import com.linagora.tmail.james.jmap.model.{EmailSendCreationId, EmailSendCreationRequest, EmailSendRequest, EmailSendResponse, EmailSubmissionCreationRequest}
import org.apache.james.core.MailAddress
import org.apache.james.jmap.core.Id
import org.apache.james.jmap.json.EmailSetSerializer
import org.apache.james.jmap.mail.{EmailCreationRequest, EmailSubmissionAddress, EmailSubmissionCreationId, Envelope}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsError, JsObject, JsPath, JsResult, JsString, JsSuccess, JsValue, Json, Reads}

import javax.inject.Inject
import scala.util.Try

class EmailSendSerializer @Inject()(emailSetSerializer: EmailSetSerializer) {
  private implicit val emailCreationRequestReads: Reads[EmailCreationRequest] = emailSetSerializer.emailCreationRequestReads

  private implicit val mailAddressReads: Reads[MailAddress] = {
    case JsString(value) => Try(JsSuccess(new MailAddress(value)))
      .fold(e => JsError(s"Invalid mailAddress: ${e.getMessage}"), mailAddress => mailAddress)
    case _ => JsError("Expecting mailAddress to be represented by a JsString")
  }

  private implicit val emailSubmissionAddressReads: Reads[EmailSubmissionAddress] = Json.reads[EmailSubmissionAddress]
  private implicit val envelopeReads: Reads[Envelope] = Json.reads[Envelope]
  private implicit val emailSubmissionCreationRequestReads: Reads[EmailSubmissionCreationRequest] = Json.reads[EmailSubmissionCreationRequest]

  private implicit val emailSendCreationCreateReads: Reads[EmailSendCreationRequest] = (
    (JsPath \ "email/create").read[EmailCreationRequest] and
      (JsPath \ "emailSubmission/set").read[EmailSubmissionCreationRequest]
    ) (EmailSendCreationRequest.apply _)

  private implicit val creationIdFormat: Reads[EmailSendCreationId] = Json.valueFormat[EmailSendCreationId]

  private implicit val mapEmailSendCreationIdAndObjectReads: Reads[Map[EmailSendCreationId, JsObject]] =
    Reads.mapReads[EmailSendCreationId, JsObject] {
      s => Id.validate(s).fold(e => JsError(e.getMessage), partId => JsSuccess(EmailSendCreationId(partId)))
    }

  private implicit val emailSendRequestReads: Reads[EmailSendRequest] = Json.reads[EmailSendRequest]

  def deserializeEmailSendCreationRequest(input: JsValue): JsResult[EmailSendCreationRequest] =
    Json.fromJson[EmailSendCreationRequest](input)

  def deserializerEmailSendRequest(input: JsValue): JsResult[EmailSendRequest] =
    Json.fromJson[EmailSendRequest](input)

  def serializerEmailSendResponse(emailSendResponse: EmailSendResponse) : JsValue = ???
}
