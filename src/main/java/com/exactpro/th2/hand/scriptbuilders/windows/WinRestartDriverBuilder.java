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

import com.exactpro.remotehand.windows.actions.RestartDriver;
import com.exactpro.th2.act.grpc.hand.RhAction;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;

import java.util.List;

public class WinRestartDriverBuilder extends WinBaseBuilder<RhWinActionsMessages.WinRestartDriver> {
	@Override
	protected RhWinActionsMessages.WinRestartDriver getMessage(RhAction action) {
		return action.getWinRestartDriver();
	}

	@Override
	protected String getActionName() {
		return RestartDriver.class.getSimpleName();
	}

	@Override
	protected void createActionDetails(RhWinActionsMessages.WinRestartDriver message, List<String> headers, List<String> values) {

	}

	@Override
	protected RhWinActionsMessages.BaseWinParams getBaseParams(RhWinActionsMessages.WinRestartDriver message) {
		return message.getBaseParams();
	}
}
