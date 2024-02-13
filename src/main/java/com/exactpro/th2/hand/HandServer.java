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
import java.util.concurrent.atomic.AtomicReference;

public class HandServer implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.getLogger(HandServer.class);

	private final Config config;
	private final MessageHandler messageHandler;
	private final Server server;
	private final List<IHandService> services;
	private final AtomicReference<Thread> watcher = new AtomicReference<>();

	public HandServer(Config config) throws Exception {
		this.config = config;
		this.messageHandler = new MessageHandler(config);
		this.services = new ArrayList<>();
		this.server = buildServer();
	}

	protected Server buildServer() throws Exception {
		for (IHandService rhService : ServiceLoader.load(IHandService.class)) {
			services.add(rhService);
			rhService.init(messageHandler);
			LOGGER.info("Service '{}' loaded", rhService.getClass().getName());
		}
		
		return config.getFactory().getGrpcRouter().startServer(services.toArray(new IHandService[0]));
	}

	/**
	 * Start serving requests.
	 * @throws IOException - if unable to bind
	 */
	public void start() throws IOException {
		Thread thread = new Thread(SessionWatcher.getWatcher());
		if (watcher.compareAndSet(null, thread)) {
			thread.start();
			server.start();
			LOGGER.info("Server started, listening on port {}", server.getPort());
		} else {
			throw new IllegalStateException(getClass().getSimpleName() + " is already started");
		}
	}

	/**
	 * Await termination on the main thread since the grpc library uses daemon
	 * threads.
	 * @throws InterruptedException - if current thread is interrupted
	 */
	public void blockUntilShutdown() throws InterruptedException {
		if (server != null) {
			server.awaitTermination();
		}
	}

	@Override
	public void close() throws InterruptedException {
		if (!server.isShutdown()) {
			server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
		}

		for (IHandService service : this.services) {
			service.dispose();
		}

		Thread thread = watcher.get();
		if (thread != null && !thread.isInterrupted()) {
			thread.interrupt();
			thread.join(30_000);
		}
	}
}