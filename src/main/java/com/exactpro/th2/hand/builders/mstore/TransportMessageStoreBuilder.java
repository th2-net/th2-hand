/*
 * Copyright 2024 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.hand.builders.mstore;

import com.exactpro.remotehand.Configuration;
import com.exactpro.th2.common.grpc.Direction;
import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.MessageId;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.RawMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.TransportUtilsKt.getTransport;

public final class TransportMessageStoreBuilder implements MessageStoreBuilder<RawMessage> {
	private static final Logger logger = LoggerFactory.getLogger(TransportMessageStoreBuilder.class);

	private final AtomicLong seqNum;

    public TransportMessageStoreBuilder(AtomicLong seqNum) {
        this.seqNum = seqNum;
	}

	@Override
	public RawMessage buildMessage(Map<String, Object> fields, Direction direction, String sessionId, String sessionGroup) {
		try {
			byte[] bytes = CommonFactory.MAPPER.writeValueAsBytes(fields);
			return buildMessage(bytes, direction, sessionId, sessionGroup);
		} catch (JsonProcessingException e) {
			logger.error("Could not encode message as JSON", e);
			return null;
		}
	}

	@Override
	public RawMessage buildMessage(byte[] bytes, Direction direction, String sessionId, String sessionGroup) {
		RawMessage.Builder builder = RawMessage.builder()
				.setId(MessageId.builder()
						.setSessionAlias(sessionId)
						.setDirection(getTransport(direction))
						.setSequence(seqNum.incrementAndGet())
						.setTimestamp(Instant.now())
						.build())
				.setBody(bytes);
		return builder.build();
	}

	@Override
	public RawMessage buildMessageFromFile(Path path, Direction direction, String sessionId, String sessionGroup) {
		String protocol = "image/" + Configuration.getInstance().getDefaultScreenWriter().getScreenshotExtension();

		try (InputStream is = Files.newInputStream(path)) {
			int length = Math.toIntExact(path.toFile().length());
			ByteBuf buffer = Unpooled.buffer(length);
			buffer.writeBytes(is, length);

			return RawMessage.builder()
					.setId(MessageId.builder()
							.setSessionAlias(sessionId)
							.setDirection(getTransport(direction))
							.setSequence(seqNum.incrementAndGet())
							.setTimestamp(Instant.now())
							.build())
					.setProtocol(protocol)
					.setBody(buffer)
					.build();
		} catch (IOException e) {
			logger.error("Cannot encode screenshot", e);
			return null;
		}
	}
}