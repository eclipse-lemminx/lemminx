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
package org.eclipse.lemminx.extensions.contentmodel;

import static org.eclipse.lemminx.XMLAssert.CDATA_SNIPPETS;
import static org.eclipse.lemminx.XMLAssert.COMMENT_SNIPPETS;
import static org.eclipse.lemminx.XMLAssert.DTDNODE_SNIPPETS;
import static org.eclipse.lemminx.XMLAssert.PROCESSING_INSTRUCTION_SNIPPETS;
import static org.eclipse.lemminx.XMLAssert.c;
import static org.eclipse.lemminx.XMLAssert.te;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemCapabilities;
import org.eclipse.lsp4j.MarkupKind;
import org.junit.jupiter.api.Test;

public class DTDCompletionExtensionsTest extends AbstractCacheBasedTest {

	@Test
	public void completionInRoot() throws BadLocationException {
		// completion on <|
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"  <!DOCTYPE catalog\r\n" + //
				"    PUBLIC \"-//OASIS//DTD Entity Resolution XML Catalog V1.0//EN\"\r\n" + //
				"           \"http://www.oasis-open.org/committees/entity/release/1.0/catalog.dtd\">\r\n" + //
				"\r\n" + //
				"  <catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\"\r\n" + //
				"           prefer=\"public\">\r\n" + //
				"\r\n" + //
				"    <|";
		testCompletionFor(xml,
				c("delegatePublic", te(8, 4, 8, 5, "<delegatePublic publicIdStartString=\"$1\" catalog=\"$2\" />$0"),
						"<delegatePublic"), //
				c("public", te(8, 4, 8, 5, "<public publicId=\"$1\" uri=\"$2\" />$0"), "<public"));
	}

	@Test
	public void completionWithChoiceAttribute() throws BadLocationException {
		// completion on <|
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"  <!DOCTYPE catalog\r\n" + //
				"    PUBLIC \"-//OASIS//DTD Entity Resolution XML Catalog V1.0//EN\"\r\n" + //
				"           \"http://www.oasis-open.org/committees/entity/release/1.0/catalog.dtd\">\r\n" + //
				"\r\n" + //
				"  <catalog |";
		testCompletionFor(xml, c("prefer", te(5, 11, 5, 11, "prefer=\"${1|system,public|}\"$0"), "prefer"));
	}

	@Test
	public void testCompletionDocumentationWithSource() throws BadLocationException {
		// completion on <|
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"  <!DOCTYPE catalog\r\n" + //
				"    PUBLIC \"-//OASIS//DTD Entity Resolution XML Catalog V1.0//EN\"\r\n" + //
				"           \"http://www.oasis-open.org/committees/entity/release/1.0/catalog.dtd\">\r\n" + //
				"\r\n" + //
				"  <|";
		testCompletionFor(xml,
				c("catalog", te(5, 2, 5, 3, "<catalog>$1</catalog>$0"), "<catalog",
						"$Id: catalog.dtd,v 1.10 2002/10/18 23:54:58 ndw Exp $" + System.lineSeparator()
								+ System.lineSeparator() + "Source: catalog.dtd",
						MarkupKind.PLAINTEXT));
	}

	@Test
	public void externalDTDCompletionElement() throws BadLocationException {
		// completion on <|
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
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
				"	|" + //
				"</Folks>";
		XMLAssert.testCompletionFor(xml, c("Person", te(11, 1, 11, 1, "<Person Pin=\"\"></Person>"), "Person"));
	}

	@Test
	public void externalDTDCompletionAttribute() throws BadLocationException {
		// completion on <|
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
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
				"	<Person |" + //
				"</Folks>";
		XMLAssert.testCompletionFor(xml, c("Pin", te(11, 9, 11, 9, "Pin=\"\""), "Pin"), //
				c("Friend", te(11, 9, 11, 9, "Friend=\"\""), "Friend"), //
				c("Likes", te(11, 9, 11, 9, "Likes=\"\""), "Likes"));
	}

