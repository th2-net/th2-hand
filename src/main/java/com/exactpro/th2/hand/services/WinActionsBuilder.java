/*
 *  Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinCheckElement;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinClick;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinClickContextMenu;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinGetActiveWindow;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinGetElementAttribute;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinGetWindow;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinLocator;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinOpen;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinScrollUsingText;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinSearchElement;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinSendText;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinToggleCheckBox;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinWait;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.WinWaitForAttribute;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WinActionsBuilder {
	private static final String WINDOWNAME = "#windowname", ACCESSIBILITY_ID = "#accessibilityid", ACTION = "#action";
	private static final Pair<String, Pair<String, String>> locatorPair
			= new ImmutablePair<>("#locator", new ImmutablePair<>("#matcher", "#matcherindex"));
	private static final Pair<String, Pair<String, String>> textLocatorPair
			= new ImmutablePair<>("#textLocator", new ImmutablePair<>("#textmatcher", "#textmatcherindex"));


	private static void addDefaults(String id, String execute, List<String> headers, List<String> values) {
		addIfNotEmpty("#id", id, headers, values);
		addIfNotEmpty("#execute", execute, headers, values);
	}

	private static void addLocator(List<WinLocator> winLocators, List<String> headers, List<String> values) {
		addLocator(winLocators, headers, values, locatorPair);
	}

	private static void addLocator(List<WinLocator> winLocators, List<String> headers, List<String> values,
	                               Pair<String, Pair<String, String>> keys) {
		if (winLocators == null || winLocators.isEmpty())
			return;

		int count = 1;
		for (WinLocator winLocator : winLocators) {

			Pair<String, String> matcherPair = keys.getValue();
			String paramSuffix = count == 1 ? "" : String.valueOf(count);

			headers.add(keys.getKey() + paramSuffix);
			values.add(winLocator.getLocator());

			headers.add(matcherPair.getKey() + paramSuffix);
			values.add(winLocator.getMatcher());

			if (winLocator.hasMatcherIndex()) {
				headers.add(matcherPair.getValue() + paramSuffix);
				values.add(String.valueOf(winLocator.getMatcherIndex().getValue()));
			}

			++count;
		}
	}

	public static void addClick(CSVPrinter printer, WinClick clickAction) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();
		
		headers.add(ACTION);
		values.add("Click");

		addDefaults(clickAction.getId(), clickAction.getExecute(), headers, values);
		addLocator(clickAction.getLocatorsList(), headers, values);
		
		WinClick.Button button = clickAction.getButton();
		if (button != WinClick.Button.UNRECOGNIZED) {
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

		if (clickAction.getAttachedBorder() != WinClick.AttachedBorder.NONE) {
			headers.add("#attachedBorder");
			values.add(String.valueOf(clickAction.getAttachedBorder()).toLowerCase());
		}
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addOpen(CSVPrinter printer, WinOpen openAction) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();
		
		headers.add(ACTION);
		values.add("Open");

		addDefaults(openAction.getId(), openAction.getExecute(), headers, values);

		addIfNotEmpty("#workdir", openAction.getWorkDir(), headers, values);
		addIfNotEmpty("#execfile", openAction.getAppFile(), headers, values);
		addIfNotEmpty("#appArgs", openAction.getAppArgs(), headers, values);
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}


	public static void addSendText(CSVPrinter printer, WinSendText sendTextAction) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();
		
		headers.add(ACTION);
		values.add("SendText");

		addDefaults(sendTextAction.getId(), sendTextAction.getExecute(), headers, values);
		addLocator(sendTextAction.getLocatorsList(), headers, values);
		
		addIfNotEmpty("#text", sendTextAction.getText(), headers, values);
		addIfNotEmpty("#clearBefore", sendTextAction.getClearBefore(), headers, values);
		addIfNotEmpty("#directSend", sendTextAction.getIsDirectText(), headers, values);

		if (sendTextAction.getNonExperimental()) {
			headers.add("#isExperimental");
			values.add(Boolean.FALSE.toString());
		}
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addActiveWindow(CSVPrinter printer, WinGetActiveWindow getActiveWindow) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();
		
		headers.add(ACTION);
		values.add("GetActiveWindow");

		addDefaults(getActiveWindow.getId(), getActiveWindow.getExecute(), headers, values);

		String windowName = getActiveWindow.getWindowName();
		if (StringUtils.isNotEmpty(windowName))
		{
			headers.add(WINDOWNAME);
			values.add(windowName);
		}
		else
		{
			headers.add(ACCESSIBILITY_ID);
			values.add(getActiveWindow.getAccessibilityId());
		}
		
		if (getActiveWindow.getMaxTimeout() > 0) {
			headers.add("#maxTimeout");
			values.add(String.valueOf(getActiveWindow.getMaxTimeout()));
		}

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addGetWindow(CSVPrinter printer, WinGetWindow getWindow) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add("GetWindow");

		addDefaults(getWindow.getId(), getWindow.getExecute(), headers, values);

		String windowName = getWindow.getWindowName();
		if (StringUtils.isNotEmpty(windowName))
		{
			headers.add(WINDOWNAME);
			values.add(windowName);	
		}
		else
		{
			headers.add(ACCESSIBILITY_ID);
			values.add(getWindow.getAccessibilityId());
		} 
		
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addGetElementAttribute(CSVPrinter printer, WinGetElementAttribute getElementAttribute) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();
		
		headers.add(ACTION);
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
		
		headers.add(ACTION);
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
		
		headers.add(ACTION);
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
		
		headers.add(ACTION);
		values.add("ClickContextMenu");

		addDefaults(clickContextMenu.getId(), clickContextMenu.getExecute(), headers, values);
		addLocator(clickContextMenu.getLocatorsList(), headers, values);
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addCheckElement(CSVPrinter printer, WinCheckElement checkElement) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add("CheckElement");

		addDefaults(checkElement.getId(), checkElement.getExecute(), headers, values);
		addLocator(checkElement.getLocatorsList(), headers, values);

		headers.add("#saveElement");
		values.add(Boolean.toString(checkElement.getSaveElement()));
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addSearchElement(CSVPrinter printer, WinSearchElement searchElement) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add("SearchElement");

		addDefaults(searchElement.getId(), searchElement.getExecute(), headers, values);
		addLocator(searchElement.getLocatorsList(), headers, values);
		
		if (searchElement.getNonExperimental()) {
			headers.add("#isExperimental");
			values.add(Boolean.FALSE.toString());
		}
		
		headers.add("#multipleElements");
		values.add(Boolean.toString(searchElement.getMultipleElements()));

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addWaitForAttribute(CSVPrinter printer, WinWaitForAttribute waitForAttribute) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
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

	public static void addScrollUsingText(CSVPrinter printer, WinScrollUsingText scrollUsingText) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add("ScrollUsingText");

		addDefaults(scrollUsingText.getId(), scrollUsingText.getExecute(), headers, values);
		addLocator(scrollUsingText.getLocatorsList(), headers, values);
		addLocator(scrollUsingText.getTextLocatorsList(), headers, values, textLocatorPair);

		addIfNotEmpty("#textToSend", scrollUsingText.getTextToSend(), headers, values);
		addIfNotEmpty("#maxIterations", scrollUsingText.getMaxIterations(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addGetDataFromClipboard(CSVPrinter printer, RhWinActionsMessages.WinGetDataFromClipboard scrollUsingText) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add("GetDataFromClipboard");

		addDefaults(scrollUsingText.getId(), scrollUsingText.getExecute(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addWinTableSearch(CSVPrinter printer, RhWinActionsMessages.WinTableSearch winTableSearch) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add("TableSearch");

		addDefaults(winTableSearch.getId(), winTableSearch.getExecute(), headers, values);
		addLocator(winTableSearch.getLocatorsList(), headers, values);

		headers.add("#filter");
		values.add(winTableSearch.getSearchParams());

		headers.add("#column");
		values.add(winTableSearch.getTargetColumn());

		addIfNotEmpty("#firstrowindex", winTableSearch.getFirstRowIndex(), headers, values);
		addIfNotEmpty("#index", winTableSearch.getColumnIndex(), headers, values);
		addIfNotEmpty("#rownameformat", winTableSearch.getRowNameFormat(), headers, values);
		addIfNotEmpty("#rowelementnameformat", winTableSearch.getRowElementNameFormat(), headers, values);
		addIfNotEmpty("#rowelementvalueformat", winTableSearch.getRowElementValueFormat(), headers, values);
		addIfNotEmpty("#saveresult", winTableSearch.getSaveResult(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addWaitForElement(CSVPrinter printer, RhWinActionsMessages.WinWaitForElement waitForElement) throws IOException {
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add("WaitForElement");

		addDefaults(waitForElement.getId(), waitForElement.getExecute(), headers, values);
		addLocator(waitForElement.getLocatorsList(), headers, values);

		addIfNotEmpty("#timeout", waitForElement.getTimeout(), headers, values);
		addIfNotEmpty("#fromRoot", waitForElement.getFromRoot(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addMaximizeMainWindow(CSVPrinter printer, RhWinActionsMessages.MaximizeMainWindow maximizeMainWindow) throws IOException {
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add("MaximizeMainWindow");

		addDefaults(maximizeMainWindow.getId(), maximizeMainWindow.getExecute(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addGetScreenshot(CSVPrinter printer, RhWinActionsMessages.WinGetScreenshot getScreenshot) throws IOException {
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add("GetScreenshot");

		addDefaults(getScreenshot.getId(), getScreenshot.getExecute(), headers, values);
		if (getScreenshot.getLocatorsCount() != 0) {
			addLocator(getScreenshot.getLocatorsList(), headers, values);
		}

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
