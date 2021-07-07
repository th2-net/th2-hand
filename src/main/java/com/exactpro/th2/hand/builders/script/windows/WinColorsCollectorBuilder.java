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

import com.exactpro.remotehand.windows.actions.ColorsCollector;
import com.exactpro.th2.act.grpc.hand.RhAction;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;

import java.util.List;

public class WinColorsCollectorBuilder extends WinBaseBuilder<RhWinActionsMessages.WinColorsCollector> {
	@Override
	protected RhWinActionsMessages.WinColorsCollector getMessage(RhAction action) {
		return action.getWinColorsCollector();
	}

	@Override
	protected String getActionName() {
		return ColorsCollector.class.getSimpleName();
	}

	@Override
	protected void createActionDetails(RhWinActionsMessages.WinColorsCollector message, List<String> headers, List<String> values) {
		addLocator(message.getLocatorsList(), headers, values);

		addIfNotEmpty("#startxoffset", message.getStartXOffset(), headers, values);
		addIfNotEmpty("#startyoffset", message.getStartYOffset(), headers, values);

		addIfNotEmpty("#endxoffset", message.getEndXOffset(), headers, values);
		addIfNotEmpty("#endyoffset", message.getEndYOffset(), headers, values);
	}

	@Override
	protected RhWinActionsMessages.BaseWinParams getBaseParams(RhWinActionsMessages.WinColorsCollector message) {
		return message.getBaseParams();
	}
}
