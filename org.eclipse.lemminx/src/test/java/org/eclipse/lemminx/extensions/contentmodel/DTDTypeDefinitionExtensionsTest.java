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
package org.eclipse.lemminx.extensions.contentmodel;

import static org.eclipse.lemminx.XMLAssert.ll;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.testTypeDefinitionFor;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.junit.jupiter.api.Test;

/**
 * DTD type definition tests.
 *
 */
public class DTDTypeDefinitionExtensionsTest extends AbstractCacheBasedTest {

	// ------- External DTD: Element type definition -------

	@Test
	public void externalDTDElementRoot() throws BadLocationException {
		String xmlFile = "src/test/resources/test-dtd-type-def.xml";
		String dtdFile = "dtd/book.dtd";

		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE book SYSTEM \"dtd/book.dtd\">\r\n" + //
				"<bo|ok title=\"\" isbn=\"\">\r\n" + //
				"</book>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetDtdURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, dtdFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetDtdURI, r(2, 1, 2, 5), r(0, 10, 0, 14)));
	}

	@Test
	public void externalDTDElementChild() throws BadLocationException {
		String xmlFile = "src/test/resources/test-dtd-type-def.xml";
		String dtdFile = "dtd/book.dtd";

		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE book SYSTEM \"dtd/book.dtd\">\r\n" + //
				"<book title=\"\" isbn=\"\">\r\n" + //
				"    <chap|ter id=\"\" title=\"\">\r\n" + //
				"    </chapter>\r\n" + //
				"</book>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetDtdURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, dtdFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetDtdURI, r(3, 5, 3, 12), r(7, 10, 7, 17)));
	}

	@Test
	public void externalDTDElementGrandchild() throws BadLocationException {
		String xmlFile = "src/test/resources/test-dtd-type-def.xml";
		String dtdFile = "dtd/book.dtd";

		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE book SYSTEM \"dtd/book.dtd\">\r\n" + //
				"<book title=\"\" isbn=\"\">\r\n" + //
				"    <chapter id=\"\" title=\"\">\r\n" + //
				"        <sec|tion name=\"\">text</section>\r\n" + //
				"    </chapter>\r\n" + //
				"</book>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetDtdURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, dtdFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetDtdURI, r(4, 9, 4, 16), r(13, 10, 13, 17)));
	}

	// ------- External DTD: Attribute type definition -------

	@Test
	public void externalDTDAttributeFirst() throws BadLocationException {
		String xmlFile = "src/test/resources/test-dtd-type-def.xml";
		String dtdFile = "dtd/book.dtd";

		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE book SYSTEM \"dtd/book.dtd\">\r\n" + //
				"<book ti|tle=\"\" isbn=\"\">\r\n" + //
				"</book>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetDtdURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, dtdFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetDtdURI, r(2, 6, 2, 11), r(2, 2, 2, 7)));
	}

	@Test
	public void externalDTDAttributeSecond() throws BadLocationException {
		String xmlFile = "src/test/resources/test-dtd-type-def.xml";
		String dtdFile = "dtd/book.dtd";

		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE book SYSTEM \"dtd/book.dtd\">\r\n" + //
				"<book title=\"\" is|bn=\"\">\r\n" + //
				"</book>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetDtdURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, dtdFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetDtdURI, r(2, 15, 2, 19), r(3, 2, 3, 6)));
	}

	@Test
	public void externalDTDAttributeOnChild() throws BadLocationException {
		String xmlFile = "src/test/resources/test-dtd-type-def.xml";
		String dtdFile = "dtd/book.dtd";

		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE book SYSTEM \"dtd/book.dtd\">\r\n" + //
				"<book title=\"\" isbn=\"\">\r\n" + //
				"    <chapter i|d=\"\" title=\"\">\r\n" + //
				"    </chapter>\r\n" + //
				"</book>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetDtdURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, dtdFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetDtdURI, r(3, 13, 3, 15), r(9, 2, 9, 4)));
	}

	// ------- Internal DTD: Element type definition -------

	@Test
	public void internalDTDElement() throws BadLocationException {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE note [\r\n" + //
				"  <!ELEMENT note (to, from, body)>\r\n" + //
				"  <!ELEMENT to (#PCDATA)>\r\n" + //
				"  <!ELEMENT from (#PCDATA)>\r\n" + //
				"  <!ELEMENT body (#PCDATA)>\r\n" + //
				"]>\r\n" + //
				"<no|te>\r\n" + //
				"  <to>user</to>\r\n" + //
				"</note>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		testTypeDefinitionFor(xmlLanguageService, xml, null,
				ll("test://test/test.xml", r(7, 1, 7, 5), r(2, 12, 2, 16)));
	}

	@Test
	public void internalDTDElementChild() throws BadLocationException {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE note [\r\n" + //
				"  <!ELEMENT note (to, from, body)>\r\n" + //
				"  <!ELEMENT to (#PCDATA)>\r\n" + //
				"  <!ELEMENT from (#PCDATA)>\r\n" + //
				"  <!ELEMENT body (#PCDATA)>\r\n" + //
				"]>\r\n" + //
				"<note>\r\n" + //
				"  <t|o>user</to>\r\n" + //
				"</note>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		testTypeDefinitionFor(xmlLanguageService, xml, null,
				ll("test://test/test.xml", r(8, 3, 8, 5), r(3, 12, 3, 14)));
	}

