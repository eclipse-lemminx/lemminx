/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.dtd;

import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.d;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.eclipse.lemminx.XMLAssert.testCodeActionsFor;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.contentmodel.participants.DTDErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

/**
 * DTD file diagnostics.
 *
 */
public class DTDDiagnosticsTest extends AbstractCacheBasedTest {

	@Test
	public void EntityDeclUnterminated() throws Exception {
		String dtd = "<!ENTITY copyright \"Copyright W3Schools.\"\r\n" + //
				"<!ELEMENT element-name (#PCDATA)>";
		testDiagnosticsFor(dtd, "test.dtd", d(0, 19, 41, DTDErrorCode.EntityDeclUnterminated));
	}

	@Test
	public void OpenQuoteExpected() throws Exception {
		String dtd = "<!ATTLIST dadesadministratives idinstitut ID >";
		Diagnostic d = d(0, 31, 41, DTDErrorCode.OpenQuoteExpected);
		testDiagnosticsFor(dtd, "test.dtd", d);
		// testCodeActionsFor(dtd, "test.dtd", d, ca(d, "test.dtd", te(0, 31, 0, 41, "\"idinstitut\"")));
	}

	public static void testDiagnosticsFor(String xml, String fileURI, Diagnostic... expected) {
		XMLAssert.testDiagnosticsFor(xml, null, null, fileURI, true, expected);
	}

	public static void testDiagnosticsFor(String xml, String fileURI, ContentModelSettings settings,
			Diagnostic... expected) {
		XMLAssert.testDiagnosticsFor(xml, null, null, fileURI, true, settings, expected);
	}

	public static void testDiagnosticsFor(XMLLanguageService ls, String xml, String fileURI,
			ContentModelSettings settings, Diagnostic... expected) {
		XMLAssert.testDiagnosticsFor(ls, xml, null, null, fileURI, true, settings, expected);
	}
}
