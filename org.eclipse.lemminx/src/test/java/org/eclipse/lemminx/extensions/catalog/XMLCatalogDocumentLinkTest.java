/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/

package org.eclipse.lemminx.extensions.catalog;

import static org.eclipse.lemminx.XMLAssert.dl;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.testDocumentLinkFor;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.junit.jupiter.api.Test;

/**
 * Tests for the document links in XML catalog files
 */
public class XMLCatalogDocumentLinkTest extends AbstractCacheBasedTest {

	private static String CATALOG_PATH = "src/test/resources/catalog.xml";

	@Test
	public void testPublicEntryDocumentLink() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <public id=\"http://example.org\" uri=\"mySchema.xsd\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(1, 39, 1, 51), "src/test/resources/mySchema.xsd"));
	}

	@Test
	public void testSystemEntryDocumentLink() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <system id=\"http://example.org\"\n" + //
				"    uri=\"otherSchema.xsd\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(2, 9, 2, 24), "src/test/resources/otherSchema.xsd"));
	}

	@Test
	public void testURIEntryDocumentLink() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <uri\n" + //
				"    id=\"http://example.org\"\n" + //
				"    uri=\"neatSchema.xsd\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(3, 9, 3, 23), "src/test/resources/neatSchema.xsd"));
	}

	@Test
	public void testCatalogWithCatalogDOCTYPE() {
		String xml = "<!DOCTYPE catalog PUBLIC \"-//OASIS//DTD XML Catalogs V1.1//EN\" \"catalog.dtd\" >\n"
				+ //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <uri\n" + //
				"    id=\"http://example.org\"\n" + //
				"    uri=\"neatSchema.xsd\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(0, 64, 0, 75), "src/test/resources/catalog.dtd"), //
				dl(r(4, 9, 4, 23), "src/test/resources/neatSchema.xsd"));

	}

	@Test
	public void testCatalogWithCatalogDOCTYPEZeroLength() {
		String xml = "<!DOCTYPE catalog PUBLIC \"-//OASIS//DTD XML Catalogs V1.1//EN\" \"catalog.dtd\" >\n"
				+ //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <uri\n" + //
				"    id=\"\"\n" + //
				"    uri=\"\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH,
				dl(r(0, 64, 0, 75), "src/test/resources/catalog.dtd"));

	}

	@Test
	public void testSystemSuffixEntryDocumentLink() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <systemSuffix id=\"http://example.org\" uri=\"mySchema.xsd\"></systemSuffix>\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(1, 45, 1, 57), "src/test/resources/mySchema.xsd"));
	}

	@Test
	public void testSystemSuffixEntryZeroLengthDocumentLink() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <systemSuffix id=\"http://example.org\" uri=\"\"></systemSuffix>\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH);
	}

	@Test
	public void testURISuffixEntryDocumentLink() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <uriSuffix id=\"http://example.org\" uri=\"mySchema.xsd\"></uriSuffix>\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(1, 42, 1, 54), "src/test/resources/mySchema.xsd"));
	}

	@Test
	public void testURISuffixEntryZeroLengthDocumentLink() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <uriSuffix id=\"\" uri=\"\"></uriSuffix>\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH);
	}

	@Test
	public void testMustBeCatalog1() {
		String xml = "<catalog><public url=\"document.xsd\" /></catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH);
	}

	@Test
	public void testMustBeCatalog2() {
		String xml = "<aaa xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <public url=\"document.xsd\" />\n" + //
				"</aaa>";
		testDocumentLinkFor(xml, CATALOG_PATH);
	}

	@Test
	public void testOnlyEntriesHaveLinks() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <aaa id=\"http://example.org\" uri=\"mySchema.xsd\"></aaa>\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH);
	}

	@Test
	public void testPublicEntryWithSpacesDocumentLink() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <public id=\"http://example.org\" uri=\"my%20schema.xsd\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(1, 39, 1, 54), "src/test/resources/my schema.xsd"));
	}

	@Test
	public void testDelegatePublicEntry() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <delegatePublic catalog=\"catalogs/catalog-public.xml\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(1, 27, 1, 54), "src/test/resources/catalogs/catalog-public.xml"));
	}

	@Test
	public void testDelegatePublicEntryZeroLength() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <delegatePublic catalog=\"\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH);
	}

	@Test
	public void testDelegateSystemEntry() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <delegateSystem catalog=\"catalogs/catalog-public.xml\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(1, 27, 1, 54), "src/test/resources/catalogs/catalog-public.xml"));
	}

	@Test
	public void testDelegateSystemEntryZeroLength() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <delegateSystem catalog=\"\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH);
	}

	@Test
	public void testDelegateUriEntry() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <delegateUri catalog=\"catalogs/catalog-public.xml\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(1, 24, 1, 51), "src/test/resources/catalogs/catalog-public.xml"));
	}

	@Test
	public void testNextCatalogEntry() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <nextCatalog catalog=\"catalogs/catalog-public.xml\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(1, 24, 1, 51), "src/test/resources/catalogs/catalog-public.xml"));
	}

	@Test
	public void testCatalogEntryWithoutCatalog() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <public catalog=\"catalogs/catalog-public.xml\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH);
	}

	@Test
	public void testCatalogEntryWithoutURI() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <nextCatalog uri=\"catalogs/catalog-public.xml\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH);
	}

	@Test
	public void testEmptyNextCatalogEntry() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <nextCatalog catalog=\"\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH);
	}

	@Test
	public void testBlankNextCatalogEntry() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <nextCatalog catalog=\"    	\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH);
	}

	@Test
	public void testEmptyPublicCatalogEntry() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <public id=\"http://example.org\" uri=\"\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH);
	}

	@Test
	public void testBlankPublicCatalogEntry() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <public id=\"http://example.org\" uri=\"    	\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH);
	}

	@Test
	public void testCatalogURIEntryWithXMLBase() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\" xml:base=\"dtd\">\n" + //
				"  <public id=\"http://example.org\" uri=\"svg.dtd\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(1, 39, 1, 46), "src/test/resources/dtd/svg.dtd"));
	}

	@Test
	public void testCatalogURIEntryWithXMLBaseRelativeReference() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\" xml:base=\"./dtd\">\n" + //
				"  <public id=\"http://example.org\" uri=\"svg.dtd\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(1, 39, 1, 46), "src/test/resources/dtd/svg.dtd"));
	}

	@Test
	public void testCatalogURIEntryWithXMLBaseTrailingBackslash() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\" xml:base=\"dtd/\">\n" + //
				"  <public id=\"http://example.org\" uri=\"svg.dtd\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(1, 39, 1, 46), "src/test/resources/dtd/svg.dtd"));
	}

	@Test
	public void testCatalogURIEntryGroupWithBase() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <group xml:base=\"dtd\">\n" + //
				"    <public id=\"http://example.org\" uri=\"svg.dtd\" />\n" + //
				"  </group>\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(2, 41, 2, 48), "src/test/resources/dtd/svg.dtd"));
	}

	@Test
	public void testCatalogURIEntryGroupWithoutBase() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <group>\n" + //
				"    <public id=\"http://example.org\" uri=\"dtd/svg.dtd\" />\n" + //
				"  </group>\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(2, 41, 2, 52), "src/test/resources/dtd/svg.dtd"));
	}

	@Test
	public void testCatalogURIWithCatalogAndGroupBase() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\" xml:base=\"abc\">\n" + //
				"  <group xml:base=\"def\">\n" + //
				"    <public id=\"http://example.org\" uri=\"svg.dtd\" />\n" + //
				"  </group>\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(2, 41, 2, 48), "src/test/resources/abc/def/svg.dtd"));
	}

	@Test
	public void testCatalogCatalogEntryWithXMLBase() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\" xml:base=\"catalog\">\n" + //
				"  <nextCatalog catalog=\"catalog-liferay.xml\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(1, 24, 1, 43), "src/test/resources/catalog/catalog-liferay.xml"));
	}

	@Test
	public void testCatalogCatalogEntryWithXMLBaseRelativeReference() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\" xml:base=\"./catalog\">\n" + //
				"  <nextCatalog catalog=\"catalog-liferay.xml\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(1, 24, 1, 43), "src/test/resources/catalog/catalog-liferay.xml"));
	}

	@Test
	public void testCatalogCatalogEntryWithXMLBaseTrailingBackslash() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\" xml:base=\"catalog/\">\n" + //
				"  <nextCatalog catalog=\"catalog-liferay.xml\" />\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(1, 24, 1, 43), "src/test/resources/catalog/catalog-liferay.xml"));
	}

	@Test
	public void testCatalogCatalogEntryGroupWithBase() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <group xml:base=\"catalog\">\n" + //
				"    <nextCatalog catalog=\"catalog-liferay.xml\" />\n" + //
				"  </group>\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(2, 26, 2, 45), "src/test/resources/catalog/catalog-liferay.xml"));
	}

	@Test
	public void testCatalogCatalogEntryGroupWithoutBase() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <group>\n" + //
				"    <nextCatalog catalog=\"catalog/catalog-liferay.xml\" />\n" + //
				"  </group>\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(2, 26, 2, 53), "src/test/resources/catalog/catalog-liferay.xml"));
	}

	@Test
	public void testCatalogCatalogWithCatalogAndGroupBase() {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\" xml:base=\"abc\">\n" + //
				"  <group xml:base=\"def\">\n" + //
				"    <nextCatalog catalog=\"catalog.xml\" />\n" + //
				"  </group>\n" + //
				"</catalog>";
		testDocumentLinkFor(xml, CATALOG_PATH, //
				dl(r(2, 26, 2, 37), "src/test/resources/abc/def/catalog.xml"));
	}

}