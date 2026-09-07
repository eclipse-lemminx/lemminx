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

import java.util.List;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMCDATASection;
import org.eclipse.lemminx.dom.DOMComment;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.dom.DOMProcessingInstruction;
import org.eclipse.lemminx.dom.DOMText;
import org.eclipse.lemminx.dom.RedTreeBuilder;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link RedTreeBuilder} produces a DOMDocument identical to
 * the one built by {@link DOMParser}.
 */
public class RedTreeBuilderTest {

	@Test
	public void simpleElement() {
		assertRedTreeEquivalent("<root></root>");
	}

	@Test
	public void selfClosing() {
		assertRedTreeEquivalent("<br/>");
	}

	@Test
	public void nestedElements() {
		assertRedTreeEquivalent("<a><b><c/></b></a>");
	}

	@Test
	public void elementWithAttributes() {
		assertRedTreeEquivalent("<div class=\"foo\" id=\"bar\"></div>");
	}

	@Test
	public void textContent() {
		assertRedTreeEquivalent("<p>hello world</p>");
	}

	@Test
	public void mixedContent() {
		assertRedTreeEquivalent("<p>hello <b>world</b> end</p>");
	}

	@Test
	public void comment() {
		assertRedTreeEquivalent("<root><!-- comment --></root>");
	}

	@Test
	public void cdataSection() {
		assertRedTreeEquivalent("<root><![CDATA[some data]]></root>");
	}

	@Test
	public void processingInstruction() {
		assertRedTreeEquivalent("<?xml version=\"1.0\"?><root/>");
	}

	@Test
	public void multipleRootChildren() {
		assertRedTreeEquivalent("<?xml version=\"1.0\"?><!-- comment --><root/>");
	}

	@Test
	public void unclosedTag() {
		assertRedTreeEquivalent("<a><b>");
	}

	@Test
	public void orphanEndTag() {
		assertRedTreeEquivalent("<root></unknown></root>");
	}

