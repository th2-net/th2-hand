/*
 * Copyright 2021-2021 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.hand.builders.events;

import com.exactpro.th2.hand.messages.eventpayload.EventPayloadMessage;
import com.exactpro.th2.hand.messages.eventpayload.EventPayloadTable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventPayloadBuilder {

	private static final Logger logger = LoggerFactory.getLogger(EventPayloadBuilder.class);
	
	private final List<Object> data;
	
	public EventPayloadBuilder() {
		this.data = new ArrayList<>();
	}

	public EventPayloadBuilder printText(String text) {
		this.data.add(new EventPayloadMessage(text));
		return this;
	}

	public EventPayloadBuilder printText(String title, String text) {
		this.data.add(new EventPayloadMessage(title));
		this.data.add(new EventPayloadMessage(text));
		return this;
	}

	public EventPayloadBuilder printTable(String tableHeader, Map<String, String> table) {
		this.data.add(new EventPayloadMessage(tableHeader));
		this.data.add(new EventPayloadTable(table, false));
		return this;
	}


	public byte[] toByteArray() {
		try {
			//FIXME: use MAPPER from AbstractCommonFactory
			return new ObjectMapper().writeValueAsBytes(this.data);
		} catch (JsonProcessingException e) {
			logger.error("Error while creating body", e);
			return e.getMessage().getBytes(StandardCharsets.UTF_8);
		}
	}

	public ByteString toByteString() {
		return ByteString.copyFrom(toByteArray());
	}
	
}
