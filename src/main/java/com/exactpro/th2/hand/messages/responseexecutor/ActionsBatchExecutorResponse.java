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

import com.exactpro.remotehand.rhdata.RhScriptResult;
import com.exactpro.th2.act.grpc.hand.RhBatchResponse;
import com.exactpro.th2.common.grpc.MessageID;

import java.util.List;

public class ActionsBatchExecutorResponse extends BaseExecutorResponse<RhBatchResponse> {
	private final RhScriptResult scriptResult;
	public ActionsBatchExecutorResponse(RhBatchResponse response, RhScriptResult scriptResult, List<MessageID> messageIDs) {
		super(response, messageIDs);
		this.scriptResult = scriptResult;
	}

	public RhScriptResult getScriptResult() {
		return scriptResult;
	}
}
