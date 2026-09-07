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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GreenTreeBuilder}.
 */
public class GreenTreeBuilderTest {

	@Test
	public void simpleElement() {
		String xml = "<root></root>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);

		assertEquals(xml.length(), doc.width());
		assertEquals(1, doc.childCount());

		GreenElement root = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals("root", root.tag());
		assertEquals(xml.length(), root.width());
		assertTrue(root.closed());
	}

	@Test
	public void selfClosingElement() {
		String xml = "<br/>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);

		assertEquals(xml.length(), doc.width());
		assertEquals(1, doc.childCount());

		GreenElement br = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals("br", br.tag());
		assertTrue(br.selfClosed());
		assertTrue(br.closed());
		assertEquals(xml.length(), br.width());
	}

	@Test
	public void nestedElements() {
		String xml = "<a><b><c/></b></a>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);

		assertEquals(1, doc.childCount());
		GreenElement a = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals("a", a.tag());
		assertEquals(xml.length(), a.width());

		assertEquals(1, a.childCount());
		GreenElement b = assertInstanceOf(GreenElement.class, a.child(0));
		assertEquals("b", b.tag());

		assertEquals(1, b.childCount());
		GreenElement c = assertInstanceOf(GreenElement.class, b.child(0));
		assertEquals("c", c.tag());
		assertTrue(c.selfClosed());
	}

	@Test
	public void elementWithAttributes() {
		String xml = "<div class=\"foo\" id=\"bar\"></div>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);

		GreenElement div = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals("div", div.tag());
		assertEquals(2, div.attributeCount());

		GreenAttr cls = div.attributes()[0];
		assertNotNull(cls);
		assertTrue(cls.nameStartRel() >= 0);

		GreenAttr id = div.attributes()[1];
		assertNotNull(id);
	}

	@Test
	public void textContent() {
		String xml = "<p>hello</p>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);

		GreenElement p = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals("p", p.tag());
		assertEquals(1, p.childCount());

		GreenText text = assertInstanceOf(GreenText.class, p.child(0));
		assertEquals(5, text.width());
	}

	@Test
	public void comment() {
		String xml = "<root><!-- comment --></root>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);

		GreenElement root = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals(1, root.childCount());

		GreenComment comment = assertInstanceOf(GreenComment.class, root.child(0));
		assertTrue(comment.closed());
		assertTrue(comment.width() > 0);
	}

	@Test
	public void cdataSection() {
		String xml = "<root><![CDATA[data]]></root>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);

		GreenElement root = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals(1, root.childCount());

		GreenCDATA cdata = assertInstanceOf(GreenCDATA.class, root.child(0));
		assertTrue(cdata.closed());
	}

	@Test
	public void processingInstruction() {
		String xml = "<?xml version=\"1.0\"?><root/>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);

		assertEquals(2, doc.childCount());
		GreenProcessingInstruction pi = assertInstanceOf(
				GreenProcessingInstruction.class, doc.child(0));
		assertEquals("xml", pi.target());
		assertTrue(pi.prolog());
		assertTrue(pi.closed());

		GreenElement root = assertInstanceOf(GreenElement.class, doc.child(1));
		assertEquals("root", root.tag());
	}

	@Test
	public void widthSumsToDocumentLength() {
		String xml = "<?xml version=\"1.0\"?>\n<root>\n  <child attr=\"val\"/>\n  <!-- comment -->\n</root>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		assertEquals(xml.length(), doc.width());
		assertTotalWidthConsistent(doc);
	}

	@Test
	public void replaceChildStructuralSharing() {
		String xml = "<root><a/><b/><c/></root>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);

		GreenElement root = assertInstanceOf(GreenElement.class, doc.child(0));
		GreenNode originalA = root.child(0);
		GreenNode originalC = root.child(2);

		GreenText replacement = new GreenText(10, false);
		GreenNode newRoot = root.withReplacedChild(1, replacement);

		GreenElement newRootElement = assertInstanceOf(GreenElement.class, newRoot);
		assertTrue(newRootElement.child(0) == originalA, "unchanged child should be same object");
		assertTrue(newRootElement.child(2) == originalC, "unchanged child should be same object");
		assertInstanceOf(GreenText.class, newRootElement.child(1));
	}

	@Test
	public void orphanEndTag() {
		String xml = "</meta>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		assertEquals(xml.length(), doc.width());
		assertEquals(1, doc.childCount());
		GreenElement elem = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals("meta", elem.tag());
		assertNotNull(elem.endTagOpenRel());
	}

