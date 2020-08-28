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

package com.exactpro.th2.hand.remotehand;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RhUtils
{
	private static final Logger logger = LoggerFactory.getLogger(RhUtils.class);
	
	private static final String INCLUDE = "#include";
	private static final String FILE_PARAM = "file";
	private static final String SHUTDOWN_SCRIPT_HEADER = "// SHUTDOWN SCRIPT";
	public static final String LINE_SEPARATOR = "&#13";
	
	private static final int DICT_RESPONSE_TIMEOUT = 120; // 2 min
	private static final int SHUTDOWN_SCRIPT_RESPONSE_TIMEOUT = 120; // 2 min

	public static RhClient createRhConnection(String host) throws IOException, RhException
	{
		return createRhConnection(host, null);
	}
	
	public static RhClient createRhConnection(String host, String pathToDictionary) throws IOException, RhException
	{
		return createRhConnection(host, pathToDictionary, null);
	}

	public static RhClient createRhConnection(String host, String pathToDictionary, String pathToShutdownScript)
			throws IOException, RhException
	{
		logger.trace("Establishing connection with RemoteHand at '{}'...", host);
		RhClient client = new HttpRhClient(HttpClientBuilder.create().build(), host);
		loginToRh(client, pathToDictionary, pathToShutdownScript);
		return client;
	}
	
	
	private static void sendScriptFromFileToRh(RhClient rhClient, String scriptFilePath, int responseTimeout) throws RhException
	{
		sendScriptToRh(rhClient, scriptFilePath, responseTimeout, true);
	}

	private static void sendRawScriptToRh(RhClient rhClient, String rawScript, int responseTimeout) throws RhException
	{
		sendScriptToRh(rhClient, rawScript, responseTimeout, false);
	}

	private static void sendScriptToRh(RhClient rhClient, String script, int responseTimeout, boolean fromFile) throws RhException
	{
		try
		{
			rhClient.send(fromFile ? getScriptFromFile(script) : script);
			rhClient.waitAndGet(responseTimeout);
		}
		catch (IOException e)
		{
			String msg = "An error occurred while sending script to RH.";
			logger.error(msg, e);
			throw new RhException(msg, e);
		}
	}

	public static void sendDictionary(RhClient rhClient, String dictionaryPath) throws RhException
	{
		logger.trace("Sending dictionary to RemoteHand...");
		sendScriptFromFileToRh(rhClient, dictionaryPath, DICT_RESPONSE_TIMEOUT);
		logger.trace("The dictionary '{}' has been successfully sent to RemoteHand.", dictionaryPath);
	}

	public static void sendShutdownScript(RhClient rhClient, String shutdownScriptPath) throws RhException
	{
		logger.trace("Sending shutdown script to RemoteHand...");
		sendRawScriptToRh(rhClient, prepareShutdownScript(shutdownScriptPath, SHUTDOWN_SCRIPT_HEADER),
				SHUTDOWN_SCRIPT_RESPONSE_TIMEOUT);
		logger.trace("The shutdown script '{}' has been successfully sent to RemoteHand.", shutdownScriptPath);
	}

	private static String prepareShutdownScript(String shutdownScriptPath, String header) throws RhException
	{
		StringBuilder shutdownScript = new StringBuilder();
		try
		{
			shutdownScript.append(header).append(LINE_SEPARATOR).append(getScriptFromFile(shutdownScriptPath));
		}
		catch (IOException e)
		{
			String msg = "An error occurred while preparing shutdown script.";
			logger.error(msg, e);
			throw new RhException(msg, e);
		}
		return shutdownScript.toString();
	}

	public static String getScriptFromFile(String fileName) throws IOException, RhException
	{
		StringBuilder sb = new StringBuilder();
		loadScriptFromFile(sb, new File(fileName), Collections.emptyMap());
		return sb.toString();
	}

	public static void loadScriptFromFile(StringBuilder scriptBuilder, File file, Map<String, String> parameters)
			throws IOException, RhException
	{
		loadScriptFromFile(scriptBuilder, file, parameters, Collections.emptySet());
	}
	
	
	private static void loginToRh(RhClient client, String pathToDictionary, String pathToShutdownScript)
			throws IOException, RhException
	{
		client.logon();
		logger.trace("Connection with RemoteHand established, session ID: '{}'", client.getSessionId());
		
		if (pathToDictionary != null)
			sendDictionary(client, pathToDictionary);
		if (pathToShutdownScript != null)
			sendShutdownScript(client, pathToShutdownScript);
	}
	
	private static void loadScriptFromFile(StringBuilder scriptBuilder, File file, Map<String, String> parameters,
								   Set<File> appliedFiles) throws IOException, RhException
	{
		try (BufferedReader br = new BufferedReader(new FileReader(file)))
		{
			String line;
			while ((line = br.readLine()) != null)
			{
				if (line.startsWith(INCLUDE))
					includeFile(scriptBuilder, line, file.getParent(), new HashSet<File>(appliedFiles));
				else
				{
					if (line.contains(RhScriptCompiler.VAR_MARK))
						line = addIncludeParameters(line, parameters);
					scriptBuilder.append(line).append(LINE_SEPARATOR);
				}
			}
		}
	}

	private static void includeFile(StringBuilder scriptBuilder, String includeLine, String baseDir,
									Set<File> appliedFiles) throws IOException, RhException
	{
		Map<String, String> parameters = parseParameters(includeLine);
		String fileName = parameters.remove(FILE_PARAM);
		if (fileName == null)
			throw new RhException("%s statement is invalid. Please specify file name by parameter %s.", INCLUDE, FILE_PARAM);
		File file = new File(baseDir, fileName);
		if (!file.exists())
			throw new RhException("%s statement is invalid. Unable to find file '%s'", INCLUDE, fileName);
		if (!appliedFiles.add(file))
			throw new RhException("%s statement is invalid. Reference is already imported '%s'", INCLUDE, fileName);
		loadScriptFromFile(scriptBuilder, file, parameters, appliedFiles);
	}
	
	static Map<String, String> parseParameters(String includeLine)
	{
		if (StringUtils.isBlank(includeLine))
			return Collections.emptyMap();
		
		includeLine = includeLine.replace(INCLUDE, "");
		
		String[] keyValuePairs = includeLine.split(",\\s*");
		Map<String, String> parameters = new LinkedHashMap<String, String>(keyValuePairs.length);
		for (String keyValuePair : keyValuePairs)
		{
			int eqIndex = keyValuePair.indexOf('=');
			if (eqIndex != -1)
			{
				parameters.put(keyValuePair.substring(0, eqIndex).trim().replace("'", ""),
						StringUtils.substring(keyValuePair, eqIndex + 1).trim().replace("'", ""));
			}
			else
				parameters.put(keyValuePair, "");
		}
		return parameters;
	}
	
	public static String addIncludeParameters(String line, Map<String, String> parameters)
	{
		for (Map.Entry<String, String> e : parameters.entrySet())
		{
			String key = wrapByMarks(e.getKey());
			if (line.contains(key))
				line = line.replace(key, e.getValue());
		}
		return line;
	}
	
	private static String wrapByMarks(String s)
	{
		return RhScriptCompiler.VAR_MARK + s + RhScriptCompiler.VAR_MARK;
	}
}