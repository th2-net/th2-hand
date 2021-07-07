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

package com.exactpro.th2.hand.scriptbuilders.windows;

import com.exactpro.remotehand.windows.SearchParams;
import com.exactpro.th2.act.grpc.hand.RhAction;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;
import com.exactpro.th2.hand.scriptbuilders.BaseBuilder;
import com.google.protobuf.GeneratedMessageV3;

import java.util.List;

public abstract class WinBaseBuilder<T extends GeneratedMessageV3> extends BaseBuilder<T> {
	protected static final String WINDOWNAME = "#windowname", ACCESSIBILITY_ID = "#accessibilityid";
	protected static final SearchParams.HeaderKeys locatorPair = SearchParams.HeaderKeys.DEFAULT;
	protected static final SearchParams.HeaderKeys textLocatorPair
			= new SearchParams.HeaderKeys("#textLocator", "#textmatcher", "#textmatcherindex");
	protected static final SearchParams.HeaderKeys toLocatorPair
			= new SearchParams.HeaderKeys("#tolocator", "#tomatcher", "#tomatcherindex");
	protected static final SearchParams.HeaderKeys actionLocatorPair
			= new SearchParams.HeaderKeys("#actionlocator", "#actionmatcher", "#actionmatcherindex");


	@Override
	protected void buildPayLoad(RhAction action, List<String> headers, List<String> values) {
		T message = getMessage(action);
		addDefaults(getBaseParams(message), headers, values);
		createActionDetails(message, headers, values);
	}

	protected void addDefaults(RhWinActionsMessages.BaseWinParams baseParams, List<String> headers, List<String> values) {
		addIfNotEmpty("#id", baseParams.getId(), headers, values);
		addIfNotEmpty("#execute", baseParams.getExecute(), headers, values);
		addIfNotEmpty("#fromRoot", String.valueOf(baseParams.getFromRoot()), headers, values);
		addIfNotEmpty("#isExperimental", String.valueOf(baseParams.getExperimentalDriver()), headers, values);
	}

	protected abstract RhWinActionsMessages.BaseWinParams getBaseParams(T message);


	protected static void addLocator(List<RhWinActionsMessages.WinLocator> winLocators, List<String> headers, List<String> values) {
		addLocator(winLocators, headers, values, locatorPair);
	}

	protected static void addLocator(List<RhWinActionsMessages.WinLocator> winLocators, List<String> headers, List<String> values,
	                                 SearchParams.HeaderKeys keys) {
		if (winLocators == null || winLocators.isEmpty())
			return;

		int count = 1;
		for (RhWinActionsMessages.WinLocator winLocator : winLocators) {
			String paramSuffix = count == 1 ? "" : String.valueOf(count);

			headers.add(keys.locator + paramSuffix);
			values.add(winLocator.getLocator());

			headers.add(keys.matcher + paramSuffix);
			values.add(winLocator.getMatcher());

			if (winLocator.hasMatcherIndex()) {
				headers.add(keys.index + paramSuffix);
				values.add(String.valueOf(winLocator.getMatcherIndex().getValue()));
			}

			++count;
		}
	}
}
