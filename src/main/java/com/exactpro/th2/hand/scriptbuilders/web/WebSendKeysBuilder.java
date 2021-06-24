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

package com.exactpro.th2.hand.scriptbuilders.web;

import com.exactpro.th2.act.grpc.hand.RhAction;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages;

import java.util.List;

public class WebSendKeysBuilder extends WebBaseBuilder<RhActionsMessages.SendKeys> {
	@Override
	protected RhActionsMessages.SendKeys getMessage(RhAction action) {
		return action.getSendKeys();
	}

	@Override
	protected String getActionName() {
		return "SendKeys";
	}

	@Override
	protected void createActionDetails(RhActionsMessages.SendKeys message, List<String> headers, List<String> values) {
		headers.add("#wait");
		values.add(String.valueOf(message.getWait()));

		headers.add("#locator");
		values.add(readLocator(message.getLocator()));

		headers.add("#matcher");
		values.add(message.getMatcher());

		headers.add("#text");
		values.add(message.getText());

		headers.add("#wait2");
		values.add(String.valueOf(message.getWait2()));

		headers.add("#locator2");
		values.add(readLocator(message.getLocator2()));

		headers.add("#matcher2");
		values.add(message.getMatcher2());

		headers.add("#text2");
		values.add(message.getText2());

		headers.add("#canBeDisabled");
		values.add(String.valueOf(message.getCanBeDisabled()));

		headers.add("#clear");
		values.add(String.valueOf(message.getClear()));

		headers.add("#checkInput");
		values.add(String.valueOf(message.getCheckInput()));

		headers.add("#needClick");
		values.add(String.valueOf(message.getNeedClick()));
	}
}
