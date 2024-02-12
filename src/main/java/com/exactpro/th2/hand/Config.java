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

package com.exactpro.th2.hand;

import java.util.Map;

import com.exactpro.remotehand.RemoteManagerType;
import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.exactpro.th2.hand.schema.CustomConfiguration;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {

	protected final CommonFactory factory;
	protected final CustomConfiguration customConfiguration;

	public Config(CommonFactory factory, CustomConfiguration customConfiguration) throws ConfigurationException {
		if (customConfiguration == null) {
			throw new ConfigurationException("Custom configuration is not found");
		}

		if (customConfiguration.getDriversMapping().isEmpty()) {
			throw new ConfigurationException("Drivers mapping should be provided in custom config.");
		}

		this.factory = factory;
		this.customConfiguration = customConfiguration;
	}

	public Config(CommonFactory factory) throws ConfigurationException {
		this(factory, factory.getCustomConfiguration(CustomConfiguration.class));
	}

	public CommonFactory getFactory() {
		return factory;
	}

	public Map<String, DriverMapping> getDriversMapping() {
		return customConfiguration.getDriversMapping();
	}

	public Map<String, String> getRhOptions() {
		return this.customConfiguration.getRhOptions();
	}

	public int getResponseTimeout() {
		return customConfiguration.getResponseTimeout();
	}

	public String getSessionAlias() {
		return customConfiguration.getSessionAlias();
	}

	public String getSessionGroup() {
		return customConfiguration.getSessionGroup();
	}

	public String getBook() {
		return factory.getBoxConfiguration().getBookName();
	}

	public String getScreenshotSessionAlias() {
		return customConfiguration.getScreenshotSessionAlias();
	}

	public boolean isUseTransport() { return customConfiguration.isUseTransport(); }

	public static class DriverMapping {
		public final RemoteManagerType type;
		public final String url;

		@JsonCreator
		public DriverMapping(
				@JsonProperty(value = "type", required = true) String type,
				@JsonProperty(value = "url", required = true) String url
		) {
			this.type = RemoteManagerType.getByLabel(type);
			if (this.type == null)
				throw new IllegalArgumentException("Unrecognized remote manager type: " + type);
			this.url = url;
		}
	}
}