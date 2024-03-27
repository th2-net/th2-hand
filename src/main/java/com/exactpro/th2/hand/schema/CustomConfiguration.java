/*
 * Copyright 2020-2024 Exactpro (Exactpro Systems Limited)
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
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class CustomConfiguration {
	private static final String DEFAULT_SESSION_GROUP = "th2-hand-group";
	private static final String DEFAULT_SESSION_ALIAS = "th2-hand";
	private static final String DEFAULT_SCREENSHOT_SESSION_ALIAS = "th2-hand-screenshot";
	private static final int DEFAULT_RESPONSE_TIMEOUT = 120;
	private static final long DEFAULT_MESSAGE_BATCH_LIMIT = 1024 * 1024; // 1 MB

	@JsonProperty(value="session-alias", required = true, defaultValue = DEFAULT_SESSION_ALIAS)
	@JsonAlias("sessionAlias")
	private String sessionAlias = DEFAULT_SESSION_ALIAS;
	@JsonProperty(value="screenshot-session-alias", required = true, defaultValue = DEFAULT_SCREENSHOT_SESSION_ALIAS)
	@JsonAlias("screenshotSessionAlias")
	private String screenshotSessionAlias = DEFAULT_SCREENSHOT_SESSION_ALIAS;

	@JsonProperty(value="session-group", defaultValue = DEFAULT_SESSION_GROUP)
	@JsonAlias("sessionGroup")
	private String sessionGroup = DEFAULT_SESSION_GROUP;

	@JsonProperty(value="message-batch-limit")
	@JsonAlias("messageBatchLimit")
	private long messageBatchLimit = DEFAULT_MESSAGE_BATCH_LIMIT;

	@JsonProperty(value="drivers-mapping", required = true)
	@JsonAlias("driversMapping")
	private Map<String, Config.DriverMapping> driversMapping;

	@JsonProperty(value="rh-options")
	@JsonAlias("rhOptions")
	private Map<String, String> rhOptions = Collections.emptyMap();

	@JsonProperty(value="response-timeout-sec")
	@JsonAlias("responseTimeoutSec")
	private int responseTimeout = DEFAULT_RESPONSE_TIMEOUT;

	@JsonProperty(value="use-transport")
	@JsonAlias("useTransport")
	private boolean useTransport = true;

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

	public boolean isUseTransport() {
		return useTransport;
	}
}