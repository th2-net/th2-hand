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

import com.exactpro.remotehand.web.actions.WaitForElement;
import com.exactpro.th2.act.grpc.hand.RhAction;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages;

import java.util.List;

public class WebWaitForElementBuilder extends WebBaseBuilder<RhActionsMessages.WaitForElement> {
	
	@Override
	protected RhActionsMessages.WaitForElement getMessage(RhAction action) {
		return action.getWaitForElement();
	}

	@Override
	protected String getActionName() {
		return WaitForElement.class.getSimpleName();
	}

	@Override
	protected void createActionDetails(RhActionsMessages.WaitForElement message, List<String> headers, List<String> values) {
		headers.add("#locator");
		values.add(String.valueOf(message.getLocator()));

		headers.add("#matcher");
		values.add(message.getMatcher());

		headers.add("#seconds");
		values.add(String.valueOf(message.getSeconds()));
	}
}
