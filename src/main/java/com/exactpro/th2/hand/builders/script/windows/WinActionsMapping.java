/*
 * Copyright 2021-2021 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.RhWinActions.ActionCase;
import com.exactpro.th2.hand.builders.script.ActionsMapping;

import java.util.EnumMap;
import java.util.function.Supplier;

public class WinActionsMapping extends ActionsMapping<ActionCase, WinBaseBuilder<?>> {

	public WinActionsMapping() {
		super("windows");
	}

	@Override
	protected EnumMap<ActionCase, Supplier<WinBaseBuilder<?>>> createMapping() {
		return new EnumMap<>(ActionCase.class) {{
			put(ActionCase.WINOPEN, WinOpenBuilder::new);
			put(ActionCase.WINCLICK, WinClickBuilder::new);
			put(ActionCase.WINGETACTIVEWINDOW, WinGetActiveWindowBuilder::new);
			put(ActionCase.WINGETELEMENTATTRIBUTE, WinGetElementAttributeBuilder::new);
			put(ActionCase.WINSENDTEXT, WinSendTextBuilder::new);
			put(ActionCase.WINCHECKELEMENT, WinCheckElementBuilder::new);
			put(ActionCase.WINCOLORSCOLLECTOR, WinColorsCollectorBuilder::new);
			put(ActionCase.WINDRAGANDDROP, WinDragAndDropBuilder::new);
			put(ActionCase.WINGETDATAFROMCLIPBOARD, WinGetDataFromClipboardBuilder::new);
			put(ActionCase.WINGETELEMENTCOLOR, WinGetElementColorBuilder::new);
			put(ActionCase.WINGETSCREENSHOT, WinGetScreenshotBuilder::new);
			put(ActionCase.WINGETWINDOW, WinGetWindowBuilder::new);
			put(ActionCase.WINMAXIMIZEMAINWINDOW, WinMaximizeMainWindowBuilder::new);
			put(ActionCase.WINRESTARTDRIVER, WinRestartDriverBuilder::new);
			put(ActionCase.WINSCROLLTOELEMENT, WinScrollToElementBuilder::new);
			put(ActionCase.WINSCROLLUSINGTEXT, WinScrollUsingTextBuilder::new);
			put(ActionCase.WINSEARCHELEMENT, WinSearchElementBuilder::new);
			put(ActionCase.WINTABLESEARCH, WinTableSearchBuilder::new);
			put(ActionCase.WINTOGGLECHECKBOX, WinToggleCheckBoxBuilder::new);
			put(ActionCase.WINWAIT, WinWaitBuilder::new);
			put(ActionCase.WINWAITFORATTRIBUTE, WinWaitForAttributeBuilder::new);
			put(ActionCase.WINWAITFORELEMENT, WinWaitForElementBuilder::new);
			put(ActionCase.WINTAKESCREENSHOT, WinTakeScreenshotBuilder::new);
		}};
	}

}
