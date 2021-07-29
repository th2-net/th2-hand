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

import com.exactpro.remotehand.windows.actions.Click;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.RhWinActions;

import java.util.List;

public class WinClickBuilder extends WinBaseBuilder<RhWinActionsMessages.WinClick> {
	@Override
	protected RhWinActionsMessages.WinClick getMessage(RhWinActions action) {
		return action.getWinClick();
	}

	@Override
	protected RhWinActionsMessages.BaseWinParams getBaseParams(RhWinActionsMessages.WinClick message) {
		return message.getBaseParams();
	}

	@Override
	protected String getActionName() {
		return Click.class.getSimpleName();
	}

	@Override
	protected void createActionDetails(RhWinActionsMessages.WinClick message, List<String> headers, List<String> values) {
		addLocator(message.getLocatorsList(), headers, values);

		RhWinActionsMessages.WinClick.Button button = message.getButton();
		if (button != RhWinActionsMessages.WinClick.Button.UNRECOGNIZED) {
			headers.add("#button");
			values.add(button.name().toLowerCase());
		}

		addIfNotEmpty("#xOffset", message.getXOffset(), headers, values);
		addIfNotEmpty("#yOffset", message.getYOffset(), headers, values);
		addIfNotEmpty("#modifiers", message.getModifiers(), headers, values);
	}
}
