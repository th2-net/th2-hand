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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to cover interaction with RemoteHand. Way to transfer data must be implemented in children
 */
public abstract class RhClient
{
	private static final Logger logger = LoggerFactory.getLogger(RhClient.class);
	
	protected static final String CR = "&#13";
	public static final int CODE_SUCCESS = 200,
			CODE_ERROR = 500;
	
	protected final RhScriptCompiler compiler;
	protected final JsonSerializer jsonSerializer;
	protected String sessionId;
	
	protected String usedBrowser;

	public RhClient()
	{
		compiler = new RhScriptCompiler();
		jsonSerializer = new JsonSerializer();
	}
	
	protected abstract RhResponse sendLogon() throws IOException;
	protected abstract RhResponse sendScript(String script) throws IOException;
	protected abstract RhResponse queryStatus() throws IOException;
	protected abstract RhResponse downloadFile(String type, String id) throws IOException;
	protected abstract RhResponse sendLogout() throws IOException;
	protected abstract RhResponse closeSession() throws IOException;
	public abstract RhResponse sendFile(File f, String path) throws IOException;
	
	
	public void logon() throws IOException, RhException
	{
		RhResponse response = sendLogon();
		if (isNotSuccess(response))
			throw new RhException("Could not login to RemoteHand: %s", response.getDataString());
		
		String responseMsg = response.getDataString();
		sessionId = null;
		usedBrowser = null;
		if (responseMsg.contains(";"))
		{
			String[] s = responseMsg.split(";");
			sessionId = s[0].replace("sessionId=", "");
			if (s.length > 1)
				usedBrowser = s[1].replace("browser=", "");
		}
		else 
			sessionId = responseMsg;
	}
	
	public String send(String message, boolean needToReconnect) throws IOException, RhException
	{
		if (needToReconnect)
		{
			closeSession();
			logon();
			logger.debug("Successfully reconnected to RH, new session ID: {}", sessionId);
		}
		
		RhResponse response = sendScript(message);
		String result = response.getDataString();
		logger.trace("Message sent. Received response: '{}'", result);

		if (isNotSuccess(response))
		{
			if (!needToReconnect && response.getCode() == 404
					&& result.equalsIgnoreCase("<h1>404 Not Found</h1>No context found for request"))
			{
				logger.warn("RH session '{}' was closed, need to re-login to RH, message will be sent via another session",
						sessionId);
				return sendWithReconnection(message);
			}
			else
			{
				throw new RhException("RH response contains error: %s", result);
			}
		}
		return result;
	}
	
	public String send(String message) throws IOException, RhException
	{
		return send(message, false);
	}
	
	public String sendWithReconnection(String message) throws IOException, RhException
	{
		return send(message, true);
	}
	
	
	public RhScriptResult get() throws RhException, IOException
	{
		RhResponse response = queryStatus();
		String rawResult = response.getDataString();
		logger.trace("Getting resource. Received response: '{}'", rawResult);
		
		if (isNotSuccess(response))
			throw new RhException("Error while executing script: '%s'", rawResult);
		
		try
		{
			return jsonSerializer.deserialize(rawResult, RhScriptResult.class);
		}
		catch (IOException e)
		{
			throw new RhException(e, "Unable to parse RH response. Probably you are using old version of RH. Raw response: [%s].", rawResult);
		}
	}
	
