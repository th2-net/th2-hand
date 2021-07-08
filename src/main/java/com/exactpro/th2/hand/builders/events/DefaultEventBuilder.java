/*
 * Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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
import com.exactpro.th2.act.grpc.hand.RhActionsList;
import com.exactpro.th2.act.grpc.hand.RhBatchResponse;
import com.exactpro.th2.common.grpc.Event;
import com.exactpro.th2.common.grpc.EventID;
import com.exactpro.th2.common.grpc.EventStatus;
import com.exactpro.th2.hand.messages.eventpayload.EventPayloadMessage;
import com.exactpro.th2.hand.messages.eventpayload.EventPayloadTable;
import com.exactpro.th2.hand.messages.responseexecutor.ActionsBatchExecutorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.exactpro.th2.hand.utils.Utils.getTimestamp;

public final class DefaultEventBuilder implements EventBuilder<Event, RhActionsList, ActionsBatchExecutorResponse> {
	private static final Logger logger = LoggerFactory.getLogger(DefaultEventBuilder.class);

	private static final ObjectMapper mapper = new ObjectMapper();


	@Override
	public Event buildEvent(RhActionsList request, ActionsBatchExecutorResponse executorResponse) {
		return buildEvent(Instant.now(), request, executorResponse);
	}

	@Override
	public Event buildEvent(Instant startTime, RhActionsList request, ActionsBatchExecutorResponse executorResponse) {
		EventID eventId = EventID.newBuilder().setId(UUID.randomUUID().toString()).build();

		Event.Builder eventBuilder = Event.newBuilder()
				.setId(eventId)
				.setName(request.getEventName())
				.setStartTimestamp(getTimestamp(startTime));

		if (request.hasParentEventId())
			eventBuilder.setParentId(request.getParentEventId());

		RhScriptResult scriptResult = executorResponse.getScriptResult();
		RhBatchResponse response = executorResponse.getHandResponse();
		eventBuilder.setStatus(scriptResult.isSuccess() ? EventStatus.SUCCESS : EventStatus.FAILED);
		ByteString payload = createPayload(scriptResult, response.getSessionId(), request.getStoreActionMessages(),
				response.getScriptStatus(), response.getResultList());
		eventBuilder.setBody(payload);
		eventBuilder.addAllAttachedMessageIds(executorResponse.getMessageIds());
		eventBuilder.setEndTimestamp(getTimestamp(Instant.now()));

		return eventBuilder.build();
	}


	private ByteString createPayload(RhScriptResult scriptResult, String sessionId, boolean storeActionMessages,
	                                 RhBatchResponse.ScriptExecutionStatus scriptStatus, List<ResultDetails> resultDetails) {
		List<Object> payload = new ArrayList<>(storeActionMessages ? 4 : 2);
		processResponsePayload(payload, scriptResult, sessionId, scriptStatus);
		if (storeActionMessages && !resultDetails.isEmpty()) {
			Map<String, String> actionMessages = resultDetails.stream().collect(
					Collectors.toMap(ResultDetails::getActionId, ResultDetails::getResult));
			processActionMessagesPayload(payload, actionMessages);
		}

		return ByteString.copyFrom(writePayloadBody(payload));
	}

	private void processResponsePayload(List<Object> payload, RhScriptResult scriptResult, String sessionId,
	                                    RhBatchResponse.ScriptExecutionStatus scriptStatus) {
		Map<String, String> responseMap = new LinkedHashMap<>();
		responseMap.put("Action status", scriptStatus.name());
		String errorMessage;
		if (StringUtils.isNotEmpty(errorMessage = scriptResult.getErrorMessage()))
			responseMap.put("Errors", errorMessage);
		responseMap.put("SessionId", sessionId);

		payload.add(new EventPayloadMessage("Response"));
		payload.add(new EventPayloadTable(responseMap, false));
	}

	private void processActionMessagesPayload(List<Object> payload, Map<String, String> remoteHandResponse ) {
		payload.add(new EventPayloadMessage("Action messages"));
		payload.add(new EventPayloadTable(remoteHandResponse, false));
	}

	private byte[] writePayloadBody(List<Object> payload) {
		try {
			return mapper.writeValueAsBytes(payload);
		} catch (JsonProcessingException e) {
			logger.error("Error while creating body", e);
			return e.getMessage().getBytes(StandardCharsets.UTF_8);
		}
	}
}