	@Test
	public void externalDTDCompletionElementDecl() throws BadLocationException {
		// completion on <|
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Folks [\r\n" + //
				"	<!ELEMENT Folks (Person*)>\r\n" + //
				"	<!ELEMENT|\r\n" + //
				"]>\r\n" + //
				"<Folks>\r\n" + //
				"	" + //
				"</Folks>";
		testCompletionFor(xml, c("Insert DTD Element Declaration",
				te(3, 1, 3, 10, "<!ELEMENT ${1:element-name} (${2:#PCDATA})>"), "<!ELEMENT"));
	}

	@Test
	public void externalDTDCompletionElementDecl2() throws BadLocationException {
		// completion on <|
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Folks [\r\n" + //
				"	<!ELEMENT Folks (Person*)>\r\n" + //
				"	<!ELEM|\r\n" + //
				"]>\r\n" + //
				"<Folks>\r\n" + //
				"	" + //
				"</Folks>";
		testCompletionFor(xml, c("Insert DTD Element Declaration",
				te(3, 1, 3, 7, "<!ELEMENT ${1:element-name} (${2:#PCDATA})>"), "<!ELEMENT"));
	}

	@Test
	public void externalDTDCompletionAllDecls() throws BadLocationException {
		// completion on <|
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Folks [\r\n" + //
				"	<!ELEMENT Folks (Person*)>\r\n" + //
				"	|\r\n" + //
				"]>\r\n" + //
				"<Folks>\r\n" + //
				"	" + //
				"</Folks>";
		testCompletionFor(xml, true, //
				DTDNODE_SNIPPETS /* DTD node snippets */ + //
						COMMENT_SNIPPETS /* Comment snippets */ , //
				c("Insert DTD Element Declaration", te(3, 1, 3, 1, "<!ELEMENT ${1:element-name} (${2:#PCDATA})>"),
						"<!ELEMENT"),
				c("Insert Internal DTD Entity Declaration",
						te(3, 1, 3, 1, "<!ENTITY ${1:entity-name} \"${2:entity-value}\">"), "<!ENTITY"),
				c("Insert DTD Attributes List Declaration",
						te(3, 1, 3, 1, "<!ATTLIST ${1:element-name} ${2:attribute-name} ${3:ID} ${4:#REQUIRED}>"),
						"<!ATTLIST"),
				c("Insert External DTD Entity Declaration",
						te(3, 1, 3, 1, "<!ENTITY ${1:entity-name} SYSTEM \"${2:entity-value}\">"), "<!ENTITY"));
	}

	@Test
	public void externalDTDCompletionAllDeclsItemDefaults() throws BadLocationException {
		// completion on <|
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Folks [\r\n" + //
				"	<!ELEMENT Folks (Person*)>\r\n" + //
				"	|\r\n" + //
				"]>\r\n" + //
				"<Folks>\r\n" + //
				"	" + //
				"</Folks>";
		testCompletionFor(xml, true, true, //
				DTDNODE_SNIPPETS /* DTD node snippets */ + //
						COMMENT_SNIPPETS /* Comment snippets */ , "catalog.xml", //
				c("Insert DTD Element Declaration", te(3, 1, 3, 1, "<!ELEMENT ${1:element-name} (${2:#PCDATA})>"),
						"<!ELEMENT"),
				c("Insert Internal DTD Entity Declaration",
						te(3, 1, 3, 1, "<!ENTITY ${1:entity-name} \"${2:entity-value}\">"), "<!ENTITY"),
				c("Insert DTD Attributes List Declaration",
						te(3, 1, 3, 1, "<!ATTLIST ${1:element-name} ${2:attribute-name} ${3:ID} ${4:#REQUIRED}>"),
						"<!ATTLIST"),
				c("Insert External DTD Entity Declaration",
						te(3, 1, 3, 1, "<!ENTITY ${1:entity-name} SYSTEM \"${2:entity-value}\">"), "<!ENTITY"));
	}

	@Test
	public void externalDTDCompletionAllDeclsSnippetsNotSupported() throws BadLocationException {
		// completion on <|
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Folks [\r\n" + //
				"	<!ELEMENT Folks (Person*)>\r\n" + //
				"	|\r\n" + //
				"]>\r\n" + //
				"<Folks>\r\n" + //
				"	" + //
				"</Folks>";
		testCompletionFor(xml, false, //
				DTDNODE_SNIPPETS /* DTD node snippets */ + //
						COMMENT_SNIPPETS /* Comment snippets */ , //
				c("Insert DTD Element Declaration", te(3, 1, 3, 1, "<!ELEMENT element-name (#PCDATA)>"), "<!ELEMENT"),
				c("Insert Internal DTD Entity Declaration", te(3, 1, 3, 1, "<!ENTITY entity-name \"entity-value\">"),
						"<!ENTITY"),
				c("Insert DTD Attributes List Declaration",
						te(3, 1, 3, 1, "<!ATTLIST element-name attribute-name ID #REQUIRED>"), "<!ATTLIST"),
				c("Insert External DTD Entity Declaration",
						te(3, 1, 3, 1, "<!ENTITY entity-name SYSTEM \"entity-value\">"), "<!ENTITY"));
	}

	@Test
	public void testNoDuplicateCompletionItems() throws BadLocationException {
		// completion on <|
		String xml = "<?xml version=\"1.0\" standalone=\"no\" ?>\n"
				+ "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.0//EN\" \"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg.dtd\">\n"
				+ "<svg xmlns=\"http://www.w3.org/2000/svg\">\n" + "    <animate attributeName=\"foo\">\n"
				+ "        <|\n" + // <-- completion
				"    </animate>\n" + "</svg>";
		testCompletionFor(xml, false, 3 + 4 /* CDATA and Comments */,
				c("desc", te(4, 8, 4, 9, "<desc></desc>"), "<desc"),
				c("metadata", te(4, 8, 4, 9, "<metadata></metadata>"), "<metadata"),
				c("title", te(4, 8, 4, 9, "<title></title>"), "<title"));
	}

	@Test
	public void elementCompletionWithDoctypeSubsetWithNoElements() throws BadLocationException {
		String xml = "<!DOCTYPE web-app PUBLIC \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\" \"http://java.sun.com/dtd/web-app_2_3.dtd\" [\r\n"
				+ //
				"  <!ENTITY mdash \"&#x2014;\">\r\n" + //
				"]>\r\n" + //
				"m|\r\n" + // <-- completion here
				"<web-app>\r\n" + //
				"  <display-name>Servlet 2.3 aWeb Application</display-name>\r\n" + //
				"</web-app>";
		testCompletionFor(xml, false, 76 + //
				CDATA_SNIPPETS /* CDATA */ + //
				COMMENT_SNIPPETS /* Comments */ + //
				PROCESSING_INSTRUCTION_SNIPPETS /* Processing Instruction Snippets */,
				c("web-app", te(3, 0, 3, 1, "<web-app></web-app>"), "web-app"),
				c("auth-constraint", te(3, 0, 3, 1, "<auth-constraint></auth-constraint>"), "auth-constraint"));
	}

	@Test
	public void attributeCompletionWithDoctypeSubsetWithNoElements() throws BadLocationException {
		String xml = "<!DOCTYPE web-app PUBLIC \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\" \"http://java.sun.com/dtd/web-app_2_3.dtd\" [\r\n"
				+ //
				"  <!ENTITY mdash \"&#x2014;\">\r\n" + //
				"]>\r\n" + //
				"<web-app a|>\r\n" + // <-- completion here
				"  <display-name>Servlet 2.3 aWeb Application</display-name>\r\n" + //
				"</web-app>";
		testCompletionFor(xml, false, 3, c("id", te(3, 9, 3, 10, "id=\"\""), "id"), //
				c("xmlns", te(3, 9, 3, 10, "xmlns=\"\""), "xmlns"), //
				c("xmlns:xsi", te(3, 9, 3, 10, "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""),
						"xmlns:xsi"));
	}

	@Test
	public void completionWithCatalogAndPublic() throws Exception {
		// This test uses the local DTD with catalog-public.xml by using the PUBLIC ID
		// -//Sun Microsystems, Inc.//DTD Web Application 2.3//EN
		// <public publicId="-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
		// uri="../dtd/web-app_2_3.dtd" />
		String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?> \r\n" + //
				"<!DOCTYPE web-app\r\n" + //
				"   PUBLIC \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\"\r\n" + //
				"   \"ABCD.dtd\">\r\n" + //
				"\r\n" + //
				"<web-app>|</web-app>";
		testCompletionFor(xml, true, null, "catalog-public.xml",
				c("icon", te(5, 9, 5, 9, "<icon>$1</icon>$0"), "icon"), //
				c("display-name", te(5, 9, 5, 9, "<display-name>$1</display-name>$0"), "display-name"));
	}

	private void testCompletionFor(String xml, CompletionItem... expectedItems) throws BadLocationException {
		testCompletionFor(xml, true, null, expectedItems);
	}

	private void testCompletionFor(String xml, boolean isSnippetsSupported, Integer expectedCount,
			CompletionItem... expectedItems) throws BadLocationException {
		testCompletionFor(xml, isSnippetsSupported, expectedCount, "catalog.xml", expectedItems);
	}

	private void testCompletionFor(String xml, boolean isSnippetsSupported, Integer expectedCount, String catalog,
			CompletionItem... expectedItems) throws BadLocationException {
		testCompletionFor(xml, isSnippetsSupported, false, expectedCount, catalog, expectedItems);
	}

	private void testCompletionFor(String xml, boolean isSnippetsSupported, boolean enableItemDefaults,
			Integer expectedCount, String catalog,
			CompletionItem... expectedItems) throws BadLocationException {
		CompletionCapabilities completionCapabilities = new CompletionCapabilities();
		CompletionItemCapabilities completionItem = new CompletionItemCapabilities(isSnippetsSupported); // activate
																											// snippets
		completionCapabilities.setCompletionItem(completionItem);

		SharedSettings sharedSettings = new SharedSettings();
		sharedSettings.getCompletionSettings().setCapabilities(completionCapabilities);
		XMLAssert.testCompletionFor(new XMLLanguageService(), xml, "src/test/resources/catalogs/" + catalog, null, null,
				expectedCount, sharedSettings, enableItemDefaults, expectedItems);
	}
}
