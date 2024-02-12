/*
 * Copyright 2020-2024 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.common.schema.factory.CommonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application
{
	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		new Application().run(args);
	}

	public void run(String[] args) {
		try (CommonFactory factory = CommonFactory.createFromArguments(args)) {
			Config config = getConfig(factory);
			try (HandServer handServer = new HandServer(config)) {
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					LOGGER.info("*** Closing hand server because JVM is shutting down");
					try {
						handServer.close();
					} catch (InterruptedException e) {
						LOGGER.warn("Server termination await was interrupted", e);
					}
					LOGGER.info("*** hand server closed");
				}));
				handServer.start();
				handServer.blockUntilShutdown();
			}
		} catch (Exception e) {
			LOGGER.error("Could not to start Hand server", e);
			closeApp();
		}
	}

	protected Config getConfig(CommonFactory factory) throws ConfigurationException {
		return new Config(factory);
	}

	private static void closeApp() {
		LOGGER.info("Application stopped");
		System.exit(1);
	}
}
