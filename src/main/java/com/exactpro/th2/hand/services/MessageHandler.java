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

import com.exactpro.th2.hand.RabbitMqConfiguration;
import com.exactpro.th2.hand.grpc.BaseRequest;
import com.exactpro.th2.infra.grpc.ConnectionID;
import com.exactpro.th2.infra.grpc.Direction;
import com.exactpro.th2.infra.grpc.EventID;
import com.exactpro.th2.infra.grpc.Message;
import com.exactpro.th2.infra.grpc.MessageID;
import com.exactpro.th2.infra.grpc.MessageMetadata;
import com.exactpro.th2.infra.grpc.Value;
import com.exactprosystems.clearth.connectivity.data.rhdata.RhScriptResult;
import com.exactprosystems.clearth.connectivity.remotehand.RhResponse;
import com.google.protobuf.Timestamp;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class MessageHandler implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

	private final RabbitMqConnectionWrapper rabbitMqConnection;

	public MessageHandler(RabbitMqConfiguration configuration) throws Exception {
		this.rabbitMqConnection = new RabbitMqConnectionWrapper(configuration);
	}

	private static Value simpleFromText(Object obj) {
		return Value.newBuilder().setSimpleValue(Objects.toString(obj)).build();
	}
	
	public void onRequest(BaseRequest baseRequest, String sessionId) {
		Map<String, Value> fields = new LinkedHashMap<>();
		fields.put("ScriptName", simpleFromText(baseRequest.getScriptName()));
		fields.put("ScriptText", simpleFromText(baseRequest.getScriptText()));
		fields.put("WaitInSeconds", simpleFromText(baseRequest.getWaitInSeconds()));
		for (Map.Entry<String, String> entry : baseRequest.getParamsMap().entrySet()) {
			if (!fields.containsKey(entry.getKey())) {
				fields.put(entry.getKey(), simpleFromText(entry.getValue()));
			}
		}
		Message message = this.buildMessage(baseRequest.getParentEventId(), fields, Direction.FIRST, sessionId);
		try {
			this.rabbitMqConnection.sendMessage(message);
		} catch (Exception e) {
			logger.error("Cannot send message to message-storage", e);
		}
	}

	public void onResponse(RhScriptResult response, String sessionId, String rhSessionId) {
		Map<String, Value> fields = new LinkedHashMap<>();
		fields.put("ScriptOutputCode", simpleFromText(String.valueOf(response.getCode())));
		fields.put("ErrorText", simpleFromText(response.getErrorMessage() == null ? "" : response.getErrorMessage()));
		fields.put("Text out", simpleFromText(StringUtils.join(response.getTextOutput(), '|')));
		fields.put("RhSessionId", simpleFromText(rhSessionId));

		Message message = this.buildMessage(null, fields, Direction.SECOND, sessionId);
		try {
			this.rabbitMqConnection.sendMessage(message);
		} catch (Exception e) {
			logger.error("Cannot send message to message-storage", e);
		}
	}


	public Message buildMessage(EventID parentEventId, Map<String, Value> fields, Direction direction, String sessionId) {
		ConnectionID connectionID = ConnectionID.newBuilder().setSessionAlias(sessionId).build();
		MessageID messageID = MessageID.newBuilder()
				.setConnectionId(connectionID)
				.setDirection(direction)
				// TODO to replace it to sequence number from 1 to ...
				.setSequence(System.nanoTime())
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
