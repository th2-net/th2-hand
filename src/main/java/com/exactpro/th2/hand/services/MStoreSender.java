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

import com.exactpro.th2.common.grpc.AnyMessage;
import com.exactpro.th2.common.grpc.MessageGroup;
import com.exactpro.th2.common.grpc.MessageGroupBatch;
import com.exactpro.th2.common.grpc.RawMessage;
import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.exactpro.th2.common.schema.message.MessageRouter;
import com.exactpro.th2.hand.schema.CustomConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

public class MStoreSender {

	private static final Logger logger = LoggerFactory.getLogger(MStoreSender.class);
	
    private final MessageRouter<MessageGroupBatch> messageRouterGroupBatch;
    
    public static final String RAW_MESSAGE_ATTRIBUTE = "raw";
    
    private final long batchLimit;

	public MStoreSender(CommonFactory factory) {
		messageRouterGroupBatch = factory.getMessageRouterMessageGroupBatch();
		CustomConfiguration customConfiguration = factory.getCustomConfiguration(CustomConfiguration.class);
		this.batchLimit = customConfiguration.getMessageBatchLimit();
		writeToLogAboutConnection(factory);
	}

	private void writeToLogAboutConnection(CommonFactory factory) {
		if (!logger.isInfoEnabled())
			return;
		StringBuilder connectionInfo = new StringBuilder("Connection to RabbitMQ with ");
		connectionInfo.append(factory.getRabbitMqConfiguration()).append(" is established \n");
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

    public void sendMessages(RawMessage messages) throws Exception {
		sendMessages(Collections.singleton(messages));
	}

	public void sendMessages(Collection<RawMessage> messages) throws Exception {
		MessageGroupBatch.Builder currentBatchBuilder = MessageGroupBatch.newBuilder();
		long currentBatchLength = 0;
		long totalLength = 0;
		int count = 0;
		int batchesCount = 0;
		for (RawMessage message : messages) {
			if (message == null) 
				continue;

			long size = this.calculateSize(message);
			
			MessageGroup.Builder mgBuilder = MessageGroup.newBuilder()
					.addMessages(AnyMessage.newBuilder().setRawMessage(message));
			//if batchlimit has incorrect value, sender should pack each message to batch
			//if one message is bigger that batchLimit it is should send to mstore anyway and reject by it
			if (currentBatchLength + size > batchLimit && currentBatchLength != 0) {
				this.messageRouterGroupBatch.sendAll(currentBatchBuilder.build(), RAW_MESSAGE_ATTRIBUTE);
				currentBatchBuilder = MessageGroupBatch.newBuilder();
				currentBatchLength = 0;
				batchesCount++;
			}
			
			currentBatchBuilder.addGroups(mgBuilder);
			currentBatchLength += size;
			totalLength += size;
			count++;
		}
		
		if (currentBatchLength != 0) {
			this.messageRouterGroupBatch.sendAll(currentBatchBuilder.build(), RAW_MESSAGE_ATTRIBUTE);
			batchesCount++;
		}

		if (count == 0) {
			logger.debug("There are no valid messages to send");
			return;
		}
		
		logger.debug("Group with {} message(s) separated by {} batches to mstore was sent ({} bytes)", 
				count, batchesCount, totalLength);
	}


	private long calculateSize(RawMessage message) {
		return message.getBody().size();
	}
	
}
