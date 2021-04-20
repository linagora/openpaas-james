package com.linagora.openpaas.james.jmap.json

import com.linagora.openpaas.james.jmap.model.{Action, AppendIn, Comparator, Condition, Field, Filter, FilterGetIds, FilterGetNotFound, FilterGetRequest, FilterGetResponse, FilterSetError, FilterSetRequest, FilterSetResponse, FilterSetUpdateResponse, FilterState, Rule, RuleWithId, Update}
import org.apache.james.jmap.mail.Name
import org.apache.james.mailbox.model.MailboxId
import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json, Reads, Writes}

import javax.inject.Inject

case class FilterSerializer @Inject()(mailboxIdFactory: MailboxId.Factory) {

  implicit val filterStateFormat: Format[FilterState] = Json.valueFormat[FilterState]
  implicit val comparatorFormat: Format[Comparator] = Json.valueFormat[Comparator]
  implicit val fieldFormat: Format[Field] = Json.valueFormat[Field]
  implicit val nameFormat: Format[Name] = Json.valueFormat[Name]

  implicit val filterGetIds: Reads[FilterGetIds] = Json.valueReads[FilterGetIds]
  implicit val filterGetRequestReads: Reads[FilterGetRequest] = Json.reads[FilterGetRequest]

  implicit val mailboxIdWrites: Writes[MailboxId] = mailboxId => JsString(mailboxId.serialize)
  implicit val appendIn: Writes[AppendIn] = Json.writes[AppendIn]
  implicit val actionWrites: Writes[Action] = Json.writes[Action]
  implicit val conditionWrites: Writes[Condition] = Json.writes[Condition]
  implicit val ruleWrites: Writes[Rule] = Json.writes[Rule]
  implicit val filterWrites: Writes[Filter] = Json.writes[Filter]
  implicit val notFoundWrites: Writes[FilterGetNotFound] = Json.valueWrites[FilterGetNotFound]
  implicit val filterState: Writes[FilterState] = Json.valueWrites[FilterState]
  implicit val filterGetResponseWrites: Writes[FilterGetResponse] = Json.writes[FilterGetResponse]

  implicit val conditionReads: Reads[Condition] = Json.reads[Condition]
  implicit val mailboxIdReads: Reads[MailboxId] = {
    case JsString(serializedMailboxId) => JsSuccess(mailboxIdFactory.fromString(serializedMailboxId))
    case _ => JsError()
  }
  implicit val appendInReads: Reads[AppendIn] = Json.reads[AppendIn]
  implicit val actionReads: Reads[Action] = Json.reads[Action]
  implicit val ruleWithIdReads: Reads[RuleWithId] = Json.reads[RuleWithId]
  implicit val updateReads: Reads[Update] = Json.valueReads[Update]
  implicit val filterSetRequestReads: Reads[FilterSetRequest] = Json.reads[FilterSetRequest]

  implicit val filterSetErrorWrites: Writes[FilterSetError] = Json.writes[FilterSetError]
  implicit val filterSetUpdateResponseWrites: Writes[FilterSetUpdateResponse] = Json.valueWrites[FilterSetUpdateResponse]
  implicit val filterSetResponseWrites: Writes[FilterSetResponse] = Json.writes[FilterSetResponse]

  def serializeFilterGetResponse(response: FilterGetResponse): JsValue = Json.toJson(response)

  def serializeFilterSetResponse(response: FilterSetResponse): JsValue = Json.toJson(response)

  def deserializeFilterGetRequest(input: JsValue): JsResult[FilterGetRequest] = Json.fromJson[FilterGetRequest](input)

  def deserializeFilterSetRequest(input: JsValue): JsResult[FilterSetRequest] = Json.fromJson[FilterSetRequest](input)
}
