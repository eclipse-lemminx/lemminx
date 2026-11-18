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
package org.eclipse.lemminx.extensions.catalog;

import static org.eclipse.lemminx.XMLAssert.c;
import static org.eclipse.lemminx.XMLAssert.d;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.contentmodel.participants.XMLSchemaErrorCode;
import org.junit.jupiter.api.Test;

/**
 * Test of catalog completion/validation which doesn't declare DTD or XML
 * Schema.
 *
 */
public class XMLCatalogExtensionTest extends AbstractCacheBasedTest {

	@Test
	public void completion() throws BadLocationException {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\r\n" + //
				"    |";

		XMLAssert.testCompletionFor(xml, 15 + 4 /* CDATA and Comments */, c("public", "<public publicId=\"\" uri=\"\" />"));
	}

	@Test
	public void diagnostics() throws BadLocationException {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\r\n" + //
				"    <bad /> // error validation, because \"bad\" doesn't belong to the XML Schema catalog\r\n" + //
				"</catalog> ";

		XMLAssert.testDiagnosticsFor(xml, d(2, 5, 8, XMLSchemaErrorCode.cvc_complex_type_2_4_a), //
				d(2, 12, 14, XMLSchemaErrorCode.cvc_complex_type_2_3), //
				d(1, 1, 1, 8, XMLCatalogErrorCode.catalog_not_registered));
	}
}
