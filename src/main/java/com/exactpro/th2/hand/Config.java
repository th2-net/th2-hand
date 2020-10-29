/*
 * Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.hand;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;

import com.exactpro.th2.hand.schema.CustomConfiguration;
import com.exactpro.th2.schema.factory.CommonFactory;

public class Config
{
	public static final String DEFAULT_SERVER_TARGET = "Default";
	public static final String DEFAULT_RH_URL = "http://localhost:8008";

    protected final CommonFactory factory;
	protected final int grpcPort;
	protected final Map<String, String> rhUrls;
	
	public Config(CommonFactory factory)
	{
	    this.factory = factory;
		this.grpcPort = doGetGrpcPort();
		this.rhUrls = doGetRhUrls();
	}

    protected int doGetGrpcPort() {
        return requireNonNull(
                requireNonNull(factory.getGrpcRouterConfiguration(), "Configuration for grpc router can not be null")
                        .getServerConfiguration(), "Configuration for grpc server can not be null")
                .getPort();
    }

	protected Map<String, String> doGetRhUrls()
	{
        CustomConfiguration customConfig = factory.getCustomConfiguration(CustomConfiguration.class);
        if (customConfig == null)
            return Collections.singletonMap(DEFAULT_SERVER_TARGET, DEFAULT_RH_URL);

        return customConfig.getRhUrls();
	}

    public CommonFactory getFactory() {
        return factory;
    }

    public int getGrpcPort()
	{
		return grpcPort;
	}

	public Map<String, String> getRhUrls()
	{
		return rhUrls;
	}

}
