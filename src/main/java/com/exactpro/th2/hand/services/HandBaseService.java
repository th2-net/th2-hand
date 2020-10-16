/*
 * Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
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

import static com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages.*;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.exactpro.th2.act.grpc.hand.*;
import com.exactpro.th2.act.grpc.hand.RhBatchGrpc.RhBatchImplBase;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages.Click;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages.Click.ModifiersList;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages.FindElement;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages.Locator;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages.Open;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages.SendKeys;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages.SendKeysToActive;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages.SwitchWindow;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;
import com.exactpro.th2.hand.RhConnectionManager;
import com.exactpro.th2.hand.remotehand.RhClient;
import com.exactpro.th2.hand.remotehand.RhResponseCode;
import com.exactpro.th2.hand.remotehand.RhScriptResult;

import com.exactpro.th2.hand.remotehand.RhUtils;
import com.exactpro.th2.infra.grpc.MessageID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.th2.hand.Config;
import com.exactpro.th2.hand.IHandService;
import com.google.protobuf.Empty;
import com.google.protobuf.TextFormat;

import io.grpc.stub.StreamObserver;

public class HandBaseService extends RhBatchImplBase implements IHandService
{
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String RH_SESSION_PREFIX = "/Ses";

	private RhConnectionManager rhConnManager;
	private MessageHandler messageHandler;
	

	@Override
	public void init(Config config, RhConnectionManager rhConnManager) throws Exception
	{
		this.rhConnManager = rhConnManager;
		this.messageHandler = new MessageHandler(config.getRabbitMqConfiguration());
	}
	
	@Override
	public void register(RhTargetServer targetServer, StreamObserver<RhSessionID> responseObserver)
	{
		String sessionId;
		try
		{
			RhClient client = rhConnManager.createClient(targetServer.getTarget());
			sessionId = client.getSessionId();
		}
		catch (Exception e)
		{
			logger.error("Error while creating RH client", e);
			sessionId = null;
		}
		RhSessionID result = RhSessionID.newBuilder().setId(sessionId).build();
		responseObserver.onNext(result);
		responseObserver.onCompleted();
	}
	
	@Override
	public void unregister(RhSessionID request, StreamObserver<Empty> responseObserver)
	{
		try
		{
			rhConnManager.closeClient(request.getId());
		}
		catch (IOException e)
		{
			logger.error("Error while closing RH client for session '"+request.getId()+"'", e);
		}
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
			RhClient rhConnection = rhConnManager.getClient(sessionId);
			rhConnection.send(buildScript(request, messageIDS, sessionId));
			scriptResult = rhConnection.waitAndGet(120);
		}
		catch (Exception e)
		{
			scriptResult = new RhScriptResult();
			scriptResult.setCode(RhResponseCode.EXECUTION_ERROR.getCode());
			String errMsg = "Error occurred while interacting with RemoteHand";
			scriptResult.setErrorMessage(errMsg);
			logger.warn(errMsg, e);
		}
		
		messageIDS.add(messageHandler.onResponse(scriptResult, createSessionId(sessionId), sessionId));
		
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
			
			if (s.contains(RhUtils.LINE_SEPARATOR)) {
				s = s.replaceAll(RhUtils.LINE_SEPARATOR, "\n");
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

		//TODO fill others codes
		switch (code) {
			case SUCCESS: return RhBatchResponse.ScriptExecutionStatus.SUCCESS;
			case EXECUTION_ERROR: return RhBatchResponse.ScriptExecutionStatus.EXECUTION_ERROR;
			case COMPILE_ERROR: return RhBatchResponse.ScriptExecutionStatus.COMPILE_ERROR;
			default: return RhBatchResponse.ScriptExecutionStatus.UNRECOGNIZED;
		}
		
		
	}

	private String buildScript(RhActionsList actionsList, List<MessageID> messageIDS, String sessionId) throws IOException
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
					case WINCLICKCONTEXTMENU:
						RhWinActionsMessages.WinClickContextMenu winClickContextMenu = action.getWinClickContextMenu();
						WinActionsBuilder.addClickContextMenu(printer, winClickContextMenu);
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
					
					default:
						logger.warn("Unsupported action: " + action.getActionCase());
						break;
				}
			}
		}
		
		String s = sb.toString();
		messageIDS.addAll(messageHandler.onRequest(actionsList, s, createSessionId(sessionId)));
		return s;
	}
	
	private String createSessionId(String sessionId) {
		if (sessionId != null && sessionId.startsWith(RH_SESSION_PREFIX)) {
			return sessionId.substring(RH_SESSION_PREFIX.length());
		}
		return sessionId;
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

	@Override
	public void dispose() {
		try {
			this.messageHandler.close();
		} catch (Exception e) {
			logger.error("Error while disposing message handler", e);
		}
		
		try {
			this.rhConnManager.dispose();
		} catch (Exception e) {
			logger.error("Error while disposing RH connections", e);
		}
	}

}