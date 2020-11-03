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
import org.apache.commons.cli.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static com.exactprosystems.remotehand.RemoteHandStarter.CONFIG_PARAM;
import static com.exactprosystems.remotehand.RemoteHandStarter.ENV_VARS_PARAM;

public class Config
{
	public static final String GRPC_PORT_ARG = "GRPC_PORT";
	public static final String PROJECT_DIR_ARG = "PROJECT_DIR";
	public static final String PROJECT_NAME_ARG = "PROJECT_NAME";
	public static final int DEFAULT_GRPC_PORT = 8080;
	public static final String DRIVERS_MAPPING_ARG = "DRIVERS_MAPPING";

	private static final String DEFAULT_SERVER_TARGET = "Default";
	private static final String DEFAULT_DRIVER_URL = "http://localhost:4444";
	private static final String DEFAULT_DRIVER_TYPE = "web";

	protected final int grpcPort;
	protected final Map<String, Pair<String, String>> driversMapping;
	protected final Path rootDir;
	protected final CommandLine commandLine;

	protected final RabbitMqConfiguration rabbitMqConfiguration;

	public Config(String[] args) throws ParseException {
		this.grpcPort = getEnvTh2GrpcPort();
		this.driversMapping = getEnvTh2DriversMapping();
		this.rootDir = getRootDir();
		this.commandLine = getCommandLine(args);

		this.rabbitMqConfiguration = new RabbitMqConfiguration();
	}

	protected Path getRootDir()
	{
		return Paths.get(ObjectUtils.defaultIfNull(System.getenv(PROJECT_DIR_ARG), System.getProperty("user.dir")));
	}

	protected Map<String, Pair<String, String>> getEnvTh2DriversMapping()
	{
		Map<String, Pair<String, String>> targetServers = Utils.readDriversMappingFromString(System.getenv(DRIVERS_MAPPING_ARG));
		return targetServers.isEmpty()
				? Collections.singletonMap(DEFAULT_SERVER_TARGET, new ImmutablePair<>(DEFAULT_DRIVER_TYPE, DEFAULT_DRIVER_URL))
				: targetServers;
	}

	protected int getEnvTh2GrpcPort()
	{
		return NumberUtils.toInt(System.getenv(GRPC_PORT_ARG), DEFAULT_GRPC_PORT);
	}

	public int getGrpcPort()
	{
		return grpcPort;
	}

	public Map<String, Pair<String, String>> getDriversMapping()
	{
		return driversMapping;
	}

	public RabbitMqConfiguration getRabbitMqConfiguration()
	{
		return rabbitMqConfiguration;
	}

	public CommandLine getCommandLine() {
		return commandLine;
	}

	@SuppressWarnings("static-access")
	protected CommandLine getCommandLine(String[] args) throws ParseException {
		Options options = new Options();

		Option envVarsMode = OptionBuilder
				.isRequired(false)
				.withDescription("Enables environment variables. Example: to option SessionExpire (in ini file) " +
						"option will be RH_SESSION_EXPIRE")
				.create(ENV_VARS_PARAM);
		options.addOption(envVarsMode);

		Option configFileOption = OptionBuilder
				.isRequired(false)
				.withArgName("file")
				.hasArg()
				.withDescription("Specify configuration file")
				.create(CONFIG_PARAM);
		options.addOption(configFileOption);

		return new GnuParser().parse(options, args);
	}
}
