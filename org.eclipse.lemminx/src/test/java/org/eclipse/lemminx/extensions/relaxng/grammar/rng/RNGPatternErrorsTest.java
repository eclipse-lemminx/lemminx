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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for RNG grammar pattern-level errors and section 7.1 restriction
 * violations.
 *
 * <p>
 * These tests validate error ranges for errors detected during RNG schema
 * parsing and simplification by the Jing RelaxNG validator, including SAX parse
 * errors, pattern-level errors, and section 7.1 restriction violations.
 * </p>
 *
 * @see <a href=
 *      "https://github.com/relaxng/jing-trang/blob/master/mod/rng-parse/src/main/com/thaiopensource/relaxng/parse/sax/resources/Messages.properties">Jing
 *      SAX Messages.properties</a>
 * @see <a href=
 *      "https://github.com/relaxng/jing-trang/blob/master/mod/pattern/src/main/com/thaiopensource/relaxng/pattern/resources/Messages.properties">Jing
 *      pattern Messages.properties</a>
 */
public class RNGPatternErrorsTest extends AbstractCacheBasedTest {

	// ---------------------------------------------------------------------------
	// SAX parse errors
	// @see com/thaiopensource/relaxng/parse/sax/resources/Messages.properties
	// ---------------------------------------------------------------------------

