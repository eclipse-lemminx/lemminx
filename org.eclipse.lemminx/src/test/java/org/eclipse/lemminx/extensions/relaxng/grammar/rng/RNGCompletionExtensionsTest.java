/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.relaxng.grammar.rng;

import static org.eclipse.lemminx.XMLAssert.CDATA_SNIPPETS;
import static org.eclipse.lemminx.XMLAssert.COMMENT_SNIPPETS;
import static org.eclipse.lemminx.XMLAssert.c;
import static org.eclipse.lemminx.XMLAssert.te;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.contentmodel.BaseFileTempTest;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.Test;

/**
 * XML completion tests for RelaxNG grammar.
 *
 */
public class RNGCompletionExtensionsTest extends BaseFileTempTest {

	@Test
	public void completionOnRoot() throws BadLocationException {
		// completion on <|
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <|\r\n" + //
				"</grammar>";
		testCompletionFor(xml, //
				4 + CDATA_SNIPPETS + COMMENT_SNIPPETS, //
				c("include", te(1, 2, 1, 3, "<include href=\"\"></include>"), "<include"), //
				c("div", te(1, 2, 1, 3, "<div></div>"), "<div"), //
				c("start", te(1, 2, 1, 3, "<start></start>"), "<start"), //
				c("define", te(1, 2, 1, 3, "<define name=\"\"></define>"), "<define"));
	}

	@Test
	public void completionOnRootItemDefaults() throws BadLocationException {
		// completion on <|
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <|\r\n" + //
				"</grammar>";
		testCompletionFor(xml, true, //
				4 + CDATA_SNIPPETS + COMMENT_SNIPPETS, //
				c("include", te(1, 2, 1, 3, "<include href=\"\"></include>"), "<include"), //
				c("div", te(1, 2, 1, 3, "<div></div>"), "<div"), //
				c("start", te(1, 2, 1, 3, "<start></start>"), "<start"), //
				c("define", te(1, 2, 1, 3, "<define name=\"\"></define>"), "<define"));
	}

	@Test
	public void completionOnElementNoName() throws BadLocationException {
		// completion on <|
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"	datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"	<start>\r\n" + //
				"		<ref name=\"foo\"/>\r\n" + //
				"	</start>\r\n" + //
				"	<define name=\"foo\">\r\n" + //
				"			<element>\r\n" + //
				"				<|\r\n" + //
				"			</element>\r\n" + //
				"	</define>\r\n" + //
				"</grammar>";
		testCompletionFor(xml, //
				4 + CDATA_SNIPPETS + COMMENT_SNIPPETS, //
				c("choice", te(7, 4, 7, 5, "<choice></choice>"), "<choice"), //
				c("nsName", te(7, 4, 7, 5, "<nsName></nsName>"), "<nsName"), //
				c("name", te(7, 4, 7, 5, "<name></name>"), "<name"), //
				c("anyName", te(7, 4, 7, 5, "<anyName></anyName>"), "<anyName"));
	}

	@Test
	public void completionOnElementWithName() throws BadLocationException {
		// completion on <|
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"	datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"	<start>\r\n" + //
				"		<ref name=\"foo\"/>\r\n" + //
				"	</start>\r\n" + //
				"	<define name=\"foo\">\r\n" + //
				"			<element name=\"foo\">\r\n" + //
				"				<|\r\n" + //
				"			</element>\r\n" + //
				"	</define>\r\n" + //
				"</grammar>";
		testCompletionFor(xml, //
				19 + CDATA_SNIPPETS + COMMENT_SNIPPETS, //
				c("interleave", te(7, 4, 7, 5, "<interleave></interleave>"), "<interleave"), //
				c("zeroOrMore", te(7, 4, 7, 5, "<zeroOrMore></zeroOrMore>"), "<zeroOrMore"), //
				c("grammar", te(7, 4, 7, 5, "<grammar></grammar>"), "<grammar"), //
				c("ref", te(7, 4, 7, 5, "<ref name=\"\"></ref>"), "<ref"), //
				c("value", te(7, 4, 7, 5, "<value></value>"), "<value"), //
				c("choice", te(7, 4, 7, 5, "<choice></choice>"), "<choice"), //
				c("notAllowed", te(7, 4, 7, 5, "<notAllowed></notAllowed>"), "<notAllowed"), //
				c("element", te(7, 4, 7, 5, "<element></element>"), "<element"), //
				c("group", te(7, 4, 7, 5, "<group></group>"), "<group"), //
				c("attribute", te(7, 4, 7, 5, "<attribute></attribute>"), "<attribute"), //
				c("text", te(7, 4, 7, 5, "<text></text>"), "<text"), //
				c("data", te(7, 4, 7, 5, "<data type=\"string\"></data>"), "<data"), //
				c("oneOrMore", te(7, 4, 7, 5, "<oneOrMore></oneOrMore>"), "<oneOrMore"), //
				c("externalRef", te(7, 4, 7, 5, "<externalRef href=\"\"></externalRef>"), "<externalRef"), //
				c("list", te(7, 4, 7, 5, "<list></list>"), "<list"), //
				c("parentRef", te(7, 4, 7, 5, "<parentRef name=\"\"></parentRef>"), "<parentRef"), //
				c("empty", te(7, 4, 7, 5, "<empty></empty>"), "<empty"), //
				c("optional", te(7, 4, 7, 5, "<optional></optional>"), "<optional"), //
				c("mixed", te(7, 4, 7, 5, "<mixed></mixed>"), "<mixed"));
	}

