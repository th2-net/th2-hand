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

import com.exactpro.remotehand.requests.ExecutionRequest;
import com.exactpro.remotehand.rhdata.RhResponseCode;
import com.exactpro.remotehand.rhdata.RhScriptResult;
import com.exactpro.th2.act.grpc.hand.*;
import com.exactpro.th2.act.grpc.hand.RhBatchGrpc.RhBatchImplBase;
import com.exactpro.th2.common.grpc.MessageID;
import com.exactpro.th2.hand.Config;
import com.exactpro.th2.hand.HandException;
import com.exactpro.th2.hand.IHandService;
import com.exactpro.th2.hand.RhConnectionManager;
import com.exactpro.th2.hand.scriptbuilders.ScriptBuilder;
import com.exactpro.th2.hand.utils.Utils;
import com.google.protobuf.Empty;
import com.google.protobuf.TextFormat;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

public class HandBaseService extends RhBatchImplBase implements IHandService
{
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String RH_SESSION_PREFIX = "/Ses";

	private Config config;
	private RhConnectionManager rhConnManager;
	private MessageHandler messageHandler;
	private int responseTimeout;
	private final ScriptBuilder scriptBuilder = new ScriptBuilder();


	@Override
	public void init(Config config, RhConnectionManager rhConnManager, AtomicLong seqNumber) throws Exception
	{
		this.config = config;
		this.responseTimeout = config.getResponseTimeout();
		this.rhConnManager = rhConnManager;
		this.messageHandler = new MessageHandler(config, seqNumber);
	}
	
	@Override
	public void register(RhTargetServer targetServer, StreamObserver<RhSessionID> responseObserver) {
		try {
			String sessionId = rhConnManager.createSessionHandler(targetServer.getTarget()).getId();
			RhSessionID result = RhSessionID.newBuilder().setId(sessionId).setSessionAlias(config.getSessionAlias()).build();
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
		rhConnManager.closeSessionHandler(request.getId());
		responseObserver.onNext(Empty.getDefaultInstance());
		responseObserver.onCompleted();
	}

	@Override
	public void executeRhActionsBatch(RhActionsList request, StreamObserver<RhBatchResponse> responseObserver)
	{
		logger.info("Action: '{}', request: '{}'", "executeRhActionsBatch", TextFormat.shortDebugString(request));
		
		RhScriptResult scriptResult;
		List<MessageID> messageIDS = new ArrayList<>();
		String sessionId = "th2_hand";
		try
		{
			sessionId = request.getSessionId().getId();
			HandSessionHandler sessionHandler = rhConnManager.getSessionHandler(sessionId);
			List<RhAction> actions = request.getRhActionList();
			String script = scriptBuilder.buildScript(actions);
			sessionHandler.handle(new ExecutionRequest(script), HandSessionExchange.getStub());
			messageIDS.addAll(getMessageIds(request));

			scriptResult = sessionHandler.waitAndGet(this.responseTimeout);
		}
		catch (Exception e)
		{
			scriptResult = new RhScriptResult();
			scriptResult.setCode(RhResponseCode.EXECUTION_ERROR.getCode());
			String errMsg = "Error occurred while executing commands";
			scriptResult.setErrorMessage(errMsg);
			logger.warn(errMsg, e);
		}
		
		messageIDS.add(messageHandler.onResponse(scriptResult, config.getSessionAlias(), sessionId));
		messageIDS.addAll(messageHandler.storeScreenshots(scriptResult.getScreenshotIds(), config.getScreenshotSessionAlias()));
		
		RhBatchResponse response = RhBatchResponse.newBuilder()
				.setScriptStatus(getScriptExecutionStatus(RhResponseCode.byCode(scriptResult.getCode())))
				.setErrorMessage(defaultIfEmpty(scriptResult.getErrorMessage(), "")).setSessionId(sessionId)
				.addAllResult(parseResultDetails(scriptResult.getTextOutput())).addAllAttachedMessageIds(messageIDS).build();
		
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
	
	private List<ResultDetails> parseResultDetails(List<String> result) {

		List<ResultDetails> details = new ArrayList<>(result.size());
		ResultDetails.Builder resultDetailsBuilder = ResultDetails.newBuilder();
		for (String s : result) {
			String id = null, detailsStr = s;
			
			if (s.contains(Utils.LINE_SEPARATOR)) {
				s = s.replaceAll(Utils.LINE_SEPARATOR, "\n");
			}
			
			int index = s.indexOf('=');
			if (index > 0) {
				id = s.substring(0, index);
				detailsStr = s.substring(index + 1);
			}

			resultDetailsBuilder.clear();
			if (id != null)
				resultDetailsBuilder.setActionId(id);
			resultDetailsBuilder.setResult(detailsStr);
			details.add(resultDetailsBuilder.build());
		}
		
		return details;
		
	}
	
	private RhBatchResponse.ScriptExecutionStatus getScriptExecutionStatus(RhResponseCode code) {
		//TODO need more enum values for RhBatchResponse.ScriptExecutionStatus !
		switch (code) {
			case SUCCESS: return RhBatchResponse.ScriptExecutionStatus.SUCCESS;
			case COMPILE_ERROR: return RhBatchResponse.ScriptExecutionStatus.COMPILE_ERROR;
			case EXECUTION_ERROR:
			case RH_ERROR:
			case TOOL_BUSY:
			case INCORRECT_REQUEST:
			default: return RhBatchResponse.ScriptExecutionStatus.EXECUTION_ERROR;
		}
	}

	protected List<MessageID> getMessageIds(RhActionsList actionsList)
	{
		return messageHandler.onRequest(actionsList, config.getSessionAlias());
	}

	@Override
	public void dispose() {
		try {
			this.rhConnManager.dispose();
		} catch (Exception e) {
			logger.error("Error while disposing RH manager", e);
		}
	}

}