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

import static org.eclipse.lemminx.XMLAssert.d;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.contentmodel.participants.DTDErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.participants.XMLSyntaxErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

/**
 * DTD doctype diagnostics services tests
 *
 */
public class DTDDoctypeDiagnosticsTest extends AbstractCacheBasedTest {

	@Test
	public void MSG_ELEMENT_TYPE_REQUIRED_IN_ELEMENTDECL() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE student [\r\n" + //
				"  <!ELEMENT \r\n" + // <-- error
				"]>\r\n" + //
				"<student />";
		XMLAssert.testDiagnosticsFor(xml, d(2, 4, 11, DTDErrorCode.MSG_ELEMENT_TYPE_REQUIRED_IN_ELEMENTDECL));
	}

	@Test
	public void ElementDeclUnterminated() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE student [\r\n" + //
				"  <!ELEMENT student (surname,id)>\r\n" + //
				"  <!ELEMENT surname (#PCDATA)\r\n" + // <- error
				"]>\r\n" + //
				"<student />";
		XMLAssert.testDiagnosticsFor(xml, d(3, 20, 29, DTDErrorCode.ElementDeclUnterminated));
	}

	@Test
	public void MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE student [\r\n" + //
				"  <!ELEMENT surname\r\n" + // <- error
				"]>\r\n" + //
				"<student />";
		XMLAssert.testDiagnosticsFor(xml,
				d(2, 12, 19, DTDErrorCode.MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN));
	}

	@Test
	public void disableDTDValidationWhenNoElementDecl() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<!DOCTYPE article [\r\n" + //
				"  <!ENTITY nbsp \"entity-value\">\r\n" + //
				"]>           \r\n" + //
				"<article>\r\n" + //
				"	&nbsp;\r\n" + //
				"</article>";
		// No error, here DOCTYPE declares only ENTITY -> DTD validation is disabled
		XMLAssert.testDiagnosticsFor(xml);

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<!DOCTYPE article [\r\n" + //
				"  <!ENTITY nbsp \"entity-value\">\r\n" + //
				"  <!ELEMENT element-name (#PCDATA)>\r\n" + //
				"]>           \r\n" + //
				"<article>\r\n" + //
				"	&nbsp;\r\n" + //
				"</article>";
		// Error -> DTD validation defines a !ELEMENT entity-value which doesn't match
		// the article root element
		XMLAssert.testDiagnosticsFor(xml, d(5, 1, 8, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<!DOCTYPE article [\r\n" + //
				"  <!ENTITY nbsp \"entity-value\">\r\n" + //
				"  <!ELEMENT article (#PCDATA)>\r\n" + //
				"]>           \r\n" + //
				"<article>\r\n" + //
				"	&nbsp;\r\n" + //
				"</article>";
		// No error, DTD validation is done and XML matches the article element
		// declaration
		XMLAssert.testDiagnosticsFor(xml);
	}

	@Test
	public void doctypeNotAllowed() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE student [\r\n" + // <-- error DOCTYPE is disallow
				"  <!ELEMENT \r\n" + //
				"]>\r\n" + //
				"<student />";
		testDiagnosticsDisallowDocTypeDecl(xml, d(1, 0, 3, 2, XMLSyntaxErrorCode.DoctypeNotAllowed));
	}

	private static void testDiagnosticsDisallowDocTypeDecl(String xml, Diagnostic diagnostic) {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(false);
		XMLValidationRootSettings validationSettings = new XMLValidationRootSettings();
		validationSettings.setDisallowDocTypeDecl(true);
		settings.setValidation(validationSettings);

		XMLAssert.testDiagnosticsFor(xml, null, null, null, true, settings, diagnostic);
	}

}
