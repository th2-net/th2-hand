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

import java.util.HashMap;
import java.util.Map;

public class Utils
{
	private static final String DEFAULT_KEY_VALUE_DELIMITER = "=";
	private static final String DEFAULT_RECORDS_DELIMITER = ";";


	public static Map<String, String> readMapFromString(String line)
	{
		return readMapFromString(line, DEFAULT_KEY_VALUE_DELIMITER, DEFAULT_RECORDS_DELIMITER);
	}

	public static Map<String, String> readMapFromString(String line, String keyValueDelimiter, String recordsDelimiter)
	{
		String[] records = line.split(recordsDelimiter);
		Map<String, String> result = new HashMap<>(records.length);
		for (String record : records)
		{
			String[] splitParams = record.split(keyValueDelimiter);
			if (splitParams.length != 2)
				continue;

			result.put(splitParams[0], splitParams[1]);
		}

		return result;
	}
}
