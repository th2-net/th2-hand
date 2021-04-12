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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventPayloadTable {
	private final List<Map<String, String>> rows;

	public EventPayloadTable(Map<String, String> singleRow)
	{
		this(singleRow, true);
	}

	public EventPayloadTable(Map<String, String> singleRow, boolean horizontal)
	{
		if (horizontal) {
			this.rows = new ArrayList<>(1);
			this.rows.add(singleRow);
		} else {
			this.rows = singleRow.entrySet().stream().map(ent -> {
				Map<String, String> map = new LinkedHashMap<>();
				map.put("Name", ent.getKey());
				map.put("Value", ent.getValue());
				return map;
			}).collect(Collectors.toList());
		}

	}

	public String getType()
	{
		return "table";
	}

	public List<Map<String, String>> getRows()
	{
		return rows;
	}
}