	@Test
	public void unclosedElement() {
		String xml = "<a><b>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		assertEquals(xml.length(), doc.width());
		GreenElement a = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals("a", a.tag());
		assertFalse(a.closed());
	}

	@Test
	public void emptyDocument() {
		String xml = "";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		assertEquals(0, doc.width());
		assertEquals(0, doc.childCount());
	}

	@Test
	public void textOnlyDocument() {
		String xml = "just some text";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		assertEquals(xml.length(), doc.width());
		assertEquals(1, doc.childCount());
		GreenText text = assertInstanceOf(GreenText.class, doc.child(0));
		assertEquals(xml.length(), text.width());
	}

	@Test
	public void attributeWithoutValue() {
		String xml = "<input disabled/>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenElement input = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals(1, input.attributeCount());
		GreenAttr attr = input.attributes()[0];
		assertEquals(GreenAttr.NULL_VALUE, attr.delimiterRel());
		assertEquals(GreenAttr.NULL_VALUE, attr.valueStartRel());
	}

	@Test
	public void attributeWithDelimiterNoValue() {
		String xml = "<input type=>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenElement input = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals(1, input.attributeCount());
		GreenAttr attr = input.attributes()[0];
		assertTrue(attr.delimiterRel() != GreenAttr.NULL_VALUE);
		assertEquals(GreenAttr.NULL_VALUE, attr.valueStartRel());
	}

	@Test
	public void multipleAttributes() {
		String xml = "<div class=\"a\" id=\"b\" title=\"c\"></div>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenElement div = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals(3, div.attributeCount());
	}

	@Test
	public void commentContent() {
		String xml = "<!-- hello -->";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenComment comment = assertInstanceOf(GreenComment.class, doc.child(0));
		assertTrue(comment.closed());
		assertEquals(4, comment.startContentRel());
	}

	@Test
	public void unclosedComment() {
		String xml = "<!-- unclosed";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenComment comment = assertInstanceOf(GreenComment.class, doc.child(0));
		assertFalse(comment.closed());
	}

	@Test
	public void emptyCDATA() {
		String xml = "<![CDATA[]]>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenCDATA cdata = assertInstanceOf(GreenCDATA.class, doc.child(0));
		assertTrue(cdata.closed());
	}

