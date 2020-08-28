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

package com.exactpro.th2.hand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.th2.hand.remotehand.RhClient;
import com.exactpro.th2.hand.remotehand.RhException;
import com.exactpro.th2.hand.remotehand.RhUtils;

import java.io.IOException;

public class Application
{
	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args)
	{
		new Application().run(args);
	}

	public void run(String[] args)
	{
		Config config = getConfig();
		RhClient rhConnection = initRhConnection(config);
		try
		{
			final HandServer handServer = new HandServer(config, rhConnection);
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					LOGGER.info("Disposing hand server");
					handServer.dispose();
				} catch (Exception e) {
					LOGGER.error("Cannot dispose hand server", e);
				} 
			}));
			handServer.start();
			handServer.blockUntilShutdown();
		}
        catch (Exception e)
		{
			LOGGER.error("Unable to start 'HandServer'", e);
			closeApp();
        }
	}

	protected Config getConfig()
	{
		return new Config();
	}

	protected RhClient initRhConnection(Config config)
	{
		LOGGER.debug("Creating Remote hand connection...");
		try
		{
			return RhUtils.createRhConnection(config.getRhUrl());
		}
		catch (IOException | RhException e)
		{
			LOGGER.error("Unable to create RH connection", e);
			closeApp();
		}

		return null;
	}

	private static void closeApp()
	{
		LOGGER.info("Application stopped");
		System.exit(1);
	}
}
