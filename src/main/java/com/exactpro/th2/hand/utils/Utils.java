/*
 *  Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.exactpro.th2.hand.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class Utils {
	public static final String LINE_SEPARATOR = "&#13";
	private static final String DEFAULT_KEY_VALUE_DELIMITER = "=";
	private static final String DEFAULT_RECORDS_DELIMITER = ";";
	private static final String DEFAULT_VALUE_DELIMITER = "@";


	public static Map<String, Pair<String, String>> readDriversMappingFromString(String line)
	{
		return readDriversMappingFromString(line, DEFAULT_RECORDS_DELIMITER, DEFAULT_KEY_VALUE_DELIMITER, DEFAULT_VALUE_DELIMITER);
	}

	public static Map<String, Pair<String, String>> readDriversMappingFromString(String line, String recordsDelimiter,
	                                                                             String keyValueDelimiter, String valueDelimiter)
	{
		if (line == null)
			return Collections.emptyMap();

		String[] records = line.split(recordsDelimiter);
		Map<String, Pair<String, String>> result = new HashMap<>(records.length);
		for (String record : records)
		{
			String[] splitParams = record.split(keyValueDelimiter); // 0 - target machine, 1 - driver type with url
			if (splitParams.length != 2)
				continue;

			String[] splitValue = splitParams[1].split(valueDelimiter); // 0 - driver type, 1 - driver url
			if (splitValue.length != 2)
				continue;

			result.put(splitParams[0], new ImmutablePair<>(splitValue[0], splitValue[1]));
		}

		return result;
	}
}
