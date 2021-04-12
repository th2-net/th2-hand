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

import com.exactpro.th2.act.grpc.hand.*;
import com.exactpro.th2.common.grpc.*;
import com.exactpro.th2.hand.Config;
import com.exactpro.th2.hand.HandException;
import com.exactpro.th2.hand.RhConnectionManager;
import com.exactpro.th2.hand.messages.RhResponseMessageBody;
import com.exactpro.th2.hand.messages.payload.EventPayloadMessage;
import com.exactpro.th2.hand.messages.payload.EventPayloadTable;
import com.exactpro.th2.hand.utils.ScriptBuilder;
import com.exactpro.th2.hand.utils.Utils;
import com.exactprosystems.remotehand.Configuration;
import com.exactprosystems.remotehand.requests.ExecutionRequest;
import com.exactprosystems.remotehand.rhdata.RhResponseCode;
import com.exactprosystems.remotehand.rhdata.RhScriptResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.*;
import io.grpc.stub.StreamObserver;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

public class MessageHandler {
	private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);
	private static final ObjectMapper mapper = new ObjectMapper();

	private final MStoreSender mStoreSender;
	private final Config config;
	private final RhConnectionManager rhConnManager;
	private final ScriptBuilder scriptBuilder;
	private final int responseTimeout;
	private final AtomicLong seqNum;

	public MessageHandler(Config config, RhConnectionManager rhConnManager, AtomicLong seqNum) {
		this.config = config;
		this.seqNum = seqNum;
		this.responseTimeout = config.getResponseTimeout();
		this.mStoreSender = new MStoreSender(config.getFactory());
		this.rhConnManager = rhConnManager;
		this.scriptBuilder = new ScriptBuilder();
	}


	public void handle(EventID parentEventId, String eventName, Consumer<Event.Builder> consumer,
	                   final MessageOrBuilder request, StreamObserver<?> responseObserver) {
		logger.info("Event name: '{}', request: '{}'", eventName, TextFormat.shortDebugString(request));

		Instant startTime = Instant.now();
		EventID eventId = EventID.newBuilder().setId(UUID.randomUUID().toString()).build();

		Event.Builder eventBuilder = Event.newBuilder()
				.setId(eventId)
				.setName(eventName)
				.setStartTimestamp(this.timestampFromInstant(startTime));

		if (parentEventId != null)
			eventBuilder.setParentId(parentEventId);

		consumer.accept(eventBuilder);

		eventBuilder.setEndTimestamp(timestampFromInstant(Instant.now()));
		mStoreSender.storeEvent(eventBuilder.build());
		responseObserver.onCompleted();
	}

	public void handleRegisterSession(RhTargetServer request, StreamObserver<RhSessionID> responseObserver) {
		logger.info("Event name: '{}', request: '{}'", "registerSession", TextFormat.shortDebugString(request));
		try {
			String sessionId = rhConnManager.createSessionHandler(request.getTarget()).getId();
			RhSessionID result = RhSessionID.newBuilder().setId(sessionId).setSessionAlias(config.getSessionAlias()).build();
			responseObserver.onNext(result);
		} catch (Exception e) {
			logger.error("Error while creating session", e);
			Exception responseException = new HandException("Error while creating session", e);
			responseObserver.onError(responseException);
		}
		responseObserver.onCompleted();
	}

	public void handleUnregisterSession(RhSessionID request, StreamObserver<Empty> responseObserver) {
		logger.info("Event name: '{}', request: '{}'", "unRegisterSession", TextFormat.shortDebugString(request));
		rhConnManager.closeSessionHandler(request.getId());
		responseObserver.onNext(Empty.getDefaultInstance());
		responseObserver.onCompleted();
	}

	public void handleExecution(RhActionsList request, StreamObserver<RhBatchResponse> responseObserver) {
		handle(request.getParentEventId(), request.getEventName(), eventBuilder -> {
			RhScriptResult scriptResult;
			List<MessageID> messageIDS = new ArrayList<>();
			String sessionId = "th2_hand";

			try {
				sessionId = request.getSessionId().getId();
				messageIDS.addAll(onRequest(request, config.getSessionAlias()));
				HandSessionHandler sessionHandler = rhConnManager.getSessionHandler(sessionId);
				sessionHandler.handle(new ExecutionRequest(scriptBuilder.buildScript(request, sessionId)),
						HandSessionExchange.getStub());
				scriptResult = sessionHandler.waitAndGet(this.responseTimeout);
			} catch (Exception e) {
				scriptResult = new RhScriptResult();
				scriptResult.setCode(RhResponseCode.EXECUTION_ERROR.getCode());
				scriptResult.setErrorMessage(e.getMessage());
				logger.warn("Error occurred while executing commands", e);
			}
			processMessageIDs(messageIDS, scriptResult, sessionId);

			RhBatchResponse.ScriptExecutionStatus scriptStatus = convertToScriptExecutionStatus(
					RhResponseCode.byCode(scriptResult.getCode()));
			List<ResultDetails> resultDetails = parseResultDetails(scriptResult.getTextOutput());
			eventBuilder.setStatus(scriptResult.isSuccess() ? EventStatus.SUCCESS : EventStatus.FAILED)
					.setBody(createPayload(scriptResult, sessionId, request.getStoreActionMessages(), scriptStatus,
							resultDetails))
					.addAllAttachedMessageIds(messageIDS);

			RhBatchResponse response = RhBatchResponse.newBuilder()
					.setScriptStatus(scriptStatus)
					.setErrorMessage(defaultIfEmpty(scriptResult.getErrorMessage(), ""))
					.setSessionId(sessionId)
					.addAllResult(resultDetails)
					.build();
			responseObserver.onNext(response);
		}, request, responseObserver);
	}

	public void dispose() {
		try {
			this.rhConnManager.dispose();
		} catch (Exception e) {
			logger.error("Error while disposing RH manager", e);
		}
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

	private void processMessageIDs(List<MessageID> messageIDS, RhScriptResult scriptResult, String sessionId) {
		messageIDS.add(onResponse(scriptResult, config.getSessionAlias(), sessionId));
		messageIDS.addAll(storeScreenshots(scriptResult.getScreenshotIds(), config.getScreenshotSessionAlias()));
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

	private List<ResultDetails> parseResultDetails(List<String> result) {
		List<ResultDetails> details = new ArrayList<>(result.size());
		ResultDetails.Builder resultDetailsBuilder = ResultDetails.newBuilder();
		for (String s : result) {

			if (s.contains(Utils.LINE_SEPARATOR)) {
				s = s.replaceAll(Utils.LINE_SEPARATOR, "\n");
			}

			int index = s.indexOf('=');
			if (index > 0) {
				resultDetailsBuilder.clear();
				resultDetailsBuilder.setActionId(s.substring(0, index));
				resultDetailsBuilder.setResult(s.substring(index + 1));
				details.add(resultDetailsBuilder.build());
			}
		}

		return details;

	}

	private Timestamp timestampFromInstant(Instant instant) {
		if (instant == null) {
			instant = Instant.now();
		}
		return Timestamp.newBuilder().setSeconds(instant.getEpochSecond())
				.setNanos(instant.getNano()).build();
	}

	private RhBatchResponse.ScriptExecutionStatus convertToScriptExecutionStatus(RhResponseCode code) {
		switch (code) {
			case SUCCESS: return RhBatchResponse.ScriptExecutionStatus.SUCCESS;
			case COMPILE_ERROR: return RhBatchResponse.ScriptExecutionStatus.COMPILE_ERROR;
			case EXECUTION_ERROR: return RhBatchResponse.ScriptExecutionStatus.EXECUTION_ERROR;
			case RH_ERROR:
			case TOOL_BUSY:
			case INCORRECT_REQUEST:
			default: return RhBatchResponse.ScriptExecutionStatus.HAND_INTERNAL_ERROR;
		}
	}

	private byte[] writePayloadBody(List<Object> payload) {
		try {
			return mapper.writeValueAsBytes(payload);
		} catch (JsonProcessingException e) {
			logger.error("Error while creating body", e);
			return e.getMessage().getBytes(StandardCharsets.UTF_8);
		}
	}

	private List<MessageID> onRequest(RhActionsList actionsList, String sessionId) {
		long sq = System.nanoTime();
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
				mStoreSender.sendMessages(message);
			} catch (Exception e) {
				logger.error("Cannot send message to message-storage", e);
			}

			return Collections.singletonList(message.getMetadata().getId());
		} else {
			logger.debug("Nothing to store to mstore");
			return Collections.emptyList();
		}
	}
	
	private List<MessageID> storeScreenshots(List<String> screenshotIds, String sessionAlias) {
		if (screenshotIds == null || screenshotIds.isEmpty()) {
			logger.debug("No screenshots to store");
			return Collections.emptyList();
		}

		List<MessageID> messageIDS = new ArrayList<>();
		List<RawMessage> rawMessages = new ArrayList<>();
		for (String screenshotId : screenshotIds) {
			logger.debug("Storing screenshot id {}", screenshotId);
			Path screenPath = Configuration.SCREENSHOTS_DIR_PATH.resolve(screenshotId);
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

	private MessageID onResponse(RhScriptResult response, String sessionId, String rhSessionId) {
		RhResponseMessageBody body = RhResponseMessageBody.fromRhScriptResult(response).setRhSessionId(rhSessionId);
		try {
			RawMessage message = buildMessage(body.getFields(), Direction.FIRST, sessionId);
			mStoreSender.sendMessages(message);
			return message.getMetadata().getId();
		} catch (Exception e) {
			logger.error("Cannot send message to message-storage", e);
		}
		
		return null;
	}

	public RawMessage buildMessageFromFile(Path path, Direction direction, String sessionId) {
		RawMessageMetadata messageMetadata = buildMetaData(direction, sessionId, "image/png");

		try (InputStream is = Files.newInputStream(path)) {
			return RawMessage.newBuilder().setMetadata(messageMetadata).setBody(ByteString.readFrom(is, 0x1000)).build();
		} catch (IOException e) {
			logger.error("Cannot encode screenshot", e);
			return null;
		}
	}

	private RawMessage buildMessage(byte[] bytes, Direction direction, String sessionId) {
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

	private RawMessage buildMessage(Map<String, Object> fields, Direction direction, String sessionId) {
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
			mStoreSender.sendMessages(rawMessages);
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
}
