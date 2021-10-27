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

package com.exactpro.th2.hand.messages;

import com.exactpro.remotehand.rhdata.RhResponseCode;
import com.exactpro.remotehand.rhdata.RhScriptResult;
import com.exactpro.th2.act.grpc.hand.MessageType;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResponseParams {
	private String sessionAlias;
	private String rhSessionId = "th2_hand";
	private String executionId;
	private Map<String, Object> params;
	private MessageType messageType = MessageType.PLAIN_STRING;


	private ResponseParams() {
	}

	public String getSessionAlias() {
		return sessionAlias;
	}

	public String getRhSessionId() {
		return rhSessionId;
	}

	public String getExecutionId() {
		return executionId;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public static ResponseParamsBuilder builder() {
		return new ResponseParamsBuilder();
	}

	public static class ResponseParamsBuilder {
		private final ResponseParams responseParams;
		private RhScriptResult scriptResult;

		private ResponseParamsBuilder() {
			this.responseParams = new ResponseParams();
		}

		public ResponseParamsBuilder setSessionAlias(String sessionAlias) {
			this.responseParams.sessionAlias = sessionAlias;
			return this;
		}

		public ResponseParamsBuilder setRhSessionId(String rhSessionId) {
			this.responseParams.rhSessionId = rhSessionId;
			return this;
		}

		public ResponseParamsBuilder setExecutionId(String executionId) {
			this.responseParams.executionId = executionId;
			return this;
		}

		public ResponseParamsBuilder setMessageType(MessageType messageType) {
			this.responseParams.messageType = messageType;
			return this;
		}

		public ResponseParamsBuilder setScriptResult(RhScriptResult scriptResult) {
			this.scriptResult = scriptResult;
			return this;
		}

		public ResponseParams build() {
			responseParams.params = createParams();
			return responseParams;
		}


		private Map<String, Object> createParams() {
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("ScriptOutputCode", RhResponseCode.byCode(scriptResult.getCode()).toString());
			params.put("ErrorText", scriptResult.getErrorMessage());
			params.put("ActionResults", scriptResult.getActionResults());
			params.put("RhSessionId", this.responseParams.rhSessionId);
			params.put("MessageType", this.responseParams.messageType);
			params.put("ExecutionId", this.responseParams.executionId);
			return params;
		}
	}
}