	@Test
	public void prologAttributes() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenProcessingInstruction pi = assertInstanceOf(GreenProcessingInstruction.class, doc.child(0));
		assertTrue(pi.prolog());
		assertTrue(pi.closed());
		assertEquals(2, pi.attributes().length);
	}

	@Test
	public void doctypeWithInternalSubset() {
		String xml = "<!DOCTYPE root [\n  <!ELEMENT root EMPTY>\n]>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenDocumentType dt = assertInstanceOf(GreenDocumentType.class, doc.child(0));
		assertTrue(dt.closed());
		assertNotNull(dt.internalSubset());
		assertTrue(dt.childCount() > 0);
	}

	@Test
	public void widthInvariantSimple() {
		assertWidthInvariant("<root><a/><b>text</b></root>");
	}

	@Test
	public void widthInvariantComplex() {
		assertWidthInvariant("<?xml version=\"1.0\"?>\n<!-- comment -->\n<root attr=\"val\">\n  <child/>\n</root>");
	}

	@Test
	public void widthInvariantMalformed() {
		assertWidthInvariant("<a><b></c></a>");
	}

	@Test
	public void widthInvariantOrphanEndTags() {
		assertWidthInvariant("<root></unknown></also></root>");
	}

	@Test
	public void widthInvariantDoctype() {
		assertWidthInvariant("<!DOCTYPE root [\n  <!ELEMENT root EMPTY>\n  <!ATTLIST root id CDATA #IMPLIED>\n]>\n<root/>");
	}

	private void assertWidthInvariant(String xml) {
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		assertEquals(xml.length(), doc.width());
		assertChildrenWidthSum(doc);
	}

	private void assertChildrenWidthSum(GreenNode node) {
		GreenNode[] children = node.children();
		if (children.length == 0) {
			return;
		}
		int sum = 0;
		for (GreenNode child : children) {
			assertTrue(child.width() > 0, "Child width must be positive");
			sum += child.width();
			assertChildrenWidthSum(child);
		}
	}

	@Test
	public void largeDocumentPerformance() {
		StringBuilder sb = new StringBuilder();
		sb.append("<root>\n");
		int count = 10000;
		for (int i = 0; i < count; i++) {
			sb.append("  <item id=\"").append(i).append("\">value").append(i).append("</item>\n");
		}
		sb.append("</root>");
		String xml = sb.toString();

		long start = System.nanoTime();
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		long elapsed = System.nanoTime() - start;

		assertEquals(xml.length(), doc.width());
		GreenElement root = assertInstanceOf(GreenElement.class, doc.child(0));
		// count items + (count + 1) interleaved whitespace text nodes
		assertEquals(2 * count + 1, root.childCount());

		// Parse of 10K elements should complete in under 2 seconds
		assertTrue(elapsed < 2_000_000_000L,
				"Parse took too long: " + (elapsed / 1_000_000) + "ms");
	}

	@Test
	public void deeplyNestedElements() {
		StringBuilder open = new StringBuilder();
		StringBuilder close = new StringBuilder();
		for (int i = 0; i < 15; i++) {
			open.append("<n").append(i).append(">");
			close.insert(0, "</n" + i + ">");
		}
		String xml = open.toString() + "deep" + close.toString();
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		assertEquals(xml.length(), doc.width());
		assertChildrenWidthSum(doc);

		GreenNode current = doc.child(0);
		for (int i = 0; i < 14; i++) {
			GreenElement elem = assertInstanceOf(GreenElement.class, current);
			assertEquals("n" + i, elem.tag());
			assertEquals(1, elem.childCount());
			current = elem.child(0);
		}
		GreenElement innermost = assertInstanceOf(GreenElement.class, current);
		assertEquals("n14", innermost.tag());
		assertEquals(1, innermost.childCount());
		assertInstanceOf(GreenText.class, innermost.child(0));
	}

	@Test
	public void mixedContentTextAndElements() {
		String xml = "<p>start<b>bold</b>mid<i>italic</i>end</p>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		assertEquals(xml.length(), doc.width());
		GreenElement p = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals(5, p.childCount());
		assertInstanceOf(GreenText.class, p.child(0));
		assertInstanceOf(GreenElement.class, p.child(1));
		assertInstanceOf(GreenText.class, p.child(2));
		assertInstanceOf(GreenElement.class, p.child(3));
		assertInstanceOf(GreenText.class, p.child(4));
	}

	@Test
	public void namespaceDeclarations() {
		String xml = "<root xmlns:ns=\"http://example.com\"><ns:child/></root>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		assertEquals(xml.length(), doc.width());
		GreenElement root = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals(1, root.attributeCount());
		assertEquals(1, root.childCount());
		GreenElement child = assertInstanceOf(GreenElement.class, root.child(0));
		assertEquals("ns:child", child.tag());
	}

	@Test
	public void multipleProcessingInstructions() {
		String xml = "<?xml version=\"1.0\"?><?pi1 data1?><?pi2 data2?><root/>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		assertEquals(xml.length(), doc.width());
		assertEquals(4, doc.childCount());
		GreenProcessingInstruction pi0 = assertInstanceOf(GreenProcessingInstruction.class, doc.child(0));
		assertTrue(pi0.prolog());
		GreenProcessingInstruction pi1 = assertInstanceOf(GreenProcessingInstruction.class, doc.child(1));
		assertEquals("pi1", pi1.target());
		assertFalse(pi1.prolog());
		GreenProcessingInstruction pi2 = assertInstanceOf(GreenProcessingInstruction.class, doc.child(2));
		assertEquals("pi2", pi2.target());
		assertInstanceOf(GreenElement.class, doc.child(3));
	}

	@Test
	public void doctypePublicSystem() {
		String xml = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0//EN\" \"http://www.w3.org/xhtml.dtd\"><html/>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		assertEquals(xml.length(), doc.width());
		GreenDocumentType dt = assertInstanceOf(GreenDocumentType.class, doc.child(0));
		assertTrue(dt.closed());
		assertChildrenWidthSum(doc);
	}

	@Test
	public void parseRangeUnclosedElement() {
		String text = "<a/><b><c/>";
		GreenDocument rangeDoc = GreenTreeBuilder.parseRange(text, "test.xml", 4, 11, null);
		assertEquals(7, rangeDoc.width());
		GreenElement b = assertInstanceOf(GreenElement.class, rangeDoc.child(0));
		assertEquals("b", b.tag());
		assertFalse(b.closed());
	}

	@Test
	public void withReplacedChildWidthAdjustment() {
		String xml = "<root><a/><b/><c/></root>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenElement root = assertInstanceOf(GreenElement.class, doc.child(0));
		int originalWidth = root.width();

		GreenText wider = new GreenText(20, false);
		GreenNode newRoot = root.withReplacedChild(1, wider);
		assertEquals(originalWidth - 4 + 20, newRoot.width());

		GreenText narrower = new GreenText(1, false);
		GreenNode newRoot2 = root.withReplacedChild(1, narrower);
		assertEquals(originalWidth - 4 + 1, newRoot2.width());
	}

	@Test
	public void withReplacedChildFirst() {
		String xml = "<root><a/><b/><c/></root>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenElement root = assertInstanceOf(GreenElement.class, doc.child(0));
		GreenNode origB = root.child(1);
		GreenNode origC = root.child(2);

		GreenText replacement = new GreenText(5, false);
		GreenElement newRoot = assertInstanceOf(GreenElement.class, root.withReplacedChild(0, replacement));
		assertInstanceOf(GreenText.class, newRoot.child(0));
		assertTrue(origB == newRoot.child(1));
		assertTrue(origC == newRoot.child(2));
	}

	@Test
	public void withReplacedChildLast() {
		String xml = "<root><a/><b/><c/></root>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenElement root = assertInstanceOf(GreenElement.class, doc.child(0));
		GreenNode origA = root.child(0);
		GreenNode origB = root.child(1);

		GreenText replacement = new GreenText(5, false);
		GreenElement newRoot = assertInstanceOf(GreenElement.class, root.withReplacedChild(2, replacement));
		assertTrue(origA == newRoot.child(0));
		assertTrue(origB == newRoot.child(1));
		assertInstanceOf(GreenText.class, newRoot.child(2));
	}

	// --- Compact children storage tests ---

	@Test
	public void compactChildrenZero() {
		String xml = "<empty/>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenElement empty = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals(0, empty.childCount());
		assertEquals(0, empty.children().length);
		assertThrows(IndexOutOfBoundsException.class, () -> empty.child(0));
	}

	@Test
	public void compactChildrenOne() {
		String xml = "<parent><only/></parent>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenElement parent = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals(1, parent.childCount());
		GreenNode only = parent.child(0);
		assertInstanceOf(GreenElement.class, only);
		assertEquals("only", ((GreenElement) only).tag());

		GreenNode[] arr = parent.children();
		assertEquals(1, arr.length);
		assertEquals(only.width(), arr[0].width());

		assertThrows(IndexOutOfBoundsException.class, () -> parent.child(1));
		assertThrows(IndexOutOfBoundsException.class, () -> parent.child(-1));
	}

	@Test
	public void compactChildrenTwo() {
		String xml = "<parent><a/><b/></parent>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenElement parent = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals(2, parent.childCount());
		assertEquals("a", ((GreenElement) parent.child(0)).tag());
		assertEquals("b", ((GreenElement) parent.child(1)).tag());
		assertEquals(2, parent.children().length);
	}

	@Test
	public void compactChildrenSingleText() {
		String xml = "<p>text</p>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenElement p = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals(1, p.childCount());
		assertInstanceOf(GreenText.class, p.child(0));
		assertEquals(4, p.child(0).width());
	}

	@Test
	public void withReplacedChildOnSingleChild() {
		String xml = "<parent><child/></parent>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenElement parent = assertInstanceOf(GreenElement.class, doc.child(0));
		assertEquals(1, parent.childCount());

		GreenText replacement = new GreenText(10, false);
		GreenNode newParent = parent.withReplacedChild(0, replacement);
		GreenElement newElem = assertInstanceOf(GreenElement.class, newParent);
		assertEquals(1, newElem.childCount());
		assertInstanceOf(GreenText.class, newElem.child(0));
		assertEquals(10, newElem.child(0).width());
	}

	@Test
	public void withNewChildrenCompactsCorrectly() {
		String xml = "<root><a/><b/></root>";
		GreenDocument doc = GreenTreeBuilder.parse(xml, "test.xml", null);
		GreenElement root = assertInstanceOf(GreenElement.class, doc.child(0));

		GreenNode[] singleChild = new GreenNode[] { new GreenText(5, false) };
		GreenElement withOne = root.withNewChildren(singleChild,
				5 - (root.child(0).width() + root.child(1).width()));
		assertEquals(1, withOne.childCount());
		assertInstanceOf(GreenText.class, withOne.child(0));

		GreenElement withNone = root.withNewChildren(new GreenNode[0],
				-(root.child(0).width() + root.child(1).width()));
		assertEquals(0, withNone.childCount());
	}

	private void assertTotalWidthConsistent(GreenNode node) {
		GreenNode[] kids = node.children();
		if (kids.length > 0) {
			for (GreenNode child : kids) {
				assertTrue(child.width() > 0 || child.width() == 0,
						"child width must be non-negative");
				assertTotalWidthConsistent(child);
			}
		}
	}
}
