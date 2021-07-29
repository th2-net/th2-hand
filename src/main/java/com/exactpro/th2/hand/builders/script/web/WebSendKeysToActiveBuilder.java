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

import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages.RhWebActions;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages;

import java.util.List;

public class WebSendKeysToActiveBuilder extends WebBaseBuilder<RhActionsMessages.SendKeysToActive> {
	@Override
	protected RhActionsMessages.SendKeysToActive getMessage(RhWebActions action) {
		return action.getSendKeysToActive();
	}

	@Override
	protected String getActionName() {
		return "SendKeysToActive";
	}

	@Override
	protected void createActionDetails(RhActionsMessages.SendKeysToActive message, List<String> headers, List<String> values) {
		headers.add("#text");
		values.add(message.getText());

		headers.add("#text2");
		values.add(message.getText2());
	}
}
