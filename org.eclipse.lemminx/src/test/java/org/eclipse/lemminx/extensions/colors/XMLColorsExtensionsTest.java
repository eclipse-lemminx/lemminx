/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.colors;

import static org.eclipse.lemminx.XMLAssert.colorInfo;
import static org.eclipse.lemminx.XMLAssert.colorPres;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.eclipse.lemminx.XMLAssert.testColorInformationFor;
import static org.eclipse.lemminx.XMLAssert.testColorPresentationFor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.colors.settings.XMLColorExpression;
import org.eclipse.lemminx.extensions.colors.settings.XMLColors;
import org.eclipse.lemminx.extensions.colors.settings.XMLColorsSettings;
import org.eclipse.lsp4j.ColorInformation;
import org.junit.jupiter.api.Test;

/**
 * XML colors tests.
 *
 */
public class XMLColorsExtensionsTest {

	@Test
	public void colorOnText() throws BadLocationException {
		// Here color is done for color/text() only
		XMLColorsSettings settings = createXMLColorsSettings();
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
				+ "<resources>\r\n"
				+ "	<color name=\"opaque_red\">#f00</color>\r\n" // <-- here color/text() is colorized
				+ "	<color name=\"translucent_red\">#80ff0000</color>\r\n" // <-- here color/text() is colorized
				+ "	<color name=\"translucent_red\">BAD_COLOR</color>\r\n" // <-- here color/text() is not done because
																			// color syntax is not good
				+ "	<no-color name=\"opaque_red\">#f00</no-color>\r\n"
				+ "	<item color=\"red\" />\r\n"
				+ "	<item color=\"BAD_COLOR\" />\r\n"
				+ "</resources>";
		testColorInformationFor(xml, "file:///test/res/values/colors.xml", settings, //
				colorInfo(1, 0, 0, 1, r(2, 26, 2, 30)), //
				colorInfo(0.5019607843137255, 1, 0, 0, r(3, 31, 3, 40)));
	}

	@Test
	public void colorOnAttr() throws BadLocationException {
		// Here color is done for item/@color only
		XMLColorsSettings settings = createXMLColorsSettings();
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
				+ "<resources>\r\n"
				+ "	<color name=\"opaque_red\">#f00</color>\r\n"
				+ "	<color name=\"translucent_red\">#80ff0000</color>\r\n"
				+ "	<color name=\"translucent_red\">BAD_COLOR</color>\r\n"
				+ "	<item color=\"red\" />\r\n" // <-- here item/@color is colorized
				+ "	<item color=\"BAD_COLOR\" />\r\n" // <-- here item/@color is not done because color syntax is not
														// good
				+ "</resources>";
		testColorInformationFor(xml, "file:///test/colors-attr.xml", settings, //
				colorInfo(1, 0, 0, 1, r(5, 14, 5, 17)));
	}

	@Test
	public void colorOnAttrWithoutValue() throws BadLocationException {
		// Here color is done for item/@color only
		XMLColorsSettings settings = createXMLColorsSettings();
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
				+ "<resources>\r\n"
				+ "	<color name=\"opaque_red\">#f00</color>\r\n"
				+ "	<color name=\"translucent_red\">#80ff0000</color>\r\n"
				+ "	<item color= />\r\n" // <-- here item/@color has no attr value
				+ "	<item color=\"red\" />\r\n" // <-- here item/@color is colorized
				+ "	<item color=\"BAD_COLOR\" />\r\n" //
				+ "</resources>";
		testColorInformationFor(xml, "file:///test/colors-attr.xml", settings, //
				colorInfo(1, 0, 0, 1, r(5, 14, 5, 17)));
	}

	@Test
	public void colorAllPresentation() throws BadLocationException {
		// Here color is done for color/text() only
		XMLColorsSettings settings = createXMLColorsSettings();
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
				+ "<resources>\r\n"
				+ "	<item color=\"green\" />\r\n" // color name
				+ "	<item color=\"#86c156\" />\r\n" // hexa color
				+ "	<item color=\"rgb(86, 143, 193)\" />\r\n" // rgb color
				+ "	<item color=\"rgb(20,20,20,0.5)\" />\r\n" // rgba color
				+ "</resources>";
		testColorInformationFor(xml, "file:///test/colors-attr.xml", settings, //
				colorInfo(0, 0.5019607843137255, 0, 1, r(2, 14, 2, 19)), //
				colorInfo(0.5254901960784314, 0.7568627450980392, 0.33725490196078434, 1, r(3, 14, 3, 21)), //
				colorInfo(0.33725490196078434, 0.5607843137254902, 0.7568627450980392, 1, r(4, 14, 4, 31)), //
				colorInfo(0.0784313725490196, 0.0784313725490196, 0.0784313725490196, 0.5, r(5, 14, 5, 31)));
	}

