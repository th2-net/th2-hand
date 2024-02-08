/*
 * Copyright 2024 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.hand.builders.events

import com.exactpro.remotehand.rhdata.RhResponseCode
import com.exactpro.remotehand.rhdata.RhScriptResult
import com.exactpro.th2.act.grpc.hand.RhActionsBatch
import com.exactpro.th2.act.grpc.hand.RhBatchResponse
import com.exactpro.th2.common.grpc.EventID
import com.exactpro.th2.common.grpc.EventStatus
import com.exactpro.th2.common.grpc.MessageID
import com.exactpro.th2.common.message.toJson
import com.exactpro.th2.common.schema.box.configuration.BoxConfiguration
import com.exactpro.th2.common.schema.factory.CommonFactory
import com.exactpro.th2.common.utils.message.toTimestamp
import com.exactpro.th2.hand.messages.responseexecutor.ActionsBatchExecutorResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isNotBlank
import strikt.assertions.isSameInstanceAs
import strikt.assertions.isTrue
import java.time.Instant
import kotlin.test.assertEquals

internal class DefaultEventBuilderTest {

    private val factory = mock<CommonFactory> {
        on { newEventIDBuilder() }.thenAnswer {
            EventID.newBuilder().setBookName(BOOK).setScope(SCOPE)
        }
        on { boxConfiguration }.thenReturn(
            BoxConfiguration().apply {
                bookName = BOOK
                boxName = BOX_NAME
            }
        )
    }
    private val builder = DefaultEventBuilder(factory)

    @BeforeEach
    fun beforeEach() {
        verify(factory).boxConfiguration
        clearInvocations(factory)
    }

    @AfterEach
    fun afterEach() {
        verifyNoMoreInteractions(factory)
    }

    @Test
    fun `build event test`() {
        val now = Instant.now()
        val rhActionsBatch = RhActionsBatch.newBuilder().apply {
            eventName = "test-event-name"
            parentEventIdBuilder.setBookName("$BOOK-1").setScope("$SCOPE-1")
            additionalEventInfoBuilder
                .setDescription("test-description")
                .setPrintTable(true)
                .addKeys("test-key")
                .addValues("test-value")
                .setRequestParamsTableTitle("test-title")
            storeActionMessages = true
        }.build()
        val rhBatchResponse = RhBatchResponse.newBuilder().apply {
            scriptStatus = RhBatchResponse.ScriptExecutionStatus.EXECUTION_ERROR
            sessionId = "test-session-id"
            addResultBuilder()
                .setActionId("test-action-id")
                .setResult("test-result")
        }.build()
        val scriptResult = RhScriptResult().apply {
            code = RhResponseCode.TOOL_BUSY.code
            errorMessage = "test-error-message"
        }
        val messageIDs = listOf<MessageID>(
            MessageID.newBuilder().apply {
                bookName = "$BOOK-1"
                sequence = 1
                timestamp = Instant.now().toTimestamp()
                connectionIdBuilder.setSessionAlias("test-session-alias")
            }.build()
        )
        val actionsBatchExecutorResponse = ActionsBatchExecutorResponse(
            rhBatchResponse,
            scriptResult,
            messageIDs
        )

        val event = builder.buildEvent(now, rhActionsBatch, actionsBatchExecutorResponse)

        expectThat(event) {
            get { id }.and {
                get { id }.isNotBlank()
                get { startTimestamp }.isEqualTo(now.toTimestamp())
                get { scope }.isSameInstanceAs(rhActionsBatch.parentEventId.scope)
                get { bookName }.isSameInstanceAs(rhActionsBatch.parentEventId.bookName)
            }
            get { name }.isEqualTo(rhActionsBatch.eventName)
            get { parentId }.isSameInstanceAs(rhActionsBatch.parentEventId)
            get { status }.isEqualTo(EventStatus.FAILED)
            get { attachedMessageIdsList }.isEqualTo(messageIDs)
            get { hasEndTimestamp() }.isTrue()
            @Suppress("SpellCheckingInspection")
            get { body.toStringUtf8() }.isEqualTo("""
                |[
                  |{"data":"Description: \ntest-description","type":"message"},
                  |{"data":"test-title","type":"message"},
                  |{
                    |"type":"table",
                    |"rows":
                      |[
                        |{
                          |"Name":"test-key",
                          |"Value":"test-value"
                        |}
                      |]
                  |},
                  |{"data":"Result","type":"message"},
                  |{
                    |"type":"table",
                    |"rows":
                      |[
                        |{"Name":"Action status","Value":"EXECUTION_ERROR"},
                        |{"Name":"Errors","Value":"test-error-message"},
                        |{"Name":"SessionId","Value":"test-session-id"}
                      |]
                  |},
                  |{"data":"Action messages","type":"message"},
                  |{
                    |"type":"table",
                    |"rows":
                      |[
                        |{"Name":"test-action-id","Value":"test-result"}
                      |]
                  |}
                |]
            """.trimMargin().replace("\n", ""))
        }
    }

    @Test
    fun `build event when message book doesn't mismatch to default book test`() {
        val now = Instant.now()

        val messageId = MessageID.newBuilder().apply {
            bookName = "$BOOK-2"
            sequence = 1
            timestamp = Instant.now().toTimestamp()
            connectionIdBuilder.setSessionAlias("test-session-alias")
        }.build()
        val messageIDs = listOf<MessageID>(messageId)

        expectThrows<IllegalStateException> {
            builder.buildEvent(
                now,
                RhActionsBatch.getDefaultInstance(),
                ActionsBatchExecutorResponse(
                    RhBatchResponse.getDefaultInstance(),
                    RhScriptResult(),
                    messageIDs
                )
            )
        }.assert("Message") {
            assertEquals(
                """
                    |Build event failure, book: '$BOOK', scope: '$SCOPE', 
                    |name: 'Unknown event name', type: 'Unknown event type', 
                    |problems: [Book name mismatch in '${messageId.toJson()}' message id]
                """.trimMargin().replace("\n", ""),
                it.message)
        }
    }

    @Test
    fun `build event when message book doesn't mismatch to parent event book test`() {
        val now = Instant.now()

        val messageId = MessageID.newBuilder().apply {
            bookName = "$BOOK-2"
            sequence = 1
            timestamp = Instant.now().toTimestamp()
            connectionIdBuilder.setSessionAlias("test-session-alias")
        }.build()
        val eventId = EventID.newBuilder().setBookName("$BOOK-1").setScope("$SCOPE-1").build()
        val messageIDs = listOf<MessageID>(messageId)

        expectThrows<IllegalStateException> {
            builder.buildEvent(
                now,
                RhActionsBatch.newBuilder().setParentEventId(eventId).build(),
                ActionsBatchExecutorResponse(
                    RhBatchResponse.getDefaultInstance(),
                    RhScriptResult(),
                    messageIDs
                )
            )
        }.assert("Message") {
            assertEquals(
                """
                    |Build event failure, book: '${eventId.bookName}', scope: '${eventId.scope}', 
                    |name: 'Unknown event name', type: 'Unknown event type', 
                    |problems: [Book name mismatch in '${messageId.toJson()}' message id]
                """.trimMargin().replace("\n", ""),
                it.message)
        }
    }

    companion object {
        const val BOX_NAME = "test-box-name"
        const val BOOK = "test-book"
        const val SCOPE = BOX_NAME
    }
}