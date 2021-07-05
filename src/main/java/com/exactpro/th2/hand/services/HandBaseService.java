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

import com.exactpro.remotehand.ActionResult;
import com.exactpro.remotehand.requests.ExecutionRequest;
import com.exactpro.remotehand.rhdata.RhResponseCode;
import com.exactpro.remotehand.rhdata.RhScriptResult;
import com.exactpro.th2.act.grpc.hand.*;
import com.exactpro.th2.act.grpc.hand.RhBatchGrpc.RhBatchImplBase;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages.*;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages.Click.ModifiersList;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;
import com.exactpro.th2.common.grpc.MessageID;
import com.exactpro.th2.hand.Config;
import com.exactpro.th2.hand.HandException;
import com.exactpro.th2.hand.IHandService;
import com.exactpro.th2.hand.RhConnectionManager;
import com.exactpro.th2.hand.utils.Utils;
import com.google.protobuf.Empty;
import com.google.protobuf.TextFormat;
import io.grpc.stub.StreamObserver;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

public class HandBaseService extends RhBatchImplBase implements IHandService
{
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String RH_SESSION_PREFIX = "/Ses";

	private Config config;
	private RhConnectionManager rhConnManager;
	private MessageHandler messageHandler;
	private int responseTimeout;

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
			sessionHandler.handle(new ExecutionRequest(buildScript(request, messageIDS, sessionId)), HandSessionExchange.getStub());
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
				.addAllResult(parseResultDetails(scriptResult.getActionResults())).addAllAttachedMessageIds(messageIDS).build();
		
		responseObserver.onNext(response);
		responseObserver.onCompleted();
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

