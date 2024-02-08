/*
 *  Copyright 2024 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.exactpro.th2.common.schema.message.MessageRouter;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.GroupBatch;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.RawMessage;
import com.exactpro.th2.common.utils.message.transport.MessageUtilsKt;
import com.exactpro.th2.hand.schema.CustomConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

public class TransportMessageStoreSender implements MessageStoreSender<RawMessage> {
	private static final Logger LOGGER = LoggerFactory.getLogger(TransportMessageStoreSender.class);
	private final MessageRouter<GroupBatch> messageRouter;
	private final long batchLimit;
	private final String book;
	private final String sessionGroup;

	public TransportMessageStoreSender(CommonFactory factory) {
		messageRouter = factory.getTransportGroupBatchRouter();
		CustomConfiguration customConfiguration = factory.getCustomConfiguration(CustomConfiguration.class);
		this.batchLimit = customConfiguration.getMessageBatchLimit();
		this.book = factory.getBoxConfiguration().getBookName();
		this.sessionGroup = customConfiguration.getSessionGroup();
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
		GroupBatch.Builder currentBatchBuilder = createBatchBuilder();
		long currentBatchLength = 0;
		long totalLength = 0;
		int count = 0;
		int batchesCount = 0;
		for (RawMessage message : messages) {
			if (message == null) 
				continue;

			long size = message.getBody().readableBytes();
			
			//if batch limit has incorrect value, sender should pack each message to batch
			//if one message is bigger that batchLimit it is should send to mstore anyway and reject by it
			if (currentBatchLength + size > batchLimit && currentBatchLength != 0) {
				this.messageRouter.sendAll(currentBatchBuilder.build(), RAW_MESSAGE_ATTRIBUTE);
				currentBatchBuilder = createBatchBuilder();
				currentBatchLength = 0;
				batchesCount++;
			}
			
			currentBatchBuilder.addGroup(MessageUtilsKt.toGroup(message));
			currentBatchLength += size;
			totalLength += size;
			count++;
		}
		
		if (currentBatchLength != 0) {
			this.messageRouter.sendAll(currentBatchBuilder.build(), RAW_MESSAGE_ATTRIBUTE);
			batchesCount++;
		}

		if (count == 0) {
			LOGGER.debug("There are no valid messages to send");
			return;
		}
		
		LOGGER.debug("Group with {} message(s) separated by {} batches to mstore was sent ({} bytes)",
				count, batchesCount, totalLength);
	}

	private GroupBatch.Builder createBatchBuilder() {
		return GroupBatch.builder()
				.setBook(book)
				.setSessionGroup(sessionGroup);
	}
}
