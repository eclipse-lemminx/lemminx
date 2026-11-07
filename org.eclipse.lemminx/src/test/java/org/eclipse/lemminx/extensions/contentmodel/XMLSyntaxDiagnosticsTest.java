/**
 *  Copyright (c) 2018 Angelo ZERR
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel;

import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.caSnippet;
import static org.eclipse.lemminx.XMLAssert.d;
import static org.eclipse.lemminx.XMLAssert.sne;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.eclipse.lemminx.XMLAssert.testCodeActionsFor;
import static org.eclipse.lemminx.XMLAssert.testDiagnosticsFor;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.contentmodel.participants.XMLSyntaxErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.extensions.contentmodel.settings.NamespacesEnabled;
import org.eclipse.lemminx.extensions.contentmodel.settings.SchemaEnabled;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLNamespacesSettings;
import org.eclipse.lemminx.settings.EnforceQuoteStyle;
import org.eclipse.lemminx.settings.QuoteStyle;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

/**
 * XML diagnostics services tests
 *
 */
public class XMLSyntaxDiagnosticsTest extends AbstractCacheBasedTest {

	/**
	 * AttributeNotUnique tests
	 *
	 * @see https://wiki.xmldation.com/Support/Validator/AttributeNotUnique
	 * @throws Exception
	 */
	@Test
	public void testAttributeNotUnique() throws Exception {
		String xml = "<InstdAmt Ccy=\"JPY\" Ccy=\"EUR\" >10000000</InstdAmt>";
		Diagnostic d = d(0, 20, 0, 23, XMLSyntaxErrorCode.AttributeNotUnique);
		testDiagnosticsFor(xml, d);
		// Without snippet support: removes the duplicate (2nd) Ccy, preserving the first value
		testCodeActionsFor(xml, d,
				ca(d, te(0, 19, 0, 29, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(0, 19, 0, 29, "$0")));
	}

	@Test
	public void testAttributeNotUnique2() throws Exception {
		String xml = "<a attr=\"one\" attr=\"two\" attr2=\"\" />";
		Diagnostic d = d(0, 14, 0, 18, XMLSyntaxErrorCode.AttributeNotUnique);
		testDiagnosticsFor(xml, d);
		// Without snippet support: removes the duplicate (2nd) attr, preserving the first value
		testCodeActionsFor(xml, d,
				ca(d, te(0, 13, 0, 24, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(0, 13, 0, 24, "$0")));
	}

	/**
	 * AttributeNSNotUnique tests
	 *
	 * @see https://wiki.xmldation.com/Support/Validator/AttributeNSNotUnique
	 * @throws Exception
	 */
	@Test
	public void testAttributeNSNotUnique() throws Exception {
		String xml = "<Document xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.001.001.03\"\r\n"
				+ "\r\n" + //
				"xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.001.001.03\"> ";
		testDiagnosticsFor(xml, d(2, 0, 2, 5, XMLSyntaxErrorCode.AttributeNSNotUnique));
	}

	@Test
	public void testAttributeNSNotUnique2() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" \r\n" + //
				"	xmlns:tns=\"http://camel.apache.org/schema/spring\"\r\n" + //
				"	xmlns:tns=\"http://camel.apache.org/schema/spring\" version=\"1.0\">";
		testDiagnosticsFor(xml, d(3, 1, 3, 10, XMLSyntaxErrorCode.AttributeNSNotUnique));
	}

	@Test
	public void testAttributePrefixUnbound() throws Exception {
		String xml = "<a xsi:xxxxx=\"\"></a>";
		testDiagnosticsFor(xml, d(0, 3, 0, 6, XMLSyntaxErrorCode.AttributePrefixUnbound));
	}

	/**
	 * ContentIllegalInProlog tests
	 *
	 * @see https://wiki.xmldation.com/Support/Validator/ContentIllegalInProlog
	 * @throws Exception
	 */
	@Test
	public void testBeforeContentIllegalInProlog() throws Exception {
		String xml = " ab?<xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";
		testDiagnosticsFor(xml, d(0, 1, 0, 4, XMLSyntaxErrorCode.ContentIllegalInProlog));
	}

	/**
	 * ContentIllegalInProlog tests
	 *
	 * @see https://wiki.xmldation.com/Support/Validator/ContentIllegalInProlog
	 * @throws Exception
	 */
	@Test
	public void testAfterContentIllegalInProlog() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>ab\ncd";
		testDiagnosticsFor(xml, d(0, 54, 1, 2, XMLSyntaxErrorCode.ContentIllegalInProlog));
	}

