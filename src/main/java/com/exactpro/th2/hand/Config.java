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
import com.exactpro.th2.hand.utils.Utils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
	public static final String DEFAULT_SERVER_TARGET = "Default";
	public static final String DEFAULT_RH_URL = "http://localhost:8008";

	protected final CommonFactory factory;
	protected final Map<String, Pair<String, String>> driversMapping;

	public Config(CommonFactory factory) {
		this.factory = factory;
		this.driversMapping = doGetDriversMappings();
	}

	protected Map<String, Pair<String, String>> doGetDriversMappings() {
		CustomConfiguration customConfig = factory.getCustomConfiguration(CustomConfiguration.class);
		Map<String, String> params = (customConfig == null) ? 
				Collections.singletonMap(DEFAULT_SERVER_TARGET, DEFAULT_RH_URL):
				customConfig.getDriversMapping();

		Map<String, Pair<String, String>> output = new LinkedHashMap<>();
		for (Map.Entry<String, String> mappings : params.entrySet()) {
			String[] key = mappings.getValue().split(Utils.DEFAULT_VALUE_DELIMITER);
			if (key.length != 2) {
				continue;
			}
			output.put(mappings.getKey(), new ImmutablePair<>(key[0], key[1]));
		}
		
		return output;
	}

	public CommonFactory getFactory() {
		return factory;
	}

	public Map<String, Pair<String, String>> getDriversMapping() {
		return driversMapping;
	}

}
