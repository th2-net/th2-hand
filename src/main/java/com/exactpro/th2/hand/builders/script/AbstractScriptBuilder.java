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

package com.exactpro.th2.hand.builders.script;

import com.exactpro.th2.act.grpc.hand.RhActionList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public abstract class AbstractScriptBuilder<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractScriptBuilder.class);

	public String buildScript(RhActionList actions) {
		StringBuilder script = new StringBuilder();
		
		try (CSVPrinter printer = CSVFormat.DEFAULT.print(script)) {
			for (T action : getActionsList(actions)) {
				BaseBuilder<?, T> builder = getBuilder(action);
				if (builder == null)
					continue;
				builder.buildScript(printer, action);
			}
			
		} catch (IOException e) {
			logger.error("An error occurred while script building", e);
		}

		return script.toString();
	}
	
	protected abstract List<T> getActionsList(RhActionList actions);
	protected abstract BaseBuilder<?, T> getBuilder(T action);
}
