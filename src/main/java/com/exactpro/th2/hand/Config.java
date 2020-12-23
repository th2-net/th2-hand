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

package com.exactpro.th2.hand;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.exactpro.th2.hand.schema.CustomConfiguration;

public class Config {

	protected final CommonFactory factory;
	protected final CustomConfiguration customConfiguration;
	protected final Map<String, DriverMapping> driversMapping;

	public Config(CommonFactory factory) throws ConfigurationException {
		this.factory = factory;
		this.customConfiguration = factory.getCustomConfiguration(CustomConfiguration.class);
		if (customConfiguration == null) {
			throw new ConfigurationException("Custom configuration is not found");
		}
		this.driversMapping = doGetDriversMappings();
	}

	protected Map<String, DriverMapping> doGetDriversMappings() throws ConfigurationException {
		Map<String, Map<String, String>> params = customConfiguration.getDriversMapping();
		if (params == null || params.isEmpty()) {
			throw new ConfigurationException("Drivers mapping should be provided in custom config.");
		}

		Map<String, DriverMapping> output = new LinkedHashMap<>();
		for (Map.Entry<String, Map<String, String>> mappings : params.entrySet()) {
			Map<String, String> value = mappings.getValue();
			String type = value.get("type");
			String url = value.get("url");
			
			output.put(mappings.getKey(), new DriverMapping(type, url));
		}
		
		return output;
	}

	public CommonFactory getFactory() {
		return factory;
	}

	public Map<String, DriverMapping> getDriversMapping() {
		return driversMapping;
	}
	
	public Map<String, String> getRhOptions() {
		Map<String, String> rhOptions = this.customConfiguration.getRhOptions();
		if (rhOptions == null) {
			rhOptions = Collections.emptyMap(); 
		}
		return rhOptions;
	}
	
	public String getSessionAlias()
	{
		return customConfiguration.getSessionAlias();
	}

	public static class DriverMapping {
		
		public final String type;
		public final String url;

		public DriverMapping(String type, String url) {
			this.type = type;
			this.url = url;
		}
	} 
	
}
