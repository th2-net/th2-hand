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
import com.exactpro.th2.infra.grpc.Value;
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
		EventID parentId = actionsList.getParentEventId();
		List<Message> messages = new ArrayList<>();
		long sq = System.nanoTime();
		for (RhAction rhAction : actionsList.getRhActionList()) {
			for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : rhAction.getAllFields().entrySet()) {
				if (entry != null) {
					Map<String, Value> fields = new LinkedHashMap<>();
					fields.put("ActionName", simpleFromText(entry.getKey().getName()));
					Object value = entry.getValue();
					if (value instanceof GeneratedMessageV3) {
						for (Map.Entry<Descriptors.FieldDescriptor, Object> entry2 : 
								((GeneratedMessageV3)value).getAllFields().entrySet()) {
							fields.put(entry2.getKey().getName(), simpleFromText(entry2.getValue()));
						}
					}
					messages.add(this.buildMessage(parentId, fields, Direction.FIRST, sessionId, sq++));
				}
			}
		}
		messages.add(this.buildMessage(parentId, Collections.singletonMap("ScriptText", simpleFromText(scriptText)),
				Direction.FIRST, sessionId, sq++));

		try {
			this.rabbitMqConnection.sendMessage(messages);
		} catch (Exception e) {
			logger.error("Cannot send message to message-storage", e);
		}
		
		return messages.stream().map(message -> message.getMetadata().getId()).collect(Collectors.toList());
	}

	public MessageID onResponse(RhScriptResult response, EventID eventId, String sessionId, String rhSessionId) {
		Map<String, Value> fields = new LinkedHashMap<>();
		fields.put("ScriptOutputCode", simpleFromText(RhResponseCode.byCode(response.getCode()).toString()));
		fields.put("ErrorText", simpleFromText(response.getErrorMessage() == null ? "" : response.getErrorMessage()));
		fields.put("Text out", simpleFromText(StringUtils.join(response.getTextOutput(), '|')));
		fields.put("RhSessionId", simpleFromText(rhSessionId));

		Message message = this.buildMessage(eventId, fields, Direction.SECOND, sessionId, System.nanoTime());
		try {
			this.rabbitMqConnection.sendMessage(message);
		} catch (Exception e) {
			logger.error("Cannot send message to message-storage", e);
		}
		
		return message.getMetadata().getId();
	}


	public Message buildMessage(EventID parentEventId, Map<String, Value> fields, Direction direction, String sessionId, Long sq) {
		ConnectionID connectionID = ConnectionID.newBuilder().setSessionAlias(sessionId).build();
		MessageID messageID = MessageID.newBuilder()
				.setConnectionId(connectionID)
				.setDirection(direction)
				// TODO to replace it to sequence number from 1 to ...
				.setSequence(sq)
				.build();
		String messageType = direction == Direction.FIRST ? "From Th2-hand to RemoteHand" : "From RemoteHand to Th2-hand";
		MessageMetadata messageMetadata = MessageMetadata.newBuilder()
				.setId(messageID)
				.setTimestamp(getTimestamp(Instant.now()))
				.setMessageType(messageType)
				.build();

		Message.Builder builder = Message.newBuilder();
		if (parentEventId != null) {
			builder.setParentEventId(parentEventId);
		}
		
		return builder.setMetadata(messageMetadata).putAllFields(fields).build();
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
