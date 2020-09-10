/*
 *  Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.exactpro.th2.hand.services;

import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinCheckElement;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinClick;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinClickContextMenu;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinGetActiveWindow;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinGetElementAttribute;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinGetWindow;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinLocator;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinOpen;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinSearchElement;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinSendText;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinToggleCheckBox;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinWait;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinWaitForAttribute;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WinActionsBuilder {

	private static void addDefaults(String id, String execute, List<String> headers, List<String> values) {
		addIfNotEmpty("#id", id, headers, values);
		addIfNotEmpty("#execute", execute, headers, values);
	}
	
	private static void addLocator(List<WinLocator> winLocators, List<String> headers, List<String> values) {
		if (winLocators == null || winLocators.isEmpty())
			return;
		
		int count = 1;
		for (WinLocator winLocator : winLocators) {
			
			if (count == 1) {
				headers.add("#locator");
				headers.add("#matcher");
			} else {
				headers.add("#locator" + count);
				headers.add("#matcher" + count);
			}
			
			++count;
			
			values.add(winLocator.getLocator());
			values.add(winLocator.getMatcher());
		}
	}

	public static void addClick(CSVPrinter printer, WinClick clickAction) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();
		
		headers.add("#action");
		values.add("Click");

		addDefaults(clickAction.getId(), clickAction.getExecute(), headers, values);
		addLocator(clickAction.getLocatorsList(), headers, values);
		
		WinClick.Button button = clickAction.getButton();
		if (button != null && button != WinClick.Button.UNRECOGNIZED) {
			headers.add("#button");
			values.add(button.name().toLowerCase());
		}
		
		if (clickAction.hasXOffset()) {
			headers.add("#xOffset");
			values.add(String.valueOf(clickAction.getXOffset().getValue()));
		}
		
		if (clickAction.hasYOffset()) {
			headers.add("#yOffset");
			values.add(String.valueOf(clickAction.getYOffset().getValue()));
		}
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addOpen(CSVPrinter printer, WinOpen openAction) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();
		
		headers.add("#action");
		values.add("Open");

		addDefaults(openAction.getId(), openAction.getExecute(), headers, values);
		
		headers.add("#workdir");
		values.add(openAction.getWorkDir());
		
		headers.add("#execfile");
		values.add(openAction.getAppFile());
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}


	public static void addSendText(CSVPrinter printer, WinSendText sendTextAction) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();
		
		headers.add("#action");
		values.add("SendText");

		addDefaults(sendTextAction.getId(), sendTextAction.getExecute(), headers, values);
		addLocator(sendTextAction.getLocatorsList(), headers, values);
		
		headers.add("#text");
		values.add(sendTextAction.getText());
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addActiveWindow(CSVPrinter printer, WinGetActiveWindow getActiveWindow) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();
		
		headers.add("#action");
		values.add("GetActiveWindow");

		addDefaults(getActiveWindow.getId(), getActiveWindow.getExecute(), headers, values);
		
		headers.add("#windowname");
		values.add(getActiveWindow.getWindowName());
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addGetWindow(CSVPrinter printer, WinGetWindow getWindow) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add("#action");
		values.add("GetWindow");

		addDefaults(getWindow.getId(), getWindow.getExecute(), headers, values);

		headers.add("#windowname");
		values.add(getWindow.getWindowName());

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addGetElementAttribute(CSVPrinter printer, WinGetElementAttribute getElementAttribute) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();
		
		headers.add("#action");
		values.add("GetElementAttribute");

		addDefaults(getElementAttribute.getId(), getElementAttribute.getExecute(), headers, values);
		addLocator(getElementAttribute.getLocatorsList(), headers, values);
		
		headers.add("#attributeName");
		values.add(getElementAttribute.getAttributeName());
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addWait(CSVPrinter printer, WinWait winWait) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();
		
		headers.add("#action");
		values.add("Wait");

		addDefaults(winWait.getId(), winWait.getExecute(), headers, values);
		
		headers.add("#millis");
		values.add(String.valueOf(winWait.getMillis()));
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addToggleCheckBox(CSVPrinter printer, WinToggleCheckBox toggleCheckBoxAction) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();
		
		headers.add("#action");
		values.add("ToggleCheckBox");

		addDefaults(toggleCheckBoxAction.getId(), toggleCheckBoxAction.getExecute(), headers, values);
		addLocator(toggleCheckBoxAction.getLocatorsList(), headers, values);
		
		headers.add("#expectedState");
		values.add(String.valueOf(toggleCheckBoxAction.getExpectedState()));
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addClickContextMenu(CSVPrinter printer, WinClickContextMenu clickContextMenu) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();
		
		headers.add("#action");
		values.add("ClickContextMenu");

		addDefaults(clickContextMenu.getId(), clickContextMenu.getExecute(), headers, values);
		addLocator(clickContextMenu.getLocatorsList(), headers, values);
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addCheckElement(CSVPrinter printer, WinCheckElement checkElement) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add("#action");
		values.add("CheckElement");

		addDefaults(checkElement.getId(), checkElement.getExecute(), headers, values);
		addLocator(checkElement.getLocatorsList(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addSearchElement(CSVPrinter printer, WinSearchElement searchElement) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add("#action");
		values.add("SearchElement");

		addDefaults(searchElement.getId(), searchElement.getExecute(), headers, values);
		addLocator(searchElement.getLocatorsList(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addWaitForAttribute(CSVPrinter printer, WinWaitForAttribute waitForAttribute) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add("#action");
		values.add("WaitForAttribute");

		addDefaults(waitForAttribute.getId(), waitForAttribute.getExecute(), headers, values);
		addLocator(waitForAttribute.getLocatorsList(), headers, values);

		addIfNotEmpty("#attributeName", waitForAttribute.getAttributeName(), headers, values);
		addIfNotEmpty("#expectedValue", waitForAttribute.getExpectedValue(), headers, values);
		addIfNotEmpty("#maxTimeout", waitForAttribute.getMaxTimeout(), headers, values);
		addIfNotEmpty("#checkInterval", waitForAttribute.getCheckInterval(), headers, values);
		addIfNotEmpty("#fromRoot", waitForAttribute.getFromRoot(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}
	
	
	private static void addIfNotEmpty(String headerName, String value, List<String> headers, List<String> values) {
		if (StringUtils.isNotBlank(value)) {
			headers.add(headerName);
			values.add(value);
		}
	}
	
}
