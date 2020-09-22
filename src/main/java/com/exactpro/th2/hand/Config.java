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

import com.exactpro.th2.hand.utils.Utils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

public class Config
{
	public static final String GRPC_PORT_ARG = "GRPC_PORT";
	public static final String PROJECT_DIR_ARG = "PROJECT_DIR";
	public static final String PROJECT_NAME_ARG = "PROJECT_NAME";
	public static final int DEFAULT_GRPC_PORT = 8080;
	public static final String RH_URLS_ARG = "RH_URLS";
	public static final String DEFAULT_SERVER_TARGET = "Default";
	public static final String DEFAULT_RH_URL = "http://localhost:8008";

	protected final int grpcPort;
	protected final Map<String, String> rhUrls;
	protected final Path rootDir;
	
	protected final RabbitMqConfiguration rabbitMqConfiguration;
	
	public Config()
	{
		this.grpcPort = getEnvTh2GrpcPort();
		this.rhUrls = getEnvTh2RhUrls();
		this.rootDir = getRootDir();
		
		this.rabbitMqConfiguration = new RabbitMqConfiguration();
	}

	protected Path getRootDir()
	{
		return Paths.get(ObjectUtils.defaultIfNull(System.getenv(PROJECT_DIR_ARG), System.getProperty("user.dir")));
	}

	protected Map<String, String> getEnvTh2RhUrls()
	{
		Map<String, String> rhUrls = Utils.readMapFromString(System.getenv(RH_URLS_ARG));
		return rhUrls.isEmpty() ? Collections.singletonMap(DEFAULT_SERVER_TARGET, DEFAULT_RH_URL) : rhUrls;
	}

	protected int getEnvTh2GrpcPort()
	{
		return NumberUtils.toInt(System.getenv(GRPC_PORT_ARG), DEFAULT_GRPC_PORT);
	}

	public int getGrpcPort()
	{
		return grpcPort;
	}

	public Map<String, String> getRhUrls()
	{
		return rhUrls;
	}

	public RabbitMqConfiguration getRabbitMqConfiguration()
	{
		return rabbitMqConfiguration;
	}
}
