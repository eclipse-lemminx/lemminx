/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.minify;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.junit.jupiter.api.Test;

/**
 * XML minifier tests.
 *
 */
public class XMLMinifierTest {

	@Test
	public void testMinifySimpleElement() throws BadLocationException {
		String content = "<root>\n" + //
				"  <child>text</child>\n" + //
				"</root>";
		String expected = "<root><child>text</child></root>";
		SharedSettings settings = new SharedSettings();
		String actual = XMLAssert.minify(content, settings);
		assertEquals(expected, actual);
	}

	@Test
	public void testMinifyWithAttributes() throws BadLocationException {
		String content = "<root   attr1=\"value1\"   attr2=\"value2\">\n" + //
				"  <child   id=\"1\">text</child>\n" + //
				"</root>";
		String expected = "<root attr1=\"value1\" attr2=\"value2\"><child id=\"1\">text</child></root>";
		SharedSettings settings = new SharedSettings();
		String actual = XMLAssert.minify(content, settings);
		assertEquals(expected, actual);
	}

	@Test
	public void testMinifyPreserveXmlSpace() throws BadLocationException {
		String content = "<root>\n" + //
				"  <child xml:space=\"preserve\">  text  with  spaces  </child>\n" + //
				"</root>";
		String expected = "<root><child xml:space=\"preserve\">  text  with  spaces  </child></root>";
		SharedSettings settings = new SharedSettings();
		String actual = XMLAssert.minify(content, settings);
		assertEquals(expected, actual);
	}

	@Test
	public void testMinifyNestedElements() throws BadLocationException {
		String content = "<root>\n" + //
				"  <parent>\n" + //
				"    <child1>text1</child1>\n" + //
				"    <child2>text2</child2>\n" + //
				"  </parent>\n" + //
				"</root>";
		String expected = "<root><parent><child1>text1</child1><child2>text2</child2></parent></root>";
		SharedSettings settings = new SharedSettings();
		String actual = XMLAssert.minify(content, settings);
		assertEquals(expected, actual);
	}

	@Test
	public void testMinifyMixedContent() throws BadLocationException {
		String content = "<root>\n" + //
				"  Some text\n" + //
				"  <child>text</child>\n" + //
				"  More text\n" + //
				"</root>";
		String expected = "<root>Some text<child>text</child>More text</root>";
		SharedSettings settings = new SharedSettings();
		String actual = XMLAssert.minify(content, settings);
		assertEquals(expected, actual);
	}

	@Test
	public void testMinifyNormalizeWhitespace() throws BadLocationException {
		String content = "<root>\n" + //
				"  <description>Text with   multiple\n" + //
				"  spaces and\n" + //
				"      newlines.</description>\n" + //
				"</root>";
		String expected = "<root><description>Text with multiple spaces and newlines.</description></root>";
		SharedSettings settings = new SharedSettings();
		String actual = XMLAssert.minify(content, settings);
		assertEquals(expected, actual);
	}

	@Test
	public void testMinifyRemovesComments() throws BadLocationException {
		String content = "<root>\n" + //
				"  <!-- This is a comment -->\n" + //
				"  <child>text</child>\n" + //
				"</root>";
		String expected = "<root><child>text</child></root>";
		SharedSettings settings = new SharedSettings();
		String actual = XMLAssert.minify(content, settings);
		assertEquals(expected, actual);
	}

	@Test
	public void testMinifyRemovesMultipleComments() throws BadLocationException {
		String content = "<!-- Header comment -->\n" + //
				"<root>\n" + //
				"  <!-- First comment -->\n" + //
				"  <child>text</child>\n" + //
				"  <!-- Second comment -->\n" + //
				"  <child2>text2</child2>\n" + //
				"  <!-- Footer comment -->\n" + //
				"</root>";
		String expected = "<root><child>text</child><child2>text2</child2></root>";
		SharedSettings settings = new SharedSettings();
		String actual = XMLAssert.minify(content, settings);
		assertEquals(expected, actual);
	}

