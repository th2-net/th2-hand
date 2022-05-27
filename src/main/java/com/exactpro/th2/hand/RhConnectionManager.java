/*
 *  Copyright 2020-2022 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.remotehand.IRemoteHandManager;
import com.exactpro.remotehand.RemoteManagerType;
import com.exactpro.remotehand.RhConfigurationException;
import com.exactpro.remotehand.grid.GridRemoteHandManager;
import com.exactpro.th2.hand.services.HandBaseService;
import com.exactpro.th2.hand.services.HandSessionHandler;
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
		gridRemoteHandManager.createConfigurations(null, config.getRhOptions());
	}


	public HandSessionHandler getSessionHandler(String sessionId) throws IllegalArgumentException {
		HandSessionHandler sessionHandler = sessions.get(sessionId);
		if (sessionHandler == null)
			throw new IllegalArgumentException("Requested client for session '"+sessionId+"' is not registered");
		return sessionHandler;
	}

	public HandSessionHandler createSessionHandler(String targetServer) throws RhConfigurationException {
		Config.DriverMapping driverSettings = config.getDriversMapping().get(targetServer);
		if (driverSettings == null)
			throw new RhConfigurationException("Driver settings for '" + targetServer + "' not found.");

		RemoteManagerType remoteManagerType = RemoteManagerType.getByLabel(driverSettings.type);
		if (remoteManagerType == null)
			throw new RhConfigurationException("Unrecognized driver manager type '"+driverSettings.type+"'");

		String sessionId = generateSessionId();
		IRemoteHandManager remoteHandManager = gridRemoteHandManager.getRemoteHandManager(remoteManagerType);
		HandSessionHandler handSessionHandler = new HandSessionHandler(sessionId, remoteHandManager, this);
		gridRemoteHandManager.saveSession(sessionId, driverSettings.url);
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
		logger.info("Closed session <{}>", sessionId);
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
