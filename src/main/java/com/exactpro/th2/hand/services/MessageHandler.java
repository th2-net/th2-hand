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
import com.exactpro.th2.infra.grpc.ConnectionID;
import com.exactpro.th2.infra.grpc.Direction;
import com.exactpro.th2.infra.grpc.EventID;
import com.exactpro.th2.infra.grpc.Message;
import com.exactpro.th2.infra.grpc.MessageID;
import com.exactpro.th2.infra.grpc.MessageMetadata;
import com.exactpro.th2.infra.grpc.RawMessage;
import com.exactpro.th2.infra.grpc.RawMessageMetadata;
import com.exactpro.th2.infra.grpc.Value;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Timestamp;
import org.apache.commons.lang3.StringUtils;
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

	private static Value simpleFromText(Object obj) {
		return Value.newBuilder().setSimpleValue(Objects.toString(obj)).build();
	}
	
	public List<MessageID> onRequest(RhActionsList actionsList, String scriptText, String sessionId) {
		List<RawMessage> messages = new ArrayList<>();
		long sq = System.nanoTime();
		for (RhAction rhAction : actionsList.getRhActionList()) {
			for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : rhAction.getAllFields().entrySet()) {
				if (entry != null) {
					Map<String, Object> fields = new LinkedHashMap<>();
					fields.put("ActionName", entry.getKey().getName());
					Object value = entry.getValue();
					if (value instanceof GeneratedMessageV3) {
						for (Map.Entry<Descriptors.FieldDescriptor, Object> entry2 : 
								((GeneratedMessageV3)value).getAllFields().entrySet()) {
							fields.put(entry2.getKey().getName(), String.valueOf(entry2.getValue()));
						}
					}
					messages.add(this.buildMessage(fields, Direction.FIRST, sessionId, sq++));
				}
			}
		}
		messages.add(this.buildMessage(Collections.singletonMap("ScriptText", scriptText),
				Direction.FIRST, sessionId, sq++));
		
		try {
			this.rabbitMqConnection.sendMessage(messages);
		} catch (Exception e) {
			logger.error("Cannot send message to message-storage", e);
		}
		
		return messages.stream().filter(Objects::nonNull).map(message -> message.getMetadata().getId()).collect(Collectors.toList());
	}

	public MessageID onResponse(RhScriptResult response, String sessionId, String rhSessionId) {
		Map<String, Object> fields = new LinkedHashMap<>();
		fields.put("ScriptOutputCode", RhResponseCode.byCode(response.getCode()).toString());
		fields.put("ErrorText", response.getErrorMessage() == null ? "" : response.getErrorMessage());
		fields.put("Text out", String.join("|", response.getTextOutput()));
		fields.put("RhSessionId", rhSessionId);
		
		try {
			RawMessage message = this.buildMessage(fields, Direction.SECOND, sessionId, System.nanoTime());
			this.rabbitMqConnection.sendMessage(message);
			return message.getMetadata().getId();
		} catch (Exception e) {
			logger.error("Cannot send message to message-storage", e);
		}
		
		return null;
	}


	public RawMessage buildMessage(Map<String, Object> fields, Direction direction, String sessionId, Long sq) {
		ConnectionID connectionID = ConnectionID.newBuilder().setSessionAlias(sessionId).build();
		MessageID messageID = MessageID.newBuilder()
				.setConnectionId(connectionID)
				.setDirection(direction)
				// TODO to replace it to sequence number from 1 to ...
				.setSequence(sq)
				.build();
		RawMessageMetadata messageMetadata = RawMessageMetadata.newBuilder()
				.setId(messageID)
				.setTimestamp(getTimestamp(Instant.now()))
				.build();
		
		RawMessage.Builder builder = RawMessage.newBuilder();
		
		ObjectMapper mapper = new ObjectMapper();
		try {
			byte[] bytes = mapper.writeValueAsBytes(fields);
			return builder.setMetadata(messageMetadata).setBody(ByteString.copyFrom(bytes)).build();
		} catch (JsonProcessingException e) {
			logger.error("Cannot encode message to JSON", e);
			return null;
		}
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
}
