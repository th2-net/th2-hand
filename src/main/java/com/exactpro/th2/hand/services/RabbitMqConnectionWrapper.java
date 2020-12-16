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

import com.exactpro.th2.common.grpc.MessageBatch;
import com.exactpro.th2.common.grpc.RawMessageBatch;
import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.exactpro.th2.common.schema.message.MessageRouter;
import com.exactpro.th2.common.schema.message.QueueAttribute;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

public class RabbitMqConnectionWrapper {

	private static final Logger logger = LoggerFactory.getLogger(RabbitMqConnectionWrapper.class);
	
    private final MessageRouter<MessageBatch> messageRouterParsedBatch;
    private final MessageRouter<RawMessageBatch> messageRouterRawBatch;

	public RabbitMqConnectionWrapper(CommonFactory factory) {
		messageRouterParsedBatch = factory.getMessageRouterParsedBatch();
		messageRouterRawBatch = factory.getMessageRouterRawBatch();
		writeToLogAboutConnection(factory);
	}

	private void writeToLogAboutConnection(CommonFactory factory) {
		if (!logger.isInfoEnabled())
			return;
		StringBuilder connectionInfo = new StringBuilder("Connection to RbbitMQ with ");
		connectionInfo.append(factory.getRabbitMqConfiguration()).append(" established \n");
		connectionInfo.append("Queues: \n");
		factory.getMessageRouterConfiguration().getQueues().forEach((name, queue) -> {
			connectionInfo.append(name).append(" : ");
			try {
				ObjectMapper mapper = new ObjectMapper();
				connectionInfo.append(mapper.writeValueAsString(queue));
			}
			catch (JsonProcessingException e) {
				logger.warn("Error occurs while convert QueueConfiguration to JSON string", e);
				connectionInfo.append("QueueConfiguration is not available");
			}
			connectionInfo.append('\n');
		});
		logger.info(connectionInfo.toString());
	}

    public void sendMessages(MessageHandler.PairMessage messages) throws Exception {
		this.sendMessages(Collections.singleton(messages));
	}

	public void sendMessages(Collection<MessageHandler.PairMessage> messages) throws Exception {
		MessageBatch.Builder builder = MessageBatch.newBuilder();
		RawMessageBatch.Builder rawBuilder = RawMessageBatch.newBuilder();
		int count = 0;
		for (MessageHandler.PairMessage message : messages) {
			if (message.valid) {
				builder.addMessages(message.message);
				rawBuilder.addMessages(message.rawMessage);
				count++;
			}
		}

		if (count == 0) {
			logger.debug("There are no valid messages to send");
			return;
		}

		MessageBatch parsed = builder.build();
		RawMessageBatch raw = rawBuilder.build();
		messageRouterParsedBatch.sendAll(parsed);
		messageRouterRawBatch.sendAll(raw);
		if (logger.isDebugEnabled()) {
			String msgTemplate = "Array with {} bytes size sent to {} queue";
			logger.debug(msgTemplate, parsed.toByteArray().length, QueueAttribute.PARSED);
			logger.debug(msgTemplate, raw.toByteArray().length, QueueAttribute.RAW);
		}
	}
	
}
