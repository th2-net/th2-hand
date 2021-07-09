/*
 * Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.hand.builders.events.DefaultEventBuilder;

public class EventStoreHandler implements AutoCloseable {
	private final EventStoreSender eventStoreSender;
	private final DefaultEventBuilder eventBuilder;


	public EventStoreHandler(EventStoreSender eventStoreSender, DefaultEventBuilder eventBuilder) {
		this.eventStoreSender = eventStoreSender;
		this.eventBuilder = eventBuilder;
	}

	public EventStoreSender getEventStoreSender() {
		return eventStoreSender;
	}

	public DefaultEventBuilder getEventBuilder() {
		return eventBuilder;
	}

	@Override
	public void close() throws Exception {
		this.eventStoreSender.close();
	}
}
