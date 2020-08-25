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

package com.exactpro.th2.hand.remotehand;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RhScriptCompiler
{
	private static final Logger logger = LoggerFactory.getLogger(RhScriptCompiler.class);
	protected static final String VAR_MARK = "%", COMPILE_ERROR = "Error while compiling RemoteHand script: ";
	
	int pointer = 0;

	protected String nextVariable(String rowScript) throws RhException
	{
		int startPos = rowScript.indexOf(VAR_MARK, pointer);
		if (startPos < 0)
			return null;
		if (startPos == rowScript.length() - 1)
			throw new RhException(COMPILE_ERROR+"unclosed variable mark at " + startPos);
		pointer = startPos;

		int endPos = rowScript.indexOf(VAR_MARK, startPos + 1);
		if (endPos<0)
			throw new RhException(COMPILE_ERROR+"unclosed variable mark at " + startPos);

		return rowScript.substring(startPos, endPos + 1);
	}

	protected String getValue(String expression, Map<String, String> variables) throws RhException
	{
		String pureExp = expression.replace(VAR_MARK, "");
		String varValue = variables != null ? variables.get(pureExp) : null;
		if (varValue == null)
			throw new RhException(COMPILE_ERROR+"no such variable - '" + expression + "'");
		return varValue;
	}

	public String compile(String script, Map<String, String> variables) throws RhException
	{
		pointer = 0;
		String expression;
		while ((expression = nextVariable(script)) != null)
		{
			final String value = getValue(expression, variables);
			script = script.replaceFirst(expression, value);
			logger.trace("Replaced '" + expression + "' with '" + value + "'");
			pointer += value.length();
		}
		return script;
	}
}
