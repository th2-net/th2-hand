/*
 * Copyright 2021-2021 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.remotehand.windows.actions.TakeScreenshot;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.RhWinActions;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;

import java.util.List;

public class WinTakeScreenshotBuilder extends WinBaseBuilder<RhWinActionsMessages.WinTakeScreenshot> {
	@Override
	protected RhWinActionsMessages.WinTakeScreenshot getMessage(RhWinActions action) {
		return action.getWinTakeScreenshot();
	}

	@Override
	protected String getActionName() {
		return TakeScreenshot.class.getSimpleName();
	}

	@Override
	protected void createActionDetails(RhWinActionsMessages.WinTakeScreenshot message, List<String> headers, List<String> values) {
		if (message.getLocatorsCount() != 0) {
			addLocator(message.getLocatorsList(), headers, values);
		}
		if (!message.getName().isEmpty()) {
			headers.add("#name");
			values.add(getActionName());
		}
	}

	@Override
	protected RhWinActionsMessages.BaseWinParams getBaseParams(RhWinActionsMessages.WinTakeScreenshot message) {
		return message.getBaseParams();
	}
}