	/**
	 * ContentIllegalInProlog tests
	 *
	 * @see https://wiki.xmldation.com/Support/Validator/ContentIllegalInProlog
	 * @throws Exception
	 */
	@Test
	public void testAfterContentIllegalInProlog2() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>ab\ncd<root>";
		testDiagnosticsFor(xml, d(0, 54, 1, 2, XMLSyntaxErrorCode.ContentIllegalInProlog));
	}

	@Test
	public void testEncodingUTF_16() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-16\" standalone=\"no\"?><root />";
		testDiagnosticsFor(xml);
	}

	/**
	 * DashDashInComment tests
	 *
	 * @see https://wiki.xmldation.com/Support/Validator/DashDashInComment
	 * @throws Exception
	 */
	@Test
	public void testDashDashInComment() throws Exception {
		String xml = "<Id>\r\n" + //
				"					<!-- comment -- text -->\r\n" + //
				"        </Id>";
		Diagnostic d = d(1, 18, 1, 20, XMLSyntaxErrorCode.DashDashInComment);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(1, 18, 1, 20, "- -")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(1, 18, 1, 20, "- -$0")));
	}

	/**
	 * ElementUnterminated tests
	 *
	 * @see https://wiki.xmldation.com/Support/Validator/ElementUnterminated
	 * @throws Exception
	 */
	@Test
	public void elementUnterminated() throws Exception {
		String xml = "<Id>\r\n" + //
				"          <OrgId\r\n" + //
				"            <Othr>\r\n" + //
				"              <Id> 222010012</Id>\r\n" + //
				"            </Othr>\r\n" + //
				"          </OrgId>\r\n" + //
				"        </Id>";
		Diagnostic d = d(1, 11, 1, 16, XMLSyntaxErrorCode.ElementUnterminated);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(1, 16, 1, 16, ">")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(1, 16, 1, 16, ">$0")));
	}

	@Test
	public void testElementUnterminatedEndsWithSlash() throws Exception {
		String xml = "<foo>\r\n" + //
				"  <bar att=\"\" /\r\n" + //
				"</foo>";
		Diagnostic d = d(1, 3, 1, 15, XMLSyntaxErrorCode.ElementUnterminated);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(1, 15, 1, 15, ">")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(1, 15, 1, 15, ">$0")));
	}

	@Test
	public void testElementUnterminatedEndsWithSlashAndSpaces() throws Exception {
		String xml = "<foo>\r\n" + //
				"  <bar att=\"\" /     \r\n" + //
				"</foo>";
		Diagnostic d = d(1, 3, 1, 15, XMLSyntaxErrorCode.ElementUnterminated);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(1, 15, 1, 15, ">")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(1, 15, 1, 15, ">$0")));
	}

	@Test
	public void testElementUnterminatedEndsWithAttributes() throws Exception {
		String xml = "<foo>\r\n" + //
				"  <bar att=\"\"\r\n" + //
				"</foo>";
		Diagnostic d = d(1, 3, 1, 13, XMLSyntaxErrorCode.ElementUnterminated);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(1, 13, 1, 13, "/>")), //
				ca(d, te(1, 13, 1, 13, "></bar>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(1, 13, 1, 13, "/>$0")), //
				caSnippet(d, sne(1, 13, 1, 13, "></bar>$0")));
	}

	@Test
	public void testElementUnterminatedEndsWithAttributesAndEndSlash() throws Exception {
		String xml = "<foo>\r\n" + //
				"  <bar att=\"\"          /\r\n" + //
				"</foo>";
		Diagnostic d = d(1, 3, 1, 24, XMLSyntaxErrorCode.ElementUnterminated);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(1, 24, 1, 24, ">")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(1, 24, 1, 24, ">$0")));
	}

	@Test
	public void testElementUnterminatedEndsAndSpaces() throws Exception {
		String xml = "<foo>\r\n" + //
				"  <bar att=\"\"    \r\n" + //
				"</foo>";
		Diagnostic d = d(1, 3, 1, 13, XMLSyntaxErrorCode.ElementUnterminated);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(1, 13, 1, 13, "/>")), //
				ca(d, te(1, 13, 1, 13, "></bar>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(1, 13, 1, 13, "/>$0")), //
				caSnippet(d, sne(1, 13, 1, 13, "></bar>$0")));
	}

	/**
	 * ElementPrefixUnbound tests
	 *
	 * @see https://wiki.xmldation.com/Support/Validator/ElementPrefixUnbound
	 * @throws Exception
	 */
	@Test
	public void testElementPrefixUnbound() throws Exception {
		String xml = "<xs:OrgId>\r\n" + //
				"  <xs:Othr>\r\n" + //
				"  </xs:Othr>\r\n" + //
				"</xs:OrgId>";
		testDiagnosticsFor(xml, d(0, 1, 0, 9, XMLSyntaxErrorCode.ElementPrefixUnbound));
	}

	/**
	 * EmptyPrefixedAttName tests
	 *
	 * @see https://wiki.xmldation.com/Support/Validator/EmptyPrefixedAttName
	 * @throws Exception
	 */
	@Test
	public void testEmptyPrefixedAttName() throws Exception {
		String xml = "<Document xmlns:xsi=\"\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.001.001.03\">";
		testDiagnosticsFor(xml, d(0, 20, 0, 22, XMLSyntaxErrorCode.EmptyPrefixedAttName));
	}

	@Test
	public void testEqRequiredInAttribute() throws Exception {
		String xml = "<a Ccy>123.456</a>";
		Diagnostic d = d(0, 3, 0, 6, XMLSyntaxErrorCode.EqRequiredInAttribute);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(0, 6, 0, 6, "=\"\"")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(0, 6, 0, 6, "=\"$1\"")));
	}

	@Test
	public void testEqRequiredInXMLDecl() throws Exception {
		String xml = "<?xml version:\"1.0\" encoding=\"UTF-8\"?><a></a>";
		testDiagnosticsFor(xml, d(0, 6, 0, 14, XMLSyntaxErrorCode.EqRequiredInXMLDecl));
	}

	@Test
	public void testNoMorePseudoAttributes() throws Exception {
		String xml = "<?xml version=\"1.0\" standalone=\"yes\" encoding=\"UTF-8\"?><a></a>";
		testDiagnosticsFor(xml, d(0, 37, 0, 45, XMLSyntaxErrorCode.NoMorePseudoAttributes));
	}

	/**
	 * ETagRequired tests
	 *
	 * @see https://wiki.xmldation.com/Support/Validator/ETagRequired * @throws
	 *      Exception
	 */
	@Test
	public void testETagRequired() throws Exception {
		String xml = "<UltmtDbtr>\r\n" + //
				"  		<Nm>Name\r\n" + //
				"		</UltmtDbtr> \r\n" + //
				"			</Nm>  ";
		Diagnostic d = d(1, 5, 1, 7, XMLSyntaxErrorCode.ETagRequired);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(1, 12, 1, 12, "</Nm>")), //
				ca(d, te(1, 7, 1, 8, "/>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(1, 12, 1, 12, "</Nm>$0")), //
				caSnippet(d, sne(1, 7, 1, 8, "/>$0")));
	}

	@Test
	public void testETagRequired2() throws Exception {
		String xml = "<UltmtDbtr>\r\n" + //
				"  		Nm>Name</Nm>\r\n" + //
				"		</UltmtDbtr>";
		testDiagnosticsFor(xml, d(0, 1, 0, 10, XMLSyntaxErrorCode.ETagRequired));
	}

	@Test
	public void testETagRequired3() throws Exception {
		String xml = "<UltmtDbtr>\r\n" + //
				"    <Nm>Name</Nm>\r\n" + //
				"    <Ad>\r\n" + //
				"    <Ph>\r\n" + //
				"</UltmtDbtr>";
		Diagnostic d = d(3, 5, 3, 7, XMLSyntaxErrorCode.ETagRequired);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(3, 8, 3, 8, "</Ph>")), //
				ca(d, te(3, 7, 3, 8, "/>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(3, 8, 3, 8, "</Ph>$0")), //
				caSnippet(d, sne(3, 7, 3, 8, "/>$0")));
	}

	@Test
	public void testETagRequiredWithReplace() throws Exception {
		String xml = "<a>\r\n" + //
				"	<b>\r\n" + //
				"		</c>";
		Diagnostic d = d(1, 2, 1, 3, XMLSyntaxErrorCode.ETagRequired);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(2, 4, 2, 5, "b")), //
				ca(d, te(2, 6, 2, 6, "\r\n\t</b>")), //
				ca(d, te(1, 3, 1, 4, "/>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(2, 4, 2, 5, "b$0")), //
				ca(d, te(2, 6, 2, 6, "\r\n\t</b>")), //
				caSnippet(d, sne(1, 3, 1, 4, "/>$0")));
	}

	@Test
	public void testETagRequiredWithText() throws Exception {
		String xml = "<root>\r\n" + //
				"<ABC>def\r\n" + //
				"</root>";
		Diagnostic d = d(1, 1, 1, 4, XMLSyntaxErrorCode.ETagRequired);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(1, 8, 1, 8, "</ABC>")), //
				ca(d, te(1, 4, 1, 5, "/>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(1, 8, 1, 8, "</ABC>$0")), //
				caSnippet(d, sne(1, 4, 1, 5, "/>$0")));
	}

	@Test
	public void testETagRequiredWithIndentation() throws Exception {
		String xml = "<foo>\r\n" + //
				"  <bar>\r\n" + //
				"    <baz></baz>\r\n" + //
				"</foo>";
		Diagnostic d = d(1, 3, 1, 6, XMLSyntaxErrorCode.ETagRequired);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(2, 15, 2, 15, "\r\n  </bar>")), //
				ca(d, te(1, 6, 1, 7, "/>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				ca(d, te(2, 15, 2, 15, "\r\n  </bar>")), //
				caSnippet(d, sne(1, 6, 1, 7, "/>$0")));
	}

	@Test
	public void testETagRequiredWithOrpheanEndTag() throws Exception {
		String xml = "<root>\r\n" + //
				"	<foo>\r\n" + //
				"		</\r\n" + //
				"</root>";
		Diagnostic d = d(1, 2, 1, 5, XMLSyntaxErrorCode.ETagRequired);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(2, 4, 2, 4, "foo>")), //
				ca(d, te(1, 5, 1, 6, "/>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(2, 4, 2, 4, "foo>$0")), //
				caSnippet(d, sne(1, 5, 1, 6, "/>$0")));
	}

	@Test
	public void testETagRequiredClosedWithOrpheanEndTag() throws Exception {
		String xml = "<root>\r\n" + //
				"	<foo>\r\n" + //
				"		</\r\n" + //
				"	</foo>\r\n" + //
				"</root>";
		Diagnostic d = d(1, 2, 1, 5, XMLSyntaxErrorCode.ETagRequired);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(2, 2, 2, 4, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(2, 2, 2, 4, "$0")));
	}

	@Test
	public void testETagRequiredClosedWithOrpheanEndTag2() throws Exception {
		String xml = "<root>\r\n" + //
				"	<foo>\r\n" + //
				"		</bar>\r\n" + //
				"	</foo>\r\n" + //
				"</root>";
		Diagnostic d = d(1, 2, 1, 5, XMLSyntaxErrorCode.ETagRequired);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(2, 2, 2, 8, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(2, 2, 2, 8, "$0")));
	}

	/**
	 * Test ETagUnterminated
	 *
	 * @see https://wiki.xmldation.com/Support/Validator/ETagUnterminated
	 * @throws Exception
	 */
	@Test
	public void testETagUnterminated() throws Exception {
		String xml = "<MsgId>ABC/090928/CCT001</MsgId\r\n" + //
				"  <CreDtTm>2009-09-28T14:07:00</CreDtTm>";
		Diagnostic d = d(0, 26, 0, 31, XMLSyntaxErrorCode.ETagUnterminated);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(0, 31, 0, 31, ">")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(0, 31, 0, 31, ">$0")));
	}

	/**
	 * Test ETagUnterminated
	 *
	 * @see https://wiki.xmldation.com/Support/Validator/ETagUnterminated
	 * @throws Exception
	 */
	@Test
	public void testETagUnterminated2() throws Exception {
		String xml = "<a>\r\n" + //
				"  <b>\r\n" + //
				"    <c></c>\r\n" + //
				"  </b\r\n" + // <- error
				"</a>";
		Diagnostic d = d(3, 4, 3, 5, XMLSyntaxErrorCode.ETagUnterminated);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(3, 5, 3, 5, ">")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(3, 5, 3, 5, ">$0")));
	}

	/**
	 * Test ETagUnterminated
	 *
	 * @see https://wiki.xmldation.com/Support/Validator/ETagUnterminated
	 * @throws Exception
	 */
	@Test
	public void testETagUnterminated3() throws Exception {
		String xml = "<foo>\r\n" + //
				"  <ba><ABCD></ABCD></bar>\r\n" + // // <-- error
				"</foo>";
		testDiagnosticsFor(xml, d(1, 21, 1, 24, XMLSyntaxErrorCode.ETagUnterminated));
	}

	/**
	 * Test ETagUnterminated
	 *
	 * @see https://wiki.xmldation.com/Support/Validator/ETagUnterminated
	 * @throws Exception
	 */
	@Test
	public void testETagUnterminated4() throws Exception {
		String xml = "<project>\r\n" + //
				"  <dependencies>\r\n" + //
				"    <dependency>\r\n" + //
				"      <scope>test</scope>\r\n" + //
				"    </dependency\r\n" + // <-- error
				"    <dependency>\r\n" + //
				"      <scope>test</scope>\r\n" + //
				"    </dependency>\r\n" + //
				"  </dependencies>\r\n" + //
				"</project>";
		testDiagnosticsFor(xml, d(4, 6, 4, 16, XMLSyntaxErrorCode.ETagUnterminated));
	}

	@Test
	public void testIllegalQName() throws Exception {
		String xml = "<a Ccy:\"JPY\">100</a>";
		testDiagnosticsFor(xml, d(0, 6, 0, 7, XMLSyntaxErrorCode.IllegalQName));
	}

	@Test
	public void testInvalidCommentStart() throws Exception {
		String xml = "<!- gdfgdfg -- starts here -->";
		testDiagnosticsFor(xml, d(0, 2, 0, 3, XMLSyntaxErrorCode.InvalidCommentStart));
	}

	@Test
	public void testLessThanAttValue() throws Exception {
		String xml = "<InstdAmt Ccy=\"<EUR\">123.45</InstdAmt> ";
		Diagnostic d = d(0, 14, 0, 20, XMLSyntaxErrorCode.LessthanInAttValue);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(0, 15, 0, 16, "&lt;")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(0, 15, 0, 16, "&lt;$0")));
	}

	@Test
	public void testMarkupEntityMismatch() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<Document xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.001.001.03\">\r\n"
				+ //
				"<CstmrCdtTrfInitn>\r\n" + //
				"</CstmrCdtTrfInitn>";
		Diagnostic d = d(1, 1, 1, 9, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(3, 19, 3, 19, "\r\n</Document>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				ca(d, te(3, 19, 3, 19, "\r\n</Document>")));
	}

	@Test
	public void testMarkupEntityMismatch2() throws Exception {
		String xml = "<ABC>";
		Diagnostic d = d(0, 1, 0, 4, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(0, 5, 0, 5, "</ABC>")), //
				ca(d, te(0, 4, 0, 5, "/>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(0, 5, 0, 5, "</ABC>$0")), //
				caSnippet(d, sne(0, 4, 0, 5, "/>$0")));
	}

	@Test
	public void testMarkupEntityMismatch3() throws Exception {
		String xml = "<";
		Diagnostic d = d(0, 1, 0, 1, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d); // no code actions
	}

	@Test
	public void testMarkupEntityMismatch4() throws Exception {
		String xml = "<?";
		Diagnostic d = d(0, 1, 0, 1, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
	}

	@Test
	public void testMarkupEntityMismatchWithoutClose() throws Exception {
		String xml = "<ABC";
		Diagnostic d = d(0, 1, 0, 4, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(0, 4, 0, 4, "/>")), //
				ca(d, te(0, 4, 0, 4, "></ABC>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(0, 4, 0, 4, "/>$0")), //
				caSnippet(d, sne(0, 4, 0, 4, "></ABC>$0")));
	}

	@Test
	public void testMarkupEntityMismatchWithoutCloseAndNewLine() throws Exception {
		String xml = "<ABC\r\n";
		Diagnostic d = d(0, 1, 0, 4, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(0, 4, 0, 4, "/>")), //
				ca(d, te(0, 4, 0, 4, "></ABC>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(0, 4, 0, 4, "/>$0")), //
				caSnippet(d, sne(0, 4, 0, 4, "></ABC>$0")));
	}

	@Test
	public void testMarkupEntityMismatchWithoutCloseAndSpaces() throws Exception {
		String xml = "<ABC    ";
		Diagnostic d = d(0, 1, 0, 4, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(0, 4, 0, 4, "/>")), //
				ca(d, te(0, 4, 0, 4, "></ABC>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(0, 4, 0, 4, "/>$0")), //
				caSnippet(d, sne(0, 4, 0, 4, "></ABC>$0")));
	}

	@Test
	public void testMarkupEntityMismatchWithAttributes() throws Exception {
		String xml = "<ABC a=''   ";
		Diagnostic d = d(0, 1, 0, 4, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(0, 9, 0, 9, "/>")), //
				ca(d, te(0, 9, 0, 9, "></ABC>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(0, 9, 0, 9, "/>$0")), //
				caSnippet(d, sne(0, 9, 0, 9, "></ABC>$0")));
	}

	@Test
	public void testMarkupEntityMismatchWithAttributesAndSlash() throws Exception {
		String xml = "<ABC a='' /  ";
		Diagnostic d = d(0, 1, 0, 11, XMLSyntaxErrorCode.ElementUnterminated);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(0, 11, 0, 11, ">")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(0, 11, 0, 11, ">$0")));
	}

	@Test
	public void testMarkupEntityMismatchWithText() throws Exception {
		String xml = "<ABC>def";
		Diagnostic d = d(0, 1, 0, 4, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(0, 8, 0, 8, "</ABC>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(0, 8, 0, 8, "</ABC>$0")));
	}

	@Test
	public void testMarkupEntityMismatchWithTextAndNewLine() throws Exception {
		String xml = "<ABC>def\r\n";
		Diagnostic d = d(0, 1, 0, 4, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(0, 8, 0, 8, "</ABC>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(0, 8, 0, 8, "</ABC>$0")));
	}

	@Test
	public void testMarkupEntityMismatchWithComment() throws Exception {
		String xml = "<root><!--note-->";
		Diagnostic d = d(0, 1, 0, 5, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, //
				ca(d, te(0, 17, 0, 17, "</root>")), //
				ca(d, te(0, 5, 0, 6, "/>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(0, 17, 0, 17, "</root>$0")), //
				caSnippet(d, sne(0, 5, 0, 6, "/>$0")));
	}

	@Test
	public void testMarkupEntityMismatchMultiLine() throws Exception {
		String xml = "<foo action=\"toot\"\r\n" + //
				"		    /";
		Diagnostic d = d(0, 1, 0, 4, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, //
				te(1, 7, 1, 7, ">")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(1, 7, 1, 7, ">$0")));
	}

	@Test
	public void testMarkupNotRecognizedInContent() throws Exception {
		String xml = "<GrpHdr>\r\n" + "<- almost a comment-->\r\n" + "<MsgId>2.012.001</MsgId>";
		testDiagnosticsFor(xml, d(1, 0, 1, 1, XMLSyntaxErrorCode.MarkupNotRecognizedInContent));
	}

	@Test
	public void testNameRequiredInReference() throws Exception {
		String xml = "<Nm>Virgay & Co</Nm>";
		testDiagnosticsFor(xml, d(0, 11, 0, 12, XMLSyntaxErrorCode.NameRequiredInReference));
	}

	@Test
	public void testOpenQuoteExpected() throws Exception {
		String xml = " <InstdAmt Ccy==\"JPY\">10000000</InstdAmt>";
		Diagnostic diagnostic1 = d(0, 11, 0, 14, XMLSyntaxErrorCode.OpenQuoteExpected);
		testDiagnosticsFor(xml, diagnostic1);
		// Without snippet support
		testCodeActionsFor(xml, diagnostic1, ca(diagnostic1, te(0, 15, 0, 15, "\"\"")));
		// With snippet support
		testCodeActionsFor(xml, diagnostic1, createSnippetEditSettings(),
				caSnippet(diagnostic1, sne(0, 15, 0, 15, "\"$1\"")));
	}

	@Test
	public void testMissingQuotesForAttribute() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<foo>\r\n" + //
				"  <bar one= two=\"\">\r\n" + //
				"  </bar>\r\n" + //
				"</foo>";
		Diagnostic diagnostic1 = d(2, 7, 2, 10, XMLSyntaxErrorCode.OpenQuoteExpected);
		testDiagnosticsFor(xml, diagnostic1);
		// Without snippet support
		testCodeActionsFor(xml, diagnostic1, ca(diagnostic1, te(2, 11, 2, 11, "\"\"")));
		// With snippet support
		testCodeActionsFor(xml, diagnostic1, createSnippetEditSettings(),
				caSnippet(diagnostic1, sne(2, 11, 2, 11, "\"$1\"")));
	}

	@Test
	public void testMissingQuotesForAttributeSelfClosing() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<foo>\r\n" + //
				"  <bar one= two=\"\"/>\r\n" + //
				"</foo>";
		Diagnostic diagnostic1 = d(2, 7, 2, 10, XMLSyntaxErrorCode.OpenQuoteExpected);
		testDiagnosticsFor(xml, diagnostic1);
		// Without snippet support
		testCodeActionsFor(xml, diagnostic1, ca(diagnostic1, te(2, 11, 2, 11, "\"\"")));
		// With snippet support
		testCodeActionsFor(xml, diagnostic1, createSnippetEditSettings(),
				caSnippet(diagnostic1, sne(2, 11, 2, 11, "\"$1\"")));
	}

	@Test
	public void testMissingQuotesForAttributeSelfClosing2() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<foo>\r\n" + //
				"  <bar one=/>\r\n" + //
				"</foo>";
		Diagnostic diagnostic1 = d(2, 7, 2, 10, XMLSyntaxErrorCode.OpenQuoteExpected);
		testDiagnosticsFor(xml, diagnostic1);
		// Without snippet support
		testCodeActionsFor(xml, diagnostic1, ca(diagnostic1, te(2, 11, 2, 11, "\"\"")));
		// With snippet support
		testCodeActionsFor(xml, diagnostic1, createSnippetEditSettings(),
				caSnippet(diagnostic1, sne(2, 11, 2, 11, "\"$1\"")));
	}

	@Test
	public void testMissingQuotesForAttributeUsingNextValue() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<foo>\r\n" + //
				"  <bar one= two/>\r\n" + //
				"</foo>";
		Diagnostic diagnostic1 = d(2, 7, 2, 10, XMLSyntaxErrorCode.OpenQuoteExpected);
		testDiagnosticsFor(xml, diagnostic1);
		testCodeActionsFor(xml, diagnostic1, ca(diagnostic1, te(2, 11, 2, 15, "\"two\"")));
	}

	@Test
	public void testMissingQuotesForAttributeSingleQuotes() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<foo>\r\n" + //
				"  <bar one= two=\"\"/>\r\n" + //
				"</foo>";
		Diagnostic diagnostic1 = d(2, 7, 2, 10, XMLSyntaxErrorCode.OpenQuoteExpected);
		testDiagnosticsFor(xml, diagnostic1);
		// Without snippet support
		SharedSettings settings = new SharedSettings();
		settings.getPreferences().setQuoteStyle(QuoteStyle.singleQuotes);
		settings.getFormattingSettings().setEnforceQuoteStyle(EnforceQuoteStyle.preferred);
		testCodeActionsFor(xml, diagnostic1, null, settings, ca(diagnostic1, te(2, 11, 2, 11, "\'\'")));
		// With snippet support
		SharedSettings snippetSettings = XMLAssert.createSnippetEditSettings();
		snippetSettings.getPreferences().setQuoteStyle(QuoteStyle.singleQuotes);
		snippetSettings.getFormattingSettings().setEnforceQuoteStyle(EnforceQuoteStyle.preferred);
		testCodeActionsFor(xml, diagnostic1, null, snippetSettings,
				caSnippet(diagnostic1, sne(2, 11, 2, 11, "\'$1\'")));
	}

	@Test
	public void testOpenQuoteExpectedDisabledPreference() throws Exception {
		String xml = " <InstdAmt Ccy==\"JPY\">10000000</InstdAmt>";
		testDiagnosticsFor(xml, null, null, null, true, XMLAssert.getContentModelSettings(false, SchemaEnabled.always)); // validation
		// is
		// disabled
	}

	@Test
	public void testPITargetRequired() throws Exception {
		String xml = "<? encoding=\"UTF-8\"?>";
		testDiagnosticsFor(xml, d(0, 0, 0, 2, XMLSyntaxErrorCode.PITargetRequired));
	}

	@Test
	public void testPseudoAttrNameExpected() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"><a></a>";
		testDiagnosticsFor(xml, d(0, 0, 0, 5, XMLSyntaxErrorCode.PseudoAttrNameExpected));
	}

	@Test
	public void testQuoteRequiredInXMLDecl() throws Exception {
		String xml = "<?xml version= encoding=\"UTF-8\"?>";
		testDiagnosticsFor(xml, d(0, 6, 0, 14, XMLSyntaxErrorCode.QuoteRequiredInXMLDecl));
	}

	@Test
	public void testRootElementTypeMustMatchDoctypedecl() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<!DOCTYPE efgh [ \r\n" + //
				"<!ELEMENT abcd (#PCDATA)>\r\n" + //
				"<!ELEMENT efgh (#PCDATA)>\r\n" + //
				"]> \r\n" + //
				"<abcd/>";
		String expectedMessage = "Document root element \"abcd\", must match DOCTYPE root \"efgh\".";
		Diagnostic d = d(5, 1, 5, 5, XMLSyntaxErrorCode.RootElementTypeMustMatchDoctypedecl, expectedMessage);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(5, 1, 5, 5, "efgh")));
		// With snippet support
		Diagnostic d2 = d(5, 1, 5, 5, XMLSyntaxErrorCode.RootElementTypeMustMatchDoctypedecl, expectedMessage);
		testCodeActionsFor(xml, d2, createSnippetEditSettings(),
				caSnippet(d2, sne(5, 1, 5, 5, "efgh$0")));
	}

	@Test
	public void testRootElementTypeMustMatchDoctypedecl2() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<!DOCTYPE efgh [ \r\n" + //
				"<!ELEMENT abcd (#PCDATA)>\r\n" + //
				"<!ELEMENT efgh (#PCDATA)>\r\n" + //
				"]> \r\n" + //
				"<abcd>test</abcd>";
		String expectedMessage = "Document root element \"abcd\", must match DOCTYPE root \"efgh\".";
		Diagnostic d = d(5, 1, 5, 5, XMLSyntaxErrorCode.RootElementTypeMustMatchDoctypedecl, expectedMessage);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(5, 1, 5, 5, "efgh"), te(5, 12, 5, 16, "efgh")));
		// With snippet support
		Diagnostic d2 = d(5, 1, 5, 5, XMLSyntaxErrorCode.RootElementTypeMustMatchDoctypedecl, expectedMessage);
		testCodeActionsFor(xml, d2, createSnippetEditSettings(),
				caSnippet(d2, sne(5, 1, 5, 5, "efgh$0"), sne(5, 12, 5, 16, "efgh$0")));
	}

	@Test
	public void testSDDeclInvalid() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"en\"?>";
		testDiagnosticsFor(xml, d(0, 48, 0, 52, XMLSyntaxErrorCode.SDDeclInvalid));
	}

	@Test
	public void testSpaceRequiredBeforeEncodingInXMLDecl() throws Exception {
		String xml = "<?xml version=\"1.0\"encoding=\"UTF-8\"?>";
		testDiagnosticsFor(xml, d(0, 1, 0, 5, XMLSyntaxErrorCode.SpaceRequiredBeforeEncodingInXMLDecl));
	}

	@Test
	public void testSpaceRequiredBeforeStandalone() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"standalone=\"no\"?>";
		testDiagnosticsFor(xml, d(0, 1, 0, 5, XMLSyntaxErrorCode.SpaceRequiredBeforeStandalone));
	}

	@Test
	public void testSpaceRequiredInPI() throws Exception {
		String xml = "<?xmlversion=\"1.0\" encoding=\"UTF-8\"?>";
		testDiagnosticsFor(xml, d(0, 1, 0, 12, XMLSyntaxErrorCode.SpaceRequiredInPI));
	}

	@Test
	public void testVersionInfoRequired() throws Exception {
		String xml = "<?xml encoding=\"UTF-8\"?>";
		testDiagnosticsFor(xml, d(0, 1, 0, 5, XMLSyntaxErrorCode.VersionInfoRequired));
	}

	@Test
	public void testVersionNotSupported() throws Exception {
		String xml = "<?xml version=\"5000.0\"encoding=\"UTF-8\"?>";
		testDiagnosticsFor(xml, d(0, 14, 0, 22, XMLSyntaxErrorCode.VersionNotSupported));
	}

	@Test
	public void testEntitySemicolonRequiredInReference() throws Exception {
		String xml = "<!DOCTYPE root [\n" + //
				"    <!ELEMENT root (#PCDATA)>\n" + //
				"    <!ENTITY mdash \"&#x2014;\">\n" + //
				"]>\n" + //
				"<root>\n" + //
				"    &mdash \n" + //
				"</root>";
		Diagnostic d = d(5, 4, 5, 10, XMLSyntaxErrorCode.SemicolonRequiredInReference);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(5, 10, 5, 10, ";")));
	}



	@Test
	public void testEntitySemicolonRequiredInReferenceOddSpacing() throws Exception {
		String xml = "<!DOCTYPE root [\n" + //
				"    <!ELEMENT root (#PCDATA)>\n" + //
				"    <!ENTITY mdash \"&#x2014;\">\n" + //
				"]>\n" + //
				"<root>\n" + //
				"    &mdash</root>";
		Diagnostic d = d(5, 4, 5, 10, XMLSyntaxErrorCode.SemicolonRequiredInReference);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(5, 10, 5, 10, ";")));
	}

	@Test
	public void testEntitySemicolonRequiredInReferenceShortName() throws Exception {
		String xml = "<!DOCTYPE root [\n" + //
				"    <!ELEMENT root (#PCDATA)>\n" + //
				"    <!ENTITY m \"&#x2014;\">\n" + //
				"]>\n" + //
				"<root>\n" + //
				"    &m \n" + //
				"</root>";
		Diagnostic d = d(5, 4, 5, 6, XMLSyntaxErrorCode.SemicolonRequiredInReference);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(5, 6, 5, 6, ";")));
	}

	@Test
	public void closeTag() throws Exception {
		String xml = "<a";
		Diagnostic d = d(0, 1, 0, 2, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(0, 2, 0, 2, "/>")), //
				ca(d, te(0, 2, 0, 2, "></a>")));

		xml = "<a>";
		d = d(0, 1, 0, 2, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, //
				ca(d, te(0, 3, 0, 3, "</a>")), //
				ca(d, te(0, 2, 0, 3, "/>")));

		xml = "<a /";
		d = d(0, 1, 0, 2, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(0, 4, 0, 4, ">")));

		xml = "<a / ";
		d = d(0, 1, 0, 4, XMLSyntaxErrorCode.ElementUnterminated);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(0, 4, 0, 4, ">")));

		xml = "<a></";
		d = d(0, 1, 0, 2, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(0, 5, 0, 5, "a>")));
	}

	// -- Code action tests for existing codes --

	@Test
	public void testContentIllegalInPrologCodeAction() throws Exception {
		String xml = " ab?<xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";
		Diagnostic d = d(0, 1, 0, 4, XMLSyntaxErrorCode.ContentIllegalInProlog);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(0, 1, 0, 4, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(0, 1, 0, 4, "$0")));
	}

	@Test
	public void namespacesSettingsWithoutXMLNS() throws Exception {
		String xml = "<foo>\r\n" + //
				"	<p:bar />\r\n" + //
				"</foo>";
		// always
		ContentModelSettings settings = getSettingsForNamespaces(NamespacesEnabled.always);
		testDiagnosticsFor(xml, null, null, null, true, settings, //
				d(1, 2, 1, 7, XMLSyntaxErrorCode.ElementPrefixUnbound));

		// never
		settings = getSettingsForNamespaces(NamespacesEnabled.never);
		testDiagnosticsFor(xml, null, null, null, true, settings);

		// onNamespaceEncountered
		settings = getSettingsForNamespaces(NamespacesEnabled.onNamespaceEncountered);
		testDiagnosticsFor(xml, null, null, null, true, settings);
	}

	@Test
	public void namespacesSettingsWithUnvalidXMLNS() throws Exception {
		String xml = "<foo xmlns=\"http:foo\" >\r\n" + //
				"	<p:bar />\r\n" + //
				"</foo>";
		// always
		ContentModelSettings settings = getSettingsForNamespaces(NamespacesEnabled.always);
		testDiagnosticsFor(xml, null, null, null, true, settings, //
				d(1, 2, 1, 7, XMLSyntaxErrorCode.ElementPrefixUnbound));

		// never
		settings = getSettingsForNamespaces(NamespacesEnabled.never);
		testDiagnosticsFor(xml, null, null, null, true, settings);

		// onNamespaceEncountered
		settings = getSettingsForNamespaces(NamespacesEnabled.onNamespaceEncountered);
		testDiagnosticsFor(xml, null, null, null, true, settings, //
				d(1, 2, 1, 7, XMLSyntaxErrorCode.ElementPrefixUnbound));
	}

	@Test
	public void namespacesSettingsWithValidXMLNS() throws Exception {
		String xml = "<foo xmlns:p=\"http:foo\" >\r\n" + //
				"	<p:bar />\r\n" + //
				"</foo>";
		// always
		ContentModelSettings settings = getSettingsForNamespaces(NamespacesEnabled.always);
		testDiagnosticsFor(xml, null, null, null, true, settings);

		// never
		settings = getSettingsForNamespaces(NamespacesEnabled.never);
		testDiagnosticsFor(xml, null, null, null, true, settings);

		// onNamespaceEncountered
		settings = getSettingsForNamespaces(NamespacesEnabled.onNamespaceEncountered);
		testDiagnosticsFor(xml, null, null, null, true, settings);
	}

	// -- Close tag with grammar (Strategy 3) tests --

	@Test
	public void testETagRequiredEmptyElementByXSD() throws Exception {
		// XSD declares webApplication, applicationManager and httpEndpoint as EMPTY
		// (complexType with only attributes, no child elements)
		String xml = "<server\r\n" + //
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/server.xsd\">\r\n" + //
				"    <webApplication id=\"app\">\r\n" + //
				"    <applicationManager autoExpand=\"true\"/>\r\n" + //
				"</server>";
		Diagnostic d = d(3, 5, 3, 19, XMLSyntaxErrorCode.ETagRequired);
		// webApplication is EMPTY by XSD: only self-close should be offered
		testCodeActionsFor(xml, d, //
				ca(d, te(3, 28, 3, 29, "/>")));
	}

	@Test
	public void testETagRequiredEmptyElementByDTD() throws Exception {
		// DTD declares bar as EMPTY
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE foo [\r\n" + //
				"  <!ELEMENT foo (bar)*>\r\n" + //
				"  <!ELEMENT bar EMPTY>\r\n" + //
				"  <!ATTLIST bar id CDATA #IMPLIED>\r\n" + //
				"]>\r\n" + //
				"<foo>\r\n" + //
				"  <bar id=\"1\">\r\n" + //
				"</foo>";
		Diagnostic d = d(7, 3, 7, 6, XMLSyntaxErrorCode.ETagRequired);
		// bar is EMPTY by DTD: only self-close should be offered
		testCodeActionsFor(xml, d, //
				ca(d, te(7, 13, 7, 14, "/>")));
	}

	@Test
	public void testETagRequiredEmptyElementByDTDWithOrphanEndTag() throws Exception {
		// DTD declares bar as EMPTY, but parser treats </baz> as child of <bar>
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE foo [\r\n" + //
				"  <!ELEMENT foo (bar)*>\r\n" + //
				"  <!ELEMENT bar EMPTY>\r\n" + //
				"  <!ATTLIST bar id CDATA #IMPLIED>\r\n" + //
				"]>\r\n" + //
				"<foo>\r\n" + //
				"  <bar id=\"1\">\r\n" + //
				"  </baz>\r\n" + //
				"</foo>";
		Diagnostic d = d(7, 3, 7, 6, XMLSyntaxErrorCode.ETagRequired);
		// bar is EMPTY by DTD: orphan replacement and self-close should be offered
		testCodeActionsFor(xml, d, //
				ca(d, te(8, 4, 8, 7, "bar")), //
				ca(d, te(7, 13, 7, 14, "/>")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(8, 4, 8, 7, "bar$0")), //
				caSnippet(d, sne(7, 13, 7, 14, "/>$0")));
	}

	private static ContentModelSettings getSettingsForNamespaces(NamespacesEnabled namespacesEnabled) {
		ContentModelSettings settings = XMLAssert.getContentModelSettings(true, SchemaEnabled.never);
		settings.getValidation().setNoGrammar("ignore");
		XMLNamespacesSettings namespaces = new XMLNamespacesSettings();
		namespaces.setEnabled(namespacesEnabled);
		settings.getValidation().setNamespaces(namespaces);
		return settings;
	}

	private static SharedSettings createSnippetEditSettings() {
		return XMLAssert.createSnippetEditSettings();
	}
}
