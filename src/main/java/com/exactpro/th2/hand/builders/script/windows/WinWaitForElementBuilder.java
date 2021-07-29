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

import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.RhWinActions;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;

import java.util.List;

public class WinWaitForElementBuilder extends WinBaseBuilder<RhWinActionsMessages.WinWaitForElement> {
	@Override
	protected RhWinActionsMessages.WinWaitForElement getMessage(RhWinActions action) {
		return action.getWinWaitForElement();
	}

	@Override
	protected String getActionName() {
		return "WaitForElement";
	}

	@Override
	protected void createActionDetails(RhWinActionsMessages.WinWaitForElement message, List<String> headers, List<String> values) {
		addLocator(message.getLocatorsList(), headers, values);

		addIfNotEmpty("#timeout", message.getTimeout(), headers, values);
	}

	@Override
	protected RhWinActionsMessages.BaseWinParams getBaseParams(RhWinActionsMessages.WinWaitForElement message) {
		return message.getBaseParams();
	}
}
