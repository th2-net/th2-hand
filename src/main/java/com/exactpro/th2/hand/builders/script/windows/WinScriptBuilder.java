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

import com.exactpro.th2.act.grpc.hand.RhActionList;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.RhWinActions;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.RhWinActions.ActionCase;
import com.exactpro.th2.hand.builders.script.AbstractScriptBuilder;
import com.exactpro.th2.hand.builders.script.ActionsMapping;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WinScriptBuilder extends AbstractScriptBuilder<RhWinActions> {
	
	protected final Map<ActionCase, WinBaseBuilder<?>> buildersMap = new ConcurrentHashMap<>();

	@Override
	protected List<RhWinActions> getActionsList(RhActionList actions) {
		return actions.getWin().getWinActionListList();
	}

	protected WinBaseBuilder<?> getBuilder(RhWinActions action) {
		ActionCase actionCase = action.getActionCase();
		return buildersMap.computeIfAbsent(actionCase, ActionsMapping.getWindowsMapping()::createInstance);
	}
	
}
