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

package com.exactpro.th2.hand.services;

import com.exactpro.remotehand.RhConfigurationException;
import com.exactpro.th2.act.grpc.hand.RhActionsBatch;
import com.exactpro.th2.act.grpc.hand.RhBatchGrpc.RhBatchImplBase;
import com.exactpro.th2.act.grpc.hand.RhBatchResponse;
import com.exactpro.th2.act.grpc.hand.RhBatchService;
import com.exactpro.th2.act.grpc.hand.RhSessionID;
import com.exactpro.th2.act.grpc.hand.RhTargetServer;
import com.exactpro.th2.hand.HandException;
import com.exactpro.th2.hand.IHandService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static com.exactpro.th2.common.message.MessageUtils.toJson;

public class HandBaseService extends RhBatchImplBase implements IHandService, RhBatchService {
	private final static Logger LOGGER = LoggerFactory.getLogger(HandBaseService.class);

	public static final String RH_SESSION_PREFIX = "/Ses";

	private MessageHandler messageHandler;

	@Override
	public void init(MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	}

	@Override
	public void register(RhTargetServer targetServer, StreamObserver<RhSessionID> responseObserver) {
		try {
			responseObserver.onNext(register(targetServer));
			responseObserver.onCompleted();
		} catch (Exception e) {
			LOGGER.error("Error while creating session", e);
			Exception responseException = new HandException("Error while creating session", e);
			responseObserver.onError(responseException);
		}
	}

	@Override
	public void unregister(RhSessionID request, StreamObserver<Empty> responseObserver) {
		try {
			responseObserver.onNext(unregister(request));
			responseObserver.onCompleted();
		} catch (Exception e) {
			LOGGER.error("Action failure, request: '{}'", toJson(request), e);
			responseObserver.onError(e);
		}

	}

	@Override
	public void executeRhActionsBatch(RhActionsBatch request, StreamObserver<RhBatchResponse> responseObserver) {
		LOGGER.trace("Action: 'executeRhActionsBatch', request: '{}'", toJson(request));
        try {
            responseObserver.onNext(executeRhActionsBatch(request));
			responseObserver.onCompleted();
        } catch (Exception e) {
			LOGGER.error("Action failure, request: '{}'", toJson(request), e);
            responseObserver.onError(e);
        }
	}

	@Override
	public void dispose() {
		try {
			this.messageHandler.close();
		} catch (Exception e) {
			LOGGER.error("Error while disposing message handler", e);
		}
	}

	@Override
	public RhSessionID register(RhTargetServer targetServer) {
        try {
			String sessionId = messageHandler.getRhConnectionManager().createSessionHandler(targetServer.getTarget()).getId();
			return RhSessionID.newBuilder().setId(sessionId).setSessionAlias(messageHandler.getConfig().getSessionAlias()).build();
		} catch (RhConfigurationException e) {
			throw new HandRuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public RhSessionID register(RhTargetServer input, Map<String, String> properties) {
		return register(input);
	}

	@Override
	public Empty unregister(RhSessionID request) {
		messageHandler.getRhConnectionManager().closeSessionHandler(request.getId());
		return Empty.getDefaultInstance();
	}

	@Override
	public Empty unregister(RhSessionID input, Map<String, String> properties) {
		return unregister(input);
	}

	@Override
	public RhBatchResponse executeRhActionsBatch(RhActionsBatch request) {
        try {
            return messageHandler.handleActionsBatchRequest(request);
        } catch (IOException e) {
			throw new HandRuntimeException(e.getMessage(), e);
        }
    }

	@Override
	public RhBatchResponse executeRhActionsBatch(RhActionsBatch input, Map<String, String> properties) {
		return executeRhActionsBatch(input);
	}
}