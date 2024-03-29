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

package com.exactpro.th2.hand.services;

import com.exactpro.remotehand.IRemoteHandManager;
import com.exactpro.remotehand.ScriptExecuteException;
import com.exactpro.remotehand.rhdata.RhResponseCode;
import com.exactpro.remotehand.rhdata.RhScriptResult;
import com.exactpro.remotehand.sessions.SessionHandler;
import com.exactpro.th2.hand.RhConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class HandSessionHandler extends SessionHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(HandSessionHandler.class);

	private final RhConnectionManager connectionManager;
	private volatile boolean closed = false;

	public HandSessionHandler(String id, IRemoteHandManager manager, RhConnectionManager connectionManager) {
		super(id, manager);
		this.connectionManager = connectionManager;
	}


	public RhScriptResult waitAndGet(int waitInSeconds) throws ScriptExecuteException {
		long timeOut = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(waitInSeconds);
		while (scriptProcessor.isBusy()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.debug("Script execution for session '" + getId() + "' has been interrupted", e);
				RhScriptResult scriptResult = new RhScriptResult();
				scriptResult.setCode(RhResponseCode.EXECUTION_ERROR.getCode());
				scriptResult.setErrorMessage("Script execution has been interrupted");
				return scriptResult;
			}

			if (timeOut <= System.currentTimeMillis())
				throw new ScriptExecuteException("Timeout after " + waitInSeconds + " seconds waiting for result.");
		}

		return scriptProcessor.getResult();
	}


	@Override
	protected void closeConnection() throws IllegalArgumentException {
	}

	@Override
	public void close() {
		if (!closed) {
			this.closed = true;
			super.close();
			this.connectionManager.closeSessionHandler(getId());
		}
	}
}
