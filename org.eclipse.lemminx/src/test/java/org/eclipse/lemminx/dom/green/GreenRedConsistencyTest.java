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
package org.eclipse.lemminx.dom.green;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMParser;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

/**
 * Validates that the green tree builder produces a structure consistent
 * with the existing DOMParser.
 */
public class GreenRedConsistencyTest {

	@Test
	public void simpleDocument() {
		assertConsistent("<root><child/></root>");
	}

	@Test
	public void nestedDocument() {
		assertConsistent("<a><b><c>text</c></b></a>");
	}

	@Test
	public void multipleChildren() {
		assertConsistent("<root><a/><b/><c/></root>");
	}

	@Test
	public void attributes() {
		assertConsistent("<root attr1=\"val1\" attr2=\"val2\"><child/></root>");
	}

	@Test
	public void mixedContent() {
		assertConsistent("<p>hello <b>world</b> end</p>");
	}

	@Test
	public void commentAndPI() {
		assertConsistent("<?xml version=\"1.0\"?><!-- comment --><root/>");
	}

	@Test
	public void selfClosing() {
		assertConsistent("<root><br/><hr/></root>");
	}

	@Test
	public void unclosedTag() {
		assertConsistent("<a><b>");
	}

	@Test
	public void orphanEndTag() {
		assertConsistent("<root></unknown></root>");
	}

	@Test
	public void cdataSection() {
		assertConsistent("<root><![CDATA[some data]]></root>");
	}

	@Test
	public void doctypeWithInternalSubset() {
		assertConsistent(
				"<!DOCTYPE root [\n" +
						"  <!ELEMENT root EMPTY>\n" +
						"  <!ATTLIST root id CDATA #IMPLIED>\n" +
						"]>\n<root/>");
	}

	@Test
	public void doctypeWithEntities() {
		assertConsistent(
				"<!DOCTYPE root [\n" +
						"  <!ENTITY copy \"&#169;\">\n" +
						"  <!ENTITY reg \"&#174;\">\n" +
						"]>\n<root/>");
	}

	@Test
	public void orphanEndTagNoMatch() {
		assertConsistent("<root><a></b></root>");
	}

	@Test
	public void multipleOrphanEndTags() {
		assertConsistent("<root></x></y></z></root>");
	}

	@Test
	public void commentBetweenElements() {
		assertConsistent("<root><a/><!-- between --><b/></root>");
	}

	@Test
	public void whitespaceOnlyContent() {
		assertConsistent("<root>   </root>");
	}

	@Test
	public void emptyDocument() {
		assertConsistent("");
	}

	@Test
	public void textOnly() {
		assertConsistent("plain text");
	}

	@Test
	public void unclosedTags() {
		assertConsistent("<a><b><c>");
	}

	@Test
	public void windowsCRLF() {
		assertConsistent("<root>\r\n  <child/>\r\n</root>");
	}

	@Test
	public void largeDocument() {
		StringBuilder sb = new StringBuilder();
		sb.append("<root>\n");
		for (int i = 0; i < 100; i++) {
			sb.append("  <item id=\"").append(i).append("\">value ").append(i).append("</item>\n");
		}
		sb.append("</root>");
		assertConsistent(sb.toString());
	}

	private void assertConsistent(String xml) {
		TextDocument textDoc = new TextDocument(xml, "test://test.xml");
		DOMDocument domDoc = DOMParser.getInstance().parse(textDoc, null);

		GreenDocument greenDoc = GreenTreeBuilder.parse(xml, "test://test.xml", null);

		assertEquals(domDoc.getEnd(), greenDoc.width(),
				"Document width mismatch");

		assertNodeConsistent(domDoc, greenDoc, 0, xml);
	}

	private void assertNodeConsistent(DOMNode domNode, GreenNode greenNode,
			int greenAbsStart, String xml) {
		assertEquals(domNode.getNodeType(), greenNode.nodeType(),
				"Node type mismatch at offset " + greenAbsStart);

		int domWidth = domNode.getEnd() - domNode.getStart();
		assertEquals(domWidth, greenNode.width(),
				"Width mismatch for " + domNode.getNodeName()
						+ " at offset " + greenAbsStart
						+ " (DOM: " + domNode.getStart() + "-" + domNode.getEnd()
						+ ", green width: " + greenNode.width() + ")");

		if (domNode.isElement() && greenNode instanceof GreenElement) {
			DOMElement domElem = (DOMElement) domNode;
			GreenElement greenElem = (GreenElement) greenNode;
			assertEquals(domElem.getTagName(), greenElem.tag(),
					"Tag name mismatch at offset " + greenAbsStart);
			assertEquals(domElem.isSelfClosed(), greenElem.selfClosed(),
					"Self-closed mismatch for " + domElem.getTagName());
		}

		List<DOMNode> domChildren = domNode.getChildren();
		List<GreenNode> filteredGreenChildren = filterGreenChildren(greenNode.children(), domChildren);
		assertEquals(domChildren.size(), filteredGreenChildren.size(),
				"Child count mismatch for " + domNode.getNodeName()
						+ " at offset " + greenAbsStart);

		for (int i = 0; i < domChildren.size(); i++) {
			DOMNode domChild = domChildren.get(i);
			GreenNode greenChild = filteredGreenChildren.get(i);

			int expectedChildStart = domChild.getStart();
			assertNodeConsistent(domChild, greenChild, expectedChildStart, xml);
		}
	}

	private List<GreenNode> filterGreenChildren(GreenNode[] greenChildren,
			List<DOMNode> domChildren) {
		boolean hasNonWhitespace = false;
		for (GreenNode child : greenChildren) {
			if (!(child instanceof GreenText && ((GreenText) child).whitespace())) {
				hasNonWhitespace = true;
				break;
			}
		}
		if (!hasNonWhitespace) {
			List<GreenNode> result = new ArrayList<>(greenChildren.length);
			for (GreenNode child : greenChildren) {
				result.add(child);
			}
			return result;
		}
		List<GreenNode> result = new ArrayList<>(domChildren.size());
		for (GreenNode child : greenChildren) {
			if (child instanceof GreenText && ((GreenText) child).whitespace()) {
				continue;
			}
			result.add(child);
		}
		return result;
	}

	private int computeChildrenStartOffset(DOMNode domNode, int greenAbsStart) {
		if (domNode.isElement()) {
			DOMElement elem = (DOMElement) domNode;
			if (elem.getStartTagCloseOffset() != DOMNode.NULL_VALUE) {
				return elem.getStartTagCloseOffset() + 1;
			}
		}
		return greenAbsStart;
	}
}