	@Test
	public void complexDocument() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
				"  <modelVersion>4.0.0</modelVersion>\n" +
				"  <groupId>com.example</groupId>\n" +
				"  <artifactId>test</artifactId>\n" +
				"  <version>1.0</version>\n" +
				"  <!-- A comment -->\n" +
				"  <dependencies>\n" +
				"    <dependency>\n" +
				"      <groupId>junit</groupId>\n" +
				"      <artifactId>junit</artifactId>\n" +
				"    </dependency>\n" +
				"  </dependencies>\n" +
				"</project>";
		assertRedTreeEquivalent(xml);
	}

	@Test
	public void largeDocument() {
		StringBuilder sb = new StringBuilder();
		sb.append("<root>\n");
		for (int i = 0; i < 1000; i++) {
			sb.append("  <item id=\"").append(i).append("\">value ").append(i).append("</item>\n");
		}
		sb.append("</root>");
		assertRedTreeEquivalent(sb.toString());
	}

	// --- Edge cases: orphan end tags ---

	@Test
	public void orphanEndTagAtRoot() {
		assertRedTreeEquivalent("</meta>");
	}

	@Test
	public void orphanEndTagEmpty() {
		assertRedTreeEquivalent("</>");
	}

	@Test
	public void orphanEndTagWithContent() {
		assertRedTreeEquivalent("<root>text</unknown>more</root>");
	}

	@Test
	public void multipleOrphanEndTags() {
		assertRedTreeEquivalent("<root></a></b></c></root>");
	}

	@Test
	public void orphanEndTagBetweenElements() {
		assertRedTreeEquivalent("<root><a/></unknown><b/></root>");
	}

	// --- Edge cases: unclosed tags ---

	@Test
	public void unclosedNestedTags() {
		assertRedTreeEquivalent("<a><b><c>");
	}

	@Test
	public void unclosedWithSiblings() {
		assertRedTreeEquivalent("<root><a><b/></root>");
	}

	@Test
	public void unclosedStartTagNoClose() {
		assertRedTreeEquivalent("<a");
	}

	@Test
	public void unclosedStartTagWithContent() {
		assertRedTreeEquivalent("<a content");
	}

	@Test
	public void unclosedWithSlash() {
		assertRedTreeEquivalent("<a/\n  content\n</a>");
	}

	// --- Edge cases: attributes ---

	@Test
	public void attributeWithoutValue() {
		assertRedTreeEquivalent("<input disabled/>");
	}

	@Test
	public void attributeWithoutValueBeforeClose() {
		assertRedTreeEquivalent("<input disabled>");
	}

	@Test
	public void multipleAttributesWithoutValues() {
		assertRedTreeEquivalent("<input disabled required checked/>");
	}

	@Test
	public void attributeWithDelimiterNoValue() {
		assertRedTreeEquivalent("<input type=>");
	}

	@Test
	public void attributeWithDelimiterNoValueAtEOS() {
		assertRedTreeEquivalent("<input type=");
	}

	@Test
	public void attributeValueSingleQuotes() {
		assertRedTreeEquivalent("<div class='foo'></div>");
	}

	@Test
	public void attributeWithSpacesAroundEquals() {
		assertRedTreeEquivalent("<div class = \"foo\" ></div>");
	}

	@Test
	public void attributeAtEOS() {
		assertRedTreeEquivalent("<input type");
	}

	@Test
	public void attributeNameTouchingPreviousValue() {
		assertRedTreeEquivalent("<a b=\"c\"d=\"e\"/>");
	}

	// --- Edge cases: comments ---

	@Test
	public void unclosedComment() {
		assertRedTreeEquivalent("<root><!-- unclosed comment");
	}

	@Test
	public void emptyComment() {
		assertRedTreeEquivalent("<root><!----></root>");
	}

	@Test
	public void commentSameLineAsEndTag() {
		assertRedTreeEquivalent("<root></root><!-- comment -->");
	}

	@Test
	public void commentOnSameLineEndTag() {
		assertRedTreeEquivalent("<a>\n  <b/>\n</a><!-- same line -->");
	}

	@Test
	public void commentOnDifferentLine() {
		assertRedTreeEquivalent("<a>\n  <b/>\n</a>\n<!-- different line -->");
	}

	// --- Edge cases: CDATA ---

	@Test
	public void emptyCDATA() {
		assertRedTreeEquivalent("<root><![CDATA[]]></root>");
	}

	@Test
	public void unclosedCDATA() {
		assertRedTreeEquivalent("<root><![CDATA[unclosed");
	}

	@Test
	public void cdataWithSpecialChars() {
		assertRedTreeEquivalent("<root><![CDATA[<>&\"']]></root>");
	}

	// --- Edge cases: processing instructions ---

	@Test
	public void piWithAttributes() {
		assertRedTreeEquivalent("<?xml version=\"1.0\" encoding=\"UTF-8\"?><root/>");
	}

	@Test
	public void piWithAttrNoValue() {
		assertRedTreeEquivalent("<?xml version=?><root/>");
	}

	@Test
	public void piNonProlog() {
		assertRedTreeEquivalent("<root><?target data?></root>");
	}

	@Test
	public void unclosedPI() {
		assertRedTreeEquivalent("<?xml version=\"1.0\"");
	}

	// --- Edge cases: DOCTYPE ---

	@Test
	public void doctypeSimple() {
		assertRedTreeEquivalent("<!DOCTYPE html><html/>");
	}

	@Test
	public void doctypeWithPublicId() {
		assertRedTreeEquivalent(
				"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
						"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
						"<html/>");
	}

	@Test
	public void doctypeWithSystemId() {
		assertRedTreeEquivalent("<!DOCTYPE root SYSTEM \"root.dtd\"><root/>");
	}

	@Test
	public void doctypeWithInternalSubset() {
		assertRedTreeEquivalent(
				"<!DOCTYPE root [\n" +
						"  <!ELEMENT root EMPTY>\n" +
						"  <!ATTLIST root id CDATA #IMPLIED>\n" +
						"]>\n<root/>");
	}

	@Test
	public void doctypeEmpty() {
		assertRedTreeEquivalent("<!DOCTYPE aaa [\n]>\n<aaa/>");
	}

	@Test
	public void doctypeUnclosed() {
		assertRedTreeEquivalent("<!DOCTYPE root");
	}

	@Test
	public void doctypeWithEntityDecl() {
		assertRedTreeEquivalent(
				"<!DOCTYPE root [\n" +
						"  <!ENTITY copy \"&#169;\">\n" +
						"]>\n<root/>");
	}

	@Test
	public void doctypeWithNotationDecl() {
		assertRedTreeEquivalent(
				"<!DOCTYPE root [\n" +
						"  <!NOTATION jpeg SYSTEM \"image/jpeg\">\n" +
						"]>\n<root/>");
	}

	@Test
	public void doctypeWithMultipleDecls() {
		assertRedTreeEquivalent(
				"<!DOCTYPE root [\n" +
						"  <!ELEMENT root (child)*>\n" +
						"  <!ELEMENT child EMPTY>\n" +
						"  <!ATTLIST child name CDATA #REQUIRED>\n" +
						"  <!ENTITY copy \"&#169;\">\n" +
						"  <!NOTATION jpeg SYSTEM \"image/jpeg\">\n" +
						"]>\n<root><child name=\"test\"/></root>");
	}

	@Test
	public void doctypeAttlistMultipleAttributes() {
		assertRedTreeEquivalent(
				"<!DOCTYPE root [\n" +
						"  <!ATTLIST elem attr1 CDATA #IMPLIED\n" +
						"                 attr2 CDATA #IMPLIED\n" +
						"                 attr3 CDATA #IMPLIED>\n" +
						"]>\n<root/>");
	}

	@Test
	public void doctypeUnrecognizedContent() {
		assertRedTreeEquivalent(
				"<!DOCTYPE foo BAD_VALUE [\n" +
						"  <!NOTATION Name SYSTEM \"PublicID\" \"SystemID\">\n" +
						"]>\n<root/>");
	}

	// --- Edge cases: end tag with spaces ---

	@Test
	public void endTagWithSpaces() {
		assertRedTreeEquivalent("<a></a   >");
	}

	@Test
	public void endTagWithNewline() {
		assertRedTreeEquivalent("<a></a\n>");
	}

	// --- Edge cases: whitespace handling ---

	@Test
	public void whitespaceOnlyContent() {
		assertRedTreeEquivalent("<root>   </root>");
	}

	@Test
	public void whitespaceBeforeAndAfterContent() {
		assertRedTreeEquivalent("<root>  text  </root>");
	}

	@Test
	public void newlinesAndIndentation() {
		assertRedTreeEquivalent("<root>\n  <child>\n    text\n  </child>\n</root>");
	}

	// --- Edge cases: malformed XML ---

	@Test
	public void startTagInsideStartTag() {
		assertRedTreeEquivalent("<a <b></b>");
	}

	@Test
	public void closeTagMismatch() {
		assertRedTreeEquivalent("<a></b>");
	}

	@Test
	public void multipleRoots() {
		assertRedTreeEquivalent("<a/><b/><c/>");
	}

	@Test
	public void emptyDocument() {
		assertRedTreeEquivalent("");
	}

	@Test
	public void textOnlyDocument() {
		assertRedTreeEquivalent("just text");
	}

	@Test
	public void openBracketOnly() {
		assertRedTreeEquivalent("<");
	}

	@Test
	public void closeBracketInContent() {
		assertRedTreeEquivalent("<root>a > b</root>");
	}

	@Test
	public void ampersandInContent() {
		assertRedTreeEquivalent("<root>a &amp; b</root>");
	}

	// --- Edge cases: mixed real-world patterns ---

	@Test
	public void xhtmlSelfClosingElements() {
		assertRedTreeEquivalent(
				"<?xml version=\"1.0\"?>\n" +
						"<html>\n" +
						"  <head><title>Test</title></head>\n" +
						"  <body>\n" +
						"    <br/>\n" +
						"    <hr/>\n" +
						"    <img src=\"test.png\"/>\n" +
						"    <input type=\"text\" value=\"test\"/>\n" +
						"  </body>\n" +
						"</html>");
	}

	@Test
	public void mavenPom() {
		assertRedTreeEquivalent(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
						"<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
						"         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
						"         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
						"  <modelVersion>4.0.0</modelVersion>\n" +
						"  <groupId>com.example</groupId>\n" +
						"  <artifactId>test</artifactId>\n" +
						"  <version>1.0-SNAPSHOT</version>\n" +
						"  <dependencies>\n" +
						"    <dependency>\n" +
						"      <groupId>junit</groupId>\n" +
						"      <artifactId>junit</artifactId>\n" +
						"      <version>4.13.2</version>\n" +
						"      <scope>test</scope>\n" +
						"    </dependency>\n" +
						"  </dependencies>\n" +
						"</project>");
	}

	@Test
	public void springConfig() {
		assertRedTreeEquivalent(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
						"<beans xmlns=\"http://www.springframework.org/schema/beans\">\n" +
						"  <bean id=\"myBean\" class=\"com.example.MyClass\">\n" +
						"    <property name=\"value\" value=\"test\"/>\n" +
						"    <!-- injection point -->\n" +
						"    <property name=\"ref\">\n" +
						"      <ref bean=\"otherBean\"/>\n" +
						"    </property>\n" +
						"  </bean>\n" +
						"</beans>");
	}

	@Test
	public void windowsCRLF() {
		assertRedTreeEquivalent("<root>\r\n  <child/>\r\n</root>");
	}

	@Test
	public void mixedLineEndings() {
		assertRedTreeEquivalent("<root>\n  <a/>\r\n  <b/>\r  <c/>\n</root>");
	}

	@Test
	public void deeplyNestedTenLevels() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			sb.append("<n").append(i).append(">");
		}
		sb.append("deep");
		for (int i = 9; i >= 0; i--) {
			sb.append("</n").append(i).append(">");
		}
		assertRedTreeEquivalent(sb.toString());
	}

	@Test
	public void piInsideElement() {
		assertRedTreeEquivalent("<root><?target data?><child/></root>");
	}

	@Test
	public void multiplePIsInsideElement() {
		assertRedTreeEquivalent("<root><?pi1 a?><child/><?pi2 b?></root>");
	}

	// --- Lazy building tests ---

	@Test
	public void lazyBuildProducesSameTree() {
		String xml = "<root><a><b>text</b></a><c/></root>";
		assertLazyTreeEquivalent(xml);
	}

	@Test
	public void lazyBuildDeferredUntilAccess() {
		String xml = "<root><a><b/></a><c><d/></c></root>";
		TextDocument textDoc = new TextDocument(xml, "test://test.xml");
		GreenDocument greenDoc = GreenTreeBuilder.parse(xml, "test://test.xml", null);
		DOMDocument doc = RedTreeBuilder.buildLazy(greenDoc, textDoc, null);

		DOMElement root = (DOMElement) doc.getFirstChild();
		assertEquals("root", root.getTagName());

		DOMElement a = (DOMElement) root.getFirstChild();
		assertEquals("a", a.getTagName());
		assertEquals(true, a.hasChildNodes());

		DOMElement b = (DOMElement) a.getFirstChild();
		assertEquals("b", b.getTagName());
	}

	@Test
	public void lazyBuildComplexDocument() {
		String xml = "<?xml version=\"1.0\"?>\n" +
				"<project>\n" +
				"  <dependencies>\n" +
				"    <dependency>\n" +
				"      <groupId>junit</groupId>\n" +
				"    </dependency>\n" +
				"  </dependencies>\n" +
				"</project>";
		assertLazyTreeEquivalent(xml);
	}

	@Test
	public void lazyBuildFindNodeAt() {
		String xml = "<root><a><b>text</b></a><c><d>deep</d></c></root>";
		TextDocument textDoc = new TextDocument(xml, "test://test.xml");
		GreenDocument greenDoc = GreenTreeBuilder.parse(xml, "test://test.xml", null);
		DOMDocument doc = RedTreeBuilder.buildLazy(greenDoc, textDoc, null);
		doc.setGreenDocument(greenDoc);

		int offset = xml.indexOf("deep") + 1;
		DOMNode found = doc.findNodeAt(offset);
		assertEquals(true, found.isText());
		assertEquals("deep", ((DOMText) found).getData());
	}

	@Test
	public void lazyBuildLargeDocument() {
		StringBuilder sb = new StringBuilder();
		sb.append("<root>\n");
		for (int i = 0; i < 1000; i++) {
			sb.append("  <item id=\"").append(i).append("\">value ").append(i).append("</item>\n");
		}
		sb.append("</root>");
		assertLazyTreeEquivalent(sb.toString());
	}

	private void assertLazyTreeEquivalent(String xml) {
		TextDocument textDoc = new TextDocument(xml, "test://test.xml");

		GreenDocument greenDoc = GreenTreeBuilder.parse(xml, "test://test.xml", null);
		DOMDocument eager = RedTreeBuilder.build(greenDoc, textDoc, null);
		DOMDocument lazy = RedTreeBuilder.buildLazy(greenDoc, textDoc, null);

		assertNodesEqual(eager, lazy, xml);
	}

	private void assertRedTreeEquivalent(String xml) {
		TextDocument textDoc = new TextDocument(xml, "test://test.xml");

		DOMDocument expected = DOMParser.getInstance().parse(textDoc, null);

		GreenDocument greenDoc = GreenTreeBuilder.parse(xml, "test://test.xml", null);
		DOMDocument actual = RedTreeBuilder.build(greenDoc, textDoc, null);

		assertNodesEqual(expected, actual, xml);
	}

	private void assertNodesEqual(DOMNode expected, DOMNode actual, String xml) {
		assertEquals(expected.getNodeType(), actual.getNodeType(),
				"Node type mismatch at offset " + expected.getStart());
		assertEquals(expected.getStart(), actual.getStart(),
				"Start offset mismatch for " + expected.getNodeName());
		assertEquals(expected.getEnd(), actual.getEnd(),
				"End offset mismatch for " + expected.getNodeName());
		assertEquals(expected.isClosed(), actual.isClosed(),
				"Closed mismatch for " + expected.getNodeName());

		if (expected.isElement()) {
			assertElementsEqual((DOMElement) expected, (DOMElement) actual);
		}
		if (expected.isComment()) {
			assertCommentsEqual((DOMComment) expected, (DOMComment) actual);
		}
		if (expected instanceof DOMCDATASection) {
			assertCDATAEqual((DOMCDATASection) expected, (DOMCDATASection) actual);
		}
		if (expected.isProcessingInstruction() || expected.isProlog()) {
			assertPIEqual((DOMProcessingInstruction) expected, (DOMProcessingInstruction) actual);
		}
		if (expected.isText()) {
			DOMText expText = (DOMText) expected;
			DOMText actText = (DOMText) actual;
			assertEquals(expText.isWhitespace(), actText.isWhitespace(),
					"Whitespace mismatch for text at " + expected.getStart());
		}

		List<DOMNode> expectedChildren = expected.getChildren();
		List<DOMNode> actualChildren = actual.getChildren();
		assertEquals(expectedChildren.size(), actualChildren.size(),
				"Child count mismatch for " + expected.getNodeName()
						+ " at " + expected.getStart());

		for (int i = 0; i < expectedChildren.size(); i++) {
			assertNodesEqual(expectedChildren.get(i), actualChildren.get(i), xml);
		}
	}

	private void assertElementsEqual(DOMElement expected, DOMElement actual) {
		assertEquals(expected.getTagName(), actual.getTagName(),
				"Tag name mismatch");
		assertEquals(expected.isSelfClosed(), actual.isSelfClosed(),
				"Self-closed mismatch for " + expected.getTagName());
		assertEquals(expected.getStartTagOpenOffset(), actual.getStartTagOpenOffset(),
				"StartTagOpen mismatch for " + expected.getTagName());
		assertEquals(expected.getStartTagCloseOffset(), actual.getStartTagCloseOffset(),
				"StartTagClose mismatch for " + expected.getTagName());
		assertEquals(expected.getEndTagOpenOffset(), actual.getEndTagOpenOffset(),
				"EndTagOpen mismatch for " + expected.getTagName());
		assertEquals(expected.getEndTagCloseOffset(), actual.getEndTagCloseOffset(),
				"EndTagClose mismatch for " + expected.getTagName());

		if (expected.hasAttributes()) {
			assertEquals(expected.getAttributeNodes().size(), actual.getAttributeNodes().size(),
					"Attribute count mismatch for " + expected.getTagName());
			for (int i = 0; i < expected.getAttributeNodes().size(); i++) {
				DOMAttr expAttr = expected.getAttributeNodes().get(i);
				DOMAttr actAttr = actual.getAttributeNodes().get(i);
				assertEquals(expAttr.getName(), actAttr.getName(),
						"Attribute name mismatch");
				assertEquals(expAttr.getOriginalValue(), actAttr.getOriginalValue(),
						"Attribute value mismatch for " + expAttr.getName());
			}
		}
	}

	private void assertCommentsEqual(DOMComment expected, DOMComment actual) {
		assertEquals(expected.getStartContent(), actual.getStartContent(),
				"Comment startContent mismatch");
		assertEquals(expected.getEndContent(), actual.getEndContent(),
				"Comment endContent mismatch");
	}

	private void assertCDATAEqual(DOMCDATASection expected, DOMCDATASection actual) {
		assertEquals(expected.getStartContent(), actual.getStartContent(),
				"CDATA startContent mismatch");
		assertEquals(expected.getEndContent(), actual.getEndContent(),
				"CDATA endContent mismatch");
	}

	private void assertPIEqual(DOMProcessingInstruction expected, DOMProcessingInstruction actual) {
		assertEquals(expected.getTarget(), actual.getTarget(),
				"PI target mismatch");
		assertEquals(expected.isProlog(), actual.isProlog(),
				"PI prolog mismatch");
		assertEquals(expected.getStartContent(), actual.getStartContent(),
				"PI startContent mismatch");
		assertEquals(expected.getEndContent(), actual.getEndContent(),
				"PI endContent mismatch");
	}
}
