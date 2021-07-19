package com.linagora.tmail.james.jmap.json

import com.linagora.tmail.james.jmap.json.Fixture.{MAILBOX_ID_FACTORY, MESSAGE_ID_FACTORY}
import org.apache.james.jmap.json.EmailSetSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import play.api.libs.json.{JsValue, Json}


class EmailSendSerializerTest {

  private val emailSetSerializer: EmailSetSerializer = new EmailSetSerializer(MESSAGE_ID_FACTORY, MAILBOX_ID_FACTORY)

  private val emailSendSerializer: EmailSendSerializer = new EmailSendSerializer(emailSetSerializer)

  @Test
  def deserializeEmailSendCreationCreateShouldSuccess(): Unit = {
    val jsInput: JsValue = Json.parse(
      """
        |{
        |    "email/create": {
        |        "mailboxIds": {
        |            "123": true
        |        },
        |        "to": [
        |            {
        |                "email": "email@domain.tld"
        |            }
        |        ],
        |        "from": [
        |            {
        |                "email": "from@domain.tld"
        |            }
        |        ]
        |    },
        |    "emailSubmission/set": {
        |        "envelope": {
        |            "mailFrom": {
        |                "email": "email3@domain.tld"
        |            },
        |            "rcptTo": [
        |                {
        |                    "email": "email4@domain.tld"
        |                }
        |            ]
        |        }
        |    }
        |}""".stripMargin)

    assertThat(emailSendSerializer.deserializeEmailSendCreationRequest(jsInput).isSuccess)
      .isEqualTo(true)
  }

  @Test
  def deserializerEmailSendRequestShouldSuccess(): Unit = {
    val jsInput: JsValue = Json.parse(
      s"""
         |{
         |    "accountId": "aHR0cHM6Ly93d3cuYmFzZTY0ZW5jb2RlLm9yZy8",
         |    "create": {
         |        "K87": {
         |            "email/create": {
         |                "mailboxIds": {
         |                    "123": true
         |                },
         |                "to": [
         |                    {
         |                        "email": "email@domain.tld"
         |                    }
         |                ],
         |                "from": [
         |                    {
         |                        "email": "from@domain.tld"
         |                    }
         |                ]
         |            },
         |            "emailSubmission/set": {
         |                "envelope": {
         |                    "mailFrom": {
         |                        "email": "email3@domain.tld"
         |                    },
         |                    "rcptTo": [
         |                        {
         |                            "email": "email4@domain.tld"
         |                        }
         |                    ]
         |                }
         |            }
         |        }
         |    },
         |    "onSuccessUpdateEmail": {
         |        "#K87": {
         |            "keywords": {
         |                "$$sent": true
         |            }
         |        }
         |    },
         |    "onSuccessDestroyEmail": [
         |        "#K87"
         |    ]
         |}""".stripMargin)

    assertThat(emailSendSerializer.deserializerEmailSendRequest(jsInput).isSuccess)
      .isEqualTo(true)
  }
}
