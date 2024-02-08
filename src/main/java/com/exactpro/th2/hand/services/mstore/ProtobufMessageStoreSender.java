/*
 *  Copyright 2020-2024 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.hand.services.mstore;

import com.exactpro.th2.common.grpc.AnyMessage;
import com.exactpro.th2.common.grpc.MessageGroup;
import com.exactpro.th2.common.grpc.MessageGroupBatch;
import com.exactpro.th2.common.grpc.RawMessage;
import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.exactpro.th2.common.schema.message.MessageRouter;
import com.exactpro.th2.hand.schema.CustomConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

public class ProtobufMessageStoreSender implements MessageStoreSender<RawMessage> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProtobufMessageStoreSender.class);
	private final MessageRouter<MessageGroupBatch> messageRouterGroupBatch;
	private final long batchLimit;


	public ProtobufMessageStoreSender(CommonFactory factory) {
		messageRouterGroupBatch = factory.getMessageRouterMessageGroupBatch();
		CustomConfiguration customConfiguration = factory.getCustomConfiguration(CustomConfiguration.class);
		this.batchLimit = customConfiguration.getMessageBatchLimit();
	}

    public void sendMessages(RawMessage messages) {
		sendMessages(Collections.singleton(messages));
	}

	public void sendMessages(Collection<RawMessage> messages) {
		try {
			sendRawMessages(messages);
		} catch (Exception e) {
			LOGGER.error("Cannot store to mstore", e);
		}
	}

	private void sendRawMessages(Collection<RawMessage> messages) throws Exception {
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
			//if batch limit has incorrect value, sender should pack each message to batch
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
			LOGGER.debug("There are no valid messages to send");
			return;
		}
		
		LOGGER.debug("Group with {} message(s) separated by {} batches to mstore was sent ({} bytes)",
				count, batchesCount, totalLength);
	}

	private long calculateSize(RawMessage message) {
		return message.getBody().size();
	}
}
