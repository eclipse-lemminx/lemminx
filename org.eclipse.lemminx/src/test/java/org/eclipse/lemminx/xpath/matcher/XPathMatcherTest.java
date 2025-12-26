/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.xpath.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.dom.DOMText;
import org.eclipse.lemminx.xpath.matcher.IXPathNodeMatcher.MatcherType;
import org.junit.jupiter.api.Test;

/**
 * Tests for XPath test matcher.
 *
 */
public class XPathMatcherTest {

	@Test
	public void emptyXPathMatcher() {
		XPathMatcher matcher = new XPathMatcher(null);
		assertEquals(1, matcher.size());
		assertEquals(MatcherType.ELEMENT, matcher.getNodeSelectorType());
		
		matcher = new XPathMatcher("");
		assertEquals(1, matcher.size());
		assertEquals(MatcherType.ELEMENT, matcher.getNodeSelectorType());
	}

	@Test
	public void matchAnyAttr() {
		String xml = "<foo>\r\n" + //
				"	<bar attr1=\"value1\" attr2=\"value2\">ABCD</bar>\r\n" + //
				"	<baz attr1=\"baz-value1\" attr2=\"baz-value2\">EFGH</baz>\r\n" + //
				"</foo>";
		DOMDocument document = DOMParser.getInstance().parse(xml, "test.xml", null);

		DOMElement bar = getElementByTagName(document.getDocumentElement(), "bar");
		DOMAttr attr1OfBar = bar.getAttributeAtIndex(0);

		XPathMatcher matcher = new XPathMatcher("//@*");
		assertTrue(matcher.match(attr1OfBar));

		matcher = new XPathMatcher("//bar/@*");
		assertTrue(matcher.match(attr1OfBar));

		matcher = new XPathMatcher("/foo/bar/@*");
		assertTrue(matcher.match(attr1OfBar));

		matcher = new XPathMatcher("//baz/@*");
		assertFalse(matcher.match(attr1OfBar));

		matcher = new XPathMatcher("/foo/baz/@*");
		assertFalse(matcher.match(attr1OfBar));
	}

	@Test
	public void matchOneAttr() {
		String xml = "<foo>\r\n" + //
				"	<bar attr1=\"value1\" attr2=\"value2\">ABCD</bar>\r\n" + //
				"	<baz attr1=\"baz-value1\" attr2=\"baz-value2\">EFGH</baz>\r\n" + //
				"</foo>";
		DOMDocument document = DOMParser.getInstance().parse(xml, "test.xml", null);

		DOMElement bar = getElementByTagName(document.getDocumentElement(), "bar");
		DOMAttr attr1OfBar = bar.getAttributeAtIndex(0);

		XPathMatcher matcher = new XPathMatcher("//@attr1");
		assertTrue(matcher.match(attr1OfBar));

		matcher = new XPathMatcher("//bar/@attr1");
		assertTrue(matcher.match(attr1OfBar));

		matcher = new XPathMatcher("/foo/bar/@attr1");
		assertTrue(matcher.match(attr1OfBar));

		matcher = new XPathMatcher("//@attr2");
		assertFalse(matcher.match(attr1OfBar));

		matcher = new XPathMatcher("//bar/@attr2");
		assertFalse(matcher.match(attr1OfBar));

		matcher = new XPathMatcher("/foo/bar/@attr2");
		assertFalse(matcher.match(attr1OfBar));

	}

	@Test
	public void matchAnyText() {
		String xml = "<foo>\r\n" + //
				"	<bar attr1=\"value1\" attr2=\"value2\">ABCD</bar>\r\n" + //
				"	<baz attr1=\"baz-value1\" attr2=\"baz-value2\">EFGH</baz>\r\n" + //
				"</foo>";
		DOMDocument document = DOMParser.getInstance().parse(xml, "test.xml", null);

		DOMElement bar = getElementByTagName(document.getDocumentElement(), "bar");
		DOMText textOfBar = (DOMText) bar.getFirstChild();

		XPathMatcher matcher = new XPathMatcher("//text()");
		assertTrue(matcher.match(textOfBar));

		matcher = new XPathMatcher("//bar/text()");
		assertTrue(matcher.match(textOfBar));

		matcher = new XPathMatcher("/foo/bar/text()");
		assertTrue(matcher.match(textOfBar));

		matcher = new XPathMatcher("//baz/text()");
		assertFalse(matcher.match(textOfBar));

		matcher = new XPathMatcher("/foo/baz/text()");
		assertFalse(matcher.match(textOfBar));
	}

