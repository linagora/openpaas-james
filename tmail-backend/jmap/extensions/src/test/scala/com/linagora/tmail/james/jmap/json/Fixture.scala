package com.linagora.tmail.james.jmap.json

import org.apache.james.jmap.core.{AccountId, Id}
import org.apache.james.mailbox.inmemory.{InMemoryId, InMemoryMessageId}
import org.apache.james.mailbox.model.MessageId

object Fixture {
  lazy val ACCOUNT_ID: AccountId = AccountId(Id.validate("aHR0cHM6Ly93d3cuYmFzZTY0ZW5jb2RlLm9yZy8").toOption.get)
  lazy val MESSAGE_ID_FACTORY: MessageId.Factory = new InMemoryMessageId.Factory
  lazy val MAILBOX_ID_FACTORY: InMemoryId.Factory = new InMemoryId.Factory
}
