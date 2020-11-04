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

public class CustomConfiguration {

	@JsonProperty(value="driversMapping")
	private Map<String, Map<String, String>> driversMapping;
	
	@JsonProperty(value="rhOptions")
	private Map<String, String> rhOptions;

	public Map<String, Map<String, String>> getDriversMapping() {
		return driversMapping;
	}

	public Map<String, String> getRhOptions() {
		return rhOptions;
	}
}
