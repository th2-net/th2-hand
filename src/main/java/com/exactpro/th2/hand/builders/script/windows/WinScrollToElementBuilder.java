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

package com.exactpro.th2.hand.builders.script.windows;

import com.exactpro.remotehand.windows.actions.ScrollToElement;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.RhWinActions;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class WinScrollToElementBuilder extends WinBaseBuilder<RhWinActionsMessages.WinScrollToElement> {
	@Override
	protected RhWinActionsMessages.WinScrollToElement getMessage(RhWinActions action) {
		return action.getWinScrollToElement();
	}

	@Override
	protected String getActionName() {
		return ScrollToElement.class.getSimpleName();
	}

	@Override
	protected void createActionDetails(RhWinActionsMessages.WinScrollToElement message, List<String> headers, List<String> values) {
		addLocator(message.getElementLocatorsList(), headers, values);
		addLocator(message.getActionLocatorsList(), headers, values, actionLocatorPair);

		addIfNotEmpty("#clickoffsetx", message.getClickOffsetX(), headers, values);
		addIfNotEmpty("#clickoffsety", message.getClickOffsetY(), headers, values);

		headers.add("#scrolltype");
		values.add(convertScrollType(message.getScrollType()));

		addIfNotEmpty("#maxiterations", message.getMaxIterations(), headers, values);
		addIfNotEmpty("#shouldbedisplayed", message.getIsElementShouldBeDisplayed(), headers, values);
		addIfNotEmpty("#elementindom", message.getIsElementInTree(), headers, values);
		addIfNotEmpty("#textvalue", message.getTextToSend(), headers, values);
	}

	@Override
	protected RhWinActionsMessages.BaseWinParams getBaseParams(RhWinActionsMessages.WinScrollToElement message) {
		return message.getBaseParams();
	}

	private static String convertScrollType(RhWinActionsMessages.WinScrollToElement.ScrollType scrollType) {
		switch (scrollType) {
			case CLICK:
				return "Click";
			case TEXT:
				return "Text";
			case UNRECOGNIZED:
			default:
				return StringUtils.EMPTY;
		}
	}
}
