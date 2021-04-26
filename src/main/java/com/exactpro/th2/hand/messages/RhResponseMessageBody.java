/*
 *  Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.hand.messages;

import com.exactpro.remotehand.rhdata.RhResponseCode;
import com.exactpro.remotehand.rhdata.RhScriptResult;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class RhResponseMessageBody
{
	private String scriptOutputCode = StringUtils.EMPTY;
	private String errorOut = StringUtils.EMPTY;
	private String textOut = StringUtils.EMPTY;
	private String rhSessionId = StringUtils.EMPTY;

	public static RhResponseMessageBody fromRhScriptResult(RhScriptResult scriptResult)
	{
		return new RhResponseMessageBody()
				.setScriptOutputCode(RhResponseCode.byCode(scriptResult.getCode()).toString())
				.setErrorOut(scriptResult.getErrorMessage())
				.setTextOut(String.join("|", scriptResult.getTextOutput()));
	}
	
	public String getScriptOutputCode()
	{
		return scriptOutputCode;
	}

	public RhResponseMessageBody setScriptOutputCode(String scriptOutputCode)
	{
		this.scriptOutputCode = StringUtils.defaultString(scriptOutputCode);
		return this;
	}

	public String getErrorOut()
	{
		return errorOut;
	}

	public RhResponseMessageBody setErrorOut(String errorOut)
	{
		this.errorOut = StringUtils.defaultString(errorOut);
		return this;
	}

	public String getTextOut()
	{
		return textOut;
	}

	public RhResponseMessageBody setTextOut(String textOut)
	{
		this.textOut = StringUtils.defaultString(textOut);
		return this;
	}

	public String getRhSessionId()
	{
		return rhSessionId;
	}

	public RhResponseMessageBody setRhSessionId(String rhSessionId)
	{
		this.rhSessionId = StringUtils.defaultString(rhSessionId);
		return this;
	}
	
	public Map<String, Object> getFields()
	{
		Map<String, Object> value = new LinkedHashMap<>();
		value.put("ScriptOutputCode", scriptOutputCode);
		value.put("ErrorText", errorOut);
		value.put("TextOut", textOut);
		value.put("RhSessionId", rhSessionId);

		return value;
	}
}
