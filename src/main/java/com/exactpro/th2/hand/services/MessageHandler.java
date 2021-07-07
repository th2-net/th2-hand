/*
 *  Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.exactpro.th2.hand.services;

import com.exactpro.remotehand.ActionResult;
import com.exactpro.remotehand.Configuration;
import com.exactpro.remotehand.rhdata.RhScriptResult;
import com.exactpro.th2.act.grpc.hand.ResultDetails;
import com.exactpro.th2.act.grpc.hand.RhAction;
import com.exactpro.th2.act.grpc.hand.RhActionsList;
import com.exactpro.th2.act.grpc.hand.RhBatchResponse;
import com.exactpro.th2.common.grpc.*;
import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.exactpro.th2.hand.Config;
import com.exactpro.th2.hand.RhConnectionManager;
import com.exactpro.th2.hand.messages.RhResponseMessageBody;
import com.exactpro.th2.hand.messages.eventpayload.EventPayloadMessage;
import com.exactpro.th2.hand.messages.eventpayload.EventPayloadTable;
import com.exactpro.th2.hand.messages.responseexecutor.ActionsBatchExecutorResponse;
import com.exactpro.th2.hand.requestexecutors.ActionsBatchExecutor;
import com.exactpro.th2.hand.scriptbuilders.ScriptBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MessageHandler implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

	private static final ObjectMapper mapper = new ObjectMapper();

	private final Config config;
	private final MessageStoreSender messageStoreSender;
	private final EventStoreSender eventStoreSender;
	private final RhConnectionManager rhConnectionManager;
	private final AtomicLong seqNum; 
	private final ScriptBuilder scriptBuilder = new ScriptBuilder();

	public MessageHandler(Config config, AtomicLong seqNum) {
		this.config = config;
		rhConnectionManager = new RhConnectionManager(config);
		this.seqNum = seqNum;
		CommonFactory factory = config.getFactory();
		this.messageStoreSender = new MessageStoreSender(factory);
		this.eventStoreSender = new EventStoreSender(factory);
	}


	public ScriptBuilder getScriptBuilder() {
		return scriptBuilder;
	}

	public Config getConfig() {
		return config;
	}

	public RhConnectionManager getRhConnectionManager() {
		return rhConnectionManager;
	}

	public RhBatchResponse handleActionsBatchRequest(RhActionsList request) {
		Instant startTime = Instant.now();
		ActionsBatchExecutor actionsBatchExecutor = new ActionsBatchExecutor(this);
		ActionsBatchExecutorResponse executorResponse = actionsBatchExecutor.execute(request);
		eventStoreSender.storeEvent(buildEvent(startTime, request, executorResponse));
		return executorResponse.getHandResponse();
	}

	private Event buildEvent(Instant startTime, RhActionsList request, ActionsBatchExecutorResponse executorResponse) {
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

	private String valueToString(Object object) {
		if (object instanceof Int32Value) {
			return String.valueOf(((Int32Value) object).getValue());
		} else if (object instanceof StringValue) {
			return ((StringValue) object).getValue();
		} else {
			return String.valueOf(object);
		}
	}
	
	private List<Map<String, Object>> processList(List<?> list) {
		List<Map<String, Object>> processed = new ArrayList<>(list.size());
		for (Object o : list) {
			if (o instanceof GeneratedMessageV3) {
				Map<String, Object> map = new LinkedHashMap<>();
				for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : ((GeneratedMessageV3) o).getAllFields().entrySet()) {
					map.put(entry.getKey().getName(), valueToString(entry.getValue()));
				}
				processed.add(map);
			} else {
				processed.add(Collections.singletonMap("Value", valueToString(o)));
			}
		}
		return processed;
	}

	public List<MessageID> onRequest(RhActionsList actionsList, String sessionId) {
		List<Map<String, Object>> allMessages = new ArrayList<>();
		for (RhAction rhAction : actionsList.getRhActionList()) {
			for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : rhAction.getAllFields().entrySet()) {
				if (entry != null) {
					Map<String, Object> fields = new LinkedHashMap<>();
					fields.put("ActionName", entry.getKey().getName());
					Object value = entry.getValue();
					if (value instanceof GeneratedMessageV3) {
						for (Map.Entry<Descriptors.FieldDescriptor, Object> entry2 : 
								((GeneratedMessageV3)value).getAllFields().entrySet()) {
							Object valueObj = entry2.getValue();
							if (valueObj instanceof List) {
								fields.put(entry2.getKey().getName(), this.processList((List<?>) valueObj));
							} else {
								fields.put(entry2.getKey().getName(), valueToString(valueObj));	
							}
							
						}
					}
					allMessages.add(fields);
				}
			}
		}
		
		RawMessage message = buildMessage(Collections.singletonMap("messages", allMessages),
				Direction.SECOND, sessionId);
		
		if (message != null) {
			try {
				messageStoreSender.sendMessages(message);
			} catch (Exception e) {
				logger.error("Cannot send message to message-storage", e);
			}

			return Collections.singletonList(message.getMetadata().getId());
		} else {
			logger.debug("Nothing to store to mstore");
			return Collections.emptyList();
		}
	}
	
	public List<MessageID> storeScreenshots(List<ActionResult> screenshotIds, String sessionAlias) {
		if (screenshotIds == null || screenshotIds.isEmpty()) {
			logger.debug("No screenshots to store");
			return Collections.emptyList();
		}

		List<MessageID> messageIDS = new ArrayList<>();
		List<RawMessage> rawMessages = new ArrayList<>();
		for (ActionResult screenshotId : screenshotIds) {
			logger.debug("Storing screenshot id {}", screenshotId);
			Path screenPath = Configuration.SCREENSHOTS_DIR_PATH.resolve(screenshotId.getData());
			if (!Files.exists(screenPath)) {
				logger.warn("Screenshot with id {} does not exists", screenshotId);
				continue;
			}
			RawMessage rawMessage = buildMessageFromFile(screenPath, Direction.FIRST, sessionAlias);
			if (rawMessage != null) {
				messageIDS.add(rawMessage.getMetadata().getId());
				rawMessages.add(rawMessage);
			}
			removeScreenshot(screenPath);
		}
		sendRawMessages(rawMessages);

		return messageIDS;
	}

	public MessageID onResponse(RhScriptResult response, String sessionId, String rhSessionId) {
		RhResponseMessageBody body = RhResponseMessageBody.fromRhScriptResult(response).setRhSessionId(rhSessionId);
		try {
			RawMessage message = buildMessage(body.getFields(), Direction.FIRST, sessionId);
			messageStoreSender.sendMessages(message);
			return message.getMetadata().getId();
		} catch (Exception e) {
			logger.error("Cannot send message to message-storage", e);
		}
		
		return null;
	}

	public RawMessage buildMessageFromFile(Path path, Direction direction, String sessionId) {
		String protocol = "image/" + Configuration.getInstance().getDefaultScreenWriter().getScreenshotExtension();
		RawMessageMetadata messageMetadata = buildMetaData(direction, sessionId, protocol);

		try (InputStream is = Files.newInputStream(path)) {
			return RawMessage.newBuilder().setMetadata(messageMetadata).setBody(ByteString.readFrom(is, 0x1000)).build();
		} catch (IOException e) {
			logger.error("Cannot encode screenshot", e);
			return null;
		}
	}
	
	public RawMessage buildMessage(byte[] bytes, Direction direction, String sessionId) {
		RawMessageMetadata messageMetadata = buildMetaData(direction, sessionId, null);
		return RawMessage.newBuilder().setMetadata(messageMetadata).setBody(ByteString.copyFrom(bytes)).build();
	}
	
	private RawMessageMetadata buildMetaData(Direction direction, String sessionId, String protocol) {
		ConnectionID connectionID = ConnectionID.newBuilder().setSessionAlias(sessionId).build();
		MessageID messageID = MessageID.newBuilder().setConnectionId(connectionID).setDirection(direction)
				.setSequence(seqNum.incrementAndGet()).build();
		RawMessageMetadata.Builder builder = RawMessageMetadata.newBuilder();
		builder.setId(messageID);
		builder.setTimestamp(getTimestamp(Instant.now()));
		if (protocol != null) {
			builder.setProtocol(protocol);
		}
		return builder.build();
	}

	public RawMessage buildMessage(Map<String, Object> fields, Direction direction, String sessionId) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			byte[] bytes = mapper.writeValueAsBytes(fields);
			return buildMessage(bytes, direction, sessionId);
		} catch (JsonProcessingException e) {
			logger.error("Could not encode message as JSON", e);
			return null;
		}
	}


	private void removeScreenshot(Path file) {
		try {
			Files.delete(file);
		} catch (IOException e) {
			logger.warn("Error deleting file: " + file.toAbsolutePath().toString(), e);
		}
	}

	private void sendRawMessages(List<RawMessage> rawMessages) {
		try {
			messageStoreSender.sendMessages(rawMessages);
		} catch (Exception e) {
			logger.error("Cannot store to mstore", e);
		}
	}


	private static Timestamp getTimestamp(Instant instant) {
		return Timestamp.newBuilder()
				.setSeconds(instant.getEpochSecond())
				.setNanos(instant.getNano())
				.build();
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

	@Override
	public void close() throws Exception {
		rhConnectionManager.dispose();
		messageStoreSender.close();
		eventStoreSender.close();
	}
}
