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
package org.eclipse.lemminx.extensions.dtd;

import static org.eclipse.lemminx.XMLAssert.dl;
import static org.eclipse.lemminx.XMLAssert.r;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.junit.jupiter.api.Test;

/**
 * DTD entities document links tests inside a DTD.
 *
 */
public class DTDDocumentLinkTest extends AbstractCacheBasedTest {

	@Test
	public void entity() throws BadLocationException {
		String xml = "<!ENTITY % document SYSTEM \"../document.ent\">";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/xml/base.dtd",
				dl(r(0, 28, 0, 43), "src/test/resources/document.ent"));
	}
	
	@Test
	public void entityZeroLength() throws BadLocationException {
		String xml = "<!ENTITY % document SYSTEM \"\">";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/xml/base.dtd");
	}
}
