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
import java.util.stream.Collectors;

public class WebClickBuilder extends WebBaseBuilder<RhActionsMessages.Click> {
	@Override
	protected RhActionsMessages.Click getMessage(RhWebActions action) {
		return action.getClick();
	}

	@Override
	protected String getActionName() {
		return "Click";
	}

	@Override
	protected void createActionDetails(RhActionsMessages.Click message, List<String> headers, List<String> values) {
		headers.add("#wait");
		values.add(String.valueOf(message.getWait()));

		headers.add("#locator");
		values.add(readLocator(message.getLocator()));
		
		headers.add("#matcher");
		values.add(message.getMatcher());

		headers.add("#button");
		values.add(message.getButton().name().toLowerCase());

		headers.add("#xOffset");
		values.add(String.valueOf(message.getXOffset()));

		headers.add("#yOffset");
		values.add(String.valueOf(message.getYOffset()));

		headers.add("#modifiers");
		values.add(readModifiers(message.getModifiers()));
	}

	private String readModifiers(RhActionsMessages.Click.ModifiersList modifiersList) {
		return modifiersList.getModifierList().stream().map(modifier -> modifier.name().toLowerCase())
				.collect(Collectors.joining(","));
	}
}
