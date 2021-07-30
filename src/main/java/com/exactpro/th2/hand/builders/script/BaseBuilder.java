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

package com.exactpro.th2.hand.builders.script;

import com.google.protobuf.GeneratedMessageV3;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseBuilder<T extends GeneratedMessageV3, K> {
	protected static final String ACTION = "#action";


	public void buildScript(CSVPrinter printer, K action) throws IOException {
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add(getActionName());

		buildPayLoad(action, headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	protected abstract T getMessage(K action);

	protected abstract String getActionName();

	protected abstract void createActionDetails(T message, List<String> headers, List<String> values);

	protected abstract void buildPayLoad(K action, List<String> headers, List<String> values);

	protected static void addIfNotEmpty(String headerName, boolean value, List<String> headers, List<String> values) {
		addIfNotEmpty(headerName, String.valueOf(value), headers, values);
	}

	protected static void addIfNotEmpty(String headerName, int value, List<String> headers, List<String> values) {
		addIfNotEmpty(headerName, String.valueOf(value), headers, values);
	}

	protected static void addIfNotEmpty(String headerName, String value, List<String> headers, List<String> values) {
		if (StringUtils.isNotEmpty(value)) {
			headers.add(headerName);
			values.add(value);
		}
	}
}
