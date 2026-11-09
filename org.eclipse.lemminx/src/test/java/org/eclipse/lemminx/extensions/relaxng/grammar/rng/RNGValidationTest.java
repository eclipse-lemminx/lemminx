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
package org.eclipse.lemminx.extensions.relaxng.grammar.rng;

import static org.eclipse.lemminx.XMLAssert.d;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.relaxng.xml.validator.RelaxNGErrorCode;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

/**
 * RNG validator tests.
 *
 */
public class RNGValidationTest extends AbstractCacheBasedTest {

	@Test
	public void missing_start_element_InRoot() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + // <-- error
				"	datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, d(0, 1, 8, RelaxNGErrorCode.missing_start_element));
	}

	@Test
	public void missing_start_element_InNestedElement() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"	datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"	<start>\r\n" + //
				"		<grammar></grammar>\r\n" + // <-- error
				"	</start>\r\n" + //
				"\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, d(4, 3, 10, RelaxNGErrorCode.missing_start_element));
	}

	@Test
	public void missing_children() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n"
				+ "	datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n"
				+ "\r\n"
				+ "	<start>\r\n" // <-- error
				+ "		\r\n"
				+ "	</start>\r\n"
				+ "\r\n"
				+ "</grammar>";
		testDiagnosticsFor(xml, d(3, 2, 7, RelaxNGErrorCode.missing_children));
	}

	@Test
	public void illegal_attribute_ignored() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n"
				+ "	datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n"
				+ "\r\n"
				+ "	<start attr=\"\" >\r\n" // <-- 2 errors
				+ "		\r\n"
				+ "	</start>\r\n"
				+ "\r\n"
				+ "</grammar>";
		testDiagnosticsFor(xml, //
				d(3, 8, 12, RelaxNGErrorCode.illegal_attribute_ignored), //
				d(3, 2, 7, RelaxNGErrorCode.missing_children));
	}

	@Test
	public void illegal_name_attribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n"
				+ "	datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n"
				+ "\r\n"
				+ "	<start name=\"\" >\r\n" // <-- 2 errors
				+ "		\r\n"
				+ "	</start>\r\n"
				+ "\r\n"
				+ "</grammar>";
		testDiagnosticsFor(xml, //
				d(3, 8, 12, RelaxNGErrorCode.illegal_name_attribute), //
				d(3, 2, 7, RelaxNGErrorCode.missing_children));
	}

	@Test
	public void missing_name_attribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"  <start>\r\n" + //
				"    <empty />\r\n" + //
				"  </start>\r\n" + //
				"\r\n" + //
				"  <define>\r\n" + // <-- error
				"    <empty />\r\n" + //
				"  </define>\r\n" + //
				"\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(7, 3, 9, RelaxNGErrorCode.missing_name_attribute));
	}

	@Test
	public void invalid_ncname_withAttrName() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"	datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"	<start>\r\n" + // <-- error
				"		<element name=\"\"></element>\r\n" + // <-- error
				"	</start>\r\n" + //
				"\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 16, 18, RelaxNGErrorCode.invalid_ncname), //
				d(4, 3, 10, RelaxNGErrorCode.missing_children));
	}

	@Test
	public void invalid_ncname_withNameTag() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <name>:::</name>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(5, 12, 15, RelaxNGErrorCode.invalid_ncname), //
				d(5, 12, 15, RelaxNGErrorCode.invalid_ncname), //
				d(4, 5, 12, RelaxNGErrorCode.missing_children));
	}

	@Test
	public void illegal_attribute_ignored_InElementName() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"	datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"	<start>\r\n" + //
				"		<element name=\"::::\"></element>\r\n" + // <-- error
				"	</start>\r\n" + //
				"\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 16, 22, RelaxNGErrorCode.invalid_ncname), //
				d(4, 16, 22, RelaxNGErrorCode.invalid_ncname), //
				d(4, 3, 10, RelaxNGErrorCode.missing_children));
	}

	@Test
	public void expected_pattern() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"  <start>\r\n" + //
				"    <foo />\r\n" + // <-- error
				"  </start>\r\n" + //
				"\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 5, 8, RelaxNGErrorCode.expected_pattern), //
				d(3, 3, 8, RelaxNGErrorCode.missing_children));
	}

	@Test
	public void missing_name_class() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"  <start>\r\n" + //
				"    <element />\r\n" + // <-- error
				"  </start>\r\n" + //
				"\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 5, 12, RelaxNGErrorCode.missing_name_class), //
				d(4, 5, 12, RelaxNGErrorCode.missing_children));
	}

	@Test
	public void missing_type_attribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"name\">\r\n" + //
				"      <data />\r\n" + // <-- error
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(5, 7, 11, RelaxNGErrorCode.missing_type_attribute));
	}

	@Test
	public void duplicate_start() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"name\">\r\n" + //
				"      <text />\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"\r\n" + //
				"  <start>\r\n" + // <-- error
				"    <element name=\"name\">\r\n" + //
				"      <text />\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(9, 3, 8, RelaxNGErrorCode.duplicate_start));
	}

	@Test
	public void duplicate_define() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"  <start>\r\n" + //
				"    <ref name=\"name\"/>\r\n" + //
				"  </start>\r\n" + //
				"\r\n" + //
				"  <define name=\"name\">\r\n" + //
				"    <empty/>\r\n" + //
				"  </define>\r\n" + //
				"\r\n" + //
				"  <define name=\"name\">\r\n" + // <-- error
				"    <empty/>\r\n" + //
				"  </define>\r\n" + //
				"  \r\n" + //
				"  <define name=\"email\">\r\n" + //
				"    <empty/>\r\n" + //
				"  </define>\r\n" + //
				"  \r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(11, 15, 21, RelaxNGErrorCode.duplicate_define));
	}

	@Test
	public void reference_to_undefined() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"  <start>\r\n" + //
				"    <ref name=\"name\"/>\r\n" + // <-- error
				"  </start>\r\n" + //
				"\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 14, 20, RelaxNGErrorCode.reference_to_undefined));
	}

	@Test
	public void unrecognized_datatype() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"  <start>\r\n" + //
				"    <choice>\r\n" + //
				"      <element name=\"name\">\r\n" + //
				"        <data type=\"string\" />\r\n" + //
				"      </element>\r\n" + //
				"      <element name=\"email\">\r\n" + //
				"        <data type=\"stringXXX\" />\r\n" + // <-- error
				"      </element>\r\n" + //
				"    </choice>\r\n" + //
				"  </start>\r\n" + //
				"\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(9, 19, 30, RelaxNGErrorCode.unrecognized_datatype));
	}

	private static void testDiagnosticsFor(String xml, Diagnostic... expected) {
		XMLAssert.testDiagnosticsFor(xml, null, null, null, true, expected);
	}

}
