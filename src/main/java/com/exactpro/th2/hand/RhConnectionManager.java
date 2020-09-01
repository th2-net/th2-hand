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

package com.exactpro.th2.hand;

import com.exactpro.th2.hand.remotehand.RhClient;
import com.exactpro.th2.hand.remotehand.RhException;
import com.exactpro.th2.hand.remotehand.RhUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RhConnectionManager {

	private static final Logger logger = LoggerFactory.getLogger(RhConnectionManager.class);
	
	private Map<String, RhClient> clients = new ConcurrentHashMap<String, RhClient>();
	private Config config;

	public RhConnectionManager(Config config) {
		this.config = config;
	}

	public RhClient getClient(String sessionId) throws IOException, RhException
	{
		RhClient result = clients.get(sessionId);
		if (result == null)
			logger.warn("Requested client for session '{}' is not registered", sessionId);
		return result;
	}
	
	public RhClient createClient() throws IOException, RhException
	{
		RhClient result = initRhConnection(config);
		clients.put(result.getSessionId(), result);
		return result;
	}
	
	public void closeClient(String sessionId) throws IOException
	{
		RhClient client = clients.remove(sessionId);
		if (client == null)
		{
			logger.warn("Client for session '{}', requested to close, is not registered", sessionId);
			return;
		}
		client.close();
	}
	
	public void dispose()
	{
		Iterator<String> it = clients.keySet().iterator();
		while (it.hasNext())
		{
			String id = it.next();
			RhClient client = clients.remove(id);
			try
			{
				client.close();
			}
			catch (IOException e)
			{
				logger.warn("Error while closing RH client for session '{}'", id);
			}
		}
	}

	protected RhClient initRhConnection(Config config) throws IOException, RhException
	{
		logger.info("Creating RemoteHand connection...");
		return RhUtils.createRhConnection(config.getRhUrl());
	}
}
