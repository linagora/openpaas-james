package com.linagora.tmail.james.jmap.method

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import com.linagora.tmail.james.jmap.json.EmailSendSerializer
import com.linagora.tmail.james.jmap.method.CapabilityIdentifier.LINAGORA_PGP
import com.linagora.tmail.james.jmap.model.EmailSubmissionHelper.{resolveEnvelope, toMimeMessage}
import com.linagora.tmail.james.jmap.model.{EmailSendCreationId, EmailSendCreationRequest, EmailSendCreationRequestInvalidException, EmailSendCreationResponse, EmailSendId, EmailSendRequest, EmailSendResults, EmailSetCreationFailure, EmailSetCreationResult, EmailSetCreationSuccess, EmailSubmissionCreationRequest}
import eu.timepit.refined.auto._
import org.apache.james.core.{MailAddress, Username}
import org.apache.james.jmap.core.CapabilityIdentifier.{CapabilityIdentifier, EMAIL_SUBMISSION, JMAP_CORE, JMAP_MAIL}
import org.apache.james.jmap.core.Invocation.{Arguments, MethodName}
import org.apache.james.jmap.core.{Invocation, UTCDate, UuidState}
import org.apache.james.jmap.json.ResponseSerializer
import org.apache.james.jmap.mail.{BlobId, Email, EmailCreationRequest, EmailCreationResponse, EmailSubmissionId, Envelope, ThreadId}
import org.apache.james.jmap.method.EmailSubmissionSetMethod.{LOGGER, MAIL_METADATA_USERNAME_ATTRIBUTE}
import org.apache.james.jmap.method.{EmailSetMethod, ForbiddenFromException, ForbiddenMailFromException, InvocationWithContext, MessageNotFoundException, Method, MethodRequiringAccountId, NoRecipientException}
import org.apache.james.jmap.routes.{BlobResolvers, ProcessingContext, SessionSupplier}
import org.apache.james.mailbox.MessageManager.AppendCommand
import org.apache.james.mailbox.model.{FetchGroup, MailboxId, MessageId}
import org.apache.james.mailbox.{MailboxManager, MailboxSession, MessageIdManager}
import org.apache.james.metrics.api.MetricFactory
import org.apache.james.mime4j.dom.Message
import org.apache.james.queue.api.MailQueueFactory.SPOOL
import org.apache.james.queue.api.{MailQueue, MailQueueFactory}
import org.apache.james.rrt.api.CanSendFrom
import org.apache.james.server.core.MailImpl
import org.apache.james.util.html.HtmlTextExtractor
import org.apache.mailet.{Attribute, AttributeValue}
import org.reactivestreams.Publisher
import play.api.libs.json.{JsError, JsObject, JsSuccess}
import reactor.core.scala.publisher.{SFlux, SMono}
import reactor.core.scheduler.Schedulers

import java.time.ZonedDateTime
import java.util.Date
import javax.annotation.PreDestroy
import javax.inject.Inject
import javax.mail.Flags
import javax.mail.internet.{InternetAddress, MimeMessage}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class EncryptedEmailFastViewGetMethodModule extends AbstractModule {
  override def configure(): Unit = {
    Multibinder.newSetBinder(binder(), classOf[Method])
      .addBinding()
      .to(classOf[EmailSendMethod])
  }
}

