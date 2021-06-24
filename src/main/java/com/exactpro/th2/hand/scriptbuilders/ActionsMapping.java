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

package com.exactpro.th2.hand.scriptbuilders;

import com.exactpro.th2.act.grpc.hand.RhAction;
import com.exactpro.th2.hand.scriptbuilders.web.*;
import com.exactpro.th2.hand.scriptbuilders.windows.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.function.Supplier;

public class ActionsMapping {
	private static final Logger logger = LoggerFactory.getLogger(ActionsMapping.class);

	private static final EnumMap<RhAction.ActionCase, Supplier<BaseBuilder<?>>> mapping = new EnumMap<>(RhAction.ActionCase.class) {{
		// Web actions
		put(RhAction.ActionCase.CLICK, WebClickBuilder::new);
		put(RhAction.ActionCase.EXECUTEJS, WebExecuteJSBuilder::new);
		put(RhAction.ActionCase.EXECUTEJSELEMENT, WebExecuteJSElementBuilder::new);
		put(RhAction.ActionCase.FINDELEMENT, WebFindElementBuilder::new);
		put(RhAction.ActionCase.GETELEMENTATTRIBUTE, WebGetElementAttributeBuilder::new);
		put(RhAction.ActionCase.GETELEMENTINNERHTML, WebGetElementInnerHtmlBuilder::new);
		put(RhAction.ActionCase.GETELEMENTSCREENSHOT, WebGetElementScreenshotBuilder::new);
		put(RhAction.ActionCase.GETELEMENTVALUE, WebGetElementValueBuilder::new);
		put(RhAction.ActionCase.GETSCREENSHOT, WebGetScreenshotBuilder::new);
		put(RhAction.ActionCase.OPEN, WebOpenBuilder::new);
		put(RhAction.ActionCase.SCROLLDIVUNTIL, WebScrollDivUntilBuilder::new);
		put(RhAction.ActionCase.SENDKEYS, WebSendKeysBuilder::new);
		put(RhAction.ActionCase.SENDKEYSTOACTIVE, WebSendKeysToActiveBuilder::new);
		put(RhAction.ActionCase.SWITCHWINDOW, WebSwitchWindowBuilder::new);
		put(RhAction.ActionCase.WAIT, WebWaitBuilder::new);

		// Win actions
		put(RhAction.ActionCase.WINOPEN, WinOpenBuilder::new);
		put(RhAction.ActionCase.WINCLICK, WinClickBuilder::new);
		put(RhAction.ActionCase.WINGETACTIVEWINDOW, WinGetActiveWindowBuilder::new);
		put(RhAction.ActionCase.WINGETELEMENTATTRIBUTE, WinGetElementAttributeBuilder::new);
		put(RhAction.ActionCase.WINSENDTEXT, WinSendTextBuilder::new);
		put(RhAction.ActionCase.WINCHECKELEMENT, WinCheckElementBuilder::new);
		put(RhAction.ActionCase.WINCOLORSCOLLECTOR, WinColorsCollectorBuilder::new);
		put(RhAction.ActionCase.WINDRAGANDDROP, WinDragAndDropBuilder::new);
		put(RhAction.ActionCase.WINGETDATAFROMCLIPBOARD, WinGetDataFromClipboardBuilder::new);
		put(RhAction.ActionCase.WINGETELEMENTCOLOR, WinGetElementColorBuilder::new);
		put(RhAction.ActionCase.WINGETSCREENSHOT, WinGetScreenshotBuilder::new);
		put(RhAction.ActionCase.WINGETWINDOW, WinGetWindowBuilder::new);
		put(RhAction.ActionCase.WINMAXIMIZEMAINWINDOW, WinMaximizeMainWindowBuilder::new);
		put(RhAction.ActionCase.WINRESTARTDRIVER, WinRestartDriverBuilder::new);
		put(RhAction.ActionCase.WINSCROLLTOELEMENT, WinScrollToElementBuilder::new);
		put(RhAction.ActionCase.WINSCROLLUSINGTEXT, WinScrollUsingTextBuilder::new);
		put(RhAction.ActionCase.WINSEARCHELEMENT, WinSearchElementBuilder::new);
		put(RhAction.ActionCase.WINTABLESEARCH, WinTableSearchBuilder::new);
		put(RhAction.ActionCase.WINTOGGLECHECKBOX, WinToggleCheckBoxBuilder::new);
		put(RhAction.ActionCase.WINWAIT, WinWaitBuilder::new);
		put(RhAction.ActionCase.WINWAITFORATTRIBUTE, WinWaitForAttributeBuilder::new);
		put(RhAction.ActionCase.WINWAITFORELEMENT, WinWaitForElementBuilder::new);
	}};


	public static BaseBuilder<?> createInstance(RhAction.ActionCase type) {
		Supplier<BaseBuilder<?>> builderSupplier = mapping.get(type);
		if (builderSupplier == null) {
			logger.warn("Unsupported action: " + type);
			return null;
		}

		return builderSupplier.get();
	} 
}
