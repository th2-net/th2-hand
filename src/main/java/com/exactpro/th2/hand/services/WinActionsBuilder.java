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

import com.exactpro.remotehand.windows.actions.*;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages.*;
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
	private static final Pair<String, Pair<String, String>> toLocatorPair
			= new ImmutablePair<>("#tolocator", new ImmutablePair<>("#tomatcher", "#tomatcherindex"));
	private static final Pair<String, Pair<String, String>> actionLocatorPair
			= new ImmutablePair<>("#actionlocator", new ImmutablePair<>("#actionmatcher", "#actionmatcherindex"));


	private static void addDefaults(RhWinActionsMessages.BaseWinParams baseParams, List<String> headers, List<String> values) {
		addIfNotEmpty("#id", baseParams.getId(), headers, values);
		addIfNotEmpty("#execute", baseParams.getExecute(), headers, values);
		addIfNotEmpty("#fromRoot", String.valueOf(baseParams.getFromRoot()), headers, values);
		addIfNotEmpty("#isExperimental", String.valueOf(baseParams.getExperimentalDriver()), headers, values);
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
		values.add(Click.class.getSimpleName());

		addDefaults(clickAction.getBaseParams(), headers, values);
		addLocator(clickAction.getLocatorsList(), headers, values);
		
		WinClick.Button button = clickAction.getButton();
		if (button != WinClick.Button.UNRECOGNIZED) {
			headers.add("#button");
			values.add(button.name().toLowerCase());
		}

		addIfNotEmpty("#xOffset", clickAction.getXOffset(), headers, values);
		addIfNotEmpty("#yOffset", clickAction.getYOffset(), headers, values);
		addIfNotEmpty("#modifiers", clickAction.getModifiers(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addOpen(CSVPrinter printer, WinOpen openAction) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();
		
		headers.add(ACTION);
		values.add("Open");

		addDefaults(openAction.getBaseParams(), headers, values);

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

		addDefaults(sendTextAction.getBaseParams(), headers, values);
		addLocator(sendTextAction.getLocatorsList(), headers, values);
		
		addIfNotEmpty("#text", sendTextAction.getText(), headers, values);
		addIfNotEmpty("#clearBefore", sendTextAction.getClearBefore(), headers, values);
		addIfNotEmpty("#directSend", sendTextAction.getIsDirectText(), headers, values);
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addActiveWindow(CSVPrinter printer, WinGetActiveWindow getActiveWindow) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();
		
		headers.add(ACTION);
		values.add("GetActiveWindow");

		addDefaults(getActiveWindow.getBaseParams(), headers, values);

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

		addDefaults(getWindow.getBaseParams(), headers, values);

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

		addDefaults(getElementAttribute.getBaseParams(), headers, values);
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

		addDefaults(winWait.getBaseParams(), headers, values);
		
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

		addDefaults(toggleCheckBoxAction.getBaseParams(), headers, values);
		addLocator(toggleCheckBoxAction.getLocatorsList(), headers, values);
		
		headers.add("#expectedState");
		values.add(String.valueOf(toggleCheckBoxAction.getExpectedState()));
		
		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addCheckElement(CSVPrinter printer, WinCheckElement checkElement) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add("CheckElement");

		addDefaults(checkElement.getBaseParams(), headers, values);
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

		addDefaults(searchElement.getBaseParams(), headers, values);
		addLocator(searchElement.getLocatorsList(), headers, values);
		
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

		addDefaults(waitForAttribute.getBaseParams(), headers, values);
		addLocator(waitForAttribute.getLocatorsList(), headers, values);

		addIfNotEmpty("#attributeName", waitForAttribute.getAttributeName(), headers, values);
		addIfNotEmpty("#expectedValue", waitForAttribute.getExpectedValue(), headers, values);
		addIfNotEmpty("#maxTimeout", waitForAttribute.getMaxTimeout(), headers, values);
		addIfNotEmpty("#checkInterval", waitForAttribute.getCheckInterval(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addScrollUsingText(CSVPrinter printer, WinScrollUsingText scrollUsingText) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add("ScrollUsingText");

		addDefaults(scrollUsingText.getBaseParams(), headers, values);
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

		addDefaults(scrollUsingText.getBaseParams(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addWinTableSearch(CSVPrinter printer, RhWinActionsMessages.WinTableSearch winTableSearch) throws IOException
	{
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add("TableSearch");

		addDefaults(winTableSearch.getBaseParams(), headers, values);
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

		addDefaults(waitForElement.getBaseParams(), headers, values);
		addLocator(waitForElement.getLocatorsList(), headers, values);

		addIfNotEmpty("#timeout", waitForElement.getTimeout(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addMaximizeMainWindow(CSVPrinter printer, RhWinActionsMessages.MaximizeMainWindow maximizeMainWindow) throws IOException {
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add("MaximizeMainWindow");

		addDefaults(maximizeMainWindow.getBaseParams(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addGetScreenshot(CSVPrinter printer, RhWinActionsMessages.WinGetScreenshot getScreenshot) throws IOException {
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add("GetScreenshot");

		addDefaults(getScreenshot.getBaseParams(), headers, values);
		if (getScreenshot.getLocatorsCount() != 0) {
			addLocator(getScreenshot.getLocatorsList(), headers, values);
		}

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addGetElementColor(CSVPrinter printer, RhWinActionsMessages.WinGetElementColor getElementColor) throws IOException {
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add(GetElementColor.class.getSimpleName());

		addDefaults(getElementColor.getBaseParams(), headers, values);
		addLocator(getElementColor.getLocatorsList(), headers, values);

		addIfNotEmpty("#xOffset", getElementColor.getXOffset(), headers, values);
		addIfNotEmpty("#yOffset", getElementColor.getXOffset(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addDragAndDrop(CSVPrinter printer, RhWinActionsMessages.WinDragAndDrop dragAndDrop) throws IOException {
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add(DragAndDropElement.class.getSimpleName());

		addDefaults(dragAndDrop.getBaseParams(), headers, values);
		addLocator(dragAndDrop.getFromLocatorsList(), headers, values);
		addLocator(dragAndDrop.getToLocatorsList(), headers, values, toLocatorPair);

		addIfNotEmpty("#fromoffsetx", dragAndDrop.getFromOffsetX(), headers, values);
		addIfNotEmpty("#fromoffsety", dragAndDrop.getFromOffsetY(), headers, values);

		addIfNotEmpty("#tooffsetx", dragAndDrop.getToOffsetX(), headers, values);
		addIfNotEmpty("#tooffsety", dragAndDrop.getToOffsetY(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addScrollToElement(CSVPrinter printer, RhWinActionsMessages.WinScrollToElement scrollToElement) throws IOException {
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add(ScrollToElement.class.getSimpleName());

		addDefaults(scrollToElement.getBaseParams(), headers, values);
		addLocator(scrollToElement.getElementLocatorsList(), headers, values);
		addLocator(scrollToElement.getActionLocatorsList(), headers, values, actionLocatorPair);

		addIfNotEmpty("#clickoffsetx", scrollToElement.getClickOffsetX(), headers, values);
		addIfNotEmpty("#clickoffsety", scrollToElement.getClickOffsetY(), headers, values);

		headers.add("#scrolltype");
		values.add(convertScrollType(scrollToElement.getScrollType()));

		addIfNotEmpty("#maxiterations", scrollToElement.getMaxIterations(), headers, values);
		addIfNotEmpty("#shouldbedisplayed", scrollToElement.getIsElementShouldBeDisplayed(), headers, values);
		addIfNotEmpty("#elementindom", scrollToElement.getIsElementInTree(), headers, values);
		addIfNotEmpty("#textvalue", scrollToElement.getTextToSend(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addRestartDriver(CSVPrinter printer, RhWinActionsMessages.WinRestartDriver restartDriver) throws IOException {
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add(RestartDriver.class.getSimpleName());

		addDefaults(restartDriver.getBaseParams(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}

	public static void addColorsCollector(CSVPrinter printer, WinColorsCollector colorsCollector) throws IOException {
		List<String> headers = new ArrayList<>(), values = new ArrayList<>();

		headers.add(ACTION);
		values.add(ColorsCollector.class.getSimpleName());
		addDefaults(colorsCollector.getBaseParams(), headers, values);
		addLocator(colorsCollector.getLocatorsList(), headers, values);

		addIfNotEmpty("#startxoffset", colorsCollector.getStartXOffset(), headers, values);
		addIfNotEmpty("#startyoffset", colorsCollector.getStartYOffset(), headers, values);

		addIfNotEmpty("#endxoffset", colorsCollector.getEndXOffset(), headers, values);
		addIfNotEmpty("#endyoffset", colorsCollector.getEndYOffset(), headers, values);

		printer.printRecord(headers);
		printer.printRecord(values);
	}


	private static void addIfNotEmpty(String headerName, int value, List<String> headers, List<String> values) {
		addIfNotEmpty(headerName, String.valueOf(value), headers, values);
	}

	private static void addIfNotEmpty(String headerName, boolean value, List<String> headers, List<String> values) {
		addIfNotEmpty(headerName, String.valueOf(value), headers, values);
	}

	private static void addIfNotEmpty(String headerName, String value, List<String> headers, List<String> values) {
		if (StringUtils.isNotBlank(value)) {
			headers.add(headerName);
			values.add(value);
		}
	}

	private static String convertScrollType(WinScrollToElement.ScrollType scrollType) {
		switch (scrollType) {
			case CLICK:
				return "Click";
			case TEXT:
				return "Text";
			case UNRECOGNIZED:
			default:
				return StringUtils.EMPTY;
		}
	}
}
