/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.exactpro.th2.hand;

import com.exactprosystems.clearth.connectivity.remotehand.RhClient;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

public class HandServer
{
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Config config;
	private final RhClient rhConnection;
	private final Server server;
	
	public HandServer(Config config, RhClient rhConnection)
	{
		this.config = config;
		this.rhConnection = rhConnection;
		this.server = buildServer();
	}

	protected Server buildServer()
	{
		ServerBuilder<?> builder = ServerBuilder.forPort(config.getGrpcPort());
		for (IHandService rhService : ServiceLoader.load(IHandService.class))
		{
			rhService.init(config, rhConnection);
			builder.addService(rhService);
			logger.info("Service '{}' was loaded", rhService.getClass().getName());
		}
		
		return builder.build();
	}

	/** Start serving requests. */
	public void start() throws IOException
	{
		server.start();
		logger.info("Server started, listening on port:{}", server.getPort());
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("*** shutting down gRPC server since JVM is shutting down");
			try 
			{
				HandServer.this.stop();
			} 
			catch (InterruptedException e) 
			{
				logger.warn("Server termination await was terminated", e);
			}
			logger.info("*** server shut down");
		}));
	}

	/** Stop serving requests and shutdown resources. */
	public void stop() throws InterruptedException {
		if (server != null) {
			server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
		}
	}

	/**
	 * Await termination on the main thread since the grpc library uses daemon threads.
	 */
	public void blockUntilShutdown() throws InterruptedException {
		if (server != null) {
			server.awaitTermination();
		}
	}
}
