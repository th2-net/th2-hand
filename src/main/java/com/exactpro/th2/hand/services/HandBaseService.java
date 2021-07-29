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

import com.exactpro.th2.act.grpc.hand.RhActionsBatch;
import com.exactpro.th2.act.grpc.hand.RhBatchGrpc.RhBatchImplBase;
import com.exactpro.th2.act.grpc.hand.RhBatchResponse;
import com.exactpro.th2.act.grpc.hand.RhSessionID;
import com.exactpro.th2.act.grpc.hand.RhTargetServer;
import com.exactpro.th2.hand.HandException;
import com.exactpro.th2.hand.IHandService;
import com.google.protobuf.Empty;
import com.google.protobuf.TextFormat;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandBaseService extends RhBatchImplBase implements IHandService {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String RH_SESSION_PREFIX = "/Ses";

	private MessageHandler messageHandler;

	@Override
	public void init(MessageHandler messageHandler) throws Exception {
		this.messageHandler = messageHandler;
	}

	@Override
	public void register(RhTargetServer targetServer, StreamObserver<RhSessionID> responseObserver) {
		try {
			String sessionId = messageHandler.getRhConnectionManager().createSessionHandler(targetServer.getTarget()).getId();
			RhSessionID result = RhSessionID.newBuilder().setId(sessionId).setSessionAlias(messageHandler.getConfig().getSessionAlias()).build();
			responseObserver.onNext(result);
		} catch (Exception e) {
			logger.error("Error while creating session", e);
			Exception responseException = new HandException("Error while creating session", e);
			responseObserver.onError(responseException);
		}
		responseObserver.onCompleted();
	}

	@Override
	public void unregister(RhSessionID request, StreamObserver<Empty> responseObserver) {
		messageHandler.getRhConnectionManager().closeSessionHandler(request.getId());
		responseObserver.onNext(Empty.getDefaultInstance());
		responseObserver.onCompleted();
	}

	@Override
	public void executeRhActionsBatch(RhActionsBatch request, StreamObserver<RhBatchResponse> responseObserver) {
		logger.trace("Action: '{}', request: '{}'", "executeRhActionsBatch", TextFormat.shortDebugString(request));
		responseObserver.onNext(messageHandler.handleActionsBatchRequest(request));
		responseObserver.onCompleted();
	}

	@Override
	public void dispose() {
		try {
			this.messageHandler.close();
		} catch (Exception e) {
			logger.error("Error while disposing message handler", e);
		}
	}
}