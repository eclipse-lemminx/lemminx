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
package org.eclipse.lemminx.dom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.lemminx.commons.TextDocument;
import org.junit.jupiter.api.Test;
import org.w3c.dom.NamedNodeMap;

/**
 * Tests for memory optimizations: DOMAttr field consolidation,
 * DOMAttr[] attribute storage, lazy children deferred loading,
 * and findFirst binary search.
 */
public class DOMMemoryOptimizationsTest {

	// --- DOMAttr field consolidation tests ---

	@Test
	public void attrStartEndFromDocument() {
		DOMDocument doc = parse("<root attr=\"value\"/>");
		DOMElement root = doc.getDocumentElement();
		DOMAttr attr = root.getAttributeNode("attr");
		assertNotNull(attr);
		assertEquals(6, attr.getStart());
		assertEquals(18, attr.getEnd());
	}

	@Test
	public void attrNameExtractedFromDocument() {
		DOMDocument doc = parse("<root foo=\"bar\"/>");
		DOMElement root = doc.getDocumentElement();
		DOMAttr attr = root.getAttributeNode("foo");
		assertNotNull(attr);
		assertEquals("foo", attr.getName());
	}

	@Test
	public void attrValueExtractedFromDocument() {
		DOMDocument doc = parse("<root key=\"val\"/>");
		DOMElement root = doc.getDocumentElement();
		DOMAttr attr = root.getAttributeNode("key");
		assertNotNull(attr);
		assertEquals("val", attr.getValue());
		assertEquals("\"val\"", attr.getOriginalValue());
	}

	@Test
	public void attrOwnerElementFromParent() {
		DOMDocument doc = parse("<root attr=\"v\"/>");
		DOMElement root = doc.getDocumentElement();
		DOMAttr attr = root.getAttributeNode("attr");
		assertNotNull(attr);
		assertEquals(root, attr.getOwnerElement());
	}

	@Test
	public void attrOwnerDocumentFromParent() {
		DOMDocument doc = parse("<root attr=\"v\"/>");
		DOMElement root = doc.getDocumentElement();
		DOMAttr attr = root.getAttributeNode("attr");
		assertNotNull(attr);
		assertEquals(doc, attr.getOwnerDocument());
	}

	@Test
	public void attrNodeAttrNameRange() {
		DOMDocument doc = parse("<root myattr=\"v\"/>");
		DOMElement root = doc.getDocumentElement();
		DOMAttr attr = root.getAttributeNode("myattr");
		assertNotNull(attr);
		DOMRange nameRange = attr.getNodeAttrName();
		assertNotNull(nameRange);
		assertEquals(6, nameRange.getStart());
		assertEquals(12, nameRange.getEnd());
	}

	@Test
	public void attrWithoutValue() {
		DOMDocument doc = parse("<root disabled/>");
		DOMElement root = doc.getDocumentElement();
		DOMAttr attr = root.getAttributeNode("disabled");
		assertNotNull(attr);
		assertEquals("disabled", attr.getName());
		assertNull(attr.getValue());
		assertEquals(6, attr.getStart());
		assertEquals(14, attr.getEnd());
	}

	@Test
	public void attrWithDelimiterNoValue() {
		DOMDocument doc = parse("<root attr=/>");
		DOMElement root = doc.getDocumentElement();
		DOMAttr attr = root.getAttributeNode("attr");
		assertNotNull(attr);
		assertTrue(attr.hasDelimiter());
		assertEquals(10, attr.getDelimiterOffset());
	}

	@Test
	public void programmaticAttr() {
		DOMDocument doc = parse("<root/>");
		DOMElement root = doc.getDocumentElement();
		root.setAttribute("dynamic", "val");
		DOMAttr attr = root.getAttributeNode("dynamic");
		assertNotNull(attr);
		assertEquals("dynamic", attr.getName());
	}

	@Test
	public void attrNamespaceURI() {
		DOMDocument doc = parse("<root xmlns:ns=\"http://example.com\" ns:attr=\"v\"/>");
		DOMElement root = doc.getDocumentElement();
		DOMAttr nsAttr = root.getAttributeNode("ns:attr");
		assertNotNull(nsAttr);
		assertEquals("ns", nsAttr.getPrefix());
		assertEquals("attr", nsAttr.getLocalName());
	}

