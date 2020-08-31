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

public class RhConnectionManager {

	private static final Logger logger = LoggerFactory.getLogger(RhConnectionManager.class);
	
	private RhClient currentClient;
	private Config config;

	public RhConnectionManager(Config config) {
		this.config = config;
	}

	public RhClient getClient() throws IOException, RhException
	{
		if (currentClient == null)
		{
			logger.info("Rh client is not created.");
			this.currentClient = initRhConnection(this.config);
		}
		return currentClient;
	}

	protected RhClient initRhConnection(Config config) throws IOException, RhException
	{
		logger.info("Creating RemoteHand connection...");
		return RhUtils.createRhConnection(config.getRhUrl());
	}
}
