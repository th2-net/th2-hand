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

import com.exactpro.th2.act.grpc.hand.RhActionsList;
import com.exactpro.th2.act.grpc.hand.RhBatchResponse;
import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.exactpro.th2.hand.Config;
import com.exactpro.th2.hand.RhConnectionManager;
import com.exactpro.th2.hand.builders.events.DefaultEventBuilder;
import com.exactpro.th2.hand.builders.mstore.DefaultMessageStoreBuilder;
import com.exactpro.th2.hand.builders.script.ScriptBuilder;
import com.exactpro.th2.hand.requestexecutors.ActionsBatchExecutor;
import com.exactpro.th2.hand.services.estore.EventStoreHandler;
import com.exactpro.th2.hand.services.estore.EventStoreSender;
import com.exactpro.th2.hand.services.mstore.MessageStoreHandler;
import com.exactpro.th2.hand.services.mstore.MessageStoreSender;

import java.util.concurrent.atomic.AtomicLong;

public class MessageHandler implements AutoCloseable {
	private final Config config;
	private final MessageStoreHandler messageStoreHandler;
	private final EventStoreHandler eventStoreHandler;
	private final RhConnectionManager rhConnectionManager;
	private final ScriptBuilder scriptBuilder = new ScriptBuilder();


	public MessageHandler(Config config, AtomicLong seqNum) {
		this.config = config;
		rhConnectionManager = new RhConnectionManager(config);
		CommonFactory factory = config.getFactory();
		this.messageStoreHandler = new MessageStoreHandler(new MessageStoreSender(factory), new DefaultMessageStoreBuilder(seqNum));
		this.eventStoreHandler = new EventStoreHandler(new EventStoreSender(factory), new DefaultEventBuilder());
	}

	public MessageStoreHandler getMessageStoreHandler() {
		return messageStoreHandler;
	}

	public EventStoreHandler getEventStoreHandler() {
		return eventStoreHandler;
	}

	public ScriptBuilder getScriptBuilder() {
		return scriptBuilder;
	}

	public Config getConfig() {
		return config;
	}

	public RhConnectionManager getRhConnectionManager() {
		return rhConnectionManager;
	}

	public RhBatchResponse handleActionsBatchRequest(RhActionsList request) {
		ActionsBatchExecutor actionsBatchExecutor = new ActionsBatchExecutor(this);
		return actionsBatchExecutor.execute(request).getHandResponse();
	}

	@Override
	public void close() throws Exception {
		rhConnectionManager.dispose();
		messageStoreHandler.close();
		eventStoreHandler.close();
	}
}
