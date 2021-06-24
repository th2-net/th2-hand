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

import com.exactpro.th2.act.grpc.hand.RhAction;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;
import com.exactpro.th2.hand.scriptbuilders.BaseBuilder;
import com.google.protobuf.GeneratedMessageV3;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public abstract class WinBaseBuilder<T extends GeneratedMessageV3> extends BaseBuilder<T> {
	protected static final String WINDOWNAME = "#windowname", ACCESSIBILITY_ID = "#accessibilityid";
	protected static final Pair<String, Pair<String, String>> locatorPair
			= new ImmutablePair<>("#locator", new ImmutablePair<>("#matcher", "#matcherindex"));
	protected static final Pair<String, Pair<String, String>> textLocatorPair
			= new ImmutablePair<>("#textLocator", new ImmutablePair<>("#textmatcher", "#textmatcherindex"));
	protected static final Pair<String, Pair<String, String>> toLocatorPair
			= new ImmutablePair<>("#tolocator", new ImmutablePair<>("#tomatcher", "#tomatcherindex"));
	protected static final Pair<String, Pair<String, String>> actionLocatorPair
			= new ImmutablePair<>("#actionlocator", new ImmutablePair<>("#actionmatcher", "#actionmatcherindex"));


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
	                                 Pair<String, Pair<String, String>> keys) {
		if (winLocators == null || winLocators.isEmpty())
			return;

		int count = 1;
		for (RhWinActionsMessages.WinLocator winLocator : winLocators) {

			Pair<String, String> matcherPair = keys.getValue();
			String paramSuffix = count == 1 ? "" : String.valueOf(count);

			headers.add(keys.getKey() + paramSuffix);
			values.add(winLocator.getLocator());

			headers.add(matcherPair.getKey() + paramSuffix);
			values.add(winLocator.getMatcher());

			if (winLocator.hasMatcherIndex()) {
				headers.add(matcherPair.getValue() + paramSuffix);
				values.add(String.valueOf(winLocator.getMatcherIndex().getValue()));
			}

			++count;
		}
	}
}
