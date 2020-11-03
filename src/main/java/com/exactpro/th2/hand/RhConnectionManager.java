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

import com.exactpro.th2.hand.services.HandBaseService;
import com.exactpro.th2.hand.services.HandSessionHandler;
import com.exactprosystems.remotehand.IRemoteHandManager;
import com.exactprosystems.remotehand.RemoteManagerType;
import com.exactprosystems.remotehand.RhConfigurationException;
import com.exactprosystems.remotehand.grid.GridRemoteHandManager;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RhConnectionManager {
	private static final Logger logger = LoggerFactory.getLogger(RhConnectionManager.class);

	private final Map<String, HandSessionHandler> sessions = new ConcurrentHashMap<String, HandSessionHandler>();
	private final GridRemoteHandManager gridRemoteHandManager;
	private final Config config;


	public RhConnectionManager(Config config) {
		this.config = config;
		gridRemoteHandManager = new GridRemoteHandManager();
		gridRemoteHandManager.createConfigurations(config.getCommandLine());
	}


	public HandSessionHandler getSessionHandler(String sessionId) throws IllegalArgumentException {
		HandSessionHandler sessionHandler = sessions.get(sessionId);
		if (sessionHandler == null)
			throw new IllegalArgumentException("Requested client for session '"+sessionId+"' is not registered");
		return sessionHandler;
	}

	public HandSessionHandler createSessionHandler(String targetServer) throws RhConfigurationException {
		Pair<String, String> driverSettings = config.getDriversMapping().get(targetServer);
		RemoteManagerType remoteManagerType = RemoteManagerType.getByLabel(driverSettings.getKey());
		if (remoteManagerType == null)
			throw new RhConfigurationException("Unrecognized driver manager type '"+driverSettings.getKey()+"'");

		String sessionId = generateSessionId();
		IRemoteHandManager remoteHandManager = gridRemoteHandManager.getRemoteHandManager(remoteManagerType);
		HandSessionHandler handSessionHandler = new HandSessionHandler(sessionId, remoteHandManager);
		gridRemoteHandManager.saveSession(sessionId, driverSettings.getValue());
		sessions.put(sessionId, handSessionHandler);

		return handSessionHandler;
	}

	public void closeSessionHandler(String sessionId) {
		HandSessionHandler sessionHandler = sessions.remove(sessionId);
		if (sessionHandler == null)
		{
			logger.warn("Session handler for session '{}', requested to close, is not registered", sessionId);
			return;
		}
		sessionHandler.close();
	}

	public void dispose() {
		for (String id : sessions.keySet()) {
			HandSessionHandler sessionHandler = sessions.remove(id);
			sessionHandler.close();
		}
		gridRemoteHandManager.clearDriverPool();
	}


	private String generateSessionId() {
		return HandBaseService.RH_SESSION_PREFIX + UUID.randomUUID();
	}
}
