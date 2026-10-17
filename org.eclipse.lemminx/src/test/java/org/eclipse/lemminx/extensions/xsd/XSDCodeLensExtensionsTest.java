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
package org.eclipse.lemminx.extensions.xsd;

import static org.eclipse.lemminx.XMLAssert.cl;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.client.ClientCommands.OPEN_BINDING_WIZARD;
import static org.eclipse.lemminx.client.ClientCommands.OPEN_URI;
import static org.eclipse.lemminx.client.ClientCommands.SHOW_REFERENCES;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.junit.jupiter.api.Test;

/**
 * XSD codelens tests
 *
 */
public class XSDCodeLensExtensionsTest extends AbstractCacheBasedTest {

	@Test
	public void codeLensOnGroupAndElement() throws BadLocationException {
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.w3.org/2001/XMLSchema\">\r\n"
				+ //
				"	<xs:element name=\"elt\" />\r\n" + //
				"	<xs:complexType name=\"aComplexType\" >\r\n" + //
				"		<xs:sequence>\r\n" + //
				"			<xs:element ref=\"xs:elt\" />\r\n" + //
				"			<xs:group ref=\"xs:elt\"></xs:group>\r\n" + //
				"		</xs:sequence>\r\n" + //
				"	</xs:complexType>\r\n" + //
				"	<xs:element name=\"elt2\" substitutionGroup=\"xs:elt\" />\r\n" + //
				"\r\n" + //
				"	<xs:group name=\"elt\" >\r\n" + //
				"		<xs:sequence></xs:sequence>\r\n" + //
				"	</xs:group>\r\n" + //
				"	\r\n" + //
				"</xs:schema>";
		XMLAssert.testCodeLensFor(xml, //
				cl(r(0, 0, 0, 0), "http://www.w3.org/2001/XMLSchema (embedded xml.xsd)", OPEN_URI), //
				cl(r(2, 13, 2, 23), "2 references", SHOW_REFERENCES), //
				cl(r(11, 11, 11, 21), "1 reference", SHOW_REFERENCES));
	}

	@Test
	public void codeLensOnComplexTypeAndSimpleType() throws BadLocationException {
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.w3.org/2001/XMLSchema\">\r\n"
				+ //
				"	<xs:element name=\"elt\" type=\"xs:aComplexType\" />\r\n" + //
				"	<xs:complexType name=\"aComplexType\">\r\n" + //
				"		<xs:simpleContent>\r\n" + //
				"			<xs:extension base=\"xs:aSimpleTypeA\"></xs:extension>\r\n" + //
				"		</xs:simpleContent>\r\n" + //
				"	</xs:complexType>\r\n" + //
				"	\r\n" + //
				"	<xs:complexType name=\"aComplexType2\">\r\n" + //
				"		<xs:complexContent>\r\n" + //
				"			<xs:extension base=\"xs:aComplexType\"></xs:extension>\r\n" + //
				"		</xs:complexContent>\r\n" + //
				"	</xs:complexType>\r\n" + //
				"\r\n" + //
				"	<xs:simpleType name=\"aSimpleTypeA\">\r\n" + //
				"		<xs:restriction base=\"xs:ENTITIES\">\r\n" + //
				"		</xs:restriction>\r\n" + //
				"	</xs:simpleType>\r\n" + //
				"</xs:schema>";
		XMLAssert.testCodeLensFor(xml, //
				cl(r(0, 0, 0, 0), "http://www.w3.org/2001/XMLSchema (embedded xml.xsd)", OPEN_URI), //
				cl(r(3, 17, 3, 36), "2 references", SHOW_REFERENCES), //
				cl(r(15, 16, 15, 35), "1 reference", SHOW_REFERENCES));
	}

	@Test
	public void codeLensEmptyDocument() throws BadLocationException {
		String xml = "";
		XMLAssert.testCodeLensFor(xml, cl(r(0, 0, 0, 0), "Bind to grammar/schema...", OPEN_BINDING_WIZARD));
	}

	@Test
	public void codeLensSpace() throws BadLocationException {
		String xml = " ";
		XMLAssert.testCodeLensFor(xml, cl(r(0, 0, 0, 0), "Bind to grammar/schema...", OPEN_BINDING_WIZARD));
	}

}