	protected String buildScript(RhActionsList actionsList, List<MessageID> messageIDS, String sessionId) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		List<RhAction> actionList = actionsList.getRhActionList();
		try (CSVPrinter printer = CSVFormat.DEFAULT.print(sb))
		{
			for (RhAction action : actionList)
			{
				switch (action.getActionCase())
				{
					case OPEN:
						Open open = action.getOpen();
						addOpen(printer, open);
						break;
					case SENDKEYS:
						SendKeys sendKeys = action.getSendKeys();
						addSendKeys(printer, sendKeys);
						break;
					case FINDELEMENT:
						FindElement findElement = action.getFindElement();
						addFindElement(printer, findElement);
						break;
					case CLICK:
						Click click = action.getClick();
						addClick(printer, click);
						break;
					case SENDKEYSTOACTIVE:
						SendKeysToActive sendKeysToActive = action.getSendKeysToActive();
						addSendKeysToActive(printer, sendKeysToActive);
						break;
					case SWITCHWINDOW:
						SwitchWindow switchWindow = action.getSwitchWindow();
						addSwitchWindow(printer, switchWindow);
						break;
					case GETELEMENTVALUE:
						GetElementValue getElementValue = action.getGetElementValue();
						addGetElementValue(printer, getElementValue);
						break;
					case SCROLLDIVUNTIL:
						ScrollDivUntil scrollDivUntil = action.getScrollDivUntil();
						addScrollDivUntil(printer, scrollDivUntil);
						break;
					case WAIT:
						Wait wait = action.getWait();
						addWait(printer, wait);
						break;
					case GETELEMENTINNERHTML:
						GetElementInnerHtml getElementInnerHtml = action.getGetElementInnerHtml();
						addGetElementInnerHtml(printer, getElementInnerHtml);
						break;
					case GETSCREENSHOT:
						GetScreenshot screenshot = action.getGetScreenshot();
						addGetScreenshot(printer, screenshot);
						break;
					case GETELEMENTSCREENSHOT:
						GetElementScreenshot elementScreenshot = action.getGetElementScreenshot();
						addGetElementScreenshot(printer, elementScreenshot);
						break;
					case EXECUTEJS:
						addExecuteJS(printer, action.getExecuteJs());
						break;
					case EXECUTEJSELEMENT:
						addExecuteJSElement(printer, action.getExecuteJsElement());
						break;
					case GETELEMENTATTRIBUTE:
						addGetElementAttribute(printer, action.getGetElementAttribute());
						break;

						// win actions
					case WINOPEN:
						RhWinActionsMessages.WinOpen winOpen = action.getWinOpen();
						WinActionsBuilder.addOpen(printer, winOpen);
						break;
					case WINCLICK:
						RhWinActionsMessages.WinClick winClick = action.getWinClick();
						WinActionsBuilder.addClick(printer, winClick);
						break;
					case WINSENDTEXT:
						RhWinActionsMessages.WinSendText winSendText = action.getWinSendText();
						WinActionsBuilder.addSendText(printer, winSendText);
						break;
					case WINGETACTIVEWINDOW:
						RhWinActionsMessages.WinGetActiveWindow winGetActiveWindow = action.getWinGetActiveWindow();
						WinActionsBuilder.addActiveWindow(printer, winGetActiveWindow);
						break;
					case WINGETELEMENTATTRIBUTE:
						RhWinActionsMessages.WinGetElementAttribute winGetElementAttribute = action.getWinGetElementAttribute();
						WinActionsBuilder.addGetElementAttribute(printer, winGetElementAttribute);
						break;
					case WINWAIT:
						RhWinActionsMessages.WinWait winWait = action.getWinWait();
						WinActionsBuilder.addWait(printer, winWait);
						break;
					case WINTOGGLECHECKBOX:
						RhWinActionsMessages.WinToggleCheckBox winToggleCheckBox = action.getWinToggleCheckBox();
						WinActionsBuilder.addToggleCheckBox(printer, winToggleCheckBox);
						break;
					case WINCHECKELEMENT:
						RhWinActionsMessages.WinCheckElement winCheckElement = action.getWinCheckElement();
						WinActionsBuilder.addCheckElement(printer, winCheckElement);
						break;
					case WINGETWINDOW:
						RhWinActionsMessages.WinGetWindow winGetWindow = action.getWinGetWindow();
						WinActionsBuilder.addGetWindow(printer, winGetWindow);
						break;
					case WINSEARCHELEMENT:
						RhWinActionsMessages.WinSearchElement winSearchElement = action.getWinSearchElement();
						WinActionsBuilder.addSearchElement(printer, winSearchElement);
						break;
					case WINWAITFORATTRIBUTE:
						RhWinActionsMessages.WinWaitForAttribute winWaitForAttribute = action.getWinWaitForAttribute();
						WinActionsBuilder.addWaitForAttribute(printer, winWaitForAttribute);
						break;
					case WINSCROLLUSINGTEXT:
						RhWinActionsMessages.WinScrollUsingText scrollUsingText = action.getWinScrollUsingText();
						WinActionsBuilder.addScrollUsingText(printer, scrollUsingText);
						break;
					case WINGETDATAFROMCLIPBOARD:
						RhWinActionsMessages.WinGetDataFromClipboard dataFromClipboard = action.getWinGetDataFromClipboard();
						WinActionsBuilder.addGetDataFromClipboard(printer, dataFromClipboard);
						break;
					case WINTABLESEARCH:
						RhWinActionsMessages.WinTableSearch winTableSearch = action.getWinTableSearch();
						WinActionsBuilder.addWinTableSearch(printer, winTableSearch);
						break;
					case WINWAITFORELEMENT:
						RhWinActionsMessages.WinWaitForElement winWaitForElement = action.getWinWaitForElement();
						WinActionsBuilder.addWaitForElement(printer, winWaitForElement);
						break;
					case WINMAXIMIZEMAINWINDOW:
						WinActionsBuilder.addMaximizeMainWindow(printer, action.getWinMaximizeMainWindow());
						break;
					case WINGETSCREENSHOT:
						WinActionsBuilder.addGetScreenshot(printer, action.getWinGetScreenshot());
						break;
					case WINRESTARTDRIVER:
						WinActionsBuilder.addRestartDriver(printer, action.getWinRestartDriver());
						break;
					case WINGETELEMENTCOLOR:
						WinActionsBuilder.addGetElementColor(printer, action.getWinGetElementColor());
						break;
					case WINDRAGANDDROP:
						WinActionsBuilder.addDragAndDrop(printer, action.getWinDragAndDrop());
						break;
					case WINSCROLLTOELEMENT:
						WinActionsBuilder.addScrollToElement(printer, action.getWinScrollToElement());
						break;
					case WINCOLORSCOLLECTOR:
						WinActionsBuilder.addColorsCollector(printer, action.getWinColorsCollector());
						break;
					default:
						logger.warn("Unsupported action: " + action.getActionCase());
						break;
				}
			}
		}
		
		String scriptText = sb.toString();
		messageIDS.addAll(getMessageIds(actionsList));
		
