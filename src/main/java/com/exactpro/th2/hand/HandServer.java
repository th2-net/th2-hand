/*
 * Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.remotehand.sessions.SessionWatcher;
import com.exactpro.th2.hand.services.MessageHandler;
import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class HandServer
{
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Config config;
	private final MessageHandler messageHandler;
	private final Server server;
	private final List<IHandService> services;

	public HandServer(Config config, long startSequences) throws Exception {
		this.config = config;
		this.messageHandler = new MessageHandler(config, new AtomicLong(startSequences));
		this.services = new ArrayList<>();
		this.server = buildServer();
	}

	protected Server buildServer() throws Exception
	{
		for (IHandService rhService : ServiceLoader.load(IHandService.class))
		{
			services.add(rhService);
			rhService.init(messageHandler);
			logger.info("Service '{}' loaded", rhService.getClass().getName());
		}
		
		return config.getFactory().getGrpcRouter().startServer(services.toArray(new IHandService[0]));
	}

	/** Start serving requests. */
	public void start() throws IOException
	{
		new Thread(SessionWatcher.getWatcher()).start();
		server.start();
		logger.info("Server started, listening on port {}", server.getPort());
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("*** shutting down gRPC server because JVM is shutting down");
			try
			{
				stop();
			}
			catch (InterruptedException e)
			{
				logger.warn("Server termination await was interrupted", e);
			}
			logger.info("*** server shut down");
		}));
	}

	/** Stop serving requests and shutdown resources. */
	public void stop() throws InterruptedException
	{
		if (server != null)
		{
			server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
		}
	}

	/**
	 * Await termination on the main thread since the grpc library uses daemon
	 * threads.
	 */
	public void blockUntilShutdown() throws InterruptedException
	{
		if (server != null)
		{
			server.awaitTermination();
		}
	}
	
	public void dispose()
	{
		for (IHandService service : this.services)
		{
			service.dispose();
		}
	}
}
