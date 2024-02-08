/*
 * Copyright 2020-2024 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.hand.builders.events;

import com.exactpro.remotehand.rhdata.RhScriptResult;
import com.exactpro.th2.act.grpc.hand.ResultDetails;
import com.exactpro.th2.act.grpc.hand.RhActionsBatch;
import com.exactpro.th2.act.grpc.hand.RhBatchResponse;
import com.exactpro.th2.common.event.bean.IRow;
import com.exactpro.th2.common.event.bean.Message;
import com.exactpro.th2.common.event.bean.Table;
import com.exactpro.th2.common.event.Event;
import com.exactpro.th2.common.schema.box.configuration.BoxConfiguration;
import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.exactpro.th2.hand.messages.responseexecutor.ActionsBatchExecutorResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.exactpro.th2.common.event.Event.Status.FAILED;
import static com.exactpro.th2.common.event.Event.Status.PASSED;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public final class DefaultEventBuilder implements EventBuilder<com.exactpro.th2.common.grpc.Event, RhActionsBatch, ActionsBatchExecutorResponse> {
	public static final Message ACTION_MESSAGES_EVENT_MESSAGE = createMessage("Action messages");
	public static final Message RESULT_EVENT_MESSAGE = createMessage("Result");

	private final String book;
	private final String scope;

	public DefaultEventBuilder(CommonFactory factory) {
		BoxConfiguration boxConfiguration = factory.getBoxConfiguration();
		this.book = boxConfiguration.getBookName();
		this.scope = boxConfiguration.getBoxName();
	}

	@Override
	public com.exactpro.th2.common.grpc.Event buildEvent(Instant startTime, RhActionsBatch request, ActionsBatchExecutorResponse executorResponse) throws IOException {
		RhScriptResult scriptResult = executorResponse.getScriptResult();
		RhBatchResponse response = executorResponse.getHandResponse();

		Event builder = Event.from(startTime)
				.name(request.getEventName())
				.status(scriptResult.isSuccess() ? PASSED : FAILED);

		createAdditionalEventInfo(builder, request.getAdditionalEventInfo());
		createResultPayload(builder, scriptResult, response.getSessionId(), response.getScriptStatus());
		createActionMessagesPayload(builder, request.getStoreActionMessages(), response.getResultList());

		executorResponse.getMessageIds().forEach(builder::messageID);

		if (request.hasParentEventId()) {
			return builder.toProto(request.getParentEventId());
		} else {
			return builder.toProto(book, scope);
		}
	}

	private void createResultPayload(com.exactpro.th2.common.event.Event builder, RhScriptResult scriptResult, String sessionId,
									 RhBatchResponse.ScriptExecutionStatus scriptStatus) {
		List<IRow> rows = new ArrayList<>();
		rows.add(new TableRow("Action status", scriptStatus.name()));

		String errorMessage = scriptResult.getErrorMessage();
		if (isNotEmpty(errorMessage)) {
			rows.add(new TableRow("Errors", errorMessage));
		}
		rows.add(new TableRow("SessionId", sessionId));

		builder.bodyData(RESULT_EVENT_MESSAGE);
		builder.bodyData(createTable(rows));
	}

	private void createActionMessagesPayload(com.exactpro.th2.common.event.Event builder, boolean storeActionMessages,
											 List<ResultDetails> resultDetails) {
		if (storeActionMessages && !resultDetails.isEmpty()) {
			List<IRow> rows = resultDetails.stream()
					.map(result -> new TableRow(result.getActionId(), result.getResult()))
					.collect(Collectors.toList());
			builder.bodyData(ACTION_MESSAGES_EVENT_MESSAGE);
			builder.bodyData(createTable(rows));
		}
	}

	private void createAdditionalEventInfo(com.exactpro.th2.common.event.Event builder, RhActionsBatch.AdditionalEventInfo info) {
		String description = info.getDescription();
		if (!description.isEmpty()) {
			builder.bodyData(createMessage("Description: \n" + description));
		}

		if (info.getPrintTable()) {
			List<IRow> rows = new ArrayList<>();

			Iterator<String> keys = info.getKeysList().iterator();
			Iterator<String> values = info.getValuesList().iterator();
			while (keys.hasNext() && values.hasNext()) {
				rows.add(new TableRow(keys.next(), values.next()));
			}

			builder.bodyData(createMessage(info.getRequestParamsTableTitle()));
			builder.bodyData(createTable(rows));
		}
	}

	private static Message createMessage(String text) {
		Message message = new Message();
		message.setData(text);
		return message;
	}

	private static Table createTable(List<IRow> rows) {
		Table table = new Table();
		table.setFields(rows);
		return table;
	}

}