	@Test
	public void testMinifyRemovesNestedComments() throws BadLocationException {
		String content = "<root>\n" + //
				"  <parent>\n" + //
				"    <!-- Comment inside parent -->\n" + //
				"    <child>text</child>\n" + //
				"  </parent>\n" + //
				"</root>";
		String expected = "<root><parent><child>text</child></parent></root>";
		SharedSettings settings = new SharedSettings();
		String actual = XMLAssert.minify(content, settings);
		assertEquals(expected, actual);
	}

	@Test
	public void testMinifyRemovesMultilineComments() throws BadLocationException {
		String content = "<root>\n" + //
				"  <!--\n" + //
				"    This is a multi-line comment\n" + //
				"    spanning several lines\n" + //
				"    with various content\n" + //
				"  -->\n" + //
				"  <child>text</child>\n" + //
				"  <!--\n" + //
				"    Another multi-line\n" + //
				"    comment\n" + //
				"  -->\n" + //
				"</root>";
		String expected = "<root><child>text</child></root>";
		SharedSettings settings = new SharedSettings();
		String actual = XMLAssert.minify(content, settings);
		assertEquals(expected, actual);
	}

	@Test
	public void testMinifyWithCDATA() throws BadLocationException {
		String content = "<root>\n" + //
				"  <child><![CDATA[  some data  ]]></child>\n" + //
				"</root>";
		String expected = "<root><child><![CDATA[  some data  ]]></child></root>";
		SharedSettings settings = new SharedSettings();
		String actual = XMLAssert.minify(content, settings);
		assertEquals(expected, actual);
	}

	@Test
	public void testMinifyProcessingInstruction() throws BadLocationException {
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<root>\n" + //
				"  <child>text</child>\n" + //
				"</root>";
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><child>text</child></root>";
		SharedSettings settings = new SharedSettings();
		String actual = XMLAssert.minify(content, settings);
		assertEquals(expected, actual);
	}

	@Test
	public void testMinifyCatalog() throws BadLocationException {
		String content = "<?xml version=\"1.0\"?>\n" + //
				"<catalog>\n" + //
				"   <book id=\"bk101\">\n" + //
				"      <author>Gambardella, Matthew</author>\n" + //
				"      <title>XML Developer's Guide</title>\n" + //
				"      <genre>Computer</genre>\n" + //
				"      <price>44.95</price>\n" + //
				"      <publish_date>2000-10-01</publish_date>\n" + //
				"      <description>An in-depth look at creating applications \n" + //
				"      with XML.</description>\n" + //
				"   </book>\n" + //
				"   <book id=\"bk102\">\n" + //
				"      <author>Ralls, Kim</author>\n" + //
				"      <title>Midnight Rain</title>\n" + //
				"      <genre>Fantasy</genre>\n" + //
				"      <price>5.95</price>\n" + //
				"      <publish_date>2000-12-16</publish_date>\n" + //
				"      <description>A former architect battles corporate zombies, \n" + //
				"      an evil sorceress, and her own childhood to become queen \n" + //
				"      of the world.</description>\n" + //
				"   </book>\n" + //
				"</catalog>";
		String expected = "<?xml version=\"1.0\"?>" + //
				"<catalog>" + //
				"<book id=\"bk101\">" + //
				"<author>Gambardella, Matthew</author>" + //
				"<title>XML Developer's Guide</title>" + //
				"<genre>Computer</genre>" + //
				"<price>44.95</price>" + //
				"<publish_date>2000-10-01</publish_date>" + //
				"<description>An in-depth look at creating applications with XML.</description>" + //
				"</book>" + //
				"<book id=\"bk102\">" + //
				"<author>Ralls, Kim</author>" + //
				"<title>Midnight Rain</title>" + //
				"<genre>Fantasy</genre>" + //
				"<price>5.95</price>" + //
				"<publish_date>2000-12-16</publish_date>" + //
				"<description>A former architect battles corporate zombies, an evil sorceress, and her own childhood to become queen of the world.</description>" + //
				"</book>" + //
				"</catalog>";
		SharedSettings settings = new SharedSettings();
		String actual = XMLAssert.minify(content, settings);
		assertEquals(expected, actual);
	}
}