	@Test
	public void attrIsXmlns() {
		DOMDocument doc = parse("<root xmlns=\"http://example.com\"/>");
		DOMElement root = doc.getDocumentElement();
		DOMAttr attr = root.getAttributeNode("xmlns");
		assertNotNull(attr);
		assertTrue(attr.isXmlns());
		assertTrue(attr.isDefaultXmlns());
	}

	// --- DOMAttr[] attribute storage tests ---

	@Test
	public void attributeArrayMultipleAttrs() {
		DOMDocument doc = parse("<root a=\"1\" b=\"2\" c=\"3\"/>");
		DOMElement root = doc.getDocumentElement();
		assertTrue(root.hasAttributes());
		List<DOMAttr> attrs = root.getAttributeNodes();
		assertEquals(3, attrs.size());
		assertEquals("a", attrs.get(0).getName());
		assertEquals("b", attrs.get(1).getName());
		assertEquals("c", attrs.get(2).getName());
	}

	@Test
	public void attributeArrayGetByIndex() {
		DOMDocument doc = parse("<root x=\"1\" y=\"2\"/>");
		DOMElement root = doc.getDocumentElement();
		assertEquals("x", root.getAttributeAtIndex(0).getName());
		assertEquals("y", root.getAttributeAtIndex(1).getName());
		assertNull(root.getAttributeAtIndex(2));
		assertNull(root.getAttributeAtIndex(-1));
	}

	@Test
	public void attributeArrayNamedNodeMap() {
		DOMDocument doc = parse("<root id=\"42\" class=\"main\"/>");
		DOMElement root = doc.getDocumentElement();
		NamedNodeMap map = root.getAttributes();
		assertNotNull(map);
		assertEquals(2, map.getLength());
		assertNotNull(map.getNamedItem("id"));
		assertNotNull(map.getNamedItem("class"));
		assertNull(map.getNamedItem("nonexistent"));
		assertEquals("id", map.item(0).getNodeName());
	}

	@Test
	public void noAttributesReturnsNull() {
		DOMDocument doc = parse("<root/>");
		DOMElement root = doc.getDocumentElement();
		assertFalse(root.hasAttributes());
		assertNull(root.getAttributeNodes());
		assertNull(root.getAttributes());
	}

	@Test
	public void attributeGetValue() {
		DOMDocument doc = parse("<root key=\"hello world\"/>");
		DOMElement root = doc.getDocumentElement();
		assertEquals("hello world", root.getAttribute("key"));
		assertNull(root.getAttribute("missing"));
	}

	// --- Lazy children deferred loading tests ---

	@Test
	public void lazyElementChildrenExpanded() {
		DOMDocument doc = parseLazy("<root><child/><child2/></root>");
		DOMElement root = doc.getDocumentElement();
		assertTrue(root.hasChildNodes());
		List<DOMNode> children = root.getChildren();
		assertEquals(2, children.size());
		assertTrue(children.get(0).isElement());
		assertEquals("child", ((DOMElement) children.get(0)).getTagName());
	}

	@Test
	public void lazyNestedElements() {
		DOMDocument doc = parseLazy("<a><b><c/></b></a>");
		DOMElement a = doc.getDocumentElement();
		assertEquals("a", a.getTagName());
		DOMElement b = (DOMElement) a.getFirstChild();
		assertEquals("b", b.getTagName());
		DOMElement c = (DOMElement) b.getFirstChild();
		assertEquals("c", c.getTagName());
	}

	@Test
	public void lazyFindNodeAt() {
		DOMDocument doc = parseLazy("<root><inner attr=\"v\"/></root>");
		DOMNode node = doc.findNodeAt(7);
		assertTrue(node.isElement());
		assertEquals("inner", ((DOMElement) node).getTagName());
	}

	@Test
	public void lazyAttributesAccessible() {
		DOMDocument doc = parseLazy("<root><elem key=\"val\"/></root>");
		DOMElement root = doc.getDocumentElement();
		DOMElement elem = (DOMElement) root.getFirstChild();
		assertTrue(elem.hasAttributes());
		assertEquals("val", elem.getAttribute("key"));
	}

	@Test
	public void lazySingleChildExpanded() {
		DOMDocument doc = parseLazy("<root><only/></root>");
		DOMElement root = doc.getDocumentElement();
		assertTrue(root.hasChildNodes());
		assertEquals(1, root.getChildren().size());
		DOMElement only = (DOMElement) root.getFirstChild();
		assertEquals("only", only.getTagName());
	}

