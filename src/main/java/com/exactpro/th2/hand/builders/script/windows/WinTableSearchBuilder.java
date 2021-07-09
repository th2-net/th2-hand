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

import com.exactpro.th2.act.grpc.hand.RhAction;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;

import java.util.List;

public class WinTableSearchBuilder extends WinBaseBuilder<RhWinActionsMessages.WinTableSearch> {
	@Override
	protected RhWinActionsMessages.WinTableSearch getMessage(RhAction action) {
		return action.getWinTableSearch();
	}

	@Override
	protected String getActionName() {
		return "TableSearch";
	}

	@Override
	protected void createActionDetails(RhWinActionsMessages.WinTableSearch message, List<String> headers, List<String> values) {
		addLocator(message.getLocatorsList(), headers, values);

		headers.add("#filter");
		values.add(message.getSearchParams());

		headers.add("#column");
		values.add(message.getTargetColumn());

		addIfNotEmpty("#firstrowindex", message.getFirstRowIndex(), headers, values);
		addIfNotEmpty("#index", message.getColumnIndex(), headers, values);
		addIfNotEmpty("#rownameformat", message.getRowNameFormat(), headers, values);
		addIfNotEmpty("#rowelementnameformat", message.getRowElementNameFormat(), headers, values);
		addIfNotEmpty("#rowelementvalueformat", message.getRowElementValueFormat(), headers, values);
		addIfNotEmpty("#saveresult", message.getSaveResult(), headers, values);
	}

	@Override
	protected RhWinActionsMessages.BaseWinParams getBaseParams(RhWinActionsMessages.WinTableSearch message) {
		return message.getBaseParams();
	}
}