	public RhScriptResult waitAndGet(int seconds) throws RhException, IOException
	{
		long timeOut = System.currentTimeMillis() + seconds * 1000; // msecs

		RhScriptResult response;
		do
		{
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException ex)
			{
				logger.debug("Script execution for session '"+sessionId+"' has been interrupted", ex);
				RhScriptResult scriptResult = new RhScriptResult();
				scriptResult.setCode(RhResponseCode.EXECUTION_ERROR.getCode());
				scriptResult.setErrorMessage("Script execution has been interrupted");
				return scriptResult;
			}
			if (timeOut < System.currentTimeMillis())
				throw new RhException("Timeout after " + seconds + " seconds waiting for result.");

			response = get();
		}
		while (RhResponseCode.TOOL_BUSY.equals(RhResponseCode.byCode(response.getCode())));
		return response;
	}
	
	
	public String compileScript(String script, Map<String, String> variables) throws RhException
	{
		return compiler.compile(script, variables);
	}
	
	public RhScriptResult executeScript(String scriptFileName, Map<String, String> variables, int waitInSeconds, String templatesDirectoryPath)
			throws RhException, IOException
	{
		String script = RhUtils.getScriptFromFile(scriptFileName);
		return executeScriptFromString(script, variables, waitInSeconds, templatesDirectoryPath);
	}
	
	public RhScriptResult executeScript(String scriptFileName, Map<String, String> variables, int waitInSeconds) throws RhException, IOException
	{
		return executeScript(scriptFileName, variables, waitInSeconds, null);
	}
	
	public RhScriptResult executeScriptFromString(String script, Map<String, String> variables, int waitInSeconds, String templatesDirectoryPath)
			throws RhException, IOException
	{
		if (templatesDirectoryPath != null)
		{
			try
			{
				script =  applyTemplates(script, templatesDirectoryPath);
			}
			catch (Exception e)
			{
				logger.error("Unable to apply templates from directory: "+templatesDirectoryPath, e);
			}
		}
		
		String compiledScript = compileScript(script, variables);
		logger.debug("Complied script:\n{}", compiledScript);
		String response = send(compiledScript);
		logger.debug("Script has been sent. Response:\n{}", response);
		return waitAndGet(waitInSeconds);
	}
	
	public RhScriptResult executeScriptFromString(String script, Map<String, String> variables, int waitInSeconds) throws RhException, IOException
	{
		return executeScriptFromString(script, variables, waitInSeconds, null);
	}
	
	
	public byte[] downloadScreenshot(String screenshotId) throws RhException, IOException
	{
		return downloadFileFromRh("screenshot", screenshotId);
	}
	
	public byte[] downloadDownloadedFile(String filePath) throws RhException, IOException
	{
		return downloadFileFromRh("downloaded", filePath);
	}
	
	
	public void close() throws IOException
	{
		RhResponse response = sendLogout();
		logger.trace("Connection '" + sessionId + "' closed. Response: '" + response.getDataString() + "'");
	}

	
	public String getSessionId()
	{
		return sessionId;
	}
	
	public String getUsedBrowser()
	{
		return usedBrowser;
	}
	
	
	protected String applyTemplates(String script, String templatesDirectoryPath) throws Exception
	{
		// try to load templates from directory
		File templatesDir = new File(templatesDirectoryPath);
		if (!templatesDir.exists() || !templatesDir.isDirectory())
			throw new IllegalArgumentException("File by specified path does not exist or is not a directory");
		File[] files = templatesDir.listFiles();
		
		// create templates map
		Map<String, File> templates = new HashMap<String, File>();
		String name;
		for (File file : files)
		{
			if ((name = file.getName()).endsWith(".csv"))
				templates.put(name.substring(0, name.length() - 4), file);
		}
		if (logger.isTraceEnabled())
			logger.trace("List of templates: " + String.join(",", templates.keySet()));
		
		// check script for use of templates and apply them if needed
		String[] scriptLines = script.split(CR);
		List<String> newScriptLines = new ArrayList<String>();
		String cachedHeader = null;
		
		for (int i = 0, size = scriptLines.length; i < size; i++)
		{
			String line = scriptLines[i];
			if (line.isEmpty() || line.startsWith("//") || line.startsWith("#include"))
			{
				newScriptLines.add(line);
			}
			else if (line.startsWith("#"))
			{
				newScriptLines.add(line);
				cachedHeader = line;
			}
			else
			{
				String[] values = line.split(",");
				
				if (templates.containsKey(values[0].trim()))
				{
					if (cachedHeader == null)
						logger.warn("Line #"+(i+1)+" contains use of template, that does not have header, unable to apply it");
					
					int lastNewLineIndex = newScriptLines.size() - 1;
					if (newScriptLines.get(lastNewLineIndex).equals(cachedHeader))
						newScriptLines.remove(lastNewLineIndex);
					
					StringBuilder templateScriptBuilder = new StringBuilder();
					RhUtils.loadScriptFromFile(templateScriptBuilder, templates.get(values[0].trim()), Collections.<String, String>emptyMap());
					Map<String, String> variables = createVariables(cachedHeader.split(","), values);
					String templateScript = applyTemplates(compileScript(templateScriptBuilder.toString(), variables), templatesDirectoryPath);
					
					newScriptLines.addAll(Arrays.asList(templateScript.split(CR)));
				}
				else
				{
					newScriptLines.add(line);
				}
			}
		}
		
		// return new script string
		StringBuilder scriptBuilder = new StringBuilder();
		for (String line : newScriptLines)
		{
			scriptBuilder.append(line).append(CR);
		}
		
		return scriptBuilder.toString();
	}
	
	protected Map<String, String> createVariables(String[] header, String[] values)
	{
		Map<String, String> variables = new HashMap<String, String>();
		for (int i = 0, len = header.length, valLen = values.length; i < len; i++)
		{
			String key = header[i].trim();
			String value = (i < valLen) ? values[i].trim() : "";
			variables.put(key.substring(1, key.length()), value);
		}
		
		return variables;
	}

	protected byte[] downloadFileFromRh(String type, String id) throws RhException, IOException
	{
		RhResponse response = downloadFile(type, id);
		if (isNotSuccess(response))
		{
			String message = response.getDataString();
			throw new RhException("Unable to download " + type + ": " + message);
		}
		
		return response.getData();
	}

	
	private boolean isNotSuccess(RhResponse response)
	{
		return response.getCode() != CODE_SUCCESS;
	}
}
