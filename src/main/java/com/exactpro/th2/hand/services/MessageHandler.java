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
import com.exactpro.th2.hand.RabbitMqConfiguration;
import com.exactpro.th2.hand.remotehand.RhResponseCode;
import com.exactpro.th2.hand.remotehand.RhScriptResult;
import com.exactpro.th2.common.grpc.ConnectionID;
import com.exactpro.th2.common.grpc.Direction;
import com.exactpro.th2.common.grpc.ListValue;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.MessageID;
import com.exactpro.th2.common.grpc.MessageMetadata;
import com.exactpro.th2.common.grpc.RawMessage;
import com.exactpro.th2.common.grpc.RawMessageMetadata;
import com.exactpro.th2.common.grpc.Value;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MessageHandler implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

	private final RabbitMqConnectionWrapper rabbitMqConnection;

	public MessageHandler(RabbitMqConfiguration configuration) throws Exception {
		this.rabbitMqConnection = new RabbitMqConnectionWrapper(configuration);
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
		List<PairMessage> messages = new ArrayList<>();
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
		messages.add(new PairMessage(allMessages, Direction.SECOND, sessionId, sq++));
		messages.add(new PairMessage("ScriptText", scriptText, Direction.SECOND, sessionId, sq++));
		
		try {
			this.rabbitMqConnection.sendMessages(messages);
		} catch (Exception e) {
			logger.error("Cannot send message to message-storage", e);
		}
		
		return messages.stream().filter(Objects::nonNull).map(PairMessage::getMessageId).collect(Collectors.toList());
	}

	public MessageID onResponse(RhScriptResult response, String sessionId, String rhSessionId) {
		Map<String, Object> fields = new LinkedHashMap<>();
		fields.put("ScriptOutputCode", RhResponseCode.byCode(response.getCode()).toString());
		fields.put("ErrorText", response.getErrorMessage() == null ? "" : response.getErrorMessage());
		fields.put("Text out", String.join("|", response.getTextOutput()));
		fields.put("RhSessionId", rhSessionId);
		
		try {
			PairMessage message = new PairMessage(fields, Direction.FIRST, sessionId, System.nanoTime());
			this.rabbitMqConnection.sendMessages(message);
			return message.getMessageId();
		} catch (Exception e) {
			logger.error("Cannot send message to message-storage", e);
		}
		
		return null;
	}

	public RawMessage buildMessage(byte[] bytes, Direction direction, String sessionId, Long sq) {
		ConnectionID connectionID = ConnectionID.newBuilder().setSessionAlias(sessionId).build();
		MessageID messageID = MessageID.newBuilder()
				.setConnectionId(connectionID)
				.setDirection(direction)
				// TODO to replace it with sequence number from 1 to ...
				.setSequence(sq)
				.build();
		RawMessageMetadata messageMetadata = RawMessageMetadata.newBuilder()
				.setId(messageID)
				.setTimestamp(getTimestamp(Instant.now()))
				.build();

		return RawMessage.newBuilder().setMetadata(messageMetadata).setBody(ByteString.copyFrom(bytes)).build();
	}

	public RawMessage buildMessage(Map<String, Object> fields, Direction direction, String sessionId, Long sq) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			byte[] bytes = mapper.writeValueAsBytes(fields);
			return buildMessage(bytes, direction, sessionId, sq);
		} catch (JsonProcessingException e) {
			logger.error("Could not encode message as JSON", e);
			return null;
		}
	}

	public Message buildParsedMessage(Map<String, Object> fileds, RawMessage rawMessage) {
		if (rawMessage == null)
			return null;
		
		RawMessageMetadata metadata1 = rawMessage.getMetadata();

		MessageMetadata metadata = MessageMetadata.newBuilder()
				.setId(metadata1.getId())
				.setTimestamp(metadata1.getTimestamp())
				.setMessageType(metadata1.getId().getDirection() == Direction.SECOND ?
						"from act to hand": "from hand to act").build();

		Message.Builder builder = buildParsedMessage(fileds);
		builder.setMetadata(metadata);
		return builder.build();
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
	
	public Message.Builder buildParsedMessage(Map<String, Object> fileds) {
		Map<String, Value> messageFields = new LinkedHashMap<>(fileds.size());
		for (Map.Entry<String, Object> entry : fileds.entrySet()) {
			messageFields.put(entry.getKey(), parseObj(entry.getValue()));
		}
		
		return Message.newBuilder().putAllFields(messageFields);
	}

	private static Timestamp getTimestamp(Instant instant) {
		return Timestamp.newBuilder()
				.setSeconds(instant.getEpochSecond())
				.setNanos(instant.getNano())
				.build();
	}

	@Override
	public void close() throws Exception {
		this.rabbitMqConnection.close();
	}

	public class PairMessage {
		public final RawMessage rawMessage;
		public final Message message;
		public final boolean valid;

		private PairMessage(RawMessage rawMessage, Message message, boolean valid) {
			this.rawMessage = rawMessage;
			this.message = message;
			this.valid = valid;
		}

		private PairMessage(String name, String text, Direction direction, String sessionId, Long sq) {
			this.rawMessage = buildMessage(text.getBytes(), direction, sessionId, sq);
			this.message = buildParsedMessage(Collections.singletonMap(name, text), rawMessage);
			this.valid = rawMessage != null && message != null;
		}

		private PairMessage(List<Map<String, Object>> fields, Direction direction, String sessionId, Long sq) {
			this(Collections.singletonMap("messages", fields), direction, sessionId, sq);
		}

		private PairMessage(Map<String, Object> fields, Direction direction, String sessionId, Long sq) {
			this.rawMessage = buildMessage(fields, direction, sessionId, sq);
			this.message = buildParsedMessage(fields, rawMessage);
			this.valid = rawMessage != null && message != null;
		}
		
		private MessageID getMessageId() {
			return valid ? rawMessage.getMetadata().getId() : null;
		}
	}
}