	/**
	 * Test for {@link RelaxNGErrorCode#expected_define}.
	 *
	 * <p>
	 * Use case: A non-grammar element (e.g. {@code <text/>}) appears directly
	 * inside {@code <grammar>} where only {@code <define>}, {@code <start>},
	 * {@code <include>}, or {@code <div>} are allowed.
	 * </p>
	 */
	@Test
	public void expected_define() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\"><text/></element>\r\n" + //
				"  </start>\r\n" + //
				"  <text />\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 3, 7, RelaxNGErrorCode.expected_define));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#expected_empty}.
	 *
	 * <p>
	 * Use case: A child element is found inside {@code <empty>} which must have
	 * no children.
	 * </p>
	 */
	@Test
	public void expected_empty() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <empty>\r\n" + //
				"        <text />\r\n" + //
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
	 * Use case: An element other than {@code <except>} appears inside
	 * {@code <anyName>} where only {@code <except>} is allowed.
	 * </p>
	 */
	@Test
	public void expected_except() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <anyName>\r\n" + //
				"        <text />\r\n" + //
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
	 * Use case: A pattern element appears where a name-class element was
	 * expected (e.g. first child of {@code <element>} without {@code name}
	 * attribute).
	 * </p>
	 */
	@Test
	public void expected_name_class() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <text />\r\n" + //
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
	 * Use case: An invalid child element appears inside {@code <data>} where
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
				"        <text />\r\n" + //
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
	 * Use case: A {@code <start>} element contains more than one pattern child.
	 * </p>
	 */
	@Test
	public void start_multi_pattern() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <text />\r\n" + //
				"    <empty />\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(3, 5, 10, RelaxNGErrorCode.start_multi_pattern));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#attribute_multi_pattern}.
	 *
	 * <p>
	 * Use case: An {@code <attribute>} element contains more than one pattern.
	 * </p>
	 */
	@Test
	public void attribute_multi_pattern() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <attribute name=\"a\">\r\n" + //
				"        <text />\r\n" + //
				"        <empty />\r\n" + //
				"      </attribute>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(5, 9, 14, RelaxNGErrorCode.attribute_multi_pattern));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#multiple_except}.
	 *
	 * <p>
	 * Use case: A {@code <data>} element contains more than one {@code <except>}.
	 * </p>
	 */
	@Test
	public void multiple_except() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"string\">\r\n" + //
				"        <except><value>a</value></except>\r\n" + //
				"        <except><value>b</value></except>\r\n" + //
				"      </data>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(6, 9, 15, RelaxNGErrorCode.multiple_except));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#missing_href_attribute}.
	 *
	 * <p>
	 * Use case: An {@code <include>} element is missing its required
	 * {@code href} attribute.
	 * </p>
	 */
	@Test
	public void missing_href_attribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\"><text/></element>\r\n" + //
				"  </start>\r\n" + //
				"  <include />\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 3, 10, RelaxNGErrorCode.missing_href_attribute));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#combine_attribute_bad_value}.
	 *
	 * <p>
	 * Use case: The {@code combine} attribute has an invalid value (not
	 * {@code "choice"} or {@code "interleave"}).
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
				"  <define name=\"a\" combine=\"choice\">\r\n" + //
				"    <empty/>\r\n" + //
				"  </define>\r\n" + //
				"</grammar>";
		// combine="bad" → col 27 (opening ") to 32 (exclusive)
		testDiagnosticsFor(xml, //
				d(4, 27, 32, RelaxNGErrorCode.combine_attribute_bad_value));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#any_name_except_contains_any_name}.
	 *
	 * <p>
	 * Use case: An {@code <anyName>} appears inside the {@code <except>} of
	 * another {@code <anyName>}.
	 * </p>
	 */
	@Test
	public void any_name_except_contains_any_name() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <anyName>\r\n" + //
				"        <except>\r\n" + //
				"          <anyName />\r\n" + //
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
	 * Use case: An {@code <anyName>} appears inside the {@code <except>} of a
	 * {@code <nsName>}.
	 * </p>
	 */
	@Test
	public void ns_name_except_contains_any_name() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <nsName ns=\"http://example.com\">\r\n" + //
				"        <except>\r\n" + //
				"          <anyName />\r\n" + //
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
	 * Use case: An {@code <nsName>} appears inside the {@code <except>} of
	 * another {@code <nsName>}.
	 * </p>
	 */
	@Test
	public void ns_name_except_contains_ns_name() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <nsName ns=\"http://example.com\">\r\n" + //
				"        <except>\r\n" + //
				"          <nsName />\r\n" + //
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
	 * Use case: The {@code datatypeLibrary} attribute value contains a fragment
	 * identifier ({@code #}).
	 * </p>
	 */
	@Test
	public void fragment_identifier_datatype_library() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes#frag\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <text />\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		// "  datatypeLibrary="..." → opening " at col 18, closing " at col 66
		testDiagnosticsFor(xml, //
				d(1, 18, 67, RelaxNGErrorCode.fragment_identifier_datatype_library));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#relative_datatype_library}.
	 *
	 * <p>
	 * Use case: The {@code datatypeLibrary} attribute value is a relative URI.
	 * </p>
	 */
	@Test
	public void relative_datatype_library() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"relative\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <text />\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		// "  datatypeLibrary="relative">" → opening " at col 18, closing " at col 27
		testDiagnosticsFor(xml, //
				d(1, 18, 28, RelaxNGErrorCode.relative_datatype_library));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#xmlns_attribute}.
	 *
	 * <p>
	 * Use case: An {@code <attribute>} pattern uses the name {@code xmlns}
	 * which is prohibited by the RELAX NG specification.
	 * </p>
	 */
	@Test
	public void xmlns_attribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <attribute name=\"xmlns\"><text/></attribute>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		// "      <attribute name=\"xmlns\">" → "attribute" at col 7-16
		testDiagnosticsFor(xml, //
				d(3, 7, 16, RelaxNGErrorCode.xmlns_attribute));
	}

	// ---------------------------------------------------------------------------
	// Pattern-level errors
	// @see com/thaiopensource/relaxng/pattern/resources/Messages.properties
	// ---------------------------------------------------------------------------

	/**
	 * Test for {@link RelaxNGErrorCode#recursive_reference}.
	 *
	 * <p>
	 * Use case: A {@code <define>} pattern references itself directly without going
	 * through an {@code <element>} boundary, creating an illegal recursive loop.
	 * The error range highlights the {@code name} attribute value on the
	 * {@code <ref>} element.
	 * </p>
	 */
	@Test
	public void recursive_reference() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <ref name=\"a\"/>\r\n" + //
				"  </start>\r\n" + //
				"  <define name=\"a\">\r\n" + //
				"    <ref name=\"a\"/>\r\n" + // line 5: recursive ref → name="a" value
				"  </define>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.recursive_reference));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#conflict_combine_define}.
	 *
	 * <p>
	 * Use case: Two {@code <define>} elements for the same name use different
	 * {@code combine} attribute values (e.g. one uses {@code "choice"} and the
	 * other uses {@code "interleave"}). The error range highlights the
	 * {@code combine} attribute value on the conflicting element.
	 * </p>
	 */
	@Test
	public void conflict_combine_define() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <ref name=\"a\"/>\r\n" + //
				"  </start>\r\n" + //
				"  <define name=\"a\" combine=\"choice\">\r\n" + //
				"    <empty/>\r\n" + //
				"  </define>\r\n" + //
				"  <define name=\"a\" combine=\"interleave\">\r\n" + // line 7: conflicting combine
				"    <empty/>\r\n" + //
				"  </define>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(7, 27, 39, RelaxNGErrorCode.conflict_combine_define));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#conflict_combine_start}.
	 *
	 * <p>
	 * Use case: Two {@code <start>} elements use different {@code combine}
	 * attribute values. The error range highlights the start tag name of the
	 * conflicting {@code <start>} element.
	 * </p>
	 */
	@Test
	public void conflict_combine_start() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start combine=\"choice\">\r\n" + //
				"    <element name=\"a\"><text/></element>\r\n" + //
				"  </start>\r\n" + //
				"  <start combine=\"interleave\">\r\n" + // line 4: conflicting
				"    <element name=\"b\"><text/></element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 3, 8, RelaxNGErrorCode.conflict_combine_start));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#unrecognized_builtin_datatype}.
	 *
	 * <p>
	 * Use case: A {@code <data>} element uses a type that is not a valid builtin
	 * datatype (builtin library only has {@code "string"} and {@code "token"}).
	 * The error range highlights the {@code type} attribute value.
	 * </p>
	 */
	@Test
	public void unrecognized_builtin_datatype() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"integer\"/>\r\n" + // line 3: "integer" not a builtin
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(3, 17, 26, RelaxNGErrorCode.unrecognized_datatype));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#builtin_param}.
	 *
	 * <p>
	 * Use case: A {@code <param>} element is used with a builtin datatype, but
	 * builtin datatypes do not accept parameters. The error range highlights the
	 * {@code <param>} start tag name.
	 * </p>
	 */
	@Test
	public void builtin_param() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"string\">\r\n" + //
				"        <param name=\"pattern\">.*</param>\r\n" + // line 4: <param at col 8, "param" at 9-14
				"      </data>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 9, 14, RelaxNGErrorCode.builtin_param));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#parent_ref_outside_grammar}.
	 *
	 * <p>
	 * Use case: A {@code <parentRef>} element is used outside a nested
	 * {@code <grammar>}, so there is no parent grammar to reference. The error
	 * range highlights the {@code <parentRef>} start tag name.
	 * </p>
	 */
	@Test
	public void parent_ref_outside_grammar() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <parentRef name=\"x\"/>\r\n" + // line 3: parentRef outside nested grammar
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(3, 7, 16, RelaxNGErrorCode.ref_outside_grammar));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#invalid_value}.
	 *
	 * <p>
	 * Use case: A {@code <value>} element has text content that is not a valid
	 * instance of its specified datatype. The error range highlights the text
	 * content of the {@code <value>} element.
	 * </p>
	 */
	@Test
	public void invalid_value() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <value type=\"integer\">abc</value>\r\n" + // line 4: "abc" is not an integer
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 28, 31, RelaxNGErrorCode.invalid_value));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#expected_name}.
	 *
	 * <p>
	 * Use case: A child element is found inside {@code <name>} where only text
	 * content (a name) is expected. The error range highlights the unexpected child
	 * element's start tag name.
	 * </p>
	 */
	@Test
	public void expected_name() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <name>\r\n" + //
				"        <text />\r\n" + // line 4: expected a name (text content), found element
				"      </name>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(4, 9, 13, RelaxNGErrorCode.expected_name), //
				d(3, 7, 11, RelaxNGErrorCode.invalid_ncname), //
				d(2, 5, 12, RelaxNGErrorCode.missing_children));
	}

	// ---------------------------------------------------------------------------
	// Section 7.1 restriction violations
	// ---------------------------------------------------------------------------

	/**
	 * Test for {@link RelaxNGErrorCode#start_contains_attribute}.
	 *
	 * <p>
	 * Use case: A {@code <start>} element directly contains an
	 * {@code <attribute>} pattern. Section 7.1 prohibits the path
	 * {@code start//attribute}.
	 * </p>
	 */
	@Test
	public void start_contains_attribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <attribute name=\"a\"><text/></attribute>\r\n" + // line 2
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.start_contains_attribute));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#start_contains_text}.
	 *
	 * <p>
	 * Use case: A {@code <start>} element contains a {@code <text/>} pattern.
	 * Section 7.1 prohibits the path {@code start//text}.
	 * </p>
	 */
	@Test
	public void start_contains_text() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <text />\r\n" + // line 2
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.start_contains_text));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#start_contains_data}.
	 *
	 * <p>
	 * Use case: A {@code <start>} element directly contains a {@code <data/>}
	 * pattern. Section 7.1 prohibits the path {@code start//data}.
	 * </p>
	 */
	@Test
	public void start_contains_data() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <data type=\"string\" />\r\n" + // line 2
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.start_contains_data));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#start_contains_empty}.
	 *
	 * <p>
	 * Use case: A {@code <start>} element directly contains an {@code <empty/>}
	 * pattern. Section 7.1 prohibits the path {@code start//empty}.
	 * </p>
	 */
	@Test
	public void start_contains_empty() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <empty />\r\n" + // line 2
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.start_contains_empty));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#start_contains_value}.
	 *
	 * <p>
	 * Use case: A {@code <start>} element directly contains a {@code <value>}
	 * pattern. Section 7.1 prohibits the path {@code start//value}.
	 * </p>
	 */
	@Test
	public void start_contains_value() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <value>x</value>\r\n" + // line 2
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.start_contains_value));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#start_contains_list}.
	 *
	 * <p>
	 * Use case: A {@code <start>} element directly contains a {@code <list>}
	 * pattern. Section 7.1 prohibits the path {@code start//list}.
	 * </p>
	 */
	@Test
	public void start_contains_list() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <list><data type=\"token\" /></list>\r\n" + // line 2
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.start_contains_list));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#start_contains_group}.
	 *
	 * <p>
	 * Use case: A {@code <start>} element directly contains a {@code <group>}
	 * pattern. Section 7.1 prohibits the path {@code start//group}.
	 * </p>
	 */
	@Test
	public void start_contains_group() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <group>\r\n" + // line 2
				"      <element name=\"a\"><text/></element>\r\n" + //
				"      <element name=\"b\"><text/></element>\r\n" + //
				"    </group>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.start_contains_group));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#start_contains_interleave}.
	 *
	 * <p>
	 * Use case: A {@code <start>} element directly contains an
	 * {@code <interleave>} pattern. Section 7.1 prohibits the path
	 * {@code start//interleave}.
	 * </p>
	 */
	@Test
	public void start_contains_interleave() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <interleave>\r\n" + // line 2
				"      <element name=\"a\"><text/></element>\r\n" + //
				"      <element name=\"b\"><text/></element>\r\n" + //
				"    </interleave>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.start_contains_interleave));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#start_contains_one_or_more}.
	 *
	 * <p>
	 * Use case: A {@code <start>} element directly contains a
	 * {@code <oneOrMore>} pattern. Section 7.1 prohibits the path
	 * {@code start//oneOrMore}.
	 * </p>
	 */
	@Test
	public void start_contains_one_or_more() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <oneOrMore>\r\n" + // line 2
				"      <element name=\"a\"><text/></element>\r\n" + //
				"    </oneOrMore>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.start_contains_one_or_more));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#attribute_contains_element}.
	 *
	 * <p>
	 * Use case: An {@code <attribute>} pattern contains an {@code <element>}
	 * pattern. Section 7.1 prohibits the path {@code attribute//element}.
	 * </p>
	 */
	@Test
	public void attribute_contains_element() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <attribute name=\"a\">\r\n" + //
				"        <element name=\"b\"><text/></element>\r\n" + // line 4
				"      </attribute>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.attribute_contains_element));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#attribute_contains_attribute}.
	 *
	 * <p>
	 * Use case: An {@code <attribute>} pattern contains another
	 * {@code <attribute>} pattern. Section 7.1 prohibits the path
	 * {@code attribute//attribute}.
	 * </p>
	 */
	@Test
	public void attribute_contains_attribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <attribute name=\"a\">\r\n" + //
				"        <attribute name=\"b\"><text/></attribute>\r\n" + // line 4
				"      </attribute>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.attribute_contains_attribute));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#list_contains_list}.
	 *
	 * <p>
	 * Use case: A {@code <list>} pattern contains another {@code <list>} pattern.
	 * Section 7.1 prohibits the path {@code list//list}.
	 * </p>
	 */
	@Test
	public void list_contains_list() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <list>\r\n" + //
				"        <list><data type=\"token\"/></list>\r\n" + // line 4
				"      </list>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.list_contains_list));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#list_contains_text}.
	 *
	 * <p>
	 * Use case: A {@code <list>} pattern contains a {@code <text/>} pattern.
	 * Section 7.1 prohibits the path {@code list//text}.
	 * </p>
	 */
	@Test
	public void list_contains_text() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <list>\r\n" + //
				"        <text />\r\n" + // line 4
				"      </list>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.list_contains_text));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#list_contains_attribute}.
	 *
	 * <p>
	 * Use case: A {@code <list>} pattern contains an {@code <attribute>} pattern.
	 * Section 7.1 prohibits the path {@code list//attribute}.
	 * </p>
	 */
	@Test
	public void list_contains_attribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <list>\r\n" + //
				"        <attribute name=\"a\"><text/></attribute>\r\n" + // line 4
				"      </list>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.list_contains_attribute));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#list_contains_element}.
	 *
	 * <p>
	 * Use case: A {@code <list>} pattern contains an {@code <element>} pattern.
	 * Section 7.1 prohibits the path {@code list//element}.
	 * </p>
	 */
	@Test
	public void list_contains_element() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <list>\r\n" + //
				"        <element name=\"a\"><text/></element>\r\n" + // line 4
				"      </list>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.list_contains_element));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#list_contains_interleave}.
	 *
	 * <p>
	 * Use case: A {@code <list>} pattern contains an {@code <interleave>} pattern.
	 * Section 7.1 prohibits the path {@code list//interleave}.
	 * </p>
	 */
	@Test
	public void list_contains_interleave() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <list>\r\n" + //
				"        <interleave>\r\n" + // line 4
				"          <data type=\"token\"/>\r\n" + //
				"          <data type=\"string\"/>\r\n" + //
				"        </interleave>\r\n" + //
				"      </list>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.list_contains_interleave));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#duplicate_attribute}.
	 *
	 * <p>
	 * Use case: The same attribute name is defined more than once in an element
	 * pattern, creating a duplicate. The error is reported on the containing
	 * {@code <element>} node.
	 * </p>
	 */
	@Test
	public void duplicate_attribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <attribute name=\"a\"><text/></attribute>\r\n" + //
				"      <attribute name=\"a\"><text/></attribute>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.duplicate_attribute));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#interleave_text_overlap}.
	 *
	 * <p>
	 * Use case: Both operands of an {@code <interleave>} contain a
	 * {@code <text/>} pattern, which would require splitting text content.
	 * The error is reported on the {@code <interleave>} element.
	 * </p>
	 */
	@Test
	public void interleave_text_overlap() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <interleave>\r\n" + // line 3
				"        <group>\r\n" + //
				"          <element name=\"a\"><text/></element>\r\n" + //
				"          <text/>\r\n" + //
				"        </group>\r\n" + //
				"        <group>\r\n" + //
				"          <element name=\"b\"><text/></element>\r\n" + //
				"          <text/>\r\n" + //
				"        </group>\r\n" + //
				"      </interleave>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.interleave_text_overlap));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#interleave_element_overlap}.
	 *
	 * <p>
	 * Use case: Overlapping element names appear in different operands of an
	 * {@code <interleave>}, making it ambiguous which operand should match.
	 * The error is reported on the {@code <interleave>} element.
	 * </p>
	 */
	@Disabled("Jing does not report this error for simple element overlap")
	@Test
	public void interleave_element_overlap() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <interleave>\r\n" + // line 3
				"        <element name=\"a\"><text/></element>\r\n" + //
				"        <element name=\"a\"><text/></element>\r\n" + //
				"      </interleave>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(3, 7, 17, RelaxNGErrorCode.interleave_element_overlap));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#open_name_class_not_repeated}.
	 *
	 * <p>
	 * Use case: An {@code <attribute>} pattern uses {@code <anyName>} or
	 * {@code <nsName>} as its name class but is not wrapped in
	 * {@code <oneOrMore>}. The error is reported on the {@code <attribute>}
	 * element.
	 * </p>
	 */
	@Test
	public void open_name_class_not_repeated() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <attribute>\r\n" + // line 3
				"        <anyName/>\r\n" + //
				"      </attribute>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.open_name_class_not_repeated));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#data_except_contains_attribute}.
	 *
	 * <p>
	 * Use case: A {@code <data>} pattern's {@code <except>} contains an
	 * {@code <attribute>} pattern which is prohibited.
	 * </p>
	 */
	@Test
	public void data_except_contains_attribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"string\">\r\n" + //
				"        <except>\r\n" + //
				"          <attribute name=\"a\"><text/></attribute>\r\n" + //
				"        </except>\r\n" + //
				"      </data>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		// "          <attribute name=\"a\"><text/></attribute>" → "attribute" at col 11-20
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.data_except_contains_attribute));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#data_except_contains_element}.
	 *
	 * <p>
	 * Use case: A {@code <data>} pattern's {@code <except>} contains an
	 * {@code <element>} pattern which is prohibited.
	 * </p>
	 */
	@Test
	public void data_except_contains_element() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"string\">\r\n" + //
				"        <except>\r\n" + //
				"          <element name=\"a\"><text/></element>\r\n" + //
				"        </except>\r\n" + //
				"      </data>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		// "          <element name=\"a\"><text/></element>" → "element" at col 11-18
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.data_except_contains_element));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#data_except_contains_text}.
	 *
	 * <p>
	 * Use case: A {@code <data>} pattern's {@code <except>} contains a
	 * {@code <text/>} pattern which is prohibited.
	 * </p>
	 */
	@Test
	public void data_except_contains_text() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"string\">\r\n" + //
				"        <except>\r\n" + //
				"          <text />\r\n" + //
				"        </except>\r\n" + //
				"      </data>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		// "          <text />" → "text" at col 11-15
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.data_except_contains_text));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#data_except_contains_empty}.
	 *
	 * <p>
	 * Use case: A {@code <data>} pattern's {@code <except>} contains an
	 * {@code <empty/>} pattern which is prohibited.
	 * </p>
	 */
	@Test
	public void data_except_contains_empty() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"string\">\r\n" + //
				"        <except>\r\n" + //
				"          <empty />\r\n" + //
				"        </except>\r\n" + //
				"      </data>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		// "          <empty />" → "empty" at col 11-16
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.data_except_contains_empty));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#data_except_contains_list}.
	 *
	 * <p>
	 * Use case: A {@code <data>} pattern's {@code <except>} contains a
	 * {@code <list>} pattern which is prohibited.
	 * </p>
	 */
	@Test
	public void data_except_contains_list() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"string\">\r\n" + //
				"        <except>\r\n" + //
				"          <list><data type=\"token\"/></list>\r\n" + //
				"        </except>\r\n" + //
				"      </data>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		// "          <list>..." → "list" at col 11-15
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.data_except_contains_list));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#data_except_contains_group}.
	 *
	 * <p>
	 * Use case: A {@code <data>} pattern's {@code <except>} contains a
	 * {@code <group>} pattern which is prohibited.
	 * </p>
	 */
	@Test
	public void data_except_contains_group() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"string\">\r\n" + //
				"        <except>\r\n" + //
				"          <group>\r\n" + //
				"            <value>a</value>\r\n" + //
				"            <value>b</value>\r\n" + //
				"          </group>\r\n" + //
				"        </except>\r\n" + //
				"      </data>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		// "          <group>" → "group" at col 11-16
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.data_except_contains_group));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#data_except_contains_interleave}.
	 *
	 * <p>
	 * Use case: A {@code <data>} pattern's {@code <except>} contains an
	 * {@code <interleave>} pattern which is prohibited.
	 * </p>
	 */
	@Test
	public void data_except_contains_interleave() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"string\">\r\n" + //
				"        <except>\r\n" + //
				"          <interleave>\r\n" + //
				"            <value>a</value>\r\n" + //
				"            <value>b</value>\r\n" + //
				"          </interleave>\r\n" + //
				"        </except>\r\n" + //
				"      </data>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		// "          <interleave>" → "interleave" at col 11-21
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.data_except_contains_interleave));
	}

	/**
	 * Test for {@link RelaxNGErrorCode#data_except_contains_one_or_more}.
	 *
	 * <p>
	 * Use case: A {@code <data>} pattern's {@code <except>} contains a
	 * {@code <oneOrMore>} pattern which is prohibited.
	 * </p>
	 */
	@Test
	public void data_except_contains_one_or_more() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"string\">\r\n" + //
				"        <except>\r\n" + //
				"          <oneOrMore>\r\n" + //
				"            <value>a</value>\r\n" + //
				"          </oneOrMore>\r\n" + //
				"        </except>\r\n" + //
				"      </data>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		// "          <oneOrMore>" → "oneOrMore" at col 11-20
		testDiagnosticsFor(xml, //
				d(0, 1, 8, RelaxNGErrorCode.data_except_contains_one_or_more));
	}

	private static void testDiagnosticsFor(String xml, Diagnostic... expected) {
		XMLAssert.testDiagnosticsFor(xml, null, null, null, true, expected);
	}
}