	@Test
	public void completionOnRefNameAttribute() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"    <start>\r\n" + //
				"        <element name=\"addressBook\">\r\n" + //
				"            <zeroOrMore>\r\n" + //
				"                <element name=\"card\">\r\n" + //
				"                    <ref name=\"|\"/>\r\n" + // completion here should show cardContent (define/@name)
				"                </element>\r\n" + //
				"            </zeroOrMore>\r\n" + //
				"        </element>\r\n" + //
				"    </start>\r\n" + //
				"    <define name=\"cardContent\">\r\n" + //
				"        <element name=\"name\">\r\n" + //
				"            <text/>\r\n" + //
				"        </element>\r\n" + //
				"        <element name=\"email\">\r\n" + //
				"            <text/>\r\n" + //
				"        </element>\r\n" + //
				"    </define>\r\n" + //
				"</grammar>";
		testCompletionFor(xml, //
				1, //
				c("cardContent", te(6, 31, 6, 31, "cardContent"), "cardContent"));
	}

	@Test
	public void completionOnRefNameAttributeWithNestedDefine() throws BadLocationException {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n"
				+ "  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n"
				+ "  <start>\r\n"
				+ "    <ref name=\"root-element\"/>\r\n"
				+ "  </start>\r\n"
				+ "  <define name=\"root-element\">\r\n"
				+ "      <element name=\"root-element\">\r\n"
				+ "        <ref name=\"|\"></ref>\r\n" // completion here should show root-element, asdf (define/@name)
				+ "      </element>\r\n"
				+ "  </define>\r\n"
				+ "  <div>\r\n"
				+ "    <div>\r\n"
				+ "      <define name=\"asdf\">\r\n"
				+ "        <attribute name=\"child\"></attribute>\r\n"
				+ "      </define>\r\n"
				+ "    </div>\r\n"
				+ "  </div>\r\n"
				+ "</grammar>";
		testCompletionFor(xml, //
				2, //
				c("root-element", te(7, 19, 7, 19, "root-element"), "root-element"), //
				c("asdf", te(7, 19, 7, 19, "asdf"), "asdf"));
	}

	@Test
	public void completionOnDataTypeAttributeValueBuiltin() throws BadLocationException {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"name\">\r\n" + //
				"      <data type=\"|\"></data>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testCompletionFor(xml, //
				2, //
				c("string", te(3, 18, 3, 18, "string"), "string"), //
				c("token", te(3, 18, 3, 18, "token"), "token"));
	}

	@Test
	public void completionOnDataTypeAttributeValueXSD() throws BadLocationException {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"name\">\r\n" + //
				"      <data type=\"|\"></data>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testCompletionFor(xml, //
				46, //
				c("string", te(4, 18, 4, 18, "string"), "string"), //
				c("token", te(4, 18, 4, 18, "token"), "token"), //
				c("boolean", te(4, 18, 4, 18, "boolean"), "boolean"), //
				c("integer", te(4, 18, 4, 18, "integer"), "integer"));
	}

	private static void testCompletionFor(String value, Integer expectedCount, CompletionItem... expectedItems)
			throws BadLocationException {
		testCompletionFor(value, false, expectedCount, expectedItems);
	}

	private static void testCompletionFor(String value, boolean enableItemDefaults, Integer expectedCount,
			CompletionItem... expectedItems) throws BadLocationException {
		XMLAssert.testCompletionFor(new XMLLanguageService(), value, null, null, "src/test/resources/relaxng/test.xml",
				expectedCount, true, expectedItems);
	}

}