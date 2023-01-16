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

package com.exactpro.th2.hand.builders.mstore;

import com.exactpro.remotehand.Configuration;
import com.exactpro.th2.common.grpc.ConnectionID;
import com.exactpro.th2.common.grpc.MessageID;
import com.exactpro.th2.common.grpc.RawMessage;
import com.exactpro.th2.common.grpc.RawMessageMetadata;
import com.exactpro.th2.common.grpc.Direction;
import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.exactpro.th2.hand.utils.Utils.getTimestamp;

public final class DefaultMessageStoreBuilder implements MessageStoreBuilder<RawMessage> {
	private static final Logger logger = LoggerFactory.getLogger(DefaultMessageStoreBuilder.class);

	private final ObjectMapper mapper = new ObjectMapper();
	private final AtomicLong seqNum;
	private final CommonFactory factory;

	public DefaultMessageStoreBuilder(CommonFactory factory, AtomicLong seqNum) {
		this.factory = factory;
		this.seqNum = seqNum;
	}

	@Override
	public RawMessage buildMessage(Map<String, Object> fields, Direction direction, String sessionId, String sessionGroup) {
		try {
			byte[] bytes = mapper.writeValueAsBytes(fields);
			return buildMessage(bytes, direction, sessionId, sessionGroup);
		} catch (JsonProcessingException e) {
			logger.error("Could not encode message as JSON", e);
			return null;
		}
	}

	@Override
	public RawMessage buildMessage(byte[] bytes, Direction direction, String sessionId, String sessionGroup) {
		RawMessageMetadata messageMetadata = buildMetaData(direction, sessionId, sessionGroup, null);
		return RawMessage.newBuilder().setMetadata(messageMetadata).setBody(ByteString.copyFrom(bytes)).build();
	}

	@Override
	public RawMessage buildMessageFromFile(Path path, Direction direction, String sessionId, String sessionGroup) {
		String protocol = "image/" + Configuration.getInstance().getDefaultScreenWriter().getScreenshotExtension();
		RawMessageMetadata messageMetadata = buildMetaData(direction, sessionId, sessionGroup, protocol);

		try (InputStream is = Files.newInputStream(path)) {
			return RawMessage.newBuilder().setMetadata(messageMetadata).setBody(ByteString.readFrom(is, 0x1000)).build();
		} catch (IOException e) {
			logger.error("Cannot encode screenshot", e);
			return null;
		}
	}

	private RawMessageMetadata buildMetaData(
			Direction direction,
			String sessionId,
			String sessionGroup,
			String protocol
	) {
		ConnectionID.Builder connectionID = ConnectionID.newBuilder().setSessionAlias(sessionId);
		if (sessionGroup != null) {
			connectionID.setSessionGroup(sessionGroup);
		}

		MessageID.Builder messageID = factory.newMessageIDBuilder()
				.setConnectionId(connectionID)
				.setDirection(direction)
				.setSequence(seqNum.incrementAndGet())
				.setTimestamp(getTimestamp(Instant.now()));
		RawMessageMetadata.Builder builder = RawMessageMetadata.newBuilder().setId(messageID);
		if (protocol != null) {
			builder.setProtocol(protocol);
		}

		return builder.build();
	}
}