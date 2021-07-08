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

package com.exactpro.th2.hand.builders.script.web;

import com.exactpro.th2.act.grpc.hand.RhAction;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages;

import java.util.List;

public class WebSwitchWindowBuilder extends WebBaseBuilder<RhActionsMessages.SwitchWindow> {
	@Override
	protected RhActionsMessages.SwitchWindow getMessage(RhAction action) {
		return action.getSwitchWindow();
	}

	@Override
	protected String getActionName() {
		return "SwitchWindow";
	}

	@Override
	protected void createActionDetails(RhActionsMessages.SwitchWindow message, List<String> headers, List<String> values) {
		headers.add("#window");
		values.add(String.valueOf(message.getWindow()));
	}
}
