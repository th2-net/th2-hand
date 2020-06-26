/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
 * https://www.exactpro.com
 * Build Software to Test Software
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
 ******************************************************************************/

package com.exactpro.th2.hand;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Config
{
	public static final String GRPC_PORT_ARG = "GRPC_PORT";
	public static final int DEFAULT_GRPC_PORT = 8080;
	public static final String RH_URL_ARG = "RH_URL";
	public static final String DEFAULT_RH_URL = "http://localhost:8008";
	public static final String TH2_HAND_DIR_ARG = "TH2_HAND_DIR";

	protected final int grpcPort;
	protected final String rhUrl;
	protected final Path cfgDir;
	protected final Path scriptsDir;
			
	public Config()
	{
		grpcPort = getEnvTh2GrpcPort();
		rhUrl = getEnvTh2RhUrl();
		cfgDir = getEnvTh2CfgDir();
		scriptsDir = cfgDir.resolve("scripts");
	}

	protected Path getEnvTh2CfgDir()
	{
		String dir = ObjectUtils.defaultIfNull(System.getenv(TH2_HAND_DIR_ARG), "user.dir");
		return Paths.get(dir).resolve("cfg");
	}

	protected String getEnvTh2RhUrl()
	{
		return ObjectUtils.defaultIfNull(System.getenv(RH_URL_ARG), DEFAULT_RH_URL);
	}

	protected int getEnvTh2GrpcPort()
	{
		return NumberUtils.toInt(System.getenv(GRPC_PORT_ARG), DEFAULT_GRPC_PORT);		
	}

	public int getGrpcPort()
	{
		return grpcPort;
	}

	public String getRhUrl()
	{
		return rhUrl;
	}

	public Path getCfgDir()
	{
		return cfgDir;
	}

	public Path getScriptsDir()
	{
		return scriptsDir;
	}
}
