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
package org.eclipse.lemminx.extensions.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMParser;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CatalogUtils}.
 */
public class CatalogUtilsTest {

	private static final String CATALOG_NS = "urn:oasis:names:tc:entity:xmlns:xml:catalog";

	// --- getCatalogIdentifierAttr ---

	@Test
	public void testGetIdentifierAttr_namespace() {
		DOMElement el = catalogElement("<namespace name=\"http://example.org/ns\" uri=\"schema.xsd\"/>");
		DOMAttr attr = CatalogUtils.getCatalogIdentifierAttr(el);
		assertEquals("name", attr.getName());
		assertEquals("http://example.org/ns", attr.getValue());
	}

	@Test
	public void testGetIdentifierAttr_uri() {
		DOMElement el = catalogElement("<uri name=\"http://example.org/ns\" uri=\"schema.xsd\"/>");
		DOMAttr attr = CatalogUtils.getCatalogIdentifierAttr(el);
		assertEquals("name", attr.getName());
		assertEquals("http://example.org/ns", attr.getValue());
	}

	@Test
	public void testGetIdentifierAttr_system() {
		DOMElement el = catalogElement("<system systemId=\"http://example.org/dtd\" uri=\"schema.dtd\"/>");
		DOMAttr attr = CatalogUtils.getCatalogIdentifierAttr(el);
		assertEquals("systemId", attr.getName());
		assertEquals("http://example.org/dtd", attr.getValue());
	}

	@Test
	public void testGetIdentifierAttr_public() {
		DOMElement el = catalogElement("<public publicId=\"-//Ex//DTD//EN\" uri=\"schema.dtd\"/>");
		DOMAttr attr = CatalogUtils.getCatalogIdentifierAttr(el);
		assertEquals("publicId", attr.getName());
		assertEquals("-//Ex//DTD//EN", attr.getValue());
	}

	@Test
	public void testGetIdentifierAttr_unknownElement() {
		DOMElement el = catalogElement("<rewriteURI uriStartString=\"http://\" rewritePrefix=\"./\"/>");
		assertNull(CatalogUtils.getCatalogIdentifierAttr(el));
	}

	@Test
	public void testGetIdentifierAttr_null() {
		assertNull(CatalogUtils.getCatalogIdentifierAttr(null));
	}

	// --- getCatalogIdentifier ---

	@Test
	public void testGetIdentifier_namespace() {
		DOMElement el = catalogElement("<namespace name=\"http://example.org/ns\" uri=\"schema.xsd\"/>");
		assertEquals("http://example.org/ns", CatalogUtils.getCatalogIdentifier(el));
	}

	@Test
	public void testGetIdentifier_uri() {
		DOMElement el = catalogElement("<uri name=\"http://foo\" uri=\"foo.xsd\"/>");
		assertEquals("http://foo", CatalogUtils.getCatalogIdentifier(el));
	}

	// --- helpers ---

	private static DOMElement catalogElement(String entryXml) {
		String src = "<catalog xmlns=\"" + CATALOG_NS + "\">\n  " + entryXml + "\n</catalog>";
		DOMDocument doc = DOMParser.getInstance().parse(src, "file:///catalog.xml", null);
		DOMElement root = doc.getDocumentElement();
		for (DOMNode child : root.children()) {
			if (child.isElement()) {
				return (DOMElement) child;
			}
		}
		return null;
	}
}
