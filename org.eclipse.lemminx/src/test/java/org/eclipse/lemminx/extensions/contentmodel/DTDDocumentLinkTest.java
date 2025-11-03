/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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

import static org.eclipse.lemminx.XMLAssert.dl;
import static org.eclipse.lemminx.XMLAssert.r;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.junit.jupiter.api.Test;

/**
 * DTD document links tests
 *
 */
public class DTDDocumentLinkTest extends AbstractCacheBasedTest {

	@Test
	public void docTypeSYSTEM() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
				"<!DOCTYPE root-element SYSTEM \"../dtd/entities/base.dtd\" [\r\n" + //
				"\r\n" + //
				"]>\r\n" + //
				"<root-element />";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/xml/base.xml",
				dl(r(1, 31, 1, 55), "src/test/resources/dtd/entities/base.dtd"));
	}

	@Test
	public void docTypeSYSTEMZeroLengh() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
				"<!DOCTYPE root-element SYSTEM \"\" [\r\n" + //
				"\r\n" + //
				"]>\r\n" + //
				"<root-element />";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/xml/base.xml");
	}

	@Test
	public void docTypePUBLIC() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
				"<!DOCTYPE root-element PUBLIC \"ABCD\" \"../dtd/entities/base.dtd\" [\r\n" + //
				"\r\n" + //
				"]>\r\n" + //
				"<root-element />";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/xml/base.xml",
				dl(r(1, 38, 1, 62), "src/test/resources/dtd/entities/base.dtd"));
	}

	@Test
	public void docTypePUBLICZeroLength() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
				"<!DOCTYPE root-element PUBLIC \"ABCD\" \"\" [\r\n" + //
				"\r\n" + //
				"]>\r\n" + //
				"<root-element />";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/xml/base.xml");
	}

	@Test
	public void noLinks() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
				"<!DOCTYPE root-element \"ABCD\" \"../dtd/entities/base.dtd\" [\r\n" + // here it misses PUBLIC
				"\r\n" + //
				"]>\r\n" + //
				"<root-element />";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/xml/base.xml");
	}

	@Test
	public void entity() throws BadLocationException {
		String xml = "<!DOCTYPE chapter PUBLIC \"-//OASIS//DTD DocBook XML V4.4//EN\" \"../docbookx.dtd\" [\r\n" + //
				"	<!ENTITY % document SYSTEM \"../document.ent\">\r\n" + //
				"	%document;\r\n" + //
				"	]> \r\n" + //
				"	<chapter>\r\n" + //
				"	</chapter>";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/xml/base.xml",
				dl(r(0, 63, 0, 78), "src/test/resources/docbookx.dtd"), //
				dl(r(1, 29, 1, 44), "src/test/resources/document.ent"));
	}

	@Test
	public void linkWithCatalogAndPublic() throws Exception {
		// This test uses the local DTD with catalog-public.xml by using the PUBLIC ID
		// -//Sun Microsystems, Inc.//DTD Web Application 2.3//EN
		// <public publicId="-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
		// uri="../dtd/web-app_2_3.dtd" />
		String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?> \r\n" + //
				"<!DOCTYPE web-app\r\n" + //
				"   PUBLIC \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\"\r\n" + //
				"   \"ABCD.dtd\">\r\n" + //
				"\r\n" + //
				"<web-app></web-app>";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/xml/base.xml",
				"src/test/resources/catalogs/catalog-public.xml",
				dl(r(3, 4, 3, 12), "src/test/resources/dtd/web-app_2_3.dtd"));
	}
}
