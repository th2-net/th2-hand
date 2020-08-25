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

import java.nio.charset.StandardCharsets;

public class RhResponse
{
	private final int code;
	private final byte[] data;
	private String dataString = null;
	
	public RhResponse(int code, byte[] data)
	{
		this.code = code;
		this.data = data;
	}
	
	public RhResponse(int code, String data)
	{
		byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
		
		this.code = code;
		this.data = bytes;
		this.dataString = data;
	}
	
	
	public int getCode()
	{
		return code;
	}
	
	public byte[] getData()
	{
		return data;
	}
	
	public String getDataString()
	{
		if (dataString == null)
			dataString = new String(data, StandardCharsets.UTF_8);
		
		return dataString;
	}
}
