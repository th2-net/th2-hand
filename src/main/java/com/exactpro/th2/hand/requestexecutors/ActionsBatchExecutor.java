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

package com.exactpro.th2.hand.requestexecutors;

import com.exactpro.remotehand.ActionResult;
import com.exactpro.remotehand.requests.ExecutionRequest;
import com.exactpro.remotehand.rhdata.RhResponseCode;
import com.exactpro.remotehand.rhdata.RhScriptResult;
import com.exactpro.th2.act.grpc.hand.ResultDetails;
import com.exactpro.th2.act.grpc.hand.RhActionList;
import com.exactpro.th2.act.grpc.hand.RhActionsBatch;
import com.exactpro.th2.act.grpc.hand.RhBatchResponse;
import com.exactpro.th2.common.grpc.Event;
import com.exactpro.th2.common.grpc.MessageID;
import com.exactpro.th2.hand.messages.responseexecutor.ActionsBatchExecutorResponse;
import com.exactpro.th2.hand.services.HandSessionExchange;
import com.exactpro.th2.hand.services.HandSessionHandler;
import com.exactpro.th2.hand.services.MessageHandler;
import com.exactpro.th2.hand.services.estore.EventStoreHandler;
import com.exactpro.th2.hand.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

public class ActionsBatchExecutor implements RequestExecutor<RhActionsBatch, ActionsBatchExecutorResponse> {
	
	private final Logger logger = LoggerFactory.getLogger(ActionsBatchExecutor.class);

	private String sessionId;
	private final MessageHandler messageHandler;
	private final List<MessageID> messageIDs = new ArrayList<>();
	private final String screenshotSessionAlias;
	private final String sessionAlias;


	public ActionsBatchExecutor(MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
		this.sessionAlias = messageHandler.getConfig().getSessionAlias();
		this.screenshotSessionAlias = messageHandler.getConfig().getScreenshotSessionAlias();
	}


	@Override
	public ActionsBatchExecutorResponse execute(RhActionsBatch request) {
		Instant executionStartTime = Instant.now();
		RhScriptResult scriptResult;
		String sessionId = "th2_hand";
		try {
			HandSessionHandler sessionHandler = getSessionHandler(request);
			messageIDs.addAll(messageHandler.getMessageStoreHandler().onRequest(request, sessionAlias));
			RhActionList actions = request.getRhAction();
			String script = messageHandler.getScriptBuilder().buildScript(actions);
			sessionHandler.handle(new ExecutionRequest(script), HandSessionExchange.getStub());
			scriptResult = sessionHandler.waitAndGet(messageHandler.getConfig().getResponseTimeout());
		} catch (Exception e) {
			scriptResult = new RhScriptResult();
			scriptResult.setCode(RhResponseCode.EXECUTION_ERROR.getCode());
			String errMsg = "Error occurred while executing commands";
			scriptResult.setErrorMessage(errMsg);
			logger.warn(errMsg, e);
		}

		messageIDs.add(messageHandler.getMessageStoreHandler().onResponse(scriptResult, sessionAlias, sessionId));
		messageIDs.addAll(messageHandler.getMessageStoreHandler().storeScreenshots(scriptResult.getScreenshotIds(), screenshotSessionAlias));

		ActionsBatchExecutorResponse executorResponse = createResponse(scriptResult);
		buildAndSendEvent(executionStartTime, request, executorResponse);
		return executorResponse;
	}


	private HandSessionHandler getSessionHandler(RhActionsBatch request) {
		sessionId = request.getSessionId().getId();
		return messageHandler.getRhConnectionManager().getSessionHandler(sessionId);
	}

	private ActionsBatchExecutorResponse createResponse(RhScriptResult scriptResult) {
		RhBatchResponse handResponse = createHandResponse(scriptResult);
		return new ActionsBatchExecutorResponse(handResponse, scriptResult, messageIDs);
	}

	private RhBatchResponse createHandResponse(RhScriptResult result) {
		return RhBatchResponse.newBuilder()
				.setScriptStatus(convertToScriptExecutionStatus(RhResponseCode.byCode(result.getCode())))
				.setErrorMessage(defaultIfEmpty(result.getErrorMessage(), ""))
				.setSessionId(sessionId)
				.addAllResult(parseResultDetails(result.getActionResults()))
				.build();
	}

	private RhBatchResponse.ScriptExecutionStatus convertToScriptExecutionStatus(RhResponseCode code) {
		switch (code) {
			case SUCCESS: return RhBatchResponse.ScriptExecutionStatus.SUCCESS;
			case COMPILE_ERROR: return RhBatchResponse.ScriptExecutionStatus.COMPILE_ERROR;
			case EXECUTION_ERROR: return RhBatchResponse.ScriptExecutionStatus.EXECUTION_ERROR;
			case RH_ERROR:
			case TOOL_BUSY:
			case INCORRECT_REQUEST:
			default: return RhBatchResponse.ScriptExecutionStatus.HAND_INTERNAL_ERROR;
		}
	}

	private List<ResultDetails> parseResultDetails(List<ActionResult> actionData) {
		List<ResultDetails> details = new ArrayList<>(actionData.size());
		ResultDetails.Builder resultDetailsBuilder = ResultDetails.newBuilder();
		for (ActionResult data : actionData) {
			if (!data.hasData())
				continue;
			String id = data.getId(), detailsStr = data.getData();

			if (detailsStr.contains(Utils.LINE_SEPARATOR))
				detailsStr = detailsStr.replaceAll(Utils.LINE_SEPARATOR, "\n");

			resultDetailsBuilder.clear();
			if (id != null)
				resultDetailsBuilder.setActionId(id);
			resultDetailsBuilder.setResult(detailsStr);
			details.add(resultDetailsBuilder.build());
		}

		return details;
	}

	private void buildAndSendEvent(Instant startTime, RhActionsBatch request, ActionsBatchExecutorResponse executorResponse) {
		EventStoreHandler eventStoreHandler = messageHandler.getEventStoreHandler();
		Event event = eventStoreHandler.getEventBuilder().buildEvent(startTime, request, executorResponse);
		eventStoreHandler.getEventStoreSender().storeEvent(event);
	}
}
