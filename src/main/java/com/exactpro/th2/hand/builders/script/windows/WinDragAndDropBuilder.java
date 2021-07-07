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

import com.exactpro.remotehand.windows.actions.DragAndDropElement;
import com.exactpro.th2.act.grpc.hand.RhAction;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;

import java.util.List;

public class WinDragAndDropBuilder extends WinBaseBuilder<RhWinActionsMessages.WinDragAndDrop> {
	@Override
	protected RhWinActionsMessages.WinDragAndDrop getMessage(RhAction action) {
		return action.getWinDragAndDrop();
	}

	@Override
	protected String getActionName() {
		return DragAndDropElement.class.getSimpleName();
	}

	@Override
	protected void createActionDetails(RhWinActionsMessages.WinDragAndDrop message, List<String> headers, List<String> values) {
		addLocator(message.getFromLocatorsList(), headers, values);
		addLocator(message.getToLocatorsList(), headers, values, toLocatorPair);

		addIfNotEmpty("#fromoffsetx", message.getFromOffsetX(), headers, values);
		addIfNotEmpty("#fromoffsety", message.getFromOffsetY(), headers, values);

		addIfNotEmpty("#tooffsetx", message.getToOffsetX(), headers, values);
		addIfNotEmpty("#tooffsety", message.getToOffsetY(), headers, values);
	}

	@Override
	protected RhWinActionsMessages.BaseWinParams getBaseParams(RhWinActionsMessages.WinDragAndDrop message) {
		return message.getBaseParams();
	}
}
