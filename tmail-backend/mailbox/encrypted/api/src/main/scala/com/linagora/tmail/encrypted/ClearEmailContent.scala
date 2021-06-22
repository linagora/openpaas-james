package com.linagora.tmail.encrypted

import org.apache.james.jmap.api.model.Preview
import org.apache.james.mailbox.model.ParsedAttachment
import org.apache.james.mailbox.store.mail.model.impl.MessageParser
import org.apache.james.mime4j.dom.Message
import org.apache.james.util.mime.MessageContentExtractor

import javax.inject.Inject
import scala.jdk.CollectionConverters._
import scala.util.Try

class ClearEmailContentFactory @Inject()(messageParser: MessageParser,
                                         messageContentExtractor: MessageContentExtractor,
                                         previewFactory: Preview.Factory) {

  def from(message: Message): Try[ClearEmailContent] = {
    for {
      attachments <- Try(messageParser.retrieveAttachments(message).asScala.toList)
      preview <- Try(previewFactory.fromMime4JMessage(message))
      html <- Try(messageContentExtractor.extract(message)
        .getHtmlBody
        .orElse(""))
    } yield {
      ClearEmailContent(
        preview = preview,
        hasAttachment = attachments.nonEmpty,
        html = html,
        attachments = attachments)
    }
  }
}

case class ClearEmailContent(preview: Preview,
                             hasAttachment: Boolean,
                             html: String,
                             attachments: List[ParsedAttachment])
