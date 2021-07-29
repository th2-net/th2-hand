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

package com.exactpro.th2.hand.builders.script;

import com.exactpro.th2.hand.builders.script.web.WebActionsMapping;
import com.exactpro.th2.hand.builders.script.windows.WinActionsMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.function.Supplier;

public abstract class ActionsMapping<T extends Enum<T>, B extends BaseBuilder<?, ?>> {
	
	private static final Logger logger = LoggerFactory.getLogger(ActionsMapping.class);

	private final EnumMap<T , Supplier<B>> mapping;
	private final String actionType;

	public ActionsMapping(String actionType) {
		this.actionType = actionType;
		this.mapping = createMapping();
	}
	
	protected abstract EnumMap<T , Supplier<B>> createMapping();


	public B createInstance(T type) {
		Supplier<B> builderSupplier = mapping.get(type);
		if (builderSupplier == null) {
			logger.warn("Unsupported " + actionType + " action: " + type);
			return null;
		}

		return builderSupplier.get();
	}
	
	private static class WinActionMappingSingleton {
		private static final WinActionsMapping INSTANCE = new WinActionsMapping();
	}

	private static class WebActionMappingSingleton {
		private static final WebActionsMapping INSTANCE = new WebActionsMapping();
	}
	
	public static WinActionsMapping getWindowsMapping() {
		return WinActionMappingSingleton.INSTANCE;
	}

	public static WebActionsMapping getWebMapping() {
		return WebActionMappingSingleton.INSTANCE;
	}
}
