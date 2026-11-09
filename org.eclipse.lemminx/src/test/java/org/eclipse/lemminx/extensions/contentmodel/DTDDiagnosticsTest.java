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

import static java.lang.System.lineSeparator;
import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.caSnippet;
import static org.eclipse.lemminx.XMLAssert.d;
import static org.eclipse.lemminx.XMLAssert.l;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.sne;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.eclipse.lemminx.XMLAssert.testCodeActionsFor;

import java.util.ArrayList;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.contentmodel.participants.DTDErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.participants.XMLSyntaxErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticRelatedInformation;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.PublishDiagnosticsCapabilities;
import org.junit.jupiter.api.Test;

/**
 * DTD diagnostics services tests
 *
 */
public class DTDDiagnosticsTest extends AbstractCacheBasedTest {

	@Test
	public void MSG_ELEMENT_NOT_DECLARED() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?> \r\n" + //
				"<!DOCTYPE web-app\r\n" + //
				"   PUBLIC \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\"\r\n" + //
				"   \"http://java.sun.com/dtd/web-app_2_3.dtd\">\r\n" + //
				"\r\n" + //
				"<web-app>\r\n" + //
				"	<XXX></XXX>\r\n" + //
				"</web-app>";
		testDiagnosticsFor(xml, d(6, 2, 5, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED),
				d(5, 1, 8, DTDErrorCode.MSG_CONTENT_INVALID));
	}

	@Test
	public void MSG_ELEMENT_NOT_DECLARED_Public() throws Exception {
		// This test uses the local DTD with catalog-public.xml by using the PUBLIC ID
		// -//Sun Microsystems, Inc.//DTD Web Application 2.3//EN
		// <public publicId="-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
		// uri="../dtd/web-app_2_3.dtd" />
		String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?> \r\n" + //
				"<!DOCTYPE web-app\r\n" + //
				"   PUBLIC \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\"\r\n" + //
				"   \"ABCD.dtd\">\r\n" + //
				"\r\n" + //
				"<web-app>\r\n" + //
				"	<XXX></XXX>\r\n" + //
				"</web-app>";
		testPublicDiagnosticsFor(xml, d(6, 2, 5, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED),
				d(5, 1, 8, DTDErrorCode.MSG_CONTENT_INVALID));
	}

	@Test
	public void MSG_CONTENT_INVALID() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE note [\r\n" + //
				"<!ELEMENT note (to,from,heading,body)>\r\n" + //
				"    <!ELEMENT to (#PCDATA)>\r\n" + //
				"        <!ELEMENT from (#PCDATA)>\r\n" + //
				"                <!ELEMENT heading (#PCDATA)>\r\n" + //
				"            <!ELEMENT body (#PCDATA)>\r\n" + //
				"]>\r\n" + //
				"<note>\r\n" + //
				"	<from>Jani</from>\r\n" + //
				"	<heading>Reminder</heading>\r\n" + //
				"	<body>Don't forget me this weekend</body>\r\n" + //
				"</note>";
		XMLAssert.testDiagnosticsFor(xml, d(8, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID));
	}

	@Test
	public void MSG_ATTRIBUTE_NOT_DECLARED() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE note [\r\n" + //
				"<!ELEMENT note (to,from,heading,body)>\r\n" + //
				"<!ELEMENT to (#PCDATA)>\r\n" + //
				"<!ELEMENT from (#PCDATA)>\r\n" + //
				"<!ELEMENT heading (#PCDATA)>\r\n" + //
				"<!ELEMENT body (#PCDATA)>\r\n" + //
				"]>\r\n" + //
				"<note>\r\n" + //
				"    <to></to>\r\n" + //
				"    <from XXXX=\"\" >Jani</from>\r\n" + // <- error
				"    <heading>Reminder</heading>\r\n" + //
				"    <body>Don't forget me this weekend</body>\r\n" + //
				"</note> ";
		XMLAssert.testDiagnosticsFor(xml, d(10, 10, 14, DTDErrorCode.MSG_ATTRIBUTE_NOT_DECLARED));
	}

	@Test
	public void MSG_FIXED_ATTVALUE_INVALID() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE address [\r\n" + //
				"  <!ELEMENT address (company)*>\r\n" + //
				"  <!ELEMENT company (#PCDATA)>\r\n" + //
				"  <!ATTLIST company name NMTOKEN #FIXED \"tutorialspoint\">\r\n" + //
				"]>\r\n" + //
				"<address>\r\n" + //
				"  <company name=\"etutorialspoint\"" // <- error
				+ ">we are a free online teaching faculty</company>\r\n" + //
				"</address>";
		XMLAssert.testDiagnosticsFor(xml, d(7, 16, 33, DTDErrorCode.MSG_FIXED_ATTVALUE_INVALID));
	}

	@Test
	public void MSG_ATTRIBUTE_VALUE_NOT_IN_LIST() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE foo [\r\n" + //
				"  <!ELEMENT foo (bar)*>\r\n" + //
				"  <!ELEMENT bar (#PCDATA)>\r\n" + //
				"  <!ATTLIST bar fruit (one | two | three) #REQUIRED>\r\n" + //
				"]>\r\n" + //
				"<foo>\r\n" + //
				"    <bar fruit=\"four\"" + // <- error
				">toto</bar>\r\n" + //
				"</foo>";
		XMLAssert.testDiagnosticsFor(xml, d(7, 15, 21, DTDErrorCode.MSG_ATTRIBUTE_VALUE_NOT_IN_LIST));
	}

	@Test
	public void MSG_CONTENT_INCOMPLETE() throws Exception {
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE foo SYSTEM \"src/test/resources/dtd/foo-enum.dtd\">" + lineSeparator() + //
				"<foo>" + lineSeparator() + //
				"    " + lineSeparator() + // <- error
				"</foo>";
		Diagnostic d = d(2, 1, 4, DTDErrorCode.MSG_CONTENT_INCOMPLETE);
		XMLAssert.testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(2, 5, 4, 0, lineSeparator() + "\t<bar fruit=\"one\"></bar>" + lineSeparator())),
				ca(d, te(2, 5, 4, 0, lineSeparator() + "\t<bar fruit=\"one\"></bar>" + lineSeparator())));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(2, 5, 4, 0, lineSeparator() + "  <bar fruit=\"${1|one,two,three|}\">$2</bar>" + lineSeparator() + "$0")),
				caSnippet(d, sne(2, 5, 4, 0, lineSeparator() + "  <bar fruit=\"${1|one,two,three|}\">$2</bar>" + lineSeparator() + "$0")));
	}

	@Test
	public void MSG_CONTENT_INCOMPLETE_selfClosed() throws Exception {
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE foo SYSTEM \"src/test/resources/dtd/foo-enum.dtd\">" + lineSeparator() + //
				"<foo />";
		Diagnostic d = d(2, 1, 4, DTDErrorCode.MSG_CONTENT_INCOMPLETE);
		XMLAssert.testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(2, 4, 2, 7, ">" + lineSeparator() + "\t<bar fruit=\"one\"></bar>" + lineSeparator() + "</foo>")),
				ca(d, te(2, 4, 2, 7, ">" + lineSeparator() + "\t<bar fruit=\"one\"></bar>" + lineSeparator() + "</foo>")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(2, 4, 2, 7, ">" + lineSeparator() + "  <bar fruit=\"${1|one,two,three|}\">$2</bar>" + lineSeparator() + "$0</foo>")),
				caSnippet(d, sne(2, 4, 2, 7, ">" + lineSeparator() + "  <bar fruit=\"${1|one,two,three|}\">$2</bar>" + lineSeparator() + "$0</foo>")));
	}

	@Test
	public void MSG_CONTENT_INCOMPLETE_withExistingContent() throws Exception {
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"  <!ELEMENT note (to,from)>" + lineSeparator() + //
				"  <!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"  <!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"    <to>Tove</to>" + lineSeparator() + //
				"</note>";
		Diagnostic d = d(6, 1, 5, DTDErrorCode.MSG_CONTENT_INCOMPLETE);
		XMLAssert.testDiagnosticsFor(xml, d);
		// Insert missing 'from' after 'to'
		testCodeActionsFor(xml, d,
				ca(d, te(8, 0, 8, 0, "    <from></from>" + lineSeparator())));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(8, 0, 8, 0, "    <from>$0</from>" + lineSeparator())));
	}

	@Test
	public void MSG_CONTENT_INCOMPLETE_insertMultipleMissing() throws Exception {
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"  <!ELEMENT note (to,from,heading,body)>" + lineSeparator() + //
				"  <!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"  <!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"  <!ELEMENT heading (#PCDATA)>" + lineSeparator() + //
				"  <!ELEMENT body (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"    <to>Tove</to>" + lineSeparator() + //
				"    <from>Jani</from>" + lineSeparator() + //
				"</note>";
		Diagnostic d = d(8, 1, 5, DTDErrorCode.MSG_CONTENT_INCOMPLETE);
		XMLAssert.testDiagnosticsFor(xml, d);
		// Insert missing 'heading' after 'from', and 'body' after 'heading'
		testCodeActionsFor(xml, d,
				ca(d, te(11, 0, 11, 0, "    <heading></heading>" + lineSeparator())),
				ca(d, te(11, 0, 11, 0, "    <body></body>" + lineSeparator())));
	}

	@Test
	public void MSG_REQUIRED_ATTRIBUTE_NOT_SPECIFIED() throws Exception {
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE foo SYSTEM \"src/test/resources/dtd/foo-enum.dtd\">" + lineSeparator() + //
				"<foo>" + lineSeparator() + //
				"    <bar />" + lineSeparator() + // <- error ("fruit" attribute is missing)
				"</foo>";
		Diagnostic d = d(3, 5, 8, DTDErrorCode.MSG_REQUIRED_ATTRIBUTE_NOT_SPECIFIED);
		XMLAssert.testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(3, 8, 3, 8, " fruit=\"one\"")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(3, 8, 3, 8, " fruit=\"${1|one,two,three|}\"")));
	}

	@Test
	public void MSG_ELEMENT_WITH_ID_REQUIRED() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<!DOCTYPE Folks [\r\n" + //
				"	<!ELEMENT Folks (Person*)>\r\n" + //
				"	<!ELEMENT Person (Name,Email?)>\r\n" + //
				"	<!ATTLIST Person Pin ID #REQUIRED>\r\n" + //
				"	<!ATTLIST Person Friend IDREF #IMPLIED>\r\n" + //
				"	<!ATTLIST Person Likes IDREFS #IMPLIED>\r\n" + //
				"	<!ELEMENT Name (#PCDATA)>\r\n" + //
				"	<!ELEMENT Email (#PCDATA)>\r\n" + //
				"	]>\r\n" + //
				"<Folks>\r\n" + //
				"    <Person Pin=\"id2\" Likes=\"vfg\"> \r\n" + //
				"        <Name>Bob</Name>\r\n" + //
				"    </Person>\r\n" + //
				"</Folks>";
		XMLAssert.testDiagnosticsFor(xml, d(10, 1, 6, DTDErrorCode.MSG_ELEMENT_WITH_ID_REQUIRED));
	}

	@Test
	public void IDInvalidWithNamespaces() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!ELEMENT Person EMPTY>\r\n" + //
				"	<!ATTLIST Person Pin ID #REQUIRED>\r\n" + //
				"	]>\r\n" + //
				"<Person Pin=\"7\" />"; // <- error on @Pin value
		XMLAssert.testDiagnosticsFor(xml, d(5, 12, 15, DTDErrorCode.IDInvalidWithNamespaces));
	}

	@Test
	public void IDREFInvalidWithNamespaces() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!ELEMENT Person EMPTY>\r\n" + //
				"	<!ATTLIST Person Friend IDREF #IMPLIED>\r\n" + //
				"]>\r\n" + //
				"<Person Friend=\"\" />"; // <- error on @Friend value
		XMLAssert.testDiagnosticsFor(xml, d(5, 15, 17, DTDErrorCode.IDREFInvalidWithNamespaces));
	}

	@Test
	public void IDREFSInvalid() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!ELEMENT Person EMPTY>\r\n" + //
				"	<!ATTLIST Person Likes IDREFS #IMPLIED>\r\n" + //
				"]>\r\n" + //
				"<Person Likes=\"\" />"; // <- error on @Likes value
		XMLAssert.testDiagnosticsFor(xml, d(5, 14, 16, DTDErrorCode.IDREFSInvalid));
	}

	@Test
	public void MSG_MARKUP_NOT_RECOGNIZED_IN_DTD() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!ELEMENT Person EMPTY>\r\n" + //
				"   Bad Value   " + "	<!ATTLIST Person Likes IDREFS #IMPLIED>\r\n" + //
				"]>\r\n" + //
				"<Person Likes=\"\" />"; // <- error on @Likes value
		XMLAssert.testDiagnosticsFor(xml, d(2, 24, 3, 16, DTDErrorCode.MSG_MARKUP_NOT_RECOGNIZED_IN_DTD));
	}

	@Test
	public void MSG_MARKUP_NOT_RECOGNIZED_IN_DTD_codeAction() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!ELEMENT Person EMPTY>\r\n" + //
				"   Bad Value   " + "	<!ATTLIST Person Likes IDREFS #IMPLIED>\r\n" + //
				"]>\r\n" + //
				"<Person Likes=\"\" />";
		Diagnostic d = d(2, 24, 3, 16, DTDErrorCode.MSG_MARKUP_NOT_RECOGNIZED_IN_DTD);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(2, 24, 3, 16, "")));
	}

	@Test
	public void QuoteRequiredInPublicID() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!NOTATION    name PUBLIC asd > \r\n" + //
				"]>\r\n" + //
				"<Person Likes=\"\" />"; // <- error on @Likes value
		XMLAssert.testDiagnosticsFor(xml, d(2, 27, 30, DTDErrorCode.QuoteRequiredInPublicID));
	}

	@Test
	public void QuoteRequiredInPublicID2() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!NOTATION name PUBLIC    >  \r\n" + //
				"]>\r\n" + //
				"<Person Likes=\"\" />"; // <- error on @Likes value
		XMLAssert.testDiagnosticsFor(xml, d(2, 23, 24, DTDErrorCode.QuoteRequiredInPublicID));
	}

	@Test
	public void QuoteRequiredInSystemID() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!NOTATION name SYSTEM    >  \r\n" + //
				"]>\r\n" + //
				"<Person Likes=\"\" />"; // <- error on @Likes value
		XMLAssert.testDiagnosticsFor(xml, d(2, 23, 24, DTDErrorCode.QuoteRequiredInSystemID));
	}

	@Test
	public void InvalidCharInPublicID() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root EMPTY>\r\n" + //
				"	<!NOTATION n1 PUBLIC \"unclosed>\r\n" + //
				"]>\r\n" + //
				"<root />";
		XMLAssert.testDiagnosticsFor(xml, d(3, 22, 31, DTDErrorCode.InvalidCharInPublicID));
	}

	@Test
	public void OpenQuoteMissingInDecl() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!ENTITY asd >   \r\n" + //
				"]>\r\n" + //
				"<Person Likes=\"\" />"; // <- error on @Likes value
		XMLAssert.testDiagnosticsFor(xml, d(2, 13, 14, DTDErrorCode.OpenQuoteMissingInDecl));
	}

	@Test
	public void SpaceRequiredAfterSYSTEM() throws Exception {
		// <!NOTATION name SYSTEM> → 'SYSTEM' is highlighted, code action inserts ' "systemId"'
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!NOTATION name SYSTEM>  \r\n" + //
				"]>\r\n" + //
				"<Person Likes=\"\" />"; // <- error on @Likes value
		Diagnostic d1 = d(2, 17, 23, DTDErrorCode.SpaceRequiredAfterSYSTEM);
		XMLAssert.testDiagnosticsFor(xml, d1);
		testCodeActionsFor(xml, d1, ca(d1, te(2, 23, 2, 23, " \"systemId\"")));
		testCodeActionsFor(xml, d1, XMLAssert.createSnippetEditSettings(),
				caSnippet(d1, sne(2, 23, 2, 23, " \"${1:systemId}\"$0")));
	}

	@Test
	public void MSG_SPACE_REQUIRED_AFTER_NOTATION_NAME_IN_NOTATIONDECL() throws Exception {
		// <!NOTATION name> → 'name' is highlighted, code action inserts SYSTEM or PUBLIC declaration
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!NOTATION name>  \r\n" + //
				"]>\r\n" + //
				"<Person Likes=\"\" />"; // <- error on @Likes value
		Diagnostic d1 = d(2, 12, 16, DTDErrorCode.MSG_SPACE_REQUIRED_AFTER_NOTATION_NAME_IN_NOTATIONDECL);
		XMLAssert.testDiagnosticsFor(xml, d1);
		testCodeActionsFor(xml, d1,
				ca(d1, te(2, 16, 2, 16, " SYSTEM \"systemId\"")),
				ca(d1, te(2, 16, 2, 16, " PUBLIC \"publicId\"")));
		testCodeActionsFor(xml, d1, XMLAssert.createSnippetEditSettings(),
				caSnippet(d1, sne(2, 16, 2, 16, " SYSTEM \"${1:systemId}\"$0")),
				caSnippet(d1, sne(2, 16, 2, 16, " PUBLIC \"${1:publicId}\"$0")));
	}

	@Test
	public void MSG_ATT_DEFAULT_INVALID() throws Exception {
		// <!ATTLIST root status (one|two|three) "four"> → '"four"' is highlighted
		// code action replaces with valid enum values "one", "two" or "three"
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE root [" + lineSeparator() + //
				"	<!ELEMENT root (#PCDATA)>" + lineSeparator() + //
				"	<!ATTLIST root status (one|two|three) \"four\">" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<root />";
		Diagnostic d = d(3, 39, 45, DTDErrorCode.MSG_ATT_DEFAULT_INVALID);
		XMLAssert.testDiagnosticsFor(xml, d, d(5, 8, 8, DTDErrorCode.MSG_ATTRIBUTE_VALUE_NOT_IN_LIST));

		// testCodeActionsFor strips the first '|' from xml (used as cursor marker),
		// so use a single-value enum to avoid corrupting the DTD content
		String xmlCA = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE root [" + lineSeparator() + //
				"	<!ELEMENT root (#PCDATA)>" + lineSeparator() + //
				"	<!ATTLIST root status (active) \"four\">" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<root />";
		Diagnostic dCA = d(3, 33, 39, DTDErrorCode.MSG_ATT_DEFAULT_INVALID);
		testCodeActionsFor(xmlCA, dCA,
				ca(dCA, te(3, 33, 3, 39, "\"active\"")));
		testCodeActionsFor(xmlCA, dCA, XMLAssert.createSnippetEditSettings(),
				caSnippet(dCA, sne(3, 33, 3, 39, "\"active\"$0")));
	}

	@Test
	public void MSG_DISTINCT_TOKENS_IN_ENUMERATION() throws Exception {
		// <!ATTLIST root status (one|two|one) #IMPLIED> → '|one' (duplicate) is highlighted
		// code action removes the duplicate token
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE root [" + lineSeparator() + //
				"	<!ELEMENT root EMPTY>" + lineSeparator() + //
				"	<!ATTLIST root status (one|two|one) #IMPLIED>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<root />";
		Diagnostic d = d(3, 31, 35, DTDErrorCode.MSG_DISTINCT_TOKENS_IN_ENUMERATION);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(3, 31, 3, 35, "")));
	}

	@Test
	public void AttTypeRequiredInAttDef() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!ATTLIST payment name BadType >  \r\n" + //
				"]>\r\n" + //
				"<Person Likes=\"\" />"; // <- error on @Likes value
		XMLAssert.testDiagnosticsFor(xml, d(2, 24, 31, DTDErrorCode.AttTypeRequiredInAttDef));
	}

	@Test
	public void AttTypeRequiredInAttDef_codeAction() throws Exception {
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE root [" + lineSeparator() + //
				"	<!ELEMENT root EMPTY>" + lineSeparator() + //
				"	<!ATTLIST root name BadType>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<root />";
		Diagnostic d = d(3, 21, 28, DTDErrorCode.AttTypeRequiredInAttDef);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(3, 21, 3, 28, "CDATA #REQUIRED")),
				ca(d, te(3, 21, 3, 28, "ID #REQUIRED")),
				ca(d, te(3, 21, 3, 28, "IDREF #REQUIRED")),
				ca(d, te(3, 21, 3, 28, "IDREFS #REQUIRED")),
				ca(d, te(3, 21, 3, 28, "ENTITY #REQUIRED")),
				ca(d, te(3, 21, 3, 28, "ENTITIES #REQUIRED")),
				ca(d, te(3, 21, 3, 28, "NMTOKEN #REQUIRED")),
				ca(d, te(3, 21, 3, 28, "NMTOKENS #REQUIRED")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(3, 21, 3, 28, "CDATA ${1|#REQUIRED,#IMPLIED|}$0")),
				caSnippet(d, sne(3, 21, 3, 28, "ID ${1|#REQUIRED,#IMPLIED|}$0")),
				caSnippet(d, sne(3, 21, 3, 28, "IDREF ${1|#REQUIRED,#IMPLIED|}$0")),
				caSnippet(d, sne(3, 21, 3, 28, "IDREFS ${1|#REQUIRED,#IMPLIED|}$0")),
				caSnippet(d, sne(3, 21, 3, 28, "ENTITY ${1|#REQUIRED,#IMPLIED|}$0")),
				caSnippet(d, sne(3, 21, 3, 28, "ENTITIES ${1|#REQUIRED,#IMPLIED|}$0")),
				caSnippet(d, sne(3, 21, 3, 28, "NMTOKEN ${1|#REQUIRED,#IMPLIED|}$0")),
				caSnippet(d, sne(3, 21, 3, 28, "NMTOKENS ${1|#REQUIRED,#IMPLIED|}$0")));
	}

	@Test
	public void ElementDeclUnterminated() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!ELEMENT element-name (element-content) \r\n" + //
				"]>\r\n" + //
				"<Person Likes=\"\" />"; // <- error on @Likes value
		Diagnostic d1 = d(2, 24, 41, DTDErrorCode.ElementDeclUnterminated);
		XMLAssert.testDiagnosticsFor(xml, d1);
		testCodeActionsFor(xml, d1, ca(d1, te(2, 41, 2, 41, ">")));
		testCodeActionsFor(xml, d1, XMLAssert.createSnippetEditSettings(),
				caSnippet(d1, sne(2, 41, 2, 41, ">$0")));
	}

	@Test
	public void PEReferenceWithinMarkup() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Folks [\r\n" + //
				"	<!ENTITY % Folks \"(%bar;)*\"> \r\n" + // <- error on "(%bar;)*"
				"]>\r\n" + //
				"<Folks></Folks>"; //
		XMLAssert.testDiagnosticsFor(xml, d(2, 18, 28, DTDErrorCode.PEReferenceWithinMarkup));
	}

	@Test
	public void MSG_ELEMENT_ALREADY_DECLARED() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Email [\r\n" + //
				"	<!ELEMENT Email (#PCDATA)> \r\n" + //
				"	<!ELEMENT Email (#PCDATA)> \r\n" + // <- error on 'Email' (duplicate)
				"]>\r\n" + //
				"<Email></Email>"; //
		XMLAssert.testDiagnosticsFor(xml, d(3, 11, 16, DTDErrorCode.MSG_ELEMENT_ALREADY_DECLARED));
	}

	@Test
	public void testDoctypeDiagnosticsRefresh() throws Exception {
		// @formatter:off
		String xml = "<?xml version=\"1.0\"?>\n" + "<!DOCTYPE student [\n" + "  <!ELEMENT student (surname,id)>\n"
				+ "  <!ELEMENT surname (#PCDATA)>\n" + "]>\n" + "<student>\n" + "  <surname>Smith</surname>\n"
				+ "  <id>567896</id>\n" + "</student>";
		// @formatter:on
		XMLAssert.testDiagnosticsFor(xml, d(7, 3, 5, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED));

		// @formatter:off
		xml = "<?xml version=\"1.0\"?>\n" + "<!DOCTYPE student [\n" + "  <!ELEMENT student (surname,id)>\n"
				+ "  <!ELEMENT surname (#PCDATA)>\n" + "  <!ELEMENT id (#PCDATA)>\n" + "]>\n" + "<student>\n"
				+ "  <surname>Smith</surname>\n" + "  <id>567896</id>\n" + "</student>";
		// @formatter:on
		XMLAssert.testDiagnosticsFor(xml, new Diagnostic[0]);

	}

	@Test
	public void testDTDNotFoundWithDocTypeSYSTEM() throws Exception {
		String xml = "<?xml version=\"1.0\" standalone=\"no\" ?>\r\n" + //
				"<!DOCTYPE inEQUAL_PMT SYSTEM \"inEQUAL_PMT.dtd\">\r\n" + // <- [1] error DTD not found
				"<inEQUAL_PMT>\r\n" + // [2]
				"  \r\n" + //
				"    <!-- The Proceeds and Term -->\r\n" + //
				"   <Proceeds>10000.00</Proceed>\r\n" + // <- [3] and [4] error, it misses 's' for </Proceed>
				"   <Term>36</Term>\r\n" + //
				"   \r\n" + //
				"</inEQUAL_PMT>";
		XMLAssert.testDiagnosticsFor(xml, //
				d(1, 30, 1, 45, DTDErrorCode.DTDNotFound), // [1]
				d(2, 1, 12, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED), // [2]
				d(5, 4, 12, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED), // [3]
				d(5, 4, 12, XMLSyntaxErrorCode.ETagRequired)); // [4]
	}

	@Test
	public void testDTDNotFoundWithEntitySYSTEM() throws Exception {
		String xml = "<!DOCTYPE root-element [\r\n" + //
				"	<!ENTITY % entity-name SYSTEM \"entity-value\">\r\n" + //
				"	%entity-name;\r\n" + //
				"]>\r\n" + //
				"<root-element>\r\n" + //
				"	<\r\n" + // [1]
				"</root-element>";
		XMLAssert.testDiagnosticsFor(xml, //
				d(5, 1, 5, 2, XMLSyntaxErrorCode.MarkupNotRecognizedInContent) // [1]
		);
	}

	@Test
	public void testDTDNotFoundWithEntitySYSTEMAndResolve() throws Exception {
		String xml = "<!DOCTYPE root-element [\r\n" + //
				"	<!ENTITY % entity-name SYSTEM \"entity-value\">\r\n" + // <- [1] error DTD not found
				"	%entity-name;\r\n" + //
				"]>\r\n" + //
				"<root-element>\r\n" + //
				"	<\r\n" + // [2]
				"</root-element>";
		ContentModelSettings settings = new ContentModelSettings();
		XMLValidationRootSettings validation = new XMLValidationRootSettings();
		validation.setResolveExternalEntities(true);
		settings.setValidation(validation);

		XMLAssert.testDiagnosticsFor(xml, null, null, null, true, settings, //
				d(1, 32, 1, 44, DTDErrorCode.DTDNotFound), // [1]
				d(5, 1, 5, 2, XMLSyntaxErrorCode.MarkupNotRecognizedInContent) // [2]
		);
	}

	@Test
	public void testDTDNotFoundWithPUBLIC() throws Exception {
		String xml = "<?xml version=\"1.0\" standalone=\"no\" ?>\r\n" + //
				"<!DOCTYPE inEQUAL_PMT PUBLIC 'X' \"inEQUAL_PMT.dtd\">\r\n" + // <- [1] error DTD not found
				"<inEQUAL_PMT>\r\n" + // [2]
				"  \r\n" + //
				"    <!-- The Proceeds and Term -->\r\n" + //
				"   <Proceeds>10000.00</Proceed>\r\n" + // <- [3] and [4] error, it misses 's' for </Proceed>
				"   <Term>36</Term>\r\n" + //
				"   \r\n" + //
				"</inEQUAL_PMT>";
		XMLAssert.testDiagnosticsFor(xml, d(1, 34, 1, 49, DTDErrorCode.DTDNotFound), // [1]
				d(2, 1, 12, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED), // [2]
				d(5, 4, 12, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED), // [3]
				d(5, 4, 12, XMLSyntaxErrorCode.ETagRequired)); // [4]
	}

	@Test
	public void MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ELEMENTDECL() throws Exception {
		String xml;

		xml = "<!DOCTYPE asdf [\n" + //
				"  <!ELEMENTasdf (#PCDATA)>\n" + //
				"]>";
		testDiagnosticsFor(xml, d(1, 4, 11, DTDErrorCode.MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ELEMENTDECL));

		xml = "<!ELEMENTasdf (#PCDATA)>";
		testDiagnosticsFor(xml, "test.dtd",
				d(0, 2, 9, DTDErrorCode.MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ELEMENTDECL));

		xml = "<!ELEMENTasdf";
		testDiagnosticsFor(xml, "test.dtd",
				d(0, 2, 9, DTDErrorCode.MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ELEMENTDECL));

		xml = "<!DOCTYPE asdf [\n" + //
				"  <!ELEMENTasdf\n" + //
				"]>";
		Diagnostic diagnostic = d(1, 4, 11, DTDErrorCode.MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ELEMENTDECL);
		testDiagnosticsFor(xml, diagnostic);
		testCodeActionsFor(xml, diagnostic, ca(diagnostic, te(1, 11, 1, 11, " ")));
		// With snippet support
		testCodeActionsFor(xml, diagnostic, XMLAssert.createSnippetEditSettings(),
				caSnippet(diagnostic, sne(1, 11, 1, 11, " $0")));
	}

	@Test
	public void MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ATTLISTDECL() throws Exception {
		String xml = "<!DOCTYPE asdf [\n" + //
				"  <!ATTLISTasdf\n" + //
				"]>";
		Diagnostic diagnostic = d(1, 4, 11, DTDErrorCode.MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ATTLISTDECL);
		testDiagnosticsFor(xml, diagnostic);
		testCodeActionsFor(xml, diagnostic, ca(diagnostic, te(1, 11, 1, 11, " ")));
		// With snippet support
		testCodeActionsFor(xml, diagnostic, XMLAssert.createSnippetEditSettings(),
				caSnippet(diagnostic, sne(1, 11, 1, 11, " $0")));
	}

	@Test
	public void MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_ENTITYDECL() throws Exception {
		String xml = "<!DOCTYPE asdf [\n" + //
				"  <!ENTITYasdf\n" + //
				"]>";
		Diagnostic diagnostic = d(1, 4, 10, DTDErrorCode.MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_ENTITYDECL);
		testDiagnosticsFor(xml, diagnostic);
		testCodeActionsFor(xml, diagnostic, ca(diagnostic, te(1, 10, 1, 10, " ")));
		// With snippet support
		testCodeActionsFor(xml, diagnostic, XMLAssert.createSnippetEditSettings(),
				caSnippet(diagnostic, sne(1, 10, 1, 10, " $0")));
	}

	@Test
	public void MSG_ATTRIBUTE_NOT_DECLARED_codeAction() throws Exception {
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"<!ELEMENT note (to,from,heading,body)>" + lineSeparator() + //
				"<!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"<!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"<!ELEMENT heading (#PCDATA)>" + lineSeparator() + //
				"<!ELEMENT body (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"    <to></to>" + lineSeparator() + //
				"    <from XXXX=\"\">Jani</from>" + lineSeparator() + //
				"    <heading>Reminder</heading>" + lineSeparator() + //
				"    <body>Don't forget me this weekend</body>" + lineSeparator() + //
				"</note>";
		Diagnostic d = d(10, 10, 14, DTDErrorCode.MSG_ATTRIBUTE_NOT_DECLARED);
		XMLAssert.testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(10, 9, 10, 17, "")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(10, 9, 10, 17, "$0")));
	}

	@Test
	public void MSG_ELEMENT_NOT_DECLARED_codeAction() throws Exception {
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note SYSTEM \"src/test/resources/dtd/note.dtd\">" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"    <too>Tove</too>" + lineSeparator() + //
				"    <from>Jani</from>" + lineSeparator() + //
				"    <body>Don't forget me this weekend</body>" + lineSeparator() + //
				"</note>";
		Diagnostic d = d(3, 5, 8, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED);
		XMLAssert.testDiagnosticsFor(xml, d, d(2, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID));
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(3, 5, 3, 8, "to"), te(3, 15, 3, 18, "to")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(3, 5, 3, 8, "to$0"), sne(3, 15, 3, 18, "to$0")));
	}

	@Test
	public void MSG_ATTRIBUTE_VALUE_NOT_IN_LIST_codeAction() throws Exception {
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE foo SYSTEM \"src/test/resources/dtd/foo-enum.dtd\">" + lineSeparator() + //
				"<foo>" + lineSeparator() + //
				"    <bar fruit=\"onne\">toto</bar>" + lineSeparator() + //
				"</foo>";
		Diagnostic d = d(3, 15, 21, DTDErrorCode.MSG_ATTRIBUTE_VALUE_NOT_IN_LIST);
		XMLAssert.testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(3, 16, 3, 20, "one")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(3, 16, 3, 20, "one$0")));
	}

	@Test
	public void MSG_ATTRIBUTE_VALUE_NOT_IN_LIST_codeAction_noSimilar() throws Exception {
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE foo SYSTEM \"src/test/resources/dtd/foo-enum.dtd\">" + lineSeparator() + //
				"<foo>" + lineSeparator() + //
				"    <bar fruit=\"xxxx\">toto</bar>" + lineSeparator() + //
				"</foo>";
		Diagnostic d = d(3, 15, 21, DTDErrorCode.MSG_ATTRIBUTE_VALUE_NOT_IN_LIST);
		XMLAssert.testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(3, 16, 3, 20, "one")),
				ca(d, te(3, 16, 3, 20, "three")),
				ca(d, te(3, 16, 3, 20, "two")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(3, 16, 3, 20, "one$0")),
				caSnippet(d, sne(3, 16, 3, 20, "three$0")),
				caSnippet(d, sne(3, 16, 3, 20, "two$0")));
	}

	@Test
	public void MSG_FIXED_ATTVALUE_INVALID_codeAction() throws Exception {
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE foo SYSTEM \"src/test/resources/dtd/foo-fixed.dtd\">" + lineSeparator() + //
				"<foo>" + lineSeparator() + //
				"    <bar status=\"wrong\">text</bar>" + lineSeparator() + //
				"</foo>";
		Diagnostic d = d(3, 16, 23, DTDErrorCode.MSG_FIXED_ATTVALUE_INVALID);
		XMLAssert.testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(3, 17, 3, 22, "active")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(3, 17, 3, 22, "active$0")));
	}

	@Test
	public void MSG_FIXED_ATTVALUE_INVALID_codeAction_inlineDTD() throws Exception {
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE address [" + lineSeparator() + //
				"  <!ELEMENT address (company)*>" + lineSeparator() + //
				"  <!ELEMENT company (#PCDATA)>" + lineSeparator() + //
				"  <!ATTLIST company name NMTOKEN #FIXED \"tutorialspoint\">" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<address>" + lineSeparator() + //
				"  <company name=\"etutorialspoint\">text</company>" + lineSeparator() + //
				"</address>";
		Diagnostic d = d(7, 16, 33, DTDErrorCode.MSG_FIXED_ATTVALUE_INVALID);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(7, 17, 7, 32, "tutorialspoint")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(7, 17, 7, 32, "tutorialspoint$0")));
	}

	@Test
	public void diagnosticRelatedInformationWithDOCTYPE() throws Exception {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(true);
		XMLValidationRootSettings validationSettings = new XMLValidationRootSettings();
		validationSettings.setCapabilities(new PublishDiagnosticsCapabilities(true)); // with related information
		settings.setValidation(validationSettings);

		String xml = "<!DOCTYPE foo SYSTEM \"dtd/foo-invalid.dtd\">\r\n" + //
				"<foo>\r\n" + //
				"   <bar></bar\r\n" + //
				"</foo>";
		Diagnostic diagnostic = new Diagnostic(r(0, 21, 0, 42), "There is '1' error in 'foo-invalid.dtd'.",
				DiagnosticSeverity.Error, "xml");
		diagnostic.setRelatedInformation(new ArrayList<>());
		String dtdFileURI = getGrammarFileURI("foo-invalid.dtd");
		diagnostic.getRelatedInformation().add(new DiagnosticRelatedInformation(l(dtdFileURI, r(0, 14, 0, 18)), ""));

		Diagnostic diagnosticBasedOnDTD = new Diagnostic(r(2, 10, 2, 13),
				"The end-tag for element type \"bar\" must end with a '>' delimiter.", DiagnosticSeverity.Error, "xml",
				XMLSyntaxErrorCode.ETagUnterminated.getCode());

		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		// First validation
		XMLAssert.testDiagnosticsFor(xmlLanguageService, xml, null, null, "src/test/resources/test.xml", false,
				settings, //
				diagnostic, diagnosticBasedOnDTD);
		// Restart the validation to check the validation is working since Xerces cache
		// the invalid DTD grammar
		XMLAssert.testDiagnosticsFor(xmlLanguageService, xml, null, null, "src/test/resources/test.xml", false,
				settings, //
				diagnostic, diagnosticBasedOnDTD);
	}

	@Test
	public void diagnosticRelatedInformationWithXMLModel() throws Exception {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(true);
		XMLValidationRootSettings validationSettings = new XMLValidationRootSettings();
		validationSettings.setCapabilities(new PublishDiagnosticsCapabilities(true)); // with related information
		settings.setValidation(validationSettings);

		String xml = "<?xml-model href=\"dtd/foo-invalid.dtd\" type=\"application/xml-dtd\"?>\r\n" + //
				"<foo>\r\n" + //
				"   <bar></bar\r\n" + //
				"</foo>";
		Diagnostic diagnostic = new Diagnostic(r(0, 17, 0, 38), "There is '1' error in 'foo-invalid.dtd'.",
				DiagnosticSeverity.Error, "xml");
		diagnostic.setRelatedInformation(new ArrayList<>());
		String dtdFileURI = getGrammarFileURI("foo-invalid.dtd");
		diagnostic.getRelatedInformation().add(new DiagnosticRelatedInformation(l(dtdFileURI, r(0, 14, 0, 18)), ""));

		Diagnostic diagnosticBasedOnDTD = new Diagnostic(r(2, 10, 2, 13),
				"The end-tag for element type \"bar\" must end with a '>' delimiter.", DiagnosticSeverity.Error, "xml",
				XMLSyntaxErrorCode.ETagUnterminated.getCode());

		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		// First validation
		XMLAssert.testDiagnosticsFor(xmlLanguageService, xml, null, null, "src/test/resources/test.xml", false,
				settings, //
				diagnostic, diagnosticBasedOnDTD);
		// Restart the validation to check the validation is working since Xerces cache
		// the invalid DTD grammar
		XMLAssert.testDiagnosticsFor(xmlLanguageService, xml, null, null, "src/test/resources/test.xml", false,
				settings, //
				diagnostic, diagnosticBasedOnDTD);
	}

	// ---------- New DTD error code tests ----------

	@Test
	public void DoctypedeclUnterminated() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!ELEMENT Person EMPTY>\r\n" + //
				"]\r\n" + //
				"<Person />";
		XMLAssert.testDiagnosticsFor(xml, d(3, 0, 1, DTDErrorCode.DoctypedeclUnterminated));
	}

	@Test
	public void DoctypedeclUnterminated_codeAction() throws Exception {
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE Person [" + lineSeparator() + //
				"	<!ELEMENT Person EMPTY>" + lineSeparator() + //
				"]" + lineSeparator() + //
				"<Person />";
		Diagnostic d = d(3, 0, 1, DTDErrorCode.DoctypedeclUnterminated);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(3, 1, 3, 1, ">")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(3, 1, 3, 1, ">$0")));
	}

	@Test
	public void MSG_CONTENT_INVALID_SPECIFIED() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE note [\r\n" + //
				"<!ELEMENT note (to,from)>\r\n" + //
				"<!ELEMENT to (#PCDATA)>\r\n" + //
				"<!ELEMENT from (#PCDATA)>\r\n" + //
				"<!ELEMENT bad (#PCDATA)>\r\n" + //
				"]>\r\n" + //
				"<note>\r\n" + //
				"	<to>Tove</to>\r\n" + //
				"	<bad>x</bad>\r\n" + //
				"</note>";
		XMLAssert.testDiagnosticsFor(xml, d(7, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID));
	}

	@Test
	public void MSG_CONTENT_INVALID_removeUndeclaredChild() throws Exception {
		// @formatter:off
		// <note> expects (to,from,heading,body) but has <XXX> which is not declared.
		// Code action proposes removing the <XXX>...</XXX> line.
		// @formatter:on
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"	<!ELEMENT note (to,from,heading,body)>" + lineSeparator() + //
				"	<!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT heading (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT body (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"	<to>Tove</to>" + lineSeparator() + //
				"	<XXX>undeclared</XXX>" + lineSeparator() + //
				"	<from>Jani</from>" + lineSeparator() + //
				"	<heading>Reminder</heading>" + lineSeparator() + //
				"	<body>Don't forget me this weekend</body>" + lineSeparator() + //
				"</note>";
		Diagnostic dNotDeclared = d(10, 2, 5, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED);
		Diagnostic dInvalid = d(8, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID);
		XMLAssert.testDiagnosticsFor(xml, dNotDeclared, dInvalid);
		// Code action on MSG_CONTENT_INVALID proposes removing <XXX> line
		testCodeActionsFor(xml, dInvalid,
				ca(dInvalid, te(10, 0, 11, 0, "")));
	}

	@Test
	public void MSG_CONTENT_INVALID_replaceWithSimilar() throws Exception {
		// @formatter:off
		// <note> expects (to,from,heading,body) but has <headign> (typo).
		// Code action proposes removing <headign> and replacing with <heading>.
		// @formatter:on
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"	<!ELEMENT note (to,from,heading,body)>" + lineSeparator() + //
				"	<!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT heading (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT body (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"	<to>Tove</to>" + lineSeparator() + //
				"	<from>Jani</from>" + lineSeparator() + //
				"	<headign>Reminder</headign>" + lineSeparator() + //
				"	<body>Don't forget me this weekend</body>" + lineSeparator() + //
				"</note>";
		Diagnostic dInvalid = d(8, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID);
		// Code action proposes removing <headign> and replacing with <heading>
		testCodeActionsFor(xml, dInvalid,
				ca(dInvalid, te(11, 0, 12, 0, "")),
				ca(dInvalid, te(11, 2, 11, 9, "heading"), te(11, 20, 11, 27, "heading")));
	}

	@Test
	public void MSG_CONTENT_INVALID_declaredButNotAllowed() throws Exception {
		// @formatter:off
		// <note> expects (to,from) but has <bad> which is declared but not allowed.
		// Code action proposes removing the <bad>...</bad> line.
		// @formatter:on
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"	<!ELEMENT note (to,from)>" + lineSeparator() + //
				"	<!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT bad (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"	<to>Tove</to>" + lineSeparator() + //
				"	<bad>x</bad>" + lineSeparator() + //
				"</note>";
		Diagnostic dInvalid = d(7, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID);
		XMLAssert.testDiagnosticsFor(xml, dInvalid);
		// Code action proposes removing <bad> line and inserting missing <from>
		testCodeActionsFor(xml, dInvalid,
				ca(dInvalid, te(9, 0, 10, 0, "")),
				ca(dInvalid, te(9, 0, 9, 0,
						"\t<from></from>" + lineSeparator())));
	}

	@Test
	public void MSG_CONTENT_INVALID_reorderChildren() throws Exception {
		// @formatter:off
		// <note> expects (to,from,heading,body) but <from> comes before <to>.
		// Code action proposes reordering children to match the content model.
		// @formatter:on
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"	<!ELEMENT note (to,from,heading,body)>" + lineSeparator() + //
				"	<!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT heading (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT body (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"	<from>Jani</from>" + lineSeparator() + //
				"	<to>Tove</to>" + lineSeparator() + //
				"	<heading>Reminder</heading>" + lineSeparator() + //
				"	<body>Don't forget me this weekend</body>" + lineSeparator() + //
				"</note>";
		Diagnostic dInvalid = d(8, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID);
		XMLAssert.testDiagnosticsFor(xml, dInvalid);
		// Code action proposes reordering: to, from, heading, body
		testCodeActionsFor(xml, dInvalid,
				ca(dInvalid, te(9, 0, 12, 42,
						"\t<to>Tove</to>" + lineSeparator() +
						"\t<from>Jani</from>" + lineSeparator() +
						"\t<heading>Reminder</heading>" + lineSeparator() +
						"\t<body>Don't forget me this weekend</body>")));
	}

	@Test
	public void MSG_CONTENT_INVALID_reorderChildren_allReversed() throws Exception {
		// @formatter:off
		// <note> expects (to,from,heading,body) but children are in reverse order.
		// @formatter:on
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"	<!ELEMENT note (to,from,heading,body)>" + lineSeparator() + //
				"	<!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT heading (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT body (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"	<body>text</body>" + lineSeparator() + //
				"	<heading>title</heading>" + lineSeparator() + //
				"	<from>Jani</from>" + lineSeparator() + //
				"	<to>Tove</to>" + lineSeparator() + //
				"</note>";
		Diagnostic dInvalid = d(8, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID);
		XMLAssert.testDiagnosticsFor(xml, dInvalid);
		testCodeActionsFor(xml, dInvalid,
				ca(dInvalid, te(9, 0, 12, 14,
						"\t<to>Tove</to>" + lineSeparator() +
						"\t<from>Jani</from>" + lineSeparator() +
						"\t<heading>title</heading>" + lineSeparator() +
						"\t<body>text</body>")));
	}

	@Test
	public void MSG_CONTENT_INVALID_reorderChildren_twoElements() throws Exception {
		// @formatter:off
		// <note> expects (to,from) but <from> comes before <to>.
		// Minimal reorder case with only 2 children.
		// @formatter:on
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"	<!ELEMENT note (to,from)>" + lineSeparator() + //
				"	<!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"	<from>Jani</from>" + lineSeparator() + //
				"	<to>Tove</to>" + lineSeparator() + //
				"</note>";
		Diagnostic dInvalid = d(6, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID);
		XMLAssert.testDiagnosticsFor(xml, dInvalid);
		// \t<to>Tove</to> = 14 chars
		testCodeActionsFor(xml, dInvalid,
				ca(dInvalid, te(7, 0, 8, 14,
						"\t<to>Tove</to>" + lineSeparator() +
						"\t<from>Jani</from>")));
	}

	@Test
	public void MSG_CONTENT_INVALID_insertMissing() throws Exception {
		// @formatter:off
		// <note> expects (to,from,heading,body) but <from> is missing.
		// Code action proposes inserting <from></from>.
		// @formatter:on
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"	<!ELEMENT note (to,from,heading,body)>" + lineSeparator() + //
				"	<!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT heading (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT body (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"	<to>Tove</to>" + lineSeparator() + //
				"	<heading>Reminder</heading>" + lineSeparator() + //
				"	<body>Don't forget me this weekend</body>" + lineSeparator() + //
				"</note>";
		Diagnostic dInvalid = d(8, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID);
		XMLAssert.testDiagnosticsFor(xml, dInvalid);
		// Insert missing <from> after <to> (line 9 → insert at line 10)
		testCodeActionsFor(xml, dInvalid,
				ca(dInvalid, te(10, 0, 10, 0,
						"\t<from></from>" + lineSeparator())));
	}

	@Test
	public void MSG_CONTENT_INVALID_insertMissing_snippet() throws Exception {
		// @formatter:off
		// Same as insertMissing but with snippet support — cursor at $0 inside element.
		// @formatter:on
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"	<!ELEMENT note (to,from,heading,body)>" + lineSeparator() + //
				"	<!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT heading (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT body (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"	<to>Tove</to>" + lineSeparator() + //
				"	<heading>Reminder</heading>" + lineSeparator() + //
				"	<body>Don't forget me this weekend</body>" + lineSeparator() + //
				"</note>";
		Diagnostic dInvalid = d(8, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID);
		testCodeActionsFor(xml, dInvalid, XMLAssert.createSnippetEditSettings(),
				caSnippet(dInvalid, sne(10, 0, 10, 0,
						"\t<from>$0</from>" + lineSeparator())));
	}

	@Test
	public void MSG_CONTENT_INVALID_insertMultipleMissing() throws Exception {
		// @formatter:off
		// <note> expects (to,from,heading,body) but <from> and <heading> are missing.
		// Code actions propose inserting each missing element after their predecessor.
		// @formatter:on
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"	<!ELEMENT note (to,from,heading,body)>" + lineSeparator() + //
				"	<!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT heading (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT body (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"	<to>Tove</to>" + lineSeparator() + //
				"	<body>Don't forget me this weekend</body>" + lineSeparator() + //
				"</note>";
		Diagnostic dInvalid = d(8, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID);
		XMLAssert.testDiagnosticsFor(xml, dInvalid);
		// Both from and heading insert after <to> (line 9 → insert at line 10)
		testCodeActionsFor(xml, dInvalid,
				ca(dInvalid, te(10, 0, 10, 0,
						"\t<from></from>" + lineSeparator())),
				ca(dInvalid, te(10, 0, 10, 0,
						"\t<heading></heading>" + lineSeparator())));
	}

	@Test
	public void MSG_CONTENT_INVALID_insertMissing_noPredecessor() throws Exception {
		// @formatter:off
		// <note> expects (to,from,heading,body) but <to> is missing. Since <to> is
		// the first expected element, it has no predecessor and inserts before the
		// first child.
		// @formatter:on
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"	<!ELEMENT note (to,from,heading,body)>" + lineSeparator() + //
				"	<!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT heading (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT body (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"	<from>Jani</from>" + lineSeparator() + //
				"	<heading>Reminder</heading>" + lineSeparator() + //
				"	<body>Don't forget me this weekend</body>" + lineSeparator() + //
				"</note>";
		Diagnostic dInvalid = d(8, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID);
		XMLAssert.testDiagnosticsFor(xml, dInvalid);
		// <to> has no predecessor → insert before first child (line 9)
		testCodeActionsFor(xml, dInvalid,
				ca(dInvalid, te(9, 0, 9, 0,
						"\t<to></to>" + lineSeparator())));
	}

	@Test
	public void MSG_CONTENT_INVALID_reorderAndInsertMissing() throws Exception {
		// @formatter:off
		// <note> expects (to,from,heading,body) but <from> is missing and
		// <heading> comes before <to>. Code actions propose both reordering and inserting.
		// @formatter:on
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"	<!ELEMENT note (to,from,heading,body)>" + lineSeparator() + //
				"	<!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT heading (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT body (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"	<heading>Reminder</heading>" + lineSeparator() + //
				"	<to>Tove</to>" + lineSeparator() + //
				"	<body>Don't forget me this weekend</body>" + lineSeparator() + //
				"</note>";
		Diagnostic dInvalid = d(8, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID);
		XMLAssert.testDiagnosticsFor(xml, dInvalid);
		// \t<body>Don't forget me this weekend</body> ends at col 42
		testCodeActionsFor(xml, dInvalid,
				// Reorder: to, heading, body
				ca(dInvalid, te(9, 0, 11, 42,
						"\t<to>Tove</to>" + lineSeparator() +
						"\t<heading>Reminder</heading>" + lineSeparator() +
						"\t<body>Don't forget me this weekend</body>")),
				// Insert missing: from after <to> (line 10 → insert at line 11)
				ca(dInvalid, te(11, 0, 11, 0,
						"\t<from></from>" + lineSeparator())));
	}

	@Test
	public void MSG_CONTENT_INVALID_removeAndInsertMissing() throws Exception {
		// @formatter:off
		// <note> expects (to,from,heading,body) but has <XXX> (undeclared) and
		// is missing <from>. Code actions propose removing <XXX> and inserting <from>.
		// @formatter:on
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"	<!ELEMENT note (to,from,heading,body)>" + lineSeparator() + //
				"	<!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT heading (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT body (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"	<to>Tove</to>" + lineSeparator() + //
				"	<XXX>bad</XXX>" + lineSeparator() + //
				"	<heading>Reminder</heading>" + lineSeparator() + //
				"	<body>Don't forget me this weekend</body>" + lineSeparator() + //
				"</note>";
		Diagnostic dNotDeclared = d(10, 2, 5, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED);
		Diagnostic dInvalid = d(8, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID);
		XMLAssert.testDiagnosticsFor(xml, dNotDeclared, dInvalid);
		testCodeActionsFor(xml, dInvalid,
				// Remove <XXX>
				ca(dInvalid, te(10, 0, 11, 0, "")),
				// Insert missing <from> after <to> (line 9 → insert at line 10)
				ca(dInvalid, te(10, 0, 10, 0,
						"\t<from></from>" + lineSeparator())));
	}

	@Test
	public void MSG_CONTENT_INVALID_removeReplaceAndInsertMissing() throws Exception {
		// @formatter:off
		// <note> expects (to,from,heading,body) but has <headign> (typo) and
		// is missing <from>. Code actions: remove, replace typo, insert missing.
		// <heading> is not inserted because <headign> is a similar typo.
		// @formatter:on
		String xml = "<?xml version=\"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE note [" + lineSeparator() + //
				"	<!ELEMENT note (to,from,heading,body)>" + lineSeparator() + //
				"	<!ELEMENT to (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT from (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT heading (#PCDATA)>" + lineSeparator() + //
				"	<!ELEMENT body (#PCDATA)>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<note>" + lineSeparator() + //
				"	<to>Tove</to>" + lineSeparator() + //
				"	<headign>Reminder</headign>" + lineSeparator() + //
				"	<body>Don't forget me this weekend</body>" + lineSeparator() + //
				"</note>";
		Diagnostic dNotDeclared = d(10, 2, 9, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED);
		Diagnostic dInvalid = d(8, 1, 5, DTDErrorCode.MSG_CONTENT_INVALID);
		XMLAssert.testDiagnosticsFor(xml, dNotDeclared, dInvalid);
		testCodeActionsFor(xml, dInvalid,
				// Remove <headign>
				ca(dInvalid, te(10, 0, 11, 0, "")),
				// Replace typo: headign → heading
				ca(dInvalid, te(10, 2, 10, 9, "heading"), te(10, 20, 10, 27, "heading")),
				// Insert missing <from> after <to> (line 9 → insert at line 10)
				ca(dInvalid, te(10, 0, 10, 0,
						"\t<from></from>" + lineSeparator())));
	}

	@Test
	public void IDInvalid() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!ELEMENT Person EMPTY>\r\n" + //
				"	<!ATTLIST Person Pin ID #REQUIRED>\r\n" + //
				"	]>\r\n" + //
				"<Person Pin=\"123\" />";
		XMLAssert.testDiagnosticsFor(xml, d(5, 12, 17, DTDErrorCode.IDInvalidWithNamespaces));
	}

	@Test
	public void IDNotUnique() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root (item*)>\r\n" + //
				"	<!ELEMENT item EMPTY>\r\n" + //
				"	<!ATTLIST item id ID #REQUIRED>\r\n" + //
				"	]>\r\n" + //
				"<root>\r\n" + //
				"	<item id=\"abc\" />\r\n" + //
				"	<item id=\"abc\" />\r\n" + //
				"</root>";
		XMLAssert.testDiagnosticsFor(xml, d(8, 10, 15, DTDErrorCode.IDNotUnique));
	}

	@Test
	public void IDREFInvalid() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!ELEMENT Person EMPTY>\r\n" + //
				"	<!ATTLIST Person Friend IDREF #IMPLIED>\r\n" + //
				"]>\r\n" + //
				"<Person Friend=\"123\" />";
		XMLAssert.testDiagnosticsFor(xml, d(5, 15, 20, DTDErrorCode.IDREFInvalidWithNamespaces));
	}

	@Test
	public void NMTOKENInvalid() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root EMPTY>\r\n" + //
				"	<!ATTLIST root token NMTOKEN #IMPLIED>\r\n" + //
				"]>\r\n" + //
				"<root token=\"a b\" />";
		XMLAssert.testDiagnosticsFor(xml, d(5, 12, 17, DTDErrorCode.NMTOKENInvalid));
	}

	@Test
	public void NMTOKENSInvalid() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root EMPTY>\r\n" + //
				"	<!ATTLIST root tokens NMTOKENS #IMPLIED>\r\n" + //
				"]>\r\n" + //
				"<root tokens=\"\" />";
		XMLAssert.testDiagnosticsFor(xml, d(5, 13, 15, DTDErrorCode.NMTOKENSInvalid));
	}

	@Test
	public void ExternalIDRequired() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!NOTATION name >\r\n" + //
				"]>\r\n" + //
				"<Person />";
		XMLAssert.testDiagnosticsFor(xml, d(2, 12, 16, DTDErrorCode.ExternalIDorPublicIDRequired));
	}

	@Test
	public void MixedContentUnterminated() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root (#PCDATA|item)>\r\n" + //
				"	<!ELEMENT item EMPTY>\r\n" + //
				"]>\r\n" + //
				"<root />";
		XMLAssert.testDiagnosticsFor(xml, d(2, 30, 31, DTDErrorCode.MixedContentUnterminated));
	}

	@Test
	public void EnumerationUnterminated() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root EMPTY>\r\n" + //
				"	<!ATTLIST root status (one|two >\r\n" + //
				"]>\r\n" + //
				"<root />";
		XMLAssert.testDiagnosticsFor(xml, d(3, 23, 31, DTDErrorCode.EnumerationUnterminated));
	}

	@Test
	public void NotationTypeUnterminated() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root EMPTY>\r\n" + //
				"	<!ATTLIST root ndata NOTATION (gif|jpg >\r\n" + //
				"]>\r\n" + //
				"<root />";
		XMLAssert.testDiagnosticsFor(xml, d(3, 31, 39, DTDErrorCode.NotationTypeUnterminated));
	}

	@Test
	public void MSG_SPACE_REQUIRED_BEFORE_CONTENTSPEC_IN_ELEMENTDECL() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root(#PCDATA)>\r\n" + //
				"]>\r\n" + //
				"<root />";
		Diagnostic d = d(2, 11, 15, DTDErrorCode.MSG_SPACE_REQUIRED_BEFORE_CONTENTSPEC_IN_ELEMENTDECL);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(2, 15, 2, 15, " ")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(2, 15, 2, 15, " $0")));
	}

	@Test
	public void SpaceRequiredAfterPUBLIC() throws Exception {
		// <!NOTATION name PUBLIC> → 'PUBLIC' is highlighted, code action inserts ' "publicId"'
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!NOTATION name PUBLIC> \r\n" + //
				"]>\r\n" + //
				"<Person />";
		Diagnostic d1 = d(2, 17, 23, DTDErrorCode.SpaceRequiredAfterPUBLIC);
		XMLAssert.testDiagnosticsFor(xml, d1);
		testCodeActionsFor(xml, d1, ca(d1, te(2, 23, 2, 23, " \"publicId\"")));
		testCodeActionsFor(xml, d1, XMLAssert.createSnippetEditSettings(),
				caSnippet(d1, sne(2, 23, 2, 23, " \"${1:publicId}\"$0")));
	}

	@Test
	public void MSG_ELEMENT_TYPE_REQUIRED_IN_MIXED_CONTENT() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root (#PCDATA| )>\r\n" + //
				"]>\r\n" + //
				"<root />";
		XMLAssert.testDiagnosticsFor(xml, d(2, 16, 27, DTDErrorCode.MSG_ELEMENT_TYPE_REQUIRED_IN_MIXED_CONTENT));
	}

	@Test
	public void MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN() throws Exception {
		// <!ELEMENT root > - 'root' is highlighted (missing content spec after element name)
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root >\r\n" + //
				"]>\r\n" + //
				"<root />";
		XMLAssert.testDiagnosticsFor(xml, d(2, 11, 15, DTDErrorCode.MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN));
	}

	@Test
	public void MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN_codeAction() throws Exception {
		// <!ELEMENT root > → code action inserts 'EMPTY', 'ANY' or '(#PCDATA)' after 'root'
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE root [" + lineSeparator() + //
				"	<!ELEMENT root >" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<root />";
		Diagnostic d = d(2, 11, 15, DTDErrorCode.MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(2, 15, 2, 15, " EMPTY")),
				ca(d, te(2, 15, 2, 15, " ANY")),
				ca(d, te(2, 15, 2, 15, " (#PCDATA)")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(2, 15, 2, 15, " EMPTY$0")),
				caSnippet(d, sne(2, 15, 2, 15, " ANY$0")),
				caSnippet(d, sne(2, 15, 2, 15, " (#PCDATA)$0")));
	}

	@Test
	public void MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN_codeAction_replace() throws Exception {
		// <!ELEMENT root XXXX> → 'root' is highlighted, code action replaces 'XXXX'
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE root [" + lineSeparator() + //
				"	<!ELEMENT root XXXX>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<root />";
		Diagnostic d = d(2, 11, 15, DTDErrorCode.MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(2, 16, 2, 20, "EMPTY")),
				ca(d, te(2, 16, 2, 20, "ANY")),
				ca(d, te(2, 16, 2, 20, "(#PCDATA)")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(2, 16, 2, 20, "EMPTY$0")),
				caSnippet(d, sne(2, 16, 2, 20, "ANY$0")),
				caSnippet(d, sne(2, 16, 2, 20, "(#PCDATA)$0")));
	}

	@Test
	public void MixedContentUnterminated_codeAction_missingParen() throws Exception {
		// Missing ')' and '*' → inserts ')*' before '>'
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root (#PCDATA|item >\r\n" + //
				"	<!ELEMENT item EMPTY>\r\n" + //
				"]>\r\n" + //
				"<root />";
		Diagnostic d = d(2, 30, 31, DTDErrorCode.MixedContentUnterminated);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(2, 30, 2, 30, ")*")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(2, 30, 2, 30, ")*$0")));
	}

	@Test
	public void MSG_CLOSE_PAREN_REQUIRED_IN_CHILDREN_codeAction() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root (a,b >\r\n" + //
				"]>\r\n" + //
				"<root />";
		Diagnostic d = d(2, 16, 21, DTDErrorCode.MSG_CLOSE_PAREN_REQUIRED_IN_CHILDREN);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(2, 21, 2, 21, ")")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(2, 21, 2, 21, ")$0")));
	}

	@Test
	public void MSG_WHITE_SPACE_IN_ELEMENT_CONTENT_WHEN_STANDALONE() throws Exception {
		// Standalone document with external element-only model;
		// whitespace between children resolves to a DOMText node.
		// The first whitespace offset lands on the text node (parent = note),
		// subsequent offsets land near sibling end tags.
		String xml = "<?xml version=\"1.0\" standalone=\"yes\"?>\r\n" + //
				"<!DOCTYPE note SYSTEM \"src/test/resources/dtd/note.dtd\">\r\n" + //
				"<note> <to>Tove</to> <from>Jani</from> <body>Test</body></note>";
		XMLAssert.testDiagnosticsFor(xml,
				d(2, 1, 5, DTDErrorCode.MSG_WHITE_SPACE_IN_ELEMENT_CONTENT_WHEN_STANDALONE),
				d(2, 8, 10, DTDErrorCode.MSG_WHITE_SPACE_IN_ELEMENT_CONTENT_WHEN_STANDALONE),
				d(2, 22, 26, DTDErrorCode.MSG_WHITE_SPACE_IN_ELEMENT_CONTENT_WHEN_STANDALONE));
	}

	@Test
	public void MSG_ELEMENT_ALREADY_DECLARED_codeAction() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root EMPTY>\r\n" + //
				"	<!ELEMENT root EMPTY>\r\n" + //
				"]>\r\n" + //
				"<root />";
		Diagnostic d = d(3, 11, 15, DTDErrorCode.MSG_ELEMENT_ALREADY_DECLARED);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(3, 0, 4, 0, "")));
	}

	@Test
	public void ExternalIDorPublicIDRequired_codeAction() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root EMPTY>\r\n" + //
				"	<!NOTATION name1 >\r\n" + //
				"]>\r\n" + //
				"<root />";
		Diagnostic d = d(3, 12, 17, DTDErrorCode.ExternalIDorPublicIDRequired);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(3, 17, 3, 17, " PUBLIC \"publicId\"")),
				ca(d, te(3, 17, 3, 17, " SYSTEM \"systemId\"")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(3, 17, 3, 17, " PUBLIC \"${1:publicId}\"$0")),
				caSnippet(d, sne(3, 17, 3, 17, " SYSTEM \"${1:systemId}\"$0")));
	}

	@Test
	public void QuoteRequiredInPublicID_codeAction() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root EMPTY>\r\n" + //
				"	<!NOTATION name1 PUBLIC asd>\r\n" + //
				"]>\r\n" + //
				"<root />";
		Diagnostic d = d(3, 25, 28, DTDErrorCode.QuoteRequiredInPublicID);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(3, 25, 3, 28, "\"asd\"")));
	}

	@Test
	public void QuoteRequiredInSystemID_codeAction() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root EMPTY>\r\n" + //
				"	<!NOTATION name2 SYSTEM asd>\r\n" + //
				"]>\r\n" + //
				"<root />";
		Diagnostic d = d(3, 25, 28, DTDErrorCode.QuoteRequiredInSystemID);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(3, 25, 3, 28, "\"asd\"")));
	}

	@Test
	public void OpenQuoteMissingInDecl_codeAction() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root EMPTY>\r\n" + //
				"	<!ENTITY myEntity value>\r\n" + //
				"]>\r\n" + //
				"<root />";
		Diagnostic d = d(3, 19, 24, DTDErrorCode.OpenQuoteMissingInDecl);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(3, 19, 3, 24, "\"value\"")));
	}

	@Test
	public void InvalidCharInPublicID_codeAction() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root EMPTY>\r\n" + //
				"	<!NOTATION n1 PUBLIC \"unclosed>\r\n" + //
				"]>\r\n" + //
				"<root />";
		Diagnostic d = d(3, 22, 31, DTDErrorCode.InvalidCharInPublicID);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(3, 31, 3, 31, "\"")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(3, 31, 3, 31, "\"$0")));
	}

	@Test
	public void EnumerationUnterminated_codeAction_attlist() throws Exception {
		// ATTLIST context: closes with ')' and adds default declaration
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root EMPTY>\r\n" + //
				"	<!ATTLIST root status (one >\r\n" + //
				"]>\r\n" + //
				"<root />";
		Diagnostic d = d(3, 23, 27, DTDErrorCode.EnumerationUnterminated);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(3, 27, 3, 27, ") #REQUIRED")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(3, 27, 3, 27, ") ${1|#REQUIRED,#IMPLIED|}$0")));
	}

	@Test
	public void ENTITYNotUnparsed() throws Exception {
		// ENTITY attribute referencing an undeclared entity;
		// Xerces reports ENTITYNotUnparsed (not ENTITYInvalid) when the entity is not declared.
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE root [\r\n" + //
				"	<!ELEMENT root EMPTY>\r\n" + //
				"	<!ATTLIST root ref ENTITY #IMPLIED>\r\n" + //
				"]>\r\n" + //
				"<root ref=\"undeclared\" />";
		XMLAssert.testDiagnosticsFor(xml, d(5, 10, 22, DTDErrorCode.ENTITYNotUnparsed));
	}


	@Test
	public void MSG_MORE_THAN_ONE_ID_ATTRIBUTE() throws Exception {
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE root [" + lineSeparator() + //
				"	<!ELEMENT root EMPTY>" + lineSeparator() + //
				"	<!ATTLIST root" + lineSeparator() + //
				"		id1 ID #REQUIRED" + lineSeparator() + //
				"		id2 ID #REQUIRED" + lineSeparator() + //
				"	>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<root id1=\"a\" id2=\"b\" />";
		Diagnostic d = d(5, 2, 5, DTDErrorCode.MSG_MORE_THAN_ONE_ID_ATTRIBUTE);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(5, 0, 6, 0, "")));
	}

	@Test
	public void NoNotationOnEmptyElement() throws Exception {
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE root [" + lineSeparator() + //
				"	<!ELEMENT root EMPTY>" + lineSeparator() + //
				"	<!NOTATION gif SYSTEM \"image/gif\">" + lineSeparator() + //
				"	<!ATTLIST root pic NOTATION (gif) #IMPLIED>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<root />";
		Diagnostic d = d(4, 16, 19, DTDErrorCode.NoNotationOnEmptyElement);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(4, 0, 5, 0, "")));
	}

	@Test
	public void MSG_NOTATION_NOT_DECLARED_FOR_UNPARSED_ENTITYDECL() throws Exception {
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE root [" + lineSeparator() + //
				"	<!ELEMENT root EMPTY>" + lineSeparator() + //
				"	<!ENTITY logo SYSTEM \"logo.gif\" NDATA gif>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<root />";
		Diagnostic d = d(3, 10, 14, DTDErrorCode.MSG_NOTATION_NOT_DECLARED_FOR_UNPARSED_ENTITYDECL);
		XMLAssert.testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(3, 0, 3, 0, "\t<!NOTATION gif SYSTEM \"\">" + lineSeparator())));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(3, 0, 3, 0, "\t<!NOTATION gif SYSTEM \"$1\">$0" + lineSeparator())));
	}

	@Test
	public void NotationTypeUnterminated_codeAction() throws Exception {
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE root [" + lineSeparator() + //
				"	<!ELEMENT root EMPTY>" + lineSeparator() + //
				"	<!ATTLIST root ndata NOTATION (gif>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<root />";
		Diagnostic d = d(3, 31, 35, DTDErrorCode.NotationTypeUnterminated);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(3, 35, 3, 35, ") #REQUIRED")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(3, 35, 3, 35, ") ${1|#REQUIRED,#IMPLIED|}$0")));
	}

	@Test
	public void DuplicateTypeInMixedContent() throws Exception {
		// @formatter:off
		// <!ELEMENT root (#PCDATA|item|item)*>
		//                              |--| ← error range on second 'item'
		//                             |----| ← code action removes '|item'
		// @formatter:on
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE root [" + lineSeparator() + //
				"	<!ELEMENT root (#PCDATA|item|item)*>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<root />";
		Diagnostic d = d(2, 30, 34, DTDErrorCode.DuplicateTypeInMixedContent);
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(2, 29, 2, 34, "")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(2, 29, 2, 34, "$0")));
	}

	@Test
	public void DuplicateTypeInMixedContent_multipleElementDecls() throws Exception {
		// @formatter:off
		// <!ELEMENT root (#PCDATA|item)*>
		// <!ELEMENT root (#PCDATA|foo|foo)*>
		//                             |--| ← error on second 'foo', code action removes '|foo'
		// @formatter:on
		String xml = "<?xml version = \"1.0\"?>" + lineSeparator() + //
				"<!DOCTYPE root [" + lineSeparator() + //
				"	<!ELEMENT root (#PCDATA|item)*>" + lineSeparator() + //
				"	<!ELEMENT root (#PCDATA|foo|foo)*>" + lineSeparator() + //
				"]>" + lineSeparator() + //
				"<root />";
		Diagnostic d1 = d(3, 11, 15, DTDErrorCode.MSG_ELEMENT_ALREADY_DECLARED);
		Diagnostic d2 = d(3, 29, 32, DTDErrorCode.DuplicateTypeInMixedContent);
		XMLAssert.testDiagnosticsFor(xml, d2, d1);
		testCodeActionsFor(xml, d2,
				ca(d2, te(3, 28, 3, 32, "")));
	}

	private static void testDiagnosticsFor(String xml, Diagnostic... expected) {
		XMLAssert.testDiagnosticsFor(xml, "src/test/resources/catalogs/catalog.xml", expected);
	}

	private static void testPublicDiagnosticsFor(String xml, Diagnostic... expected) {
		XMLAssert.testDiagnosticsFor(xml, "src/test/resources/catalogs/catalog-public.xml", expected);
	}

	private static void testDiagnosticsFor(String xml, String fileURI, Diagnostic... expected) {
		XMLAssert.testDiagnosticsFor(xml, "src/test/resources/catalogs/catalog.xml", null, fileURI, expected);
	}

	private static String getGrammarFileURI(String grammarURI) throws MalformedURIException {
		int index = grammarURI.lastIndexOf('.');
		String path = grammarURI.substring(index + 1, grammarURI.length());
		return XMLEntityManager.expandSystemId(grammarURI, "src/test/resources/" + path + "/test.xml", true);
	}
}
