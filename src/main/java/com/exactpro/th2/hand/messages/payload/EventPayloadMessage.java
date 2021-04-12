/*
 * Copyright (c) 2020-2021, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 */

package com.exactpro.th2.hand.messages.payload;

public class EventPayloadMessage {
	private final String data;

	public EventPayloadMessage(String data)
	{
		this.data = data;
	}

	public String getType()
	{
		return "message";
	}

	public String getData()
	{
		return data;
	}
}