	@Test
	public void lazySingleTextChildExpanded() {
		DOMDocument doc = parseLazy("<root>content</root>");
		DOMElement root = doc.getDocumentElement();
		assertTrue(root.hasChildNodes());
		DOMNode text = root.getFirstChild();
		assertTrue(text.isText());
	}

	@Test
	public void lazyEmptyElementNoChildren() {
		DOMDocument doc = parseLazy("<root><empty/></root>");
		DOMElement root = doc.getDocumentElement();
		DOMElement empty = (DOMElement) root.getFirstChild();
		assertFalse(empty.hasChildNodes());
		assertEquals(0, empty.getChildren().size());
	}

	@Test
	public void findNodeAtSingleChild() {
		DOMDocument doc = parseLazy("<root><child attr=\"v\"/></root>");
		DOMNode node = doc.findNodeAt(7);
		assertTrue(node.isElement());
		assertEquals("child", ((DOMElement) node).getTagName());
	}

	// --- findFirst binary search tests ---

	@Test
	public void findNodeBeforeInChildren() {
		DOMDocument doc = parse("<root><a/><b/><c/></root>");
		DOMNode found = doc.findNodeBefore(12);
		assertTrue(found.isElement());
		assertEquals("b", ((DOMElement) found).getTagName());
	}

	@Test
	public void findNodeAtOffset() {
		DOMDocument doc = parse("<root><first/><second/></root>");
		DOMNode found = doc.findNodeAt(15);
		assertTrue(found.isElement());
		assertEquals("second", ((DOMElement) found).getTagName());
	}

	@Test
	public void findNodeBeforeAtDocStart() {
		DOMDocument doc = parse("<root><a/></root>");
		DOMNode found = doc.findNodeBefore(1);
		assertTrue(found.isElement());
		assertEquals("root", ((DOMElement) found).getTagName());
	}

	// --- attributeNodes field scoping tests ---

	@Test
	public void textNodeHasNoAttributes() {
		DOMDocument doc = parse("<root>hello</root>");
		DOMNode text = doc.getDocumentElement().getFirstChild();
		assertTrue(text.isText());
		assertFalse(text.hasAttributes());
		assertNull(text.getAttributeNodes());
		assertNull(text.getAttributes());
		assertNull(text.getAttributeAtIndex(0));
	}

	@Test
	public void prologHasAttributes() {
		DOMDocument doc = parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?><root/>");
		DOMNode prolog = doc.getFirstChild();
		assertTrue(prolog.isProlog());
		assertTrue(prolog.hasAttributes());
		assertEquals("1.0", prolog.getAttribute("version"));
		assertEquals("UTF-8", prolog.getAttribute("encoding"));
	}

	@Test
	public void setAttributeOnElement() {
		DOMDocument doc = parse("<root/>");
		DOMElement root = doc.getDocumentElement();
		assertFalse(root.hasAttributes());
		root.setAttribute("key", "val");
		assertTrue(root.hasAttributes());
		assertEquals("val", root.getAttribute("key"));
	}

	@Test
	public void textNodeHasNoChildrenArray() {
		DOMDocument doc = parse("<root>hello</root>");
		DOMNode text = doc.getDocumentElement().getFirstChild();
		assertTrue(text.isText());
		assertNull(text.getChildrenArray());
		assertFalse(text.hasChildNodes());
	}

	@Test
	public void commentNodeHasNoChildrenArray() {
		DOMDocument doc = parse("<root><!-- comment --></root>");
		DOMNode comment = doc.getDocumentElement().getFirstChild();
		assertTrue(comment.isComment());
		assertNull(comment.getChildrenArray());
		assertFalse(comment.hasChildNodes());
	}

	@Test
	public void elementHasChildrenArray() {
		DOMDocument doc = parse("<root><child/></root>");
		DOMElement root = doc.getDocumentElement();
		assertTrue(root.hasChildNodes());
		assertNotNull(root.getChildrenArray());
		assertEquals(1, root.getChildren().size());
	}

	@Test
	public void documentHasChildrenArray() {
		DOMDocument doc = parse("<root/>");
		assertNotNull(doc.getChildrenArray());
		assertTrue(doc.hasChildNodes());
	}

	// --- Helpers ---

	private DOMDocument parse(String xml) {
		return DOMParser.getInstance().parse(xml, "test://test.xml", null, true);
	}

	private DOMDocument parseLazy(String xml) {
		TextDocument textDoc = new TextDocument(xml, "test://test.xml");
		return DOMParser.getInstance().parse(textDoc, null, true, null);
	}
}
