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
package org.eclipse.lemminx.extensions.relaxng.grammar.rng;

import static org.eclipse.lemminx.XMLAssert.d;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.relaxng.xml.validator.RelaxNGErrorCode;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

/**
 * Tests for RNG SAX parse error codes.
 *
 * <p>
 * Each test creates a minimal RNG grammar that triggers a specific Jing SAX
 * parse error and validates that the correct error code and range are reported.
 * </p>
 *
 * @see <a href=
 *      "https://github.com/relaxng/jing-trang/blob/master/mod/rng-parse/src/main/com/thaiopensource/relaxng/parse/sax/resources/Messages.properties">Jing
 *      SAX Messages</a>
 */
public class RNGSAXParseErrorsTest extends AbstractCacheBasedTest {

	/**
	 * Test for {@link RelaxNGErrorCode#expected_define}.
	 *
	 * <p>
	 * Use case: a {@code <text/>} element appears directly inside
	 * {@code <grammar>} where only {@code <define>}, {@code <start>},
	 * {@code <include>} or {@code <div>} are allowed.
	 * </p>
	 */
	@Test
	public void expected_define() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\"><text/></element>\r\n" + //
				"  </start>\r\n" + //
				"  <text/>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 3, 7, RelaxNGErrorCode.expected_define));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#expected_empty}.
	 *
	 * <p>
	 * Use case: a child element appears inside {@code <empty>} which must have
	 * no children.
	 * </p>
	 */
	@Test
	public void expected_empty() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <empty>\r\n" + //
				"        <text/>\r\n" + //
				"      </empty>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 9, 13, RelaxNGErrorCode.expected_empty));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#expected_except}.
	 *
	 * <p>
	 * Use case: a non-{@code <except>} element appears inside
	 * {@code <anyName>} where only {@code <except>} is allowed.
	 * </p>
	 */
	@Test
	public void expected_except() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <anyName>\r\n" + //
				"        <text/>\r\n" + //
				"      </anyName>\r\n" + //
				"      <text/>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 9, 13, RelaxNGErrorCode.expected_except));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#expected_name_class}.
	 *
	 * <p>
	 * Use case: a non-name-class element appears as the first child of
	 * {@code <element>}.
	 * </p>
	 */
	@Test
	public void expected_name_class() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <data type=\"string\"/>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(3, 7, 11, RelaxNGErrorCode.expected_name_class), //
				d(2, 5, 12, RelaxNGErrorCode.missing_name_class), //
				d(2, 5, 12, RelaxNGErrorCode.missing_children));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#expected_param_except}.
	 *
	 * <p>
	 * Use case: a {@code <text/>} element appears inside {@code <data>} where
	 * only {@code <param>} and {@code <except>} are allowed.
	 * </p>
	 */
	@Test
	public void expected_param_except() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"string\">\r\n" + //
				"        <text/>\r\n" + //
				"      </data>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(5, 9, 13, RelaxNGErrorCode.expected_param_except));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#start_multi_pattern}.
	 *
	 * <p>
	 * Use case: {@code <start>} contains two pattern children.
	 * </p>
	 */
	@Test
	public void start_multi_pattern() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"a\"><text/></element>\r\n" + //
				"    <element name=\"b\"><text/></element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(3, 5, 12, RelaxNGErrorCode.start_multi_pattern));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#attribute_multi_pattern}.
	 *
	 * <p>
	 * Use case: {@code <attribute>} contains two pattern children.
	 * </p>
	 */
	@Test
	public void attribute_multi_pattern() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <attribute name=\"a\">\r\n" + //
				"        <text/>\r\n" + //
				"        <text/>\r\n" + //
				"      </attribute>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(5, 9, 13, RelaxNGErrorCode.attribute_multi_pattern));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#multiple_except}.
	 *
	 * <p>
	 * Use case: {@code <anyName>} contains two {@code <except>} children.
	 * </p>
	 */
	@Test
	public void multiple_except() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <anyName>\r\n" + //
				"        <except><nsName ns=\"urn:a\"/></except>\r\n" + //
				"        <except><nsName ns=\"urn:b\"/></except>\r\n" + //
				"      </anyName>\r\n" + //
				"      <text/>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(5, 9, 15, RelaxNGErrorCode.multiple_except));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#missing_href_attribute} on
	 * {@code <include>}.
	 *
	 * <p>
	 * Use case: an {@code <include>} element has no {@code href} attribute.
	 * </p>
	 */
	@Test
	public void missing_href_attribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\"><text/></element>\r\n" + //
				"  </start>\r\n" + //
				"  <include/>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 3, 10, RelaxNGErrorCode.missing_href_attribute));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#missing_href_attribute} on
	 * {@code <externalRef>}.
	 *
	 * <p>
	 * Use case: an {@code <externalRef>} element has no {@code href} attribute.
	 * </p>
	 */
	@Test
	public void missing_href_attribute_externalRef() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <externalRef/>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(2, 5, 16, RelaxNGErrorCode.missing_href_attribute));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#combine_attribute_bad_value}.
	 *
	 * <p>
	 * Use case: the {@code combine} attribute has a value other than
	 * {@code "choice"} or {@code "interleave"}.
	 * </p>
	 */
	@Test
	public void combine_attribute_bad_value() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <ref name=\"a\"/>\r\n" + //
				"  </start>\r\n" + //
				"  <define name=\"a\" combine=\"bad\">\r\n" + //
				"    <empty/>\r\n" + //
				"  </define>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 27, 32, RelaxNGErrorCode.combine_attribute_bad_value));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#any_name_except_contains_any_name}.
	 *
	 * <p>
	 * Use case: {@code <except>} inside {@code <anyName>} contains another
	 * {@code <anyName>}.
	 * </p>
	 */
	@Test
	public void any_name_except_contains_any_name() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <anyName>\r\n" + //
				"        <except>\r\n" + //
				"          <anyName/>\r\n" + //
				"        </except>\r\n" + //
				"      </anyName>\r\n" + //
				"      <text/>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(5, 11, 18, RelaxNGErrorCode.any_name_except_contains_any_name), //
				d(4, 9, 15, RelaxNGErrorCode.missing_name_class));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#ns_name_except_contains_any_name}.
	 *
	 * <p>
	 * Use case: {@code <except>} inside {@code <nsName>} contains
	 * {@code <anyName>}.
	 * </p>
	 */
	@Test
	public void ns_name_except_contains_any_name() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <nsName>\r\n" + //
				"        <except>\r\n" + //
				"          <anyName/>\r\n" + //
				"        </except>\r\n" + //
				"      </nsName>\r\n" + //
				"      <text/>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(5, 11, 18, RelaxNGErrorCode.ns_name_except_contains_any_name), //
				d(4, 9, 15, RelaxNGErrorCode.missing_name_class));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#ns_name_except_contains_ns_name}.
	 *
	 * <p>
	 * Use case: {@code <except>} inside {@code <nsName>} contains another
	 * {@code <nsName>}.
	 * </p>
	 */
	@Test
	public void ns_name_except_contains_ns_name() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <nsName>\r\n" + //
				"        <except>\r\n" + //
				"          <nsName/>\r\n" + //
				"        </except>\r\n" + //
				"      </nsName>\r\n" + //
				"      <text/>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(5, 11, 17, RelaxNGErrorCode.ns_name_except_contains_ns_name), //
				d(4, 9, 15, RelaxNGErrorCode.missing_name_class));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#fragment_identifier_datatype_library}.
	 *
	 * <p>
	 * Use case: the {@code datatypeLibrary} attribute value contains a
	 * fragment identifier ({@code #}).
	 * </p>
	 */
	@Test
	public void fragment_identifier_datatype_library() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes#frag\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"string\"/>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(1, 18, 67, RelaxNGErrorCode.fragment_identifier_datatype_library), //
				d(1, 18, 67, RelaxNGErrorCode.unrecognized_datatype_library));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#relative_datatype_library}.
	 *
	 * <p>
	 * Use case: the {@code datatypeLibrary} attribute value is a relative URI.
	 * </p>
	 */
	@Test
	public void relative_datatype_library() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"relative/path\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"string\"/>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(1, 18, 33, RelaxNGErrorCode.relative_datatype_library), //
				d(1, 18, 33, RelaxNGErrorCode.unrecognized_datatype_library));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#href_fragment_id}.
	 *
	 * <p>
	 * Use case: the {@code href} attribute of {@code <include>} contains a
	 * fragment identifier.
	 * </p>
	 */
	@Test
	public void href_fragment_id() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\"><text/></element>\r\n" + //
				"  </start>\r\n" + //
				"  <include href=\"other.rng#frag\"/>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 16, 32, RelaxNGErrorCode.href_fragment_id));
	}

	private static void testDiagnosticsFor(String xml, Diagnostic... expected) {
		XMLAssert.testDiagnosticsFor(xml, null, null, null, true, expected);
	}
}
