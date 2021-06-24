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

package com.exactpro.th2.hand.scriptbuilders.windows;

import com.exactpro.th2.act.grpc.hand.RhAction;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;

import java.util.List;

public class WinScrollUsingTextBuilder extends WinBaseBuilder<RhWinActionsMessages.WinScrollUsingText> {
	@Override
	protected RhWinActionsMessages.WinScrollUsingText getMessage(RhAction action) {
		return action.getWinScrollUsingText();
	}

	@Override
	protected String getActionName() {
		return "ScrollUsingText";
	}

	@Override
	protected void createActionDetails(RhWinActionsMessages.WinScrollUsingText message, List<String> headers, List<String> values) {
		addLocator(message.getLocatorsList(), headers, values);
		addLocator(message.getTextLocatorsList(), headers, values, textLocatorPair);

		addIfNotEmpty("#textToSend", message.getTextToSend(), headers, values);
		addIfNotEmpty("#maxIterations", message.getMaxIterations(), headers, values);
	}

	@Override
	protected RhWinActionsMessages.BaseWinParams getBaseParams(RhWinActionsMessages.WinScrollUsingText message) {
		return message.getBaseParams();
	}
}
