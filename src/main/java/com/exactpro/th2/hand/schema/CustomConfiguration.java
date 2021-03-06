/*
 * Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.hand.schema;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class CustomConfiguration {
	private static final String DEFAULT_SESSION_ALIAS = "th2-hand";
	private static final int DEFAULT_RESPONSE_TIMEOUT = 120;
	private static final long DEFAULT_MESSAGE_BATCH_LIMIT = 1048576; //1MB = 1 * 1024 * 1024

	@JsonProperty(value="session-alias", required = true, defaultValue = DEFAULT_SESSION_ALIAS)
	private String sessionAlias = DEFAULT_SESSION_ALIAS;
	private String screenshotSessionAlias = null;

	@JsonProperty(value="message-batch-limit")
	private long messageBatchLimit = DEFAULT_MESSAGE_BATCH_LIMIT;
	
	@JsonProperty(value="driversMapping")
	private Map<String, Map<String, String>> driversMapping;
	
	@JsonProperty(value="rhOptions")
	private Map<String, String> rhOptions;
	
	@JsonProperty(value="responseTimeoutSec")
	private int responseTimeout = DEFAULT_RESPONSE_TIMEOUT;

	public Map<String, Map<String, String>> getDriversMapping() {
		return driversMapping;
	}

	public Map<String, String> getRhOptions() {
		return rhOptions;
	}

	public String getSessionAlias()
	{
		return sessionAlias;
	}

	public int getResponseTimeout() {
		return responseTimeout;
	}

	public String getScreenshotSessionAlias() {
		if (this.screenshotSessionAlias == null) {
			this.screenshotSessionAlias = this.sessionAlias + "_screenshots";
		}
		return this.screenshotSessionAlias;
	}

	public long getMessageBatchLimit() {
		return messageBatchLimit;
	}
}