		return scriptText;
	}

	protected List<MessageID> getMessageIds(RhActionsList actionsList)
	{
		return messageHandler.onRequest(actionsList, config.getSessionAlias());
	}

	private void addWait(CSVPrinter printer, Wait wait) throws IOException
	{
		printer.print("#action");
		printer.print("#seconds");
		printer.println();
		printer.print("Wait");
		printer.print(wait.getSeconds());
		printer.println();
	}

	private void addOpen(CSVPrinter printer, Open open) throws IOException
	{
		// #action,#url
		printer.print("#action");
		printer.print("#url");
		printer.println();
		printer.print("Open");
		printer.print(open.getUrl());
		printer.println();
	}

	private void addSendKeys(CSVPrinter printer, SendKeys sendKeys) throws IOException
	{
		// #action,#wait,#locator,#matcher,#text,#wait2,#locator2,#matcher2,#text2,#canBeDisabled,#clear,#checkInput,#needClick
		printer.print("#action");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.print("#text");
		printer.print("#wait2");
		printer.print("#locator2");
		printer.print("#matcher2");
		printer.print("#text2");
		printer.print("#canBeDisabled");
		printer.print("#clear");
		printer.print("#checkInput");
		printer.print("#needClick");
		printer.println();

		printer.print("SendKeys");
		printer.print(String.valueOf(sendKeys.getWait()));
		printer.print(readLocator(sendKeys.getLocator()));
		printer.print(sendKeys.getMatcher());
		printer.print(sendKeys.getText());
		printer.print(String.valueOf(sendKeys.getWait2()));
		printer.print(readLocator(sendKeys.getLocator2()));
		printer.print(sendKeys.getMatcher2());
		printer.print(sendKeys.getText2());
		printer.print(String.valueOf(sendKeys.getCanBeDisabled()));
		printer.print(String.valueOf(sendKeys.getClear()));
		printer.print(String.valueOf(sendKeys.getCheckInput()));
		printer.print(String.valueOf(sendKeys.getNeedClick()));
		printer.println();
	}

	private String readLocator(Locator locator)
	{
		if (locator == null)
			return null;

		switch (locator)
		{
		case CSS_SELECTOR:
			return "cssSelector";
		case TAG_NAME:
			return "tagName";
		case ID:
			return "id";
		case XPATH:
			return "xpath";
		default:
			logger.warn("Unsupported locator: " + locator);
		}
		return null;
	}

	private void addFindElement(CSVPrinter printer, FindElement findElement) throws IOException
	{
		// #action,#wait,#locator,#matcher,#id
		printer.print("#action");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.print("#id");
		printer.println();
		
		printer.print("FindElement");
		printer.print(String.valueOf(findElement.getWait()));
		printer.print(readLocator(findElement.getLocator()));
		printer.print(findElement.getMatcher());
		printer.print(findElement.getId());
		printer.println();
	}

	private void addClick(CSVPrinter printer, Click click) throws IOException
	{
		// #action,#wait,#locator,#matcher,#button,#xOffset,#yOffset,#modifiers
		printer.print("#action");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.print("#button");
		printer.print("#xOffset");
		printer.print("#yOffset");
		printer.print("#modifiers");
		printer.println();
		
		printer.print("Click");
		printer.print(String.valueOf(click.getWait()));
		printer.print(readLocator(click.getLocator()));
		printer.print(click.getMatcher());
		printer.print(click.getButton().name().toLowerCase());
		printer.print(String.valueOf(click.getXOffset()));
		printer.print(String.valueOf(click.getYOffset()));
		printer.print(readModifiers(click.getModifiers()));
		printer.println();
	}

	private String readModifiers(ModifiersList modifiersList)
	{
		return modifiersList.getModifierList().stream().map(modifier -> modifier.name().toLowerCase())
				.collect(Collectors.joining(","));
	}

	private void addSendKeysToActive(CSVPrinter printer, SendKeysToActive sendKeysToActive) throws IOException
	{
		// #action,#text,#text2
		printer.print("#action");
		printer.print("#text");
		printer.print("#text2");
		printer.println();
		
		printer.print("SendKeysToActive");
		printer.print(sendKeysToActive.getText());
		printer.print(sendKeysToActive.getText2());
		printer.println();
	}

	private void addSwitchWindow(CSVPrinter printer, SwitchWindow switchWindow) throws IOException
	{
		// #action,#window
		printer.print("#action");
		printer.print("#window");
		printer.println();
		
		printer.print("SwitchWindow");
		printer.print(String.valueOf(switchWindow.getWindow()));
		printer.println();
	}

	private void addScrollDivUntil(CSVPrinter printer, ScrollDivUntil scrollDivUntil) throws IOException
	{
		// #action,#wait,#locator,#matcher,#wait2,#locator2,#matcher2,#searchdir,#searchoffset,#doscrollto,#yoffset
		printer.print("#action");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.print("#wait2");
		printer.print("#locator2");
		printer.print("#matcher2");
		printer.print("#searchdir");
		printer.print("#searchoffset");
		printer.print("#doscrollto");
		printer.print("#yoffset");
		printer.println();

		printer.print("ScrollDivUntil");
		printer.print(String.valueOf(scrollDivUntil.getWait()));
		printer.print(readLocator(scrollDivUntil.getLocator()));
		printer.print(scrollDivUntil.getMatcher());
		printer.print(String.valueOf(scrollDivUntil.getWait2()));
		printer.print(readLocator(scrollDivUntil.getLocator2()));
		printer.print(scrollDivUntil.getMatcher2());
		printer.print(scrollDivUntil.getSearchDir().name().toLowerCase());
		printer.print(String.valueOf(scrollDivUntil.getSearchOffset()));
		printer.print(String.valueOf(scrollDivUntil.getDoScrollTo()));
		printer.print(String.valueOf(scrollDivUntil.getYOffset()));
		printer.println();
	}

	private void addGetElementValue(CSVPrinter printer, GetElementValue getElementValue) throws IOException
	{
		// #action,#wait,#locator,#matcher
		printer.print("#action");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.println();

		printer.print("GetElementValue");
		printer.print(String.valueOf(getElementValue.getWait()));
		printer.print(readLocator(getElementValue.getLocator()));
		printer.print(getElementValue.getMatcher());
		printer.println();
	}

	private void addGetElementInnerHtml(CSVPrinter printer, GetElementInnerHtml getElementInnerHtml) throws IOException
	{
		// #action,#wait,#locator,#matcher
		printer.print("#action");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.println();

		printer.print("GetElementInnerHtml");
		printer.print(String.valueOf(getElementInnerHtml.getWait()));
		printer.print(readLocator(getElementInnerHtml.getLocator()));
		printer.print(getElementInnerHtml.getMatcher());
		printer.println();
	}

	private void addGetScreenshot(CSVPrinter printer, GetScreenshot getScreenshot) throws IOException
	{
		// #action,#wait,#locator,#matcher
		printer.print("#action");
		printer.print("#name");
		printer.println();

		printer.print("GetScreenshot");
		printer.print(getScreenshot.getName());
		printer.println();
	}

	private void addExecuteJS(CSVPrinter printer, ExecuteJS execJs) throws IOException
	{
		printer.print("#action");
		printer.print("#commands");
		printer.println();

		printer.print("ExecuteJS");
		printer.print(execJs.getCommands());
		printer.println();
	}

	private void addExecuteJSElement(CSVPrinter printer, ExecuteJSElement execJs) throws IOException
	{
		printer.print("#action");
		printer.print("#commands");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.println();

		printer.print("ExecuteJsOnElement");
		printer.print(execJs.getCommands());
		printer.print(String.valueOf(execJs.getWait()));
		printer.print(readLocator(execJs.getLocator()));
		printer.print(execJs.getMatcher());
		printer.println();
	}

	private void addGetElementAttribute(CSVPrinter printer, GetElementAttribute getElAttr) throws IOException
	{
		printer.print("#action");
		printer.print("#attribute");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.println();

		printer.print("GetElementAttribute");
		printer.print(getElAttr.getAttribute());
		printer.print(String.valueOf(getElAttr.getWait()));
		printer.print(readLocator(getElAttr.getLocator()));
		printer.print(getElAttr.getMatcher());
		printer.println();
	}
	
	private void addGetElementScreenshot(CSVPrinter printer, GetElementScreenshot getScreenshot) throws IOException
	{
		// #action,#wait,#locator,#matcher
		printer.print("#action");
		printer.print("#name");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.println();

		printer.print("GetElementScreenshot");
		printer.print(getScreenshot.getName());
		printer.print(String.valueOf(getScreenshot.getWait()));
		printer.print(readLocator(getScreenshot.getLocator()));
		printer.print(getScreenshot.getMatcher());
		printer.println();
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