	// ------- Internal DTD: Attribute type definition -------

	@Test
	public void internalDTDAttribute() throws BadLocationException {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE note [\r\n" + //
				"  <!ELEMENT note (#PCDATA)>\r\n" + //
				"  <!ATTLIST note\r\n" + //
				"    date CDATA #REQUIRED\r\n" + //
				"  >\r\n" + //
				"]>\r\n" + //
				"<note da|te=\"2024-01-01\">Hello</note>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		testTypeDefinitionFor(xmlLanguageService, xml, null,
				ll("test://test/test.xml", r(7, 6, 7, 10), r(4, 4, 4, 8)));
	}

	@Test
	public void internalDTDAttributeSecondInMultiAttlist() throws BadLocationException {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE note [\r\n" + //
				"  <!ELEMENT note (#PCDATA)>\r\n" + //
				"  <!ATTLIST note\r\n" + //
				"    date CDATA #REQUIRED\r\n" + //
				"    author CDATA #IMPLIED\r\n" + //
				"  >\r\n" + //
				"]>\r\n" + //
				"<note date=\"\" au|thor=\"\">Hello</note>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		testTypeDefinitionFor(xmlLanguageService, xml, null,
				ll("test://test/test.xml", r(8, 14, 8, 20), r(5, 4, 5, 10)));
	}

	@Test
	public void internalDTDAttributeWithSeparateAttlists() throws BadLocationException {
		// Each attribute declared in its own <!ATTLIST> tag
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE note [\r\n" + //
				"  <!ELEMENT note (#PCDATA)>\r\n" + //
				"  <!ATTLIST note date CDATA #REQUIRED>\r\n" + //
				"  <!ATTLIST note author CDATA #IMPLIED>\r\n" + //
				"]>\r\n" + //
				"<note date=\"\" au|thor=\"\">Hello</note>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		testTypeDefinitionFor(xmlLanguageService, xml, null,
				ll("test://test/test.xml", r(6, 14, 6, 20), r(4, 17, 4, 23)));
	}

	// ------- Parameter entity: Element type definition -------

	@Test
	public void parameterEntityElement() throws BadLocationException {
		// book-main.dtd includes book-chapter.ent via %chapter;
		// chapter is declared in book-chapter.ent, not directly in book-main.dtd
		String xmlFile = "src/test/resources/test-dtd-type-def.xml";
		String entityFile = "dtd/book-chapter.ent";

		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE book SYSTEM \"dtd/book-main.dtd\">\r\n" + //
				"<book title=\"\" isbn=\"\">\r\n" + //
				"    <chap|ter id=\"\" title=\"\">\r\n" + //
				"    </chapter>\r\n" + //
				"</book>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetEntityURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, entityFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetEntityURI, r(3, 5, 3, 12), r(0, 10, 0, 17)));
	}

	@Test
	public void parameterEntityAttribute() throws BadLocationException {
		// chapter's ATTLIST is declared in book-chapter.ent
		String xmlFile = "src/test/resources/test-dtd-type-def.xml";
		String entityFile = "dtd/book-chapter.ent";

		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE book SYSTEM \"dtd/book-main.dtd\">\r\n" + //
				"<book title=\"\" isbn=\"\">\r\n" + //
				"    <chapter i|d=\"\" title=\"\">\r\n" + //
				"    </chapter>\r\n" + //
				"</book>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetEntityURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, entityFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetEntityURI, r(3, 13, 3, 15), r(2, 2, 2, 4)));
	}

	@Test
	public void parameterEntityElementDirect() throws BadLocationException {
		// book is declared directly in book-main.dtd (not via parameter entity)
		String xmlFile = "src/test/resources/test-dtd-type-def.xml";
		String dtdFile = "dtd/book-main.dtd";

		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE book SYSTEM \"dtd/book-main.dtd\">\r\n" + //
				"<bo|ok title=\"\" isbn=\"\">\r\n" + //
				"</book>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetDtdURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, dtdFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetDtdURI, r(2, 1, 2, 5), r(0, 10, 0, 14)));
	}

	// ------- No type definition -------

	@Test
	public void noTypeDefinitionWithoutGrammar() throws BadLocationException {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<ro|ot>\r\n" + //
				"</root>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		testTypeDefinitionFor(xmlLanguageService, xml, null);
	}

	@Test
	public void noTypeDefinitionForUndeclaredElement() throws BadLocationException {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE note [\r\n" + //
				"  <!ELEMENT note (#PCDATA)>\r\n" + //
				"]>\r\n" + //
				"<note>\r\n" + //
				"  <un|known>text</unknown>\r\n" + //
				"</note>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		testTypeDefinitionFor(xmlLanguageService, xml, null);
	}

	@Test
	public void noTypeDefinitionForUndeclaredAttribute() throws BadLocationException {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE note [\r\n" + //
				"  <!ELEMENT note (#PCDATA)>\r\n" + //
				"]>\r\n" + //
				"<note un|known=\"\">Hello</note>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		testTypeDefinitionFor(xmlLanguageService, xml, null);
	}
}
