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

import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages.RhWebActions;
import com.exactpro.th2.hand.builders.script.BaseBuilder;
import com.google.protobuf.GeneratedMessageV3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class WebBaseBuilder<T extends GeneratedMessageV3> extends BaseBuilder<T, RhWebActions> {
	private static final Logger logger = LoggerFactory.getLogger(WebBaseBuilder.class);

	@Override
	protected void buildPayLoad(RhWebActions action, List<String> headers, List<String> values) {
		T message = getMessage(action);
		createActionDetails(message, headers, values);
	}

	protected String readLocator(RhActionsMessages.Locator locator) {
		if (locator == null)
			return null;

		switch (locator) {
			case CSS_SELECTOR:
				return "cssSelector";
			case TAG_NAME:
				return "tagName";
			case ID:
				return "id";
			case XPATH:
				return "xpath";
			default:
				logger.warn("Unsupported locator: " + locator);
		}
		return null;
	}
}
