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

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.io.IOException;
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
import com.exactpro.th2.hand.remotehand.RhClient;
import com.exactpro.th2.hand.remotehand.RhException;
import com.exactpro.th2.hand.remotehand.RhResponseCode;
import com.exactpro.th2.hand.remotehand.RhScriptResult;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.th2.hand.Config;
import com.exactpro.th2.hand.IHandService;
import com.google.protobuf.TextFormat;

import io.grpc.stub.StreamObserver;

public class HandBaseService extends RhBatchImplBase implements IHandService
{
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private RhClient rhConnection;

	@Override
	public void executeRhActionsBatch(RhActionsList request, StreamObserver<RhBatchResponse> responseObserver)
	{
		logger.info("Action: '{}', request: '{}'", "executeRhActionsBatch", TextFormat.shortDebugString(request));

		RhScriptResult scriptResult;
		try
		{
			rhConnection.send(buildScript(request));
			scriptResult = rhConnection.waitAndGet(120);
		}
		catch (RhException | IOException e)
		{
			scriptResult = new RhScriptResult();
			scriptResult.setCode(RhResponseCode.UNKNOWN.getCode());
			String errMsg = "Error occurred while interacting with RemoteHand";
			scriptResult.setErrorMessage(errMsg);
			logger.warn(errMsg, e);
		}
		RhBatchResponse response = RhBatchResponse.newBuilder()
				.setScriptResult(RhResponseCode.byCode(scriptResult.getCode()).toString())
				.setErrorMessage(defaultIfEmpty(scriptResult.getErrorMessage(), "")).setSessionId(rhConnection.getSessionId())
				.addAllTextOut(scriptResult.getTextOutput()).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void init(Config config, RhClient rhConnection)
	{
		this.rhConnection = rhConnection;
	}

	private String buildScript(RhActionsList actionsList) throws IOException
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
					addOpen(printer, action.getOpen());
					break;
				case SENDKEYS:
					addSendKeys(printer, action.getSendKeys());
					break;
				case FINDELEMENT:
					addFindElement(printer, action.getFindElement());
					break;
				case CLICK:
					addClick(printer, action.getClick());
					break;
				case SENDKEYSTOACTIVE:
					addSendKeysToActive(printer, action.getSendKeysToActive());
					break;
				case SWITCHWINDOW:
					addSwitchWindow(printer, action.getSwitchWindow());
					break;
				default:
					logger.warn("Unsupported action: " + action.getActionCase());
					break;
				}
			}
		}

		return sb.toString();
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
}