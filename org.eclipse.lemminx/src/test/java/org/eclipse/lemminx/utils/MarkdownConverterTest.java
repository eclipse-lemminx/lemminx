/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.utils;

import static org.eclipse.lemminx.utils.MarkdownConverter.convert;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * MarkdownConverterTest
 */
public class MarkdownConverterTest {

	@Test
	public void testHTMLConversion() {
		assertEquals("This is `my code`", convert("This is <code>my code</code>"));
		assertThat("This is\s\s" + System.lineSeparator() + "**bold**").isEqualToIgnoringWhitespace(convert("This is<br><b>bold</b>"));
		assertEquals("The `<project>` element is the root of the descriptor.", convert("The <code>&lt;project&gt;</code> element is the root of the descriptor."));
		assertEquals("# Hey Man", convert("<h1>Hey Man</h1>"));
		assertEquals("[Placeholder](https://www.xml.com)", convert("<a href=\"https://www.xml.com\">Placeholder</a>"));

		String htmlList =
			"<ul>" + System.lineSeparator() + 
			"  <li>Coffee</li>" + System.lineSeparator() +
			"  <li>Tea</li>" + System.lineSeparator() +
			"  <li>Milk</li>" + System.lineSeparator() +
			"</ul>";
		String expectedList = 
			" *  Coffee" + System.lineSeparator() +
			" *  Tea" + System.lineSeparator() +
			" *  Milk";
		assertThat(expectedList).isEqualToIgnoringWhitespace(convert(htmlList));
		assertEquals("ONLY_THIS_TEXT", convert("<p>ONLY_THIS_TEXT</p>"));

		String multilineHTML = 
			"multi" + System.lineSeparator() +
			"line" + System.lineSeparator() +
			"<code>HTML</code> "+
			"stuff";
		assertEquals("multi line `HTML` stuff", convert(multilineHTML));

		String multilineHTML2 = 
			"<p>multi<p>" + System.lineSeparator() +
			"line <code>HTML</code> stuff";
			String multilineHTML2Expected = 
			"multi" + System.lineSeparator() +
			"" + System.lineSeparator() +
			"line `HTML` stuff";
		assertThat(multilineHTML2Expected).isEqualToIgnoringWhitespace(convert(multilineHTML2));
	}
	
	@Test
	public void testMarkdownConversion() {
		assertEquals("This is `my code`", convert("This is `my code`"));
		assertEquals("The `<thing>` element is the root of the descriptor.", convert("The `<thing>` element is the root of the descriptor."));
		assertEquals("The `<project>` element is the root of the descriptor.", convert("The `&lt;project&gt;` element is the root of the descriptor."));
	}

}