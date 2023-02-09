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

package com.exactpro.th2.hand.services.mstore;

import com.exactpro.remotehand.ActionResult;
import com.exactpro.remotehand.Configuration;
import com.exactpro.remotehand.rhdata.RhScriptResult;
import com.exactpro.th2.act.grpc.hand.RhActionList;
import com.exactpro.th2.act.grpc.hand.RhActionsBatch;
import com.exactpro.th2.common.grpc.Direction;
import com.exactpro.th2.common.grpc.MessageID;
import com.exactpro.th2.common.grpc.RawMessage;
import com.exactpro.th2.hand.builders.mstore.DefaultMessageStoreBuilder;
import com.exactpro.th2.hand.messages.RhResponseMessageBody;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class MessageStoreHandler implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MessageStoreSender.class);

	private final String sessionGroup;
	private final MessageStoreSender messageStoreSender;
	private final DefaultMessageStoreBuilder messageStoreBuilder;

	public MessageStoreHandler(String sessionGroup, MessageStoreSender messageStoreSender, DefaultMessageStoreBuilder defaultMessageStoreBuilder) {
		this.sessionGroup = sessionGroup;
		this.messageStoreSender = messageStoreSender;
		this.messageStoreBuilder = defaultMessageStoreBuilder;
	}
	
	private List<? extends GeneratedMessageV3> getActionsList (RhActionsBatch actionsList) {
		RhActionList rhActionList = actionsList.getRhAction();
		switch (rhActionList.getListCase()) {
			case WIN:
				return rhActionList.getWin().getWinActionListList();
			case WEB:
				return rhActionList.getWeb().getWebActionListList();
			default:
				logger.warn("Actions list is not set");
				return Collections.emptyList();
		}
	}

	public List<MessageID> onRequest(RhActionsBatch actionsList, String sessionId) {
		List<Map<String, Object>> allMessages = new ArrayList<>();
		for (GeneratedMessageV3 rhAction : getActionsList(actionsList)) {
			for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : rhAction.getAllFields().entrySet()) {
				if (entry != null) {
					Map<String, Object> fields = new LinkedHashMap<>();
					fields.put("ActionName", entry.getKey().getName());
					Object value = entry.getValue();
					if (value instanceof GeneratedMessageV3) {
						for (Map.Entry<Descriptors.FieldDescriptor, Object> entry2 :
								((GeneratedMessageV3)value).getAllFields().entrySet()) {
							Object valueObj = entry2.getValue();
							if (valueObj instanceof List) {
								fields.put(entry2.getKey().getName(), this.processList((List<?>) valueObj));
							} else {
								fields.put(entry2.getKey().getName(), valueToString(valueObj));
							}

						}
					}
					allMessages.add(fields);
				}
			}
		}

		RawMessage message = messageStoreBuilder.buildMessage(Collections.singletonMap("messages", allMessages),
				Direction.SECOND, sessionId, sessionGroup);

		if (message != null) {
			messageStoreSender.sendMessages(message);
			return Collections.singletonList(message.getMetadata().getId());
		} else {
			logger.debug("Nothing to store to mstore");
			return Collections.emptyList();
		}
	}

	public List<MessageID> storeScreenshots(List<ActionResult> screenshotIds, String sessionAlias) {
		if (screenshotIds == null || screenshotIds.isEmpty()) {
			logger.debug("No screenshots to store");
			return Collections.emptyList();
		}

		List<MessageID> messageIDS = new ArrayList<>();
		List<RawMessage> rawMessages = new ArrayList<>();
		for (ActionResult screenshotId : screenshotIds) {
			logger.debug("Storing screenshot id {}", screenshotId);
			Path screenPath = Configuration.SCREENSHOTS_DIR_PATH.resolve(screenshotId.getData());
			if (!Files.exists(screenPath)) {
				logger.warn("Screenshot with id {} does not exists", screenshotId);
				continue;
			}
			RawMessage rawMessage = messageStoreBuilder.buildMessageFromFile(screenPath, Direction.FIRST, sessionAlias, sessionGroup);
			if (rawMessage != null) {
				messageIDS.add(rawMessage.getMetadata().getId());
				rawMessages.add(rawMessage);
			}
			removeScreenshot(screenPath);
		}
		messageStoreSender.sendMessages(rawMessages);

		return messageIDS;
	}

	public MessageID onResponse(RhScriptResult response, String sessionId, String rhSessionId) {
		RhResponseMessageBody body = RhResponseMessageBody.fromRhScriptResult(response).setRhSessionId(rhSessionId);
		try {
			RawMessage message = messageStoreBuilder.buildMessage(body.getFields(), Direction.FIRST, sessionId, sessionGroup);
			messageStoreSender.sendMessages(message);
			return message.getMetadata().getId();
		} catch (Exception e) {
			logger.error("Cannot send message to message-storage", e);
		}

		return null;
	}

	private void removeScreenshot(Path file) {
		try {
			Files.delete(file);
		} catch (IOException e) {
			logger.warn("Error deleting file: " + file.toAbsolutePath(), e);
		}
	}

	private String valueToString(Object object) {
		if (object instanceof Int32Value) {
			return String.valueOf(((Int32Value) object).getValue());
		} else if (object instanceof StringValue) {
			return ((StringValue) object).getValue();
		} else {
			return String.valueOf(object);
		}
	}

	private List<Map<String, Object>> processList(List<?> list) {
		List<Map<String, Object>> processed = new ArrayList<>(list.size());
		for (Object o : list) {
			if (o instanceof GeneratedMessageV3) {
				Map<String, Object> map = new LinkedHashMap<>();
				for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : ((GeneratedMessageV3) o).getAllFields().entrySet()) {
					map.put(entry.getKey().getName(), valueToString(entry.getValue()));
				}
				processed.add(map);
			} else {
				processed.add(Collections.singletonMap("Value", valueToString(o)));
			}
		}
		return processed;
	}

	@Override
	public void close() throws Exception {
		this.messageStoreSender.close();
	}
}