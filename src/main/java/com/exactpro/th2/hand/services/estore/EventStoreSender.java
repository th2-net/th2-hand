/*
 * Copyright 2020-2023 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.hand.services.estore;

import com.exactpro.th2.common.grpc.Event;
import com.exactpro.th2.common.grpc.EventBatch;
import com.exactpro.th2.common.schema.message.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class EventStoreSender implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventStoreSender.class);

	private final MessageRouter<EventBatch> eventBatchRouter;

	public EventStoreSender(MessageRouter<EventBatch> eventBatchRouter) {
		this.eventBatchRouter = eventBatchRouter;
	}

	public void storeEvent(Event event) {
		try {
			eventBatchRouter.send(EventBatch.newBuilder().addEvents(event).build());
			LOGGER.info("Event ID = " + event.getId());
		} catch (IOException e) {
			LOGGER.error("Could not store event with id: " + event.getId(), e);
		}
	}

	@Override
	public void close() throws Exception {
		eventBatchRouter.close();
	}
}