	@Test
	public void matchAnyWithPredicate() {
		String xml = "<foo>\r\n" + //
				"	<bar attr1=\"value1\" attr2=\"value1\">ABCD</bar>\r\n" + //
				"	<bar attr1=\"bar-value1\" attr2=\"value1\">EFGH</bar>\r\n" + //
				"	<baz attr1=\"value1\" attr2=\"value1\">IJKL</baz>\r\n" + //
				"	<baz attr1=\"baz-value1\" attr2=\"value1\">MNOP</baz>\r\n" + //
				"</foo>";
		DOMDocument document = DOMParser.getInstance().parse(xml, "test.xml", null);
		DOMElement root = document.getDocumentElement();

		DOMElement bar1 = (DOMElement) root.getChildren().get(0);
		DOMElement bar2 = (DOMElement) root.getChildren().get(1);
		DOMElement baz1 = (DOMElement) root.getChildren().get(2);
		DOMElement baz2 = (DOMElement) root.getChildren().get(3);

		XPathMatcher matcher = new XPathMatcher("//*[@attr1='value1']");
		assertTrue(matcher.match(bar1), "Expected bar1 to match //*[@attr1='value1']");
		assertFalse(matcher.match(bar2), "Expected bar2 NOT to match //*[@attr1='value1'] due to mismatched attr1 value");
		assertTrue(matcher.match(baz1), "Expected baz1 to match //*[@attr1='value1']");
		assertFalse(matcher.match(baz2), "Expected baz2 NOT to match //*[@attr1='value1'] due to mismatched attr1 value");
	}

	@Test
	public void matchOneWithPredicate() {
		String xml = "<foo>\r\n" + //
				"	<bar attr1=\"value1\" attr2=\"value1\">ABCD</bar>\r\n" + //
				"	<bar attr1=\"bar-value1\" attr2=\"value1\">EFGH</bar>\r\n" + //
				"	<baz attr1=\"value1\" attr2=\"value1\">IJKL</baz>\r\n" + //
				"	<baz attr1=\"baz-value1\" attr2=\"value1\">MNOP</baz>\r\n" + //
				"</foo>";
		DOMDocument document = DOMParser.getInstance().parse(xml, "test.xml", null);
		DOMElement root = document.getDocumentElement();

		DOMElement bar1 = (DOMElement) root.getChildren().get(0);
		DOMElement bar2 = (DOMElement) root.getChildren().get(1);
		DOMElement baz1 = (DOMElement) root.getChildren().get(2);
		DOMElement baz2 = (DOMElement) root.getChildren().get(3);

		XPathMatcher matcher = new XPathMatcher("//bar[@attr1='value1']");
		assertTrue(matcher.match(bar1), "Expected bar1 to match //bar[@attr1='value1']");
		assertFalse(matcher.match(bar2), "Expected bar2 NOT to match //bar[@attr1='value1'] due to mismatched attr1 value");
		assertFalse(matcher.match(baz1), "Expected baz1 NOT to match //bar[@attr1='value1'] due to mismatched tag name");
		assertFalse(matcher.match(baz2), "Expected baz2 NOT to match //bar[@attr1='value1'] due to mismatched tag name and attr1 value");
	}

	private static DOMElement getElementByTagName(DOMElement parent, String tagName) {
		List<DOMNode> children = parent.getChildren();
		if (children != null) {
			for (DOMNode node : children) {
				if (node.isElement() && ((DOMElement) node).isSameTag(tagName)) {
					return (DOMElement) node;
				}
			}
		}
		return null;
	}
}
