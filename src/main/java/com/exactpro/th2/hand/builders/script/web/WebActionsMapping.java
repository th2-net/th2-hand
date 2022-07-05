/*
 * Copyright 2021-2022 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages.RhWebActions.ActionCase;
import com.exactpro.th2.hand.builders.script.ActionsMapping;

import java.util.EnumMap;
import java.util.function.Supplier;

public class WebActionsMapping extends ActionsMapping<ActionCase, WebBaseBuilder<?>> {

	public WebActionsMapping() {
		super("web");
	}

	@Override
	protected EnumMap<ActionCase, Supplier<WebBaseBuilder<?>>> createMapping() {
		return new EnumMap<>(ActionCase.class) {{
			// Web actions
			put(ActionCase.CLICK, WebClickBuilder::new);
			put(ActionCase.EXECUTEJS, WebExecuteJSBuilder::new);
			put(ActionCase.EXECUTEJSELEMENT, WebExecuteJSElementBuilder::new);
			put(ActionCase.FINDELEMENT, WebFindElementBuilder::new);
			put(ActionCase.GETELEMENTATTRIBUTE, WebGetElementAttributeBuilder::new);
			put(ActionCase.GETELEMENTINNERHTML, WebGetElementInnerHtmlBuilder::new);
			put(ActionCase.GETELEMENTSCREENSHOT, WebGetElementScreenshotBuilder::new);
			put(ActionCase.GETELEMENTVALUE, WebGetElementValueBuilder::new);
			put(ActionCase.GETSCREENSHOT, WebGetScreenshotBuilder::new);
			put(ActionCase.OPEN, WebOpenBuilder::new);
			put(ActionCase.SCROLLDIVUNTIL, WebScrollDivUntilBuilder::new);
			put(ActionCase.SENDKEYS, WebSendKeysBuilder::new);
			put(ActionCase.SENDKEYSTOACTIVE, WebSendKeysToActiveBuilder::new);
			put(ActionCase.SWITCHWINDOW, WebSwitchWindowBuilder::new);
			put(ActionCase.WAIT, WebWaitBuilder::new);
			put(ActionCase.WAITFORELEMENT, WebWaitForElementBuilder::new);
			put(ActionCase.SELECTFRAME, WebSelectFrameBuilder::new);
		}};
	}

}
