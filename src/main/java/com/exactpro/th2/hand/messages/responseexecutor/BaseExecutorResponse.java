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

package com.exactpro.th2.hand.messages.responseexecutor;

import com.exactpro.th2.common.grpc.MessageID;
import com.google.protobuf.GeneratedMessageV3;

import java.util.List;

public class BaseExecutorResponse<T extends GeneratedMessageV3> {
	protected List<MessageID> messageIDs;
	protected T handResponse;


	public BaseExecutorResponse(T handResponse, List<MessageID> messageIDs) {
		this.handResponse = handResponse;
		this.messageIDs = messageIDs;
	}

	public T getHandResponse() {
		return handResponse;
	}

	public List<MessageID> getMessageIds() {
		return messageIDs;
	}

	public void addMessage(MessageID messageID) {
		messageIDs.add(messageID);
	}
}