class EmailSendMethod @Inject()(serializer: EmailSendSerializer,
                                messageIdManager: MessageIdManager,
                                mailQueueFactory: MailQueueFactory[_ <: MailQueue],
                                canSendFrom: CanSendFrom,
                                blobResolvers: BlobResolvers,
                                htmlTextExtractor: HtmlTextExtractor,
                                mailboxManager: MailboxManager,
                                emailSetMethod: EmailSetMethod,
                                val metricFactory: MetricFactory,
                                val sessionSupplier: SessionSupplier) extends MethodRequiringAccountId[EmailSendRequest] {

  override val methodName: MethodName = MethodName("Email/send")

  override val requiredCapabilities: Set[CapabilityIdentifier] = Set(JMAP_CORE, JMAP_MAIL, EMAIL_SUBMISSION, LINAGORA_PGP)

  var queue: MailQueue = _

  def init: Unit = queue = mailQueueFactory.createQueue(SPOOL)

  @PreDestroy def dispose: Unit =
    Try(queue.close())
      .recover(e => LOGGER.debug("error closing queue", e))

  override def getRequest(mailboxSession: MailboxSession,
                          invocation: Invocation): Either[Exception, EmailSendRequest] = {
    serializer.deserializerEmailSendRequest(invocation.arguments.value) match {
      case JsSuccess(emailSendRequest, _) => emailSendRequest.validate
      case errors: JsError => Left(new IllegalArgumentException(ResponseSerializer.serialize(errors).toString))
    }
  }

  override def doProcess(capabilities: Set[CapabilityIdentifier],
                         invocation: InvocationWithContext,
                         mailboxSession: MailboxSession,
                         request: EmailSendRequest): Publisher[InvocationWithContext] =
    create(request, mailboxSession, invocation.processingContext)
      .flatMapMany(createdResults => {
        val explicitInvocation: InvocationWithContext = InvocationWithContext(
          invocation = Invocation(
            methodName = invocation.invocation.methodName,
            arguments = Arguments(serializer.serializerEmailSendResponse(
              createdResults._1.asResponse(request.accountId, UuidState.INSTANCE))
              .as[JsObject]),
            methodCallId = invocation.invocation.methodCallId),
          processingContext = createdResults._2)

        val emailSetCall: SMono[InvocationWithContext] = request.implicitEmailSetRequest(createdResults._1.resolveMessageId)
          .fold(e => SMono.error(e),
            maybeEmailSetRequest => maybeEmailSetRequest.map(emailSetRequest => emailSetMethod.doProcess(
              capabilities = capabilities,
              invocation = invocation,
              mailboxSession = mailboxSession,
              request = emailSetRequest))
              .getOrElse(SMono.empty))

        SFlux.concat(SMono.just(explicitInvocation), emailSetCall)
      })

  private def create(request: EmailSendRequest,
                     mailboxSession: MailboxSession,
                     processingContext: ProcessingContext): SMono[(EmailSendResults, ProcessingContext)] =
    SFlux.fromIterable(request.create.view)
      .fold[SMono[(EmailSendResults, ProcessingContext)]](SMono.just(EmailSendResults.empty -> processingContext)) {
        (acc: SMono[(EmailSendResults, ProcessingContext)], elem: (EmailSendCreationId, JsObject)) => {
          val (emailSendCreationId, jsObject) = elem
          acc.flatMap {
            case (creationResult, processingContext) =>
              createEach(emailSendCreationId, jsObject, mailboxSession, processingContext)
                .map(e => EmailSendResults.merge(creationResult, e._1) -> e._2)
          }
        }
      }
      .flatMap(any => any)
      .subscribeOn(Schedulers.elastic())

  private def createEach(clientId: EmailSendCreationId,
                         jsObject: JsObject,
                         mailboxSession: MailboxSession,
                         processingContext: ProcessingContext): SMono[(EmailSendResults, ProcessingContext)] = {
    parseCreationRequest(jsObject)
      .fold(error => SMono.error(error),
        createEmailAndEmailSubmission(clientId, mailboxSession, _))
      .map(response => EmailSendResults.created(clientId, response) -> processingContext)
      .onErrorResume(error => SMono.just(EmailSendResults.notCreated(clientId, error) -> processingContext))
  }

  private def createEmailAndEmailSubmission(clientId: EmailSendCreationId,
                                            mailboxSession: MailboxSession,
                                            request: EmailSendCreationRequest): SMono[EmailSendCreationResponse] =
    createEmail(clientId, mailboxSession, request.emailCreate)
      .flatMap {
        case failure: EmailSetCreationFailure => SMono.error(failure.error)
        case success: EmailSetCreationSuccess => createEmailSubmission(mailboxSession, success.response, request.emailSubmissionSet)
      }

  private def parseCreationRequest(jsObject: JsObject): Either[Exception, EmailSendCreationRequest] =
  EmailSendCreationRequest.validateProperties(jsObject)
    .flatMap(validJson => serializer.deserializeEmailSendCreationRequest(validJson) match {
      case JsSuccess(createRequest, _) => Right(createRequest)
      case JsError(errors) => Left(EmailSendCreationRequestInvalidException.parse(errors))
    })

  def createEmail(clientId: EmailSendCreationId,
                  mailboxSession: MailboxSession,
                  request: EmailCreationRequest): SMono[EmailSetCreationResult] = {
    val mailboxIds: List[MailboxId] = request.mailboxIds.value
    if (mailboxIds.size != 1) {
      SMono.just(EmailSetCreationFailure(clientId, new IllegalArgumentException("mailboxIds need to have size 1")))
    } else {
      SMono.fromCallable(() => request.toMime4JMessage(blobResolvers, htmlTextExtractor, mailboxSession))
        .flatMap(either => either.fold(error => SMono.just(EmailSetCreationFailure(clientId, error)),
          message => append(clientId, request, message, mailboxSession, mailboxIds)))
        .onErrorResume(e => SMono.just[EmailSetCreationResult](EmailSetCreationFailure(clientId, e)))
        .subscribeOn(Schedulers.elastic())
    }
  }

  def createEmailSubmission(mailboxSession: MailboxSession,
                            emailCreationResponse: EmailCreationResponse,
                            request: EmailSubmissionCreationRequest): SMono[EmailSendCreationResponse] = {
    val emailId: MessageId = emailCreationResponse.id
    for {
      message <- SFlux(messageIdManager.getMessagesReactive(List(emailId).asJava, FetchGroup.FULL_CONTENT, mailboxSession))
        .next
        .switchIfEmpty(SMono.error(MessageNotFoundException(emailId)))
      submissionId = EmailSubmissionId.generate
      emailSendId = EmailSendId.generate
      message <- SMono.fromTry(toMimeMessage(submissionId.value.value, message))
      envelope <- SMono.fromTry(resolveEnvelope(message, request.envelope))
      _ <- SMono.fromTry(validate(mailboxSession)(message, envelope))
      mail <- SMono.fromCallable(() => {
        val mailImpl: MailImpl = MailImpl.builder()
          .name(submissionId.value.value)
          .addRecipients(envelope.rcptTo.map(_.email).asJava)
          .sender(envelope.mailFrom.email)
          .addAttribute(new Attribute(MAIL_METADATA_USERNAME_ATTRIBUTE, AttributeValue.of(mailboxSession.getUser.asString())))
          .build()
        mailImpl.setMessageNoCopy(message)
        mailImpl
      })
      _ <- SMono(queue.enqueueReactive(mail)).`then`(SMono.just(submissionId))
    } yield {
      EmailSendCreationResponse(
        id = emailSendId,
        emailSubmissionId = submissionId,
        messageId = emailId,
        blobId = emailCreationResponse.blobId,
        threadId = emailCreationResponse.threadId,
        size = emailCreationResponse.size)
    }
  }

  private def validate(session: MailboxSession)(mimeMessage: MimeMessage, envelope: Envelope): Try[MimeMessage] = {
    val forbiddenMailFrom: List[String] = (Option(mimeMessage.getSender).toList ++ Option(mimeMessage.getFrom).toList.flatten)
      .map(_.asInstanceOf[InternetAddress].getAddress)
      .filter(addressAsString => !canSendFrom.userCanSendFrom(session.getUser, Username.fromMailAddress(new MailAddress(addressAsString))))
    if (forbiddenMailFrom.nonEmpty) {
      Failure(ForbiddenMailFromException(forbiddenMailFrom))
    } else if (envelope.rcptTo.isEmpty) {
      Failure(NoRecipientException())
    } else if (!canSendFrom.userCanSendFrom(session.getUser, Username.fromMailAddress(envelope.mailFrom.email))) {
      Failure(ForbiddenFromException(envelope.mailFrom.email.asString))
    } else {
      Success(mimeMessage)
    }
  }

  private def append(clientId: EmailSendCreationId,
                     request: EmailCreationRequest,
                     message: Message,
                     mailboxSession: MailboxSession,
                     mailboxIds: List[MailboxId]): SMono[EmailSetCreationSuccess] = {
    val appendCommand: AppendCommand = AppendCommand.builder()
      .recent()
      .withFlags(request.keywords.map(_.asFlags).getOrElse(new Flags()))
      .withInternalDate(Date.from(request.receivedAt.getOrElse(UTCDate(ZonedDateTime.now())).asUTC.toInstant))
      .build(message)

    for {
      mailbox <- SMono(mailboxManager.getMailboxReactive(mailboxIds.head, mailboxSession))
      appendResult <- SMono(mailbox.appendMessageReactive(appendCommand, mailboxSession))
    } yield {
      val blobId: Option[BlobId] = BlobId.of(appendResult.getId.getMessageId).toOption
      val threadId: ThreadId = ThreadId.fromJava(appendResult.getThreadId)
      EmailSetCreationSuccess(clientId, EmailCreationResponse(appendResult.getId.getMessageId, blobId, threadId, Email.sanitizeSize(appendResult.getSize)))
    }
  }
}

