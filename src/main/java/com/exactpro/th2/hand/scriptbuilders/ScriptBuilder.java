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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptBuilder {
	private static final Logger logger = LoggerFactory.getLogger(ScriptBuilder.class);
	protected final Map<RhAction.ActionCase, BaseBuilder<?>> buildersMap = new ConcurrentHashMap<>(RhAction.ActionCase.values().length);


	public String buildScript(List<RhAction> actions) {
		StringBuilder script = new StringBuilder();

		try (CSVPrinter printer = CSVFormat.DEFAULT.print(script)) {
			for (RhAction action : actions) {
				BaseBuilder<?> builder = getBuilder(action);
				if (builder == null)
					continue;
				builder.buildScript(printer, action);
			}
		} catch (IOException e) {
			logger.error("An error occurred while script building", e);
		}

		return script.toString();
	}


	protected BaseBuilder<?> getBuilder(RhAction.ActionCase type) {
		return buildersMap.computeIfAbsent(type, ActionsMapping::createInstance);
	}

	protected BaseBuilder<?> getBuilder(RhAction action) {
		return getBuilder(action.getActionCase());
	}
}
