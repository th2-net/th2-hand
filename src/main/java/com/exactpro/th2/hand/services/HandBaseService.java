/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.exactpro.th2.hand.services;

import static com.google.protobuf.TextFormat.shortDebugString;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.exactpro.th2.hand.grpc.rhbatch.RhAction;
import com.exactpro.th2.hand.grpc.rhbatch.RhActionsList;
import com.exactpro.th2.hand.grpc.rhbatch.RhBatchGrpc.RhBatchImplBase;
import com.exactpro.th2.hand.grpc.rhbatch.RhBatchResponse;
import com.exactpro.th2.hand.grpc.rhbatch.rhactions.RhActionsMessages.*;
import com.exactpro.th2.hand.grpc.rhbatch.rhactions.RhActionsMessages.Click.ModifiersList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.th2.hand.Config;
import com.exactpro.th2.hand.IHandService;
import com.exactprosystems.clearth.connectivity.remotehand.RhClient;
import com.exactprosystems.clearth.connectivity.remotehand.RhException;

import io.grpc.stub.StreamObserver;

public class HandBaseService extends RhBatchImplBase implements IHandService
{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private Config config;
	private RhClient rhConnection;

    @Override
    public void executeRhActionsBatch(RhActionsList request, StreamObserver<RhBatchResponse> responseObserver) {
        logger.info("Action: '{}', request: '{}'", "executeRhActionsBatch", shortDebugString(request));

        int code = 200;
        String dataString = null;
        try {
			dataString = rhConnection.send(buildScript(request));
        } 
        catch (RhException | IOException e ) {
            code = 500; // FIXME RhClient has no public methods returning RhResponse, so code is lost
            logger.warn("Error occurred while fetching data from Remotehand", e);
        }
        RhBatchResponse response = RhBatchResponse.newBuilder()
                .setCode(code)
                .setData(dataString)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
	public void init(Config config, RhClient rhConnection) {
		this.config = config;
		this.rhConnection = rhConnection;
	}

	private String buildScript(RhActionsList actionsList)
	{
		StringBuilder sb = new StringBuilder();

		List<RhAction> actionList = actionsList.getRhActionList();
		for (RhAction action : actionList)
		{
			switch (action.getActionCase())
			{
				case OPEN:
					addOpen(sb, action.getOpen());
					break;
				case SENDKEYS:
					addSendKeys(sb, action.getSendKeys());
					break;
				case FINDELEMENT:
					addFindElement(sb, action.getFindElement());
					break;
				case CLICK:
					addClick(sb, action.getClick());
					break;
				case SENDKEYSTOACTIVE:
					addSendKeysToActive(sb, action.getSendKeysToActive());
					break;
				case SWITCHWINDOW:
					addSwitchWindow(sb, action.getSwitchWindow());
					break;
			}
			sb.append(System.getProperty("line.separator"));
		}

    	return sb.toString();
	}

	private void appendAndComma(StringBuilder sb, String text) {
    	if (text != null) {
			if (text.contains(","))
				sb.append("\"").append(text).append("\"");
			else
				sb.append(text);
		}
		sb.append(",");
	}

	private void addOpen(StringBuilder sb, Open open) {
    	//#action,#url
		appendAndComma(sb,"#action");
		appendAndComma(sb, "#url");
		sb.append(System.getProperty("line.separator"));
    	appendAndComma(sb, "Open");
    	appendAndComma(sb, open.getUrl());
	}

	private void addSendKeys(StringBuilder sb, SendKeys sendKeys) {
    	//#action,#wait,#locator,#matcher,#text,#wait2,#locator2,#matcher2,#text2,#canBeDisabled,#clear,#checkInput,#needClick
		appendAndComma(sb, "#action");
		appendAndComma(sb, "#wait");
		appendAndComma(sb, "#locator");
		appendAndComma(sb, "#matcher");
		appendAndComma(sb, "#text");
		appendAndComma(sb, "#wait2");
		appendAndComma(sb, "#locator2");
		appendAndComma(sb, "#matcher2");
		appendAndComma(sb, "#text2");
		appendAndComma(sb, "#canBeDisabled");
		appendAndComma(sb, "#clear");
		appendAndComma(sb, "#checkInput");
		appendAndComma(sb, "#needClick");
		sb.append(System.getProperty("line.separator"));
		appendAndComma(sb, "SendKeys");
		appendAndComma(sb, String.valueOf(sendKeys.getWait()));
		appendAndComma(sb, readLocator(sendKeys.getLocator()));
		appendAndComma(sb, sendKeys.getMatcher());
		appendAndComma(sb, sendKeys.getText());
		appendAndComma(sb, String.valueOf(sendKeys.getWait2()));
		appendAndComma(sb, readLocator(sendKeys.getLocator2()));
		appendAndComma(sb, sendKeys.getMatcher2());
		appendAndComma(sb, sendKeys.getText2());
		appendAndComma(sb, String.valueOf(sendKeys.getCanBeDisabled()));
		appendAndComma(sb, String.valueOf(sendKeys.getClear()));
		appendAndComma(sb, String.valueOf(sendKeys.getCheckInput()));
		appendAndComma(sb, String.valueOf(sendKeys.getNeedClick()));
	}

	private String readLocator(Locator locator) {
    	if (locator == null)
    		return null;

    	switch (locator) {
			case CSS_SELECTOR:
				return "cssSelector";
			case TAG_NAME:
				return "tagName";
			case ID:
				return "id";
			case XPATH:
				return "xpath";
		}
		return null;
	}

	private void addFindElement(StringBuilder sb, FindElement findElement) {
    	// #action,#wait,#locator,#matcher,#id
		appendAndComma(sb, "#action");
		appendAndComma(sb, "#wait");
		appendAndComma(sb, "#locator");
		appendAndComma(sb, "#matcher");
		appendAndComma(sb, "#id");
		sb.append(System.getProperty("line.separator"));
		appendAndComma(sb, "FindElement");
		appendAndComma(sb, String.valueOf(findElement.getWait()));
		appendAndComma(sb, readLocator(findElement.getLocator()));
		appendAndComma(sb, findElement.getMatcher());
		appendAndComma(sb, findElement.getId());
	}

	private void addClick(StringBuilder sb, Click click) {
		// #action,#wait,#locator,#matcher,#button,#xOffset,#yOffset,#modifiers
		appendAndComma(sb, "#action");
		appendAndComma(sb, "#wait");
		appendAndComma(sb, "#locator");
		appendAndComma(sb, "#matcher");
		appendAndComma(sb, "#button");
		appendAndComma(sb, "#xOffset");
		appendAndComma(sb, "#yOffset");
		appendAndComma(sb, "#modifiers");
		sb.append(System.getProperty("line.separator"));
		appendAndComma(sb, "Click");
		appendAndComma(sb, String.valueOf(click.getWait()));
		appendAndComma(sb, readLocator(click.getLocator()));
		appendAndComma(sb, click.getMatcher());
		appendAndComma(sb, click.getButton().name().toLowerCase());
		appendAndComma(sb, String.valueOf(click.getXOffset()));
		appendAndComma(sb, String.valueOf(click.getYOffset()));
		appendAndComma(sb, readModifiers(click.getModifiers()));
	}

	private String readModifiers(ModifiersList modifiersList) {
    	return modifiersList.getModifierList().stream()
				.map(modifier -> modifier.name().toLowerCase())
				.collect(Collectors.joining(","));
	}

	private void addSendKeysToActive(StringBuilder sb, SendKeysToActive sendKeysToActive) {
		// #action,#text,#text2
		appendAndComma(sb, "#action");
		appendAndComma(sb, "#text");
		appendAndComma(sb, "#text2");
		sb.append(System.getProperty("line.separator"));
		appendAndComma(sb, "SendKeysToActive");
		appendAndComma(sb, sendKeysToActive.getText());
		appendAndComma(sb, sendKeysToActive.getText2());
	}

	private void addSwitchWindow(StringBuilder sb, SwitchWindow switchWindow) {
    	// #action,#window
		appendAndComma(sb, "#action");
		appendAndComma(sb, "#window");
		sb.append(System.getProperty("line.separator"));
		appendAndComma(sb, "SwitchWindow");
		appendAndComma(sb, String.valueOf(switchWindow.getWindow()));
	}
}