	@Test
	public void colorPresentation() throws BadLocationException {

		ColorInformation green = colorInfo(0, 0.5019607843137255, 0, 1, r(2, 14, 2, 19));

		XMLColorsSettings settings = createXMLColorsSettings();
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
				+ "<resources>\r\n"
				+ "	<item color=\"green\" />\r\n" // color name
				+ "	<item color=\"#86c156\" />\r\n" // hexa color
				+ "	<item color=\"rgb(86, 143, 193)\" />\r\n" // rgb color
				+ "	<item color=\"rgb(20,20,20,0.5)\" />\r\n" // rgba color
				+ "</resources>";
		testColorPresentationFor(xml, "file:///test/colors-attr.xml", green.getColor(), green.getRange(), settings, //
				colorPres("rgb(0,128,0)", te(2, 14, 2, 19, "rgb(0,128,0)")), //
				colorPres("#008000", te(2, 14, 2, 19, "#008000")));
	}

	@Test
	public void hexWithHashInfo() throws BadLocationException {
		// Here color is done for color/text() only
		XMLColorsSettings settings = createXMLColorsSettings();
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
				+ "<resources>\r\n"
				+ "	<color name=\"opaque_red\">f00</color>\r\n" // <-- here color/text() is colorized
				+ "	<color name=\"translucent_pale_red\">F110</color>\r\n"
				+ "</resources>";
		testColorInformationFor(xml, "file:///test/res/values/colors.xml", settings, //
				colorInfo(1, 0, 0, 1, r(2, 26, 2, 29)), //
				colorInfo(1, 0.06666666666666667, 0.06666666666666667, 0, r(3, 36, 3, 40)));
	}

	@Test
	public void hexWithoutHashPresentation() throws BadLocationException {
		ColorInformation green = colorInfo(1, 0, 1, 1, r(2, 14, 2, 20));
		ColorInformation seeThroughRed = colorInfo(1,0, 0, 0, r(3, 14, 3, 22));

		XMLColorsSettings settings = createXMLColorsSettings();
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
				+ "<resources>\r\n"
				+ "	<item color=\"FF00FF\" />\r\n" // hex color
				+ "	<item color=\"FF000000\" />\r\n" // hexa color
				+ "</resources>";
		testColorPresentationFor(xml, "file:///test/colors-attr.xml", green.getColor(), green.getRange(), settings, //
				colorPres("ff00ff", te(2, 14, 2, 20, "ff00ff")));

		testColorPresentationFor(xml, "file:///test/colors-attr.xml", seeThroughRed.getColor(), seeThroughRed.getRange(), settings, //
				colorPres("ff000000", te(3, 14, 3, 22, "ff000000")));
	}

	private static XMLColorsSettings createXMLColorsSettings() {
		XMLColorsSettings settings = new XMLColorsSettings();
		List<XMLColors> colors = new ArrayList<>();
		settings.setColors(colors);

		// color/text()
		XMLColors colorsOnText = new XMLColors();
		colors.add(colorsOnText);
		colorsOnText.setPattern("**/res/values/colors.xml");
		XMLColorExpression expressionOnText = new XMLColorExpression();
		expressionOnText.setXPath("resources/color/text()");
		colorsOnText.setExpressions(Arrays.asList(expressionOnText));

		// color/text()
		XMLColors colorsOnAttr = new XMLColors();
		colors.add(colorsOnAttr);
		colorsOnAttr.setPattern("**/colors-attr.xml");
		XMLColorExpression expressionOnAttr = new XMLColorExpression();
		expressionOnAttr.setXPath("item/@color");
		colorsOnAttr.setExpressions(Arrays.asList(expressionOnAttr));

		return settings;
	}

}
