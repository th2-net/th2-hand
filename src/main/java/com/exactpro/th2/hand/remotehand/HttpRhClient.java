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

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * Implementation of RhClient to send requests via HTTP
 */
public class HttpRhClient extends RhClient
{
	protected final CloseableHttpClient httpClient;
	protected String sessionUrl;
	protected final String baseUrl;
	
	public HttpRhClient(CloseableHttpClient httpClient, String host)
	{
		super();
		this.httpClient = httpClient;
		this.baseUrl = host.endsWith("/") ? host : host + '/';
	}
	
	public String getBaseUrl()
	{
		return baseUrl;
	}

	public String getSessionUrl()
	{
		return sessionUrl;
	}
	
	public CloseableHttpClient getHttpClient()
	{
		return httpClient;
	}
	
	
	@Override
	protected RhResponse sendLogon() throws IOException
	{
		return executeRequest(new HttpGet(baseUrl + "login"));
	}
	
	@Override
	protected RhResponse sendScript(String script) throws IOException
	{
		HttpPost post = new HttpPost(sessionUrl);
		post.setEntity(new StringEntity(script));
		
		return executeRequest(post);
	}

	@Override
	protected RhResponse queryStatus() throws IOException
	{
		return executeRequest(new HttpGet(sessionUrl));
	}
	
	@Override
	protected RhResponse downloadFile(String type, String id) throws IOException
	{
		return executeRequest(new HttpGet(baseUrl + "download?type=" + type + "&id=" + id));
	}
	
	@Override
	protected RhResponse sendLogout() throws IOException
	{
		RhResponse response = closeSession();
		httpClient.close();
		return response;
	}
	
	@Override
	protected RhResponse closeSession() throws IOException
	{
		return convertResponse(httpClient.execute(new HttpDelete(sessionUrl)));
	}
	
	
	@Override
	public RhResponse sendFile(File f, String path) throws IOException
	{
		HttpPost post = new HttpPost(sessionUrl);
		post.addHeader("Transfer-filename", path);
		post.setEntity(new FileEntity(f, ContentType.APPLICATION_OCTET_STREAM));
		return executeRequest(post);
	}
	
	@Override
	public void logon() throws IOException, RhException
	{
		super.logon();
		this.sessionUrl = baseUrl + sessionId;
	}
	
	
	private RhResponse convertResponse(HttpResponse response) throws ParseException, IOException
	{
		Header[] header = response.getAllHeaders();
		boolean byteArrayContent = false;
		for (Header header1 : header) {
			if ("Content-Type".equalsIgnoreCase(header1.getName())) {
				String value = header1.getValue();
				byteArrayContent = "image/png".equalsIgnoreCase(value) || "application/octet-stream".equalsIgnoreCase(value);
				break;
			}
		}
		
		if (byteArrayContent) {
			byte[] bytes = EntityUtils.toByteArray(response.getEntity());
			EntityUtils.consume(response.getEntity());
			return new RhResponse(response.getStatusLine().getStatusCode(), bytes);
		} else {
			String result = EntityUtils.toString(response.getEntity());
			EntityUtils.consume(response.getEntity());
			return new RhResponse(response.getStatusLine().getStatusCode(), result);
		}
	}
	
	private RhResponse executeRequest(HttpUriRequest request) throws IOException
	{
		return convertResponse(httpClient.execute(request));
	}
}
