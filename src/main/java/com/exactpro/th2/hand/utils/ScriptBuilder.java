/*
 * Copyright (c) 2020-2021, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 */

package com.exactpro.th2.hand.utils;

import com.exactpro.th2.act.grpc.hand.RhAction;
import com.exactpro.th2.act.grpc.hand.RhActionsList;
import com.exactpro.th2.act.grpc.hand.rhactions.RhActionsMessages;
import com.exactpro.th2.act.grpc.hand.rhactions.RhWinActionsMessages;
import com.exactpro.th2.hand.services.WinActionsBuilder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ScriptBuilder {
	private static final Logger logger = LoggerFactory.getLogger(ScriptBuilder.class);


	public String buildScript(RhActionsList actionsList, String sessionId) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		List<RhAction> actionList = actionsList.getRhActionList();
		try (CSVPrinter printer = CSVFormat.DEFAULT.print(sb))
		{
			for (RhAction action : actionList)
			{
				switch (action.getActionCase())
				{
					case OPEN:
						RhActionsMessages.Open open = action.getOpen();
						addOpen(printer, open);
						break;
					case SENDKEYS:
						RhActionsMessages.SendKeys sendKeys = action.getSendKeys();
						addSendKeys(printer, sendKeys);
						break;
					case FINDELEMENT:
						RhActionsMessages.FindElement findElement = action.getFindElement();
						addFindElement(printer, findElement);
						break;
					case CLICK:
						RhActionsMessages.Click click = action.getClick();
						addClick(printer, click);
						break;
					case SENDKEYSTOACTIVE:
						RhActionsMessages.SendKeysToActive sendKeysToActive = action.getSendKeysToActive();
						addSendKeysToActive(printer, sendKeysToActive);
						break;
					case SWITCHWINDOW:
						RhActionsMessages.SwitchWindow switchWindow = action.getSwitchWindow();
						addSwitchWindow(printer, switchWindow);
						break;
					case GETELEMENTVALUE:
						RhActionsMessages.GetElementValue getElementValue = action.getGetElementValue();
						addGetElementValue(printer, getElementValue);
						break;
					case SCROLLDIVUNTIL:
						RhActionsMessages.ScrollDivUntil scrollDivUntil = action.getScrollDivUntil();
						addScrollDivUntil(printer, scrollDivUntil);
						break;
					case WAIT:
						RhActionsMessages.Wait wait = action.getWait();
						addWait(printer, wait);
						break;
					case GETELEMENTINNERHTML:
						RhActionsMessages.GetElementInnerHtml getElementInnerHtml = action.getGetElementInnerHtml();
						addGetElementInnerHtml(printer, getElementInnerHtml);
						break;
					case GETSCREENSHOT:
						RhActionsMessages.GetScreenshot screenshot = action.getGetScreenshot();
						addGetScreenshot(printer, screenshot);
						break;
					case GETELEMENTSCREENSHOT:
						RhActionsMessages.GetElementScreenshot elementScreenshot = action.getGetElementScreenshot();
						addGetElementScreenshot(printer, elementScreenshot);
						break;
					case EXECUTEJS:
						addExecuteJS(printer, action.getExecuteJs());
						break;
					case EXECUTEJSELEMENT:
						addExecuteJSElement(printer, action.getExecuteJsElement());
						break;
					case GETELEMENTATTRIBUTE:
						addGetElementAttribute(printer, action.getGetElementAttribute());
						break;

					// win actions
					case WINOPEN:
						RhWinActionsMessages.WinOpen winOpen = action.getWinOpen();
						WinActionsBuilder.addOpen(printer, winOpen);
						break;
					case WINCLICK:
						RhWinActionsMessages.WinClick winClick = action.getWinClick();
						WinActionsBuilder.addClick(printer, winClick);
						break;
					case WINSENDTEXT:
						RhWinActionsMessages.WinSendText winSendText = action.getWinSendText();
						WinActionsBuilder.addSendText(printer, winSendText);
						break;
					case WINGETACTIVEWINDOW:
						RhWinActionsMessages.WinGetActiveWindow winGetActiveWindow = action.getWinGetActiveWindow();
						WinActionsBuilder.addActiveWindow(printer, winGetActiveWindow);
						break;
					case WINGETELEMENTATTRIBUTE:
						RhWinActionsMessages.WinGetElementAttribute winGetElementAttribute = action.getWinGetElementAttribute();
						WinActionsBuilder.addGetElementAttribute(printer, winGetElementAttribute);
						break;
					case WINWAIT:
						RhWinActionsMessages.WinWait winWait = action.getWinWait();
						WinActionsBuilder.addWait(printer, winWait);
						break;
					case WINTOGGLECHECKBOX:
						RhWinActionsMessages.WinToggleCheckBox winToggleCheckBox = action.getWinToggleCheckBox();
						WinActionsBuilder.addToggleCheckBox(printer, winToggleCheckBox);
						break;
					case WINCLICKCONTEXTMENU:
						RhWinActionsMessages.WinClickContextMenu winClickContextMenu = action.getWinClickContextMenu();
						WinActionsBuilder.addClickContextMenu(printer, winClickContextMenu);
						break;
					case WINCHECKELEMENT:
						RhWinActionsMessages.WinCheckElement winCheckElement = action.getWinCheckElement();
						WinActionsBuilder.addCheckElement(printer, winCheckElement);
						break;
					case WINGETWINDOW:
						RhWinActionsMessages.WinGetWindow winGetWindow = action.getWinGetWindow();
						WinActionsBuilder.addGetWindow(printer, winGetWindow);
						break;
					case WINSEARCHELEMENT:
						RhWinActionsMessages.WinSearchElement winSearchElement = action.getWinSearchElement();
						WinActionsBuilder.addSearchElement(printer, winSearchElement);
						break;
					case WINWAITFORATTRIBUTE:
						RhWinActionsMessages.WinWaitForAttribute winWaitForAttribute = action.getWinWaitForAttribute();
						WinActionsBuilder.addWaitForAttribute(printer, winWaitForAttribute);
						break;
					case WINSCROLLUSINGTEXT:
						RhWinActionsMessages.WinScrollUsingText scrollUsingText = action.getWinScrollUsingText();
						WinActionsBuilder.addScrollUsingText(printer, scrollUsingText);
						break;
					case WINGETDATAFROMCLIPBOARD:
						RhWinActionsMessages.WinGetDataFromClipboard dataFromClipboard = action.getWinGetDataFromClipboard();
						WinActionsBuilder.addGetDataFromClipboard(printer, dataFromClipboard);
						break;
					case WINTABLESEARCH:
						RhWinActionsMessages.WinTableSearch winTableSearch = action.getWinTableSearch();
						WinActionsBuilder.addWinTableSearch(printer, winTableSearch);
						break;
					case WINWAITFORELEMENT:
						RhWinActionsMessages.WinWaitForElement winWaitForElement = action.getWinWaitForElement();
						WinActionsBuilder.addWaitForElement(printer, winWaitForElement);
						break;
					case WINMAXIMIZEMAINWINDOW:
						WinActionsBuilder.addMaximizeMainWindow(printer, action.getWinMaximizeMainWindow());
						break;
					case WINGETSCREENSHOT:
						WinActionsBuilder.addGetScreenshot(printer, action.getWinGetScreenshot());
						break;
					default:
						logger.warn("Unsupported action: " + action.getActionCase());
						break;
				}
			}
		}
		return sb.toString();
	}


	private void addWait(CSVPrinter printer, RhActionsMessages.Wait wait) throws IOException
	{
		printer.print("#action");
		printer.print("#seconds");
		printer.println();
		printer.print("Wait");
		printer.print(wait.getSeconds());
		printer.println();
	}

	private void addOpen(CSVPrinter printer, RhActionsMessages.Open open) throws IOException
	{
		// #action,#url
		printer.print("#action");
		printer.print("#url");
		printer.println();
		printer.print("Open");
		printer.print(open.getUrl());
		printer.println();
	}

	private void addSendKeys(CSVPrinter printer, RhActionsMessages.SendKeys sendKeys) throws IOException
	{
		// #action,#wait,#locator,#matcher,#text,#wait2,#locator2,#matcher2,#text2,#canBeDisabled,#clear,#checkInput,#needClick
		printer.print("#action");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.print("#text");
		printer.print("#wait2");
		printer.print("#locator2");
		printer.print("#matcher2");
		printer.print("#text2");
		printer.print("#canBeDisabled");
		printer.print("#clear");
		printer.print("#checkInput");
		printer.print("#needClick");
		printer.println();

		printer.print("SendKeys");
		printer.print(String.valueOf(sendKeys.getWait()));
		printer.print(readLocator(sendKeys.getLocator()));
		printer.print(sendKeys.getMatcher());
		printer.print(sendKeys.getText());
		printer.print(String.valueOf(sendKeys.getWait2()));
		printer.print(readLocator(sendKeys.getLocator2()));
		printer.print(sendKeys.getMatcher2());
		printer.print(sendKeys.getText2());
		printer.print(String.valueOf(sendKeys.getCanBeDisabled()));
		printer.print(String.valueOf(sendKeys.getClear()));
		printer.print(String.valueOf(sendKeys.getCheckInput()));
		printer.print(String.valueOf(sendKeys.getNeedClick()));
		printer.println();
	}

	private String readLocator(RhActionsMessages.Locator locator)
	{
		if (locator == null)
			return null;

		switch (locator)
		{
			case CSS_SELECTOR:
				return "cssSelector";
			case TAG_NAME:
				return "tagName";
			case ID:
				return "id";
			case XPATH:
				return "xpath";
			default:
				logger.warn("Unsupported locator: " + locator);
		}
		return null;
	}

	private void addFindElement(CSVPrinter printer, RhActionsMessages.FindElement findElement) throws IOException
	{
		// #action,#wait,#locator,#matcher,#id
		printer.print("#action");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.print("#id");
		printer.println();

		printer.print("FindElement");
		printer.print(String.valueOf(findElement.getWait()));
		printer.print(readLocator(findElement.getLocator()));
		printer.print(findElement.getMatcher());
		printer.print(findElement.getId());
		printer.println();
	}

	private void addClick(CSVPrinter printer, RhActionsMessages.Click click) throws IOException
	{
		// #action,#wait,#locator,#matcher,#button,#xOffset,#yOffset,#modifiers
		printer.print("#action");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.print("#button");
		printer.print("#xOffset");
		printer.print("#yOffset");
		printer.print("#modifiers");
		printer.println();

		printer.print("Click");
		printer.print(String.valueOf(click.getWait()));
		printer.print(readLocator(click.getLocator()));
		printer.print(click.getMatcher());
		printer.print(click.getButton().name().toLowerCase());
		printer.print(String.valueOf(click.getXOffset()));
		printer.print(String.valueOf(click.getYOffset()));
		printer.print(readModifiers(click.getModifiers()));
		printer.println();
	}

	private String readModifiers(RhActionsMessages.Click.ModifiersList modifiersList)
	{
		return modifiersList.getModifierList().stream().map(modifier -> modifier.name().toLowerCase())
				.collect(Collectors.joining(","));
	}

	private void addSendKeysToActive(CSVPrinter printer, RhActionsMessages.SendKeysToActive sendKeysToActive) throws IOException
	{
		// #action,#text,#text2
		printer.print("#action");
		printer.print("#text");
		printer.print("#text2");
		printer.println();

		printer.print("SendKeysToActive");
		printer.print(sendKeysToActive.getText());
		printer.print(sendKeysToActive.getText2());
		printer.println();
	}

	private void addSwitchWindow(CSVPrinter printer, RhActionsMessages.SwitchWindow switchWindow) throws IOException
	{
		// #action,#window
		printer.print("#action");
		printer.print("#window");
		printer.println();

		printer.print("SwitchWindow");
		printer.print(String.valueOf(switchWindow.getWindow()));
		printer.println();
	}

	private void addScrollDivUntil(CSVPrinter printer, RhActionsMessages.ScrollDivUntil scrollDivUntil) throws IOException
	{
		// #action,#wait,#locator,#matcher,#wait2,#locator2,#matcher2,#searchdir,#searchoffset,#doscrollto,#yoffset
		printer.print("#action");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.print("#wait2");
		printer.print("#locator2");
		printer.print("#matcher2");
		printer.print("#searchdir");
		printer.print("#searchoffset");
		printer.print("#doscrollto");
		printer.print("#yoffset");
		printer.println();

		printer.print("ScrollDivUntil");
		printer.print(String.valueOf(scrollDivUntil.getWait()));
		printer.print(readLocator(scrollDivUntil.getLocator()));
		printer.print(scrollDivUntil.getMatcher());
		printer.print(String.valueOf(scrollDivUntil.getWait2()));
		printer.print(readLocator(scrollDivUntil.getLocator2()));
		printer.print(scrollDivUntil.getMatcher2());
		printer.print(scrollDivUntil.getSearchDir().name().toLowerCase());
		printer.print(String.valueOf(scrollDivUntil.getSearchOffset()));
		printer.print(String.valueOf(scrollDivUntil.getDoScrollTo()));
		printer.print(String.valueOf(scrollDivUntil.getYOffset()));
		printer.println();
	}

	private void addGetElementValue(CSVPrinter printer, RhActionsMessages.GetElementValue getElementValue) throws IOException
	{
		// #action,#wait,#locator,#matcher
		printer.print("#action");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.println();

		printer.print("GetElementValue");
		printer.print(String.valueOf(getElementValue.getWait()));
		printer.print(readLocator(getElementValue.getLocator()));
		printer.print(getElementValue.getMatcher());
		printer.println();
	}

	private void addGetElementInnerHtml(CSVPrinter printer, RhActionsMessages.GetElementInnerHtml getElementInnerHtml) throws IOException
	{
		// #action,#wait,#locator,#matcher
		printer.print("#action");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.println();

		printer.print("GetElementInnerHtml");
		printer.print(String.valueOf(getElementInnerHtml.getWait()));
		printer.print(readLocator(getElementInnerHtml.getLocator()));
		printer.print(getElementInnerHtml.getMatcher());
		printer.println();
	}

	private void addGetScreenshot(CSVPrinter printer, RhActionsMessages.GetScreenshot getScreenshot) throws IOException
	{
		// #action,#wait,#locator,#matcher
		printer.print("#action");
		printer.print("#name");
		printer.println();

		printer.print("GetScreenshot");
		printer.print(getScreenshot.getName());
		printer.println();
	}

	private void addExecuteJS(CSVPrinter printer, RhActionsMessages.ExecuteJS execJs) throws IOException
	{
		printer.print("#action");
		printer.print("#commands");
		printer.println();

		printer.print("ExecuteJS");
		printer.print(execJs.getCommands());
		printer.println();
	}

	private void addExecuteJSElement(CSVPrinter printer, RhActionsMessages.ExecuteJSElement execJs) throws IOException
	{
		printer.print("#action");
		printer.print("#commands");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.println();

		printer.print("ExecuteJsOnElement");
		printer.print(execJs.getCommands());
		printer.print(String.valueOf(execJs.getWait()));
		printer.print(readLocator(execJs.getLocator()));
		printer.print(execJs.getMatcher());
		printer.println();
	}

	private void addGetElementAttribute(CSVPrinter printer, RhActionsMessages.GetElementAttribute getElAttr) throws IOException
	{
		printer.print("#action");
		printer.print("#attribute");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.println();

		printer.print("GetElementAttribute");
		printer.print(getElAttr.getAttribute());
		printer.print(String.valueOf(getElAttr.getWait()));
		printer.print(readLocator(getElAttr.getLocator()));
		printer.print(getElAttr.getMatcher());
		printer.println();
	}

	private void addGetElementScreenshot(CSVPrinter printer, RhActionsMessages.GetElementScreenshot getScreenshot) throws IOException
	{
		// #action,#wait,#locator,#matcher
		printer.print("#action");
		printer.print("#name");
		printer.print("#wait");
		printer.print("#locator");
		printer.print("#matcher");
		printer.println();

		printer.print("GetElementScreenshot");
		printer.print(getScreenshot.getName());
		printer.print(String.valueOf(getScreenshot.getWait()));
		printer.print(readLocator(getScreenshot.getLocator()));
		printer.print(getScreenshot.getMatcher());
		printer.println();
	}
}
