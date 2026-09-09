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
package org.eclipse.lemminx.extensions.relaxng.xml.diagnostics;

import static org.eclipse.lemminx.XMLAssert.d;
import static org.eclipse.lemminx.XMLAssert.testDiagnosticsFor;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.extensions.relaxng.xml.validator.RelaxNGErrorCode;
import org.junit.jupiter.api.Test;

/**
 * XML Validation tests with RelaxNG by using xml-model processing instruction
 * association.
 *
 */
public class RelaxNGDiagnosticsV1Test extends AbstractCacheBasedTest {

	@Test
	public void valid() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v1.rng\" ?>\r\n" + //
				"<addressBook>\r\n" + //
				"  <card>\r\n" + //
				"    <name>John Smith</name>\r\n" + //
				"    <email>js@example.com</email>\r\n" + //
				"  </card>\r\n" + //
				"  <card>\r\n" + //
				"    <name>Fred Bloggs</name>\r\n" + //
				"    <email>fb@example.net</email>\r\n" + //
				"  </card>\r\n" + //
				"</addressBook>";
		testDiagnosticsFor(xml);
	}

	@Test
	public void invalidFileRng() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBookXXX.rng\" ?>\r\n" + // addressBookXXX.rng
																									// doesn't exists
				"<addressBook>\r\n" + //
				"  <card>\r\n" + //
				"    <name>John Smith</name>\r\n" + //
				"    <email>js@example.com</email>\r\n" + //
				"  </card>\r\n" + //
				"  <card>\r\n" + //
				"    <name>Fred Bloggs</name>\r\n" + //
				"    <email>fb@example.net</email>\r\n" + //
				"  </card>\r\n" + //
				"</addressBook>";
		testDiagnosticsFor(xml, //
				d(0, 17, 64, RelaxNGErrorCode.RelaxNGNotFound));
	}

	@Test
	public void unkwown_element() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v1.rng\" ?>\r\n" + //
				"<addressBook>\r\n" + //
				"  <card>\r\n" + //
				"    <nameXXX>John Smith</nameXXX>\r\n" + // unknown_element -> element "nameXXX" not allowed anywhere;
															// expected element "name"
				"    <email>js@example.com</email>\r\n" + // unexpected_element_required_element_missing -> "element
															// "email" not allowed yet; missing required element "name""
				"  </card>\r\n" + //
				"  <card>\r\n" + //
				"    <name>Fred Bloggs</name>\r\n" + //
				"    <email>fb@example.net</email>\r\n" + //
				"  </card>\r\n" + //
				"</addressBook>";
		testDiagnosticsFor(xml, //
				d(3, 5, 12, RelaxNGErrorCode.unknown_element), //
				d(4, 5, 10, RelaxNGErrorCode.unexpected_element_required_element_missing));
	}

	@Test
	public void incomplete_element_required_element_missing() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v1.rng\" ?>\r\n" + //
				"<addressBook>\r\n" + //
				"  <card>\r\n" + // incomplete_element_required_element_missing --> "element "card" incomplete; missing required element "name""
				"  </card>\r\n" + //
				"  <card>\r\n" + //
				"    <name>Fred Bloggs</name>\r\n" + //
				"    <email>fb@example.net</email>\r\n" + //
				"  </card>\r\n" + //
				"</addressBook>";
		testDiagnosticsFor(xml, //
				d(2, 3, 7, RelaxNGErrorCode.incomplete_element_required_element_missing));
	}
	
	@Test
	public void no_attributes_allowed() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v1.rng\" ?>\r\n" + //
				"<addressBook id=\"\" >\r\n" + // no_attributes_allowed --> found attribute "id", but no attributes allowed here
				"  <card>\r\n" + //
				"    <name>Fred Bloggs</name>\r\n" + //
				"    <email>fb@example.net</email>\r\n" + //
				"  </card>\r\n" + //
				"</addressBook>";
		testDiagnosticsFor(xml, //
				d(1, 13, 15, RelaxNGErrorCode.no_attributes_allowed));
	}
	
	@Test
	public void patternRegexValid() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/patternRegex.rng\" ?>\r\n" + //
				"<root>\r\n" + //
				"  <item lang=\"en\">text</item>\r\n" + //
				"</root>";
		testDiagnosticsFor(xml);
	}

	@Test
	public void patternRegexInvalid() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/patternRegex.rng\" ?>\r\n" + //
				"<root>\r\n" + //
				"  <item lang=\"123\">text</item>\r\n" + //
				"</root>";
		testDiagnosticsFor(xml, //
				d(2, 13, 18, RelaxNGErrorCode.invalid_attribute_value));
	}

	@Test
	public void multiple_no_attributes_allowed() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v1.rng\" ?>\r\n" + //
				"<addressBook id=\"\" name=\"\" >\r\n" + // no_attributes_allowed
				// --> found attribute "id", but no attributes allowed
				// --> found attribute "name", but no attributes allowed
				"  <card>\r\n" + //
				"    <name>Fred Bloggs</name>\r\n" + //
				"    <email>fb@example.net</email>\r\n" + //
				"  </card>\r\n" + //
				"</addressBook>";
		testDiagnosticsFor(xml, //
				d(1, 13, 15, RelaxNGErrorCode.no_attributes_allowed),
				d(1, 19, 23, RelaxNGErrorCode.no_attributes_allowed));
	}
}
