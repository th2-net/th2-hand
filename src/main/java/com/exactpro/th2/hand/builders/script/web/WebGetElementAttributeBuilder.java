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

public class WebGetElementAttributeBuilder extends WebBaseBuilder<RhActionsMessages.GetElementAttribute> {
	@Override
	protected RhActionsMessages.GetElementAttribute getMessage(RhWebActions action) {
		return action.getGetElementAttribute();
	}

	@Override
	protected String getActionName() {
		return "GetElementAttribute";
	}

	@Override
	protected void createActionDetails(RhActionsMessages.GetElementAttribute message, List<String> headers, List<String> values) {
		headers.add("#attribute");
		values.add(message.getAttribute());

		headers.add("#wait");
		values.add(String.valueOf(message.getWait()));

		headers.add("#locator");
		values.add(readLocator(message.getLocator()));

		headers.add("#matcher");
		values.add(message.getMatcher());
	}
}
