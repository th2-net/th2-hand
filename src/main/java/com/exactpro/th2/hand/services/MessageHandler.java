/*
 *  Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.act.grpc.hand.RhAction;
import com.exactpro.th2.act.grpc.hand.RhActionsList;
import com.exactpro.th2.common.grpc.ListValue;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.Value;
import com.exactpro.th2.common.grpc.*;
import com.exactpro.th2.hand.Config;
import com.exactpro.th2.hand.messages.RhResponseMessageBody;
import com.exactprosystems.remotehand.rhdata.RhScriptResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MessageHandler{

	private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

	private final RabbitMqConnectionWrapper rabbitMqConnection;
	private final Config config;
	private final AtomicLong seqNum; 

	public MessageHandler(Config config, AtomicLong seqNum) {
		this.config = config;
		this.seqNum = seqNum;
		this.rabbitMqConnection = new RabbitMqConnectionWrapper(config.getFactory());
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

	public List<MessageID> onRequest(RhActionsList actionsList, String scriptText, String sessionId) {
		List<RawMessage> messages = new ArrayList<>();
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
		messages.add(buildMessage(Collections.singletonMap("messages", allMessages), Direction.SECOND, sessionId));
		messages.add(buildMessage(scriptText.getBytes(), Direction.SECOND, sessionId));
		
		try {
			rabbitMqConnection.sendMessages(messages);
		} catch (Exception e) {
			logger.error("Cannot send message to message-storage", e);
		}
		
		return messages.stream().filter(Objects::nonNull).map(m -> m.getMetadata().getId()).collect(Collectors.toList());
	}

	public MessageID onResponse(RhScriptResult response, String sessionId, String rhSessionId) {
		RhResponseMessageBody body = RhResponseMessageBody.fromRhScriptResult(response).setRhSessionId(rhSessionId);
		try {
			RawMessage message = buildMessage(body.getFields(), Direction.FIRST, sessionId);
			rabbitMqConnection.sendMessages(message);
			return message.getMetadata().getId();
		} catch (Exception e) {
			logger.error("Cannot send message to message-storage", e);
		}
		
		return null;
	}
	
	public RawMessage buildMessage(byte[] bytes, Direction direction, String sessionId) {
		ConnectionID connectionID = ConnectionID.newBuilder().setSessionAlias(config.getSessionAlias()).build();
		MessageID messageID = MessageID.newBuilder()
				.setConnectionId(connectionID)
				.setDirection(direction)
				.setSequence(seqNum.incrementAndGet())
				.build();
		RawMessageMetadata messageMetadata = RawMessageMetadata.newBuilder()
				.setId(messageID)
				.setTimestamp(getTimestamp(Instant.now()))
				.build();

		return RawMessage.newBuilder().setMetadata(messageMetadata).setBody(ByteString.copyFrom(bytes)).build();
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

	private Value parseObj(Object value) {
		if (value instanceof List) {
			ListValue.Builder listValueBuilder = ListValue.newBuilder();
			for (Object o : ((List<?>) value)) {

				if (o instanceof Map) {
					Message.Builder msgBuilder = Message.newBuilder();
					for (Map.Entry<?, ?> o1 : ((Map<?, ?>) o).entrySet()) {
						msgBuilder.putFields(String.valueOf(o1.getKey()), parseObj(o1.getValue()));
					}
					listValueBuilder.addValues(Value.newBuilder().setMessageValue(msgBuilder.build()));
				} else {
					listValueBuilder.addValues(Value.newBuilder().setSimpleValue(String.valueOf(o)));
				}
			}
			return Value.newBuilder().setListValue(listValueBuilder).build();
		} else {
			return Value.newBuilder().setSimpleValue(String.valueOf(value)).build();
		}
	}
	
	private static Timestamp getTimestamp(Instant instant) {
		return Timestamp.newBuilder()
				.setSeconds(instant.getEpochSecond())
				.setNanos(instant.getNano())
				.build();
	}

}
