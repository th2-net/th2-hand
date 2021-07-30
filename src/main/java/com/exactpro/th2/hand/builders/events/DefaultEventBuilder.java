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
import com.exactpro.th2.act.grpc.hand.RhActionsBatch;
import com.exactpro.th2.act.grpc.hand.RhBatchResponse;
import com.exactpro.th2.common.grpc.Event;
import com.exactpro.th2.common.grpc.EventID;
import com.exactpro.th2.common.grpc.EventStatus;
import com.exactpro.th2.hand.messages.responseexecutor.ActionsBatchExecutorResponse;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.exactpro.th2.hand.utils.Utils.getTimestamp;

public final class DefaultEventBuilder implements EventBuilder<Event, RhActionsBatch, ActionsBatchExecutorResponse> {
	
	@Override
	public Event buildEvent(RhActionsBatch request, ActionsBatchExecutorResponse executorResponse) {
		return buildEvent(Instant.now(), request, executorResponse);
	}

	@Override
	public Event buildEvent(Instant startTime, RhActionsBatch request, ActionsBatchExecutorResponse executorResponse) {
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
		
		EventPayloadBuilder payloadBuilder = new EventPayloadBuilder();
		createAdditionalEventInfo(payloadBuilder, request.getAdditionalEventInfo());
		createResultPayload(payloadBuilder, scriptResult, response.getSessionId(), response.getScriptStatus());
		createActionMessagesPayload(payloadBuilder, request.getStoreActionMessages(), response.getResultList());
		eventBuilder.setBody(payloadBuilder.toByteString());
		
		eventBuilder.addAllAttachedMessageIds(executorResponse.getMessageIds());
		eventBuilder.setEndTimestamp(getTimestamp(Instant.now()));

		return eventBuilder.build();
	}


	private void createResultPayload(EventPayloadBuilder payloadBuilder, RhScriptResult scriptResult, String sessionId,
	                                 RhBatchResponse.ScriptExecutionStatus scriptStatus) {
		Map<String, String> responseMap = new LinkedHashMap<>();
		responseMap.put("Action status", scriptStatus.name());
		String errorMessage;
		if (StringUtils.isNotEmpty(errorMessage = scriptResult.getErrorMessage()))
			responseMap.put("Errors", errorMessage);
		responseMap.put("SessionId", sessionId);

		payloadBuilder.printTable("Result", responseMap);
	}

	private void createActionMessagesPayload(EventPayloadBuilder payloadBuilder, boolean storeActionMessages, 
									 List<ResultDetails> resultDetails) {
		if (storeActionMessages && !resultDetails.isEmpty()) {
			Map<String, String> actionMessages = resultDetails.stream().collect(
					Collectors.toMap(ResultDetails::getActionId, ResultDetails::getResult));
			payloadBuilder.printTable("Action messages", actionMessages);
		}
	}

	private void createAdditionalEventInfo(EventPayloadBuilder payloadBuilder, RhActionsBatch.AdditionalEventInfo info) {
		String description = info.getDescription();
		if (!description.isEmpty()) {
			payloadBuilder.printText("Description: \n" + description);
		}
		
		if (info.getPrintTable()) {
			Map<String, String> table = new LinkedHashMap<>(info.getKeysCount());
			for (Iterator<String> keysIt = info.getKeysList().iterator(), valuesIt = info.getValuesList().iterator();
				 keysIt.hasNext() && valuesIt.hasNext();
				 table.put(keysIt.next(), valuesIt.next()));
			payloadBuilder.printTable(info.getRequestParamsTableTitle(), table);
		}
	}
	
}
