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

package com.exactpro.th2.hand.services;

import com.exactpro.th2.act.grpc.hand.RhActionsList;
import com.exactpro.th2.act.grpc.hand.RhBatchGrpc.RhBatchImplBase;
import com.exactpro.th2.act.grpc.hand.RhBatchResponse;
import com.exactpro.th2.act.grpc.hand.RhSessionID;
import com.exactpro.th2.act.grpc.hand.RhTargetServer;
import com.exactpro.th2.hand.Config;
import com.exactpro.th2.hand.IHandService;
import com.exactpro.th2.hand.RhConnectionManager;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicLong;

public class HandBaseService extends RhBatchImplBase implements IHandService {
	public static final String RH_SESSION_PREFIX = "/Ses";

	private MessageHandler messageHandler;

	@Override
	public void init(Config config, RhConnectionManager rhConnManager, AtomicLong seqNumber) throws Exception {
		this.messageHandler = new MessageHandler(config, rhConnManager, seqNumber);
	}

	@Override
	public void register(RhTargetServer request, StreamObserver<RhSessionID> responseObserver) {
		messageHandler.handleRegisterSession(request, responseObserver);
	}

	@Override
	public void unregister(RhSessionID request, StreamObserver<Empty> responseObserver) {
		messageHandler.handleUnregisterSession(request, responseObserver);
	}

	@Override
	public void executeRhActionsBatch(RhActionsList request, StreamObserver<RhBatchResponse> responseObserver) {
		messageHandler.handleExecution(request, responseObserver);
	}

	@Override
	public void dispose() {
		this.messageHandler.dispose();
	}
}