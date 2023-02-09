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

package com.exactpro.th2.hand.schema;

import java.util.Collections;
import java.util.Map;

import com.exactpro.th2.hand.Config;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class CustomConfiguration {
	private static final String DEFAULT_SESSION_ALIAS = "th2-hand";
	private static final int DEFAULT_RESPONSE_TIMEOUT = 120;
	private static final long DEFAULT_MESSAGE_BATCH_LIMIT = 1024 * 1024; // 1 MB

	@JsonProperty(value="session-alias", required = true, defaultValue = DEFAULT_SESSION_ALIAS)
	private String sessionAlias = DEFAULT_SESSION_ALIAS;
	private String screenshotSessionAlias = null;

	@JsonProperty(value="sessionGroup")
	private String sessionGroup = null;

	@JsonProperty(value="message-batch-limit")
	private long messageBatchLimit = DEFAULT_MESSAGE_BATCH_LIMIT;

	@JsonProperty(value="driversMapping", required = true)
	private Map<String, Config.DriverMapping> driversMapping;

	@JsonProperty(value="rhOptions")
	private Map<String, String> rhOptions = Collections.emptyMap();;

	@JsonProperty(value="responseTimeoutSec")
	private int responseTimeout = DEFAULT_RESPONSE_TIMEOUT;

	public Map<String, Config.DriverMapping> getDriversMapping() {
		return driversMapping;
	}

	public Map<String, String> getRhOptions() {
		return rhOptions;
	}

	public String getSessionAlias() {
		return sessionAlias;
	}

	public String getSessionGroup() {
		return sessionGroup;
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