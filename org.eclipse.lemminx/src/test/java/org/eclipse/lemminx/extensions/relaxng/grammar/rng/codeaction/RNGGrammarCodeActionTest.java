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
package org.eclipse.lemminx.extensions.relaxng.grammar.rng.codeaction;

import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.caSnippet;
import static org.eclipse.lemminx.XMLAssert.d;
import static org.eclipse.lemminx.XMLAssert.sne;
import static org.eclipse.lemminx.XMLAssert.te;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.relaxng.xml.validator.RelaxNGErrorCode;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

/**
 * Code action tests for RNG grammar errors.
 *
 * <ul>
 * <li>Insert missing required attributes (href, name, type) with
 * SnippetTextEdit.</li>
 * <li>Remove illegal or undeclared attributes.</li>
 * </ul>
 */
public class RNGGrammarCodeActionTest extends AbstractCacheBasedTest {

	// ---------- Insert missing 'type' attribute ----------

	/**
	 * Use case: {@code <data />} without required 'type' attribute and without
	 * {@code datatypeLibrary}.
	 *
	 * <p>
	 * Without snippet support: inserts {@code type=""}.
	 * With snippet support: inserts {@code type="${1|string,token|}"} with a
	 * choice of builtin types.
	 * </p>
	 */
	@Test
	public void missing_type_attribute_builtin_insertAttribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"name\">\r\n" + //
				"      <data />\r\n" + // line 4: <data at col 6, "data" at 7-11
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"\r\n" + //
				"</grammar>";
		Diagnostic d = d(4, 7, 11, RelaxNGErrorCode.missing_type_attribute);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(4, 11, 4, 11, " type=\"string\"")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(4, 11, 4, 11, " type=\"${1|string,token|}\"")));
	}

	/**
	 * Use case: {@code <data />} without required 'type' attribute, with XSD
	 * {@code datatypeLibrary}.
	 *
	 * <p>
	 * Without snippet support: inserts {@code type=""}.
	 * With snippet support: inserts {@code type="${1|string,token,...|}"} with a
	 * choice of all types (builtin + XSD).
	 * </p>
	 */
	@Test
	public void missing_type_attribute_xsd_insertAttribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"name\">\r\n" + //
				"      <data />\r\n" + // line 5: <data at col 6, "data" at 7-11
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"\r\n" + //
				"</grammar>";
		Diagnostic d = d(5, 7, 11, RelaxNGErrorCode.missing_type_attribute);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(5, 11, 5, 11, " type=\"string\"")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(5, 11, 5, 11, " type=\"${1|string,token," //
						+ "ENTITIES,ENTITY,ID,IDREF,IDREFS," //
						+ "NCName,NMTOKEN,NMTOKENS,NOTATION,Name,QName," //
						+ "anyAtomicType,anyURI,base64Binary,boolean,byte," //
						+ "date,dateTime,decimal,double,duration,float," //
						+ "gDay,gMonth,gMonthDay,gYear,gYearMonth," //
						+ "hexBinary,int,integer,language,long," //
						+ "negativeInteger,nonNegativeInteger,nonPositiveInteger,normalizedString," //
						+ "positiveInteger,short,time," //
						+ "unsignedByte,unsignedInt,unsignedLong,unsignedShort," //
						+ "untypedAtomic|}\"")));
	}

	// ---------- Insert missing 'name' attribute ----------

	/**
	 * Use case: {@code <define>} element without required 'name' attribute.
	 *
	 * <pre>
	 * &lt;define&gt;
	 *   &lt;empty/&gt;
	 * &lt;/define&gt;
	 * </pre>
	 *
	 * Code action: insert {@code name=""} attribute with cursor inside quotes.
	 */
	@Test
	public void missing_name_attribute_insertAttribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"  <start>\r\n" + //
				"    <empty />\r\n" + //
				"  </start>\r\n" + //
				"\r\n" + //
				"  <define>\r\n" + // line 7: <define at col 2, "define" at 3-9
				"    <empty />\r\n" + //
				"  </define>\r\n" + //
				"\r\n" + //
				"</grammar>";
		Diagnostic d = d(7, 3, 9, RelaxNGErrorCode.missing_name_attribute);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(7, 9, 7, 9, " name=\"\"")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(7, 9, 7, 9, " name=\"$1\"")));
	}

	// ---------- Remove illegal attribute ----------

	/**
	 * Use case: {@code <start>} element with illegal 'attr' attribute.
	 *
	 * <pre>
	 * &lt;start attr="" &gt;
	 * </pre>
	 *
	 * Code action: remove the illegal attribute.
	 */
	@Test
	public void illegal_attribute_ignored_removeAttribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"\tdatatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"\t<start attr=\"\" >\r\n" + // line 3: "attr" at cols 8-12
				"\t\t\r\n" + //
				"\t</start>\r\n" + //
				"\r\n" + //
				"</grammar>";
		Diagnostic d1 = d(3, 8, 12, RelaxNGErrorCode.illegal_attribute_ignored);
		Diagnostic d2 = d(3, 2, 7, RelaxNGErrorCode.missing_children);
		testDiagnosticsFor(xml, d1, d2);
		testCodeActionsFor(xml, d1,
				ca(d1, te(3, 7, 3, 15, "")));
		testCodeActionsFor(xml, d1, XMLAssert.createSnippetEditSettings(),
				caSnippet(d1, sne(3, 7, 3, 15, "$0")));
	}

	// ---------- Remove illegal 'name' attribute ----------

	/**
	 * Use case: {@code <start>} element with illegal 'name' attribute.
	 *
	 * <pre>
	 * &lt;start name="" &gt;
	 * </pre>
	 *
	 * Code action: remove the 'name' attribute.
	 */
	@Test
	public void illegal_name_attribute_removeAttribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"\tdatatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"\r\n" + //
				"\t<start name=\"\" >\r\n" + // line 3: "name" at cols 8-12
				"\t\t\r\n" + //
				"\t</start>\r\n" + //
				"\r\n" + //
				"</grammar>";
		Diagnostic d1 = d(3, 8, 12, RelaxNGErrorCode.illegal_name_attribute);
		Diagnostic d2 = d(3, 2, 7, RelaxNGErrorCode.missing_children);
		testDiagnosticsFor(xml, d1, d2);
		testCodeActionsFor(xml, d1,
				ca(d1, te(3, 7, 3, 15, "")));
		testCodeActionsFor(xml, d1, XMLAssert.createSnippetEditSettings(),
				caSnippet(d1, sne(3, 7, 3, 15, "$0")));
	}

	// ---------- Insert missing 'href' attribute ----------

	/**
	 * Use case: {@code <include/>} element without required 'href' attribute.
	 *
	 * <pre>
	 * &lt;include/&gt;
	 * </pre>
	 *
	 * Code action: insert {@code href=""} attribute with cursor inside quotes.
	 */
	@Test
	public void missing_href_attribute_insertAttribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\"><text/></element>\r\n" + //
				"  </start>\r\n" + //
				"  <include/>\r\n" + // line 4: <include at col 2, "include" at 3-10
				"</grammar>";
		Diagnostic d = d(4, 3, 10, RelaxNGErrorCode.missing_href_attribute);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(4, 10, 4, 10, " href=\"\"")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(4, 10, 4, 10, " href=\"$1\"")));
	}

	/**
	 * Use case: {@code <externalRef/>} element without required 'href' attribute.
	 *
	 * <pre>
	 * &lt;externalRef/&gt;
	 * </pre>
	 *
	 * Code action: insert {@code href=""} attribute with cursor inside quotes.
	 */
	@Test
	public void missing_href_attribute_externalRef_insertAttribute() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <externalRef/>\r\n" + // line 2: <externalRef at col 4, "externalRef" at 5-16
				"  </start>\r\n" + //
				"</grammar>";
		Diagnostic d = d(2, 5, 16, RelaxNGErrorCode.missing_href_attribute);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(2, 16, 2, 16, " href=\"\"")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(2, 16, 2, 16, " href=\"$1\"")));
	}

	// ---------- Replace invalid element (expected_define) ----------

	/**
	 * Use case: {@code <text/>} appears at the {@code <grammar>} level where only
	 * {@code <define>}, {@code <start>}, {@code <include>}, or {@code <div>} are
	 * allowed.
	 *
	 * <p>
	 * Code action: replace with one of the valid grammar children.
	 * </p>
	 */
	@Test
	public void expected_define_replaceElement() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\"><text/></element>\r\n" + //
				"  </start>\r\n" + //
				"  <text/>\r\n" + // line 4: <text/> at col 2-9
				"</grammar>";
		Diagnostic d = d(4, 3, 7, RelaxNGErrorCode.expected_define);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(4, 2, 4, 9, "<define name=\"\"></define>")),
				ca(d, te(4, 2, 4, 9, "<start></start>")),
				ca(d, te(4, 2, 4, 9, "<include href=\"\" />")),
				ca(d, te(4, 2, 4, 9, "<div></div>")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(4, 2, 4, 9, "<define name=\"$1\">\n$0\n</define>")),
				caSnippet(d, sne(4, 2, 4, 9, "<start>\n$0\n</start>")),
				caSnippet(d, sne(4, 2, 4, 9, "<include href=\"$1\" />")),
				caSnippet(d, sne(4, 2, 4, 9, "<div>$0</div>")));
	}

	// ---------- Replace invalid element (expected_pattern) ----------

	/**
	 * Use case: {@code <foo/>} appears inside {@code <start>} where only valid
	 * RELAX NG pattern elements are allowed.
	 *
	 * <p>
	 * Code action: replace with one of the 19 valid pattern elements.
	 * </p>
	 */
	@Test
	public void expected_pattern_replaceElement() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <foo/>\r\n" + // line 2: <foo/> at col 4-10
				"  </start>\r\n" + //
				"</grammar>";
		Diagnostic d = d(2, 5, 8, RelaxNGErrorCode.expected_pattern);
		Diagnostic d2 = d(1, 3, 8, RelaxNGErrorCode.missing_children);
		testDiagnosticsFor(xml, d, d2);
		testCodeActionsFor(xml, d,
				ca(d, te(2, 4, 2, 10, "<element name=\"\"></element>")),
				ca(d, te(2, 4, 2, 10, "<attribute name=\"\"></attribute>")),
				ca(d, te(2, 4, 2, 10, "<group></group>")),
				ca(d, te(2, 4, 2, 10, "<interleave></interleave>")),
				ca(d, te(2, 4, 2, 10, "<choice></choice>")),
				ca(d, te(2, 4, 2, 10, "<optional></optional>")),
				ca(d, te(2, 4, 2, 10, "<zeroOrMore></zeroOrMore>")),
				ca(d, te(2, 4, 2, 10, "<oneOrMore></oneOrMore>")),
				ca(d, te(2, 4, 2, 10, "<list></list>")),
				ca(d, te(2, 4, 2, 10, "<mixed></mixed>")),
				ca(d, te(2, 4, 2, 10, "<ref name=\"\" />")),
				ca(d, te(2, 4, 2, 10, "<parentRef name=\"\" />")),
				ca(d, te(2, 4, 2, 10, "<empty />")),
				ca(d, te(2, 4, 2, 10, "<text />")),
				ca(d, te(2, 4, 2, 10, "<value type=\"\"></value>")),
				ca(d, te(2, 4, 2, 10, "<data type=\"\" />")),
				ca(d, te(2, 4, 2, 10, "<notAllowed />")),
				ca(d, te(2, 4, 2, 10, "<externalRef href=\"\" />")),
				ca(d, te(2, 4, 2, 10, "<grammar></grammar>")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(2, 4, 2, 10, "<element name=\"$1\">\n$0\n</element>")),
				caSnippet(d, sne(2, 4, 2, 10, "<attribute name=\"$1\">\n$0\n</attribute>")),
				caSnippet(d, sne(2, 4, 2, 10, "<group>$0</group>")),
				caSnippet(d, sne(2, 4, 2, 10, "<interleave>$0</interleave>")),
				caSnippet(d, sne(2, 4, 2, 10, "<choice>$0</choice>")),
				caSnippet(d, sne(2, 4, 2, 10, "<optional>$0</optional>")),
				caSnippet(d, sne(2, 4, 2, 10, "<zeroOrMore>$0</zeroOrMore>")),
				caSnippet(d, sne(2, 4, 2, 10, "<oneOrMore>$0</oneOrMore>")),
				caSnippet(d, sne(2, 4, 2, 10, "<list>$0</list>")),
				caSnippet(d, sne(2, 4, 2, 10, "<mixed>$0</mixed>")),
				caSnippet(d, sne(2, 4, 2, 10, "<ref name=\"$1\" />")),
				caSnippet(d, sne(2, 4, 2, 10, "<parentRef name=\"$1\" />")),
				caSnippet(d, sne(2, 4, 2, 10, "<empty />")),
				caSnippet(d, sne(2, 4, 2, 10, "<text />")),
				caSnippet(d, sne(2, 4, 2, 10, "<value type=\"$1\">$0</value>")),
				caSnippet(d, sne(2, 4, 2, 10, "<data type=\"$1\" />")),
				caSnippet(d, sne(2, 4, 2, 10, "<notAllowed />")),
				caSnippet(d, sne(2, 4, 2, 10, "<externalRef href=\"$1\" />")),
				caSnippet(d, sne(2, 4, 2, 10, "<grammar>$0</grammar>")));
	}

	// ---------- Replace invalid name class in except (anyName) ----------

	/**
	 * Use case: {@code <anyName/>} inside {@code <anyName><except>} is not allowed.
	 *
	 * <p>
	 * Code action: replace with valid name class elements ({@code <nsName>},
	 * {@code <name>}).
	 * </p>
	 */
	@Test
	public void any_name_except_contains_any_name_replaceElement() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <anyName>\r\n" + //
				"        <except>\r\n" + //
				"          <anyName/>\r\n" + // line 5: <anyName/> at col 10-20
				"        </except>\r\n" + //
				"      </anyName>\r\n" + //
				"      <text/>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		Diagnostic d = d(5, 11, 18, RelaxNGErrorCode.any_name_except_contains_any_name);
		Diagnostic d2 = d(4, 9, 15, RelaxNGErrorCode.missing_name_class);
		testDiagnosticsFor(xml, d, d2);
		testCodeActionsFor(xml, d,
				ca(d, te(5, 10, 5, 20, "<nsName ns=\"\" />")),
				ca(d, te(5, 10, 5, 20, "<name></name>")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(5, 10, 5, 20, "<nsName ns=\"$1\" />")),
				caSnippet(d, sne(5, 10, 5, 20, "<name>$1</name>")));
	}

	// ---------- Replace invalid name class in except (nsName contains anyName) ----------

	/**
	 * Use case: {@code <anyName/>} inside {@code <nsName><except>} is not allowed.
	 *
	 * <p>
	 * Code action: replace with {@code <name>}.
	 * </p>
	 */
	@Test
	public void ns_name_except_contains_any_name_replaceElement() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <nsName>\r\n" + //
				"        <except>\r\n" + //
				"          <anyName/>\r\n" + // line 5: <anyName/> at col 10-20
				"        </except>\r\n" + //
				"      </nsName>\r\n" + //
				"      <text/>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		Diagnostic d = d(5, 11, 18, RelaxNGErrorCode.ns_name_except_contains_any_name);
		Diagnostic d2 = d(4, 9, 15, RelaxNGErrorCode.missing_name_class);
		testDiagnosticsFor(xml, d, d2);
		testCodeActionsFor(xml, d,
				ca(d, te(5, 10, 5, 20, "<name></name>")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(5, 10, 5, 20, "<name>$1</name>")));
	}

	// ---------- Replace invalid name class in except (nsName contains nsName) ----------

	/**
	 * Use case: {@code <nsName/>} inside {@code <nsName><except>} is not allowed.
	 *
	 * <p>
	 * Code action: replace with {@code <name>}.
	 * </p>
	 */
	@Test
	public void ns_name_except_contains_ns_name_replaceElement() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element>\r\n" + //
				"      <nsName>\r\n" + //
				"        <except>\r\n" + //
				"          <nsName/>\r\n" + // line 5: <nsName/> at col 10-19
				"        </except>\r\n" + //
				"      </nsName>\r\n" + //
				"      <text/>\r\n" + //
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		Diagnostic d = d(5, 11, 17, RelaxNGErrorCode.ns_name_except_contains_ns_name);
		Diagnostic d2 = d(4, 9, 15, RelaxNGErrorCode.missing_name_class);
		testDiagnosticsFor(xml, d, d2);
		testCodeActionsFor(xml, d,
				ca(d, te(5, 10, 5, 19, "<name></name>")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(5, 10, 5, 19, "<name>$1</name>")));
	}

	// ---------- Replace unrecognized datatype (builtin) ----------

	/**
	 * Use case: {@code <data type="integer"/>} without {@code datatypeLibrary} uses
	 * the builtin library which only supports {@code "string"} and {@code "token"}.
	 *
	 * <p>
	 * Code action: replace the type attribute value with valid builtin types only.
	 * </p>
	 */
	@Test
	public void unrecognized_datatype_builtin_replaceType() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"integer\"/>\r\n" + // line 3: "integer" at col 17-26 (with quotes)
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		Diagnostic d = d(3, 17, 26, RelaxNGErrorCode.unrecognized_datatype);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(3, 17, 3, 26, "\"string\"")),
				ca(d, te(3, 17, 3, 26, "\"token\"")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(3, 17, 3, 26, "\"string\"")),
				caSnippet(d, sne(3, 17, 3, 26, "\"token\"")));
	}

	// ---------- Replace unrecognized datatype (XSD) ----------

	/**
	 * Use case: {@code <data type="integerr"/>} with XSD datatypeLibrary has a typo.
	 *
	 * <p>
	 * Code action: replace the type attribute value with valid XSD types.
	 * </p>
	 */
	@Test
	public void unrecognized_datatype_xsd_replaceType() throws Exception {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n" + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n" + //
				"  <start>\r\n" + //
				"    <element name=\"root\">\r\n" + //
				"      <data type=\"integerr\"/>\r\n" + // line 4: "integerr" at col 17-27 (with quotes)
				"    </element>\r\n" + //
				"  </start>\r\n" + //
				"</grammar>";
		Diagnostic d = d(4, 17, 27, RelaxNGErrorCode.unrecognized_datatype);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				// builtin types
				ca(d, te(4, 17, 4, 27, "\"string\"")),
				ca(d, te(4, 17, 4, 27, "\"token\"")),
				// XSD types (sorted alphabetically)
				ca(d, te(4, 17, 4, 27, "\"ENTITIES\"")),
				ca(d, te(4, 17, 4, 27, "\"ENTITY\"")),
				ca(d, te(4, 17, 4, 27, "\"ID\"")),
				ca(d, te(4, 17, 4, 27, "\"IDREF\"")),
				ca(d, te(4, 17, 4, 27, "\"IDREFS\"")),
				ca(d, te(4, 17, 4, 27, "\"NCName\"")),
				ca(d, te(4, 17, 4, 27, "\"NMTOKEN\"")),
				ca(d, te(4, 17, 4, 27, "\"NMTOKENS\"")),
				ca(d, te(4, 17, 4, 27, "\"NOTATION\"")),
				ca(d, te(4, 17, 4, 27, "\"Name\"")),
				ca(d, te(4, 17, 4, 27, "\"QName\"")),
				ca(d, te(4, 17, 4, 27, "\"anyAtomicType\"")),
				ca(d, te(4, 17, 4, 27, "\"anyURI\"")),
				ca(d, te(4, 17, 4, 27, "\"base64Binary\"")),
				ca(d, te(4, 17, 4, 27, "\"boolean\"")),
				ca(d, te(4, 17, 4, 27, "\"byte\"")),
				ca(d, te(4, 17, 4, 27, "\"date\"")),
				ca(d, te(4, 17, 4, 27, "\"dateTime\"")),
				ca(d, te(4, 17, 4, 27, "\"decimal\"")),
				ca(d, te(4, 17, 4, 27, "\"double\"")),
				ca(d, te(4, 17, 4, 27, "\"duration\"")),
				ca(d, te(4, 17, 4, 27, "\"float\"")),
				ca(d, te(4, 17, 4, 27, "\"gDay\"")),
				ca(d, te(4, 17, 4, 27, "\"gMonth\"")),
				ca(d, te(4, 17, 4, 27, "\"gMonthDay\"")),
				ca(d, te(4, 17, 4, 27, "\"gYear\"")),
				ca(d, te(4, 17, 4, 27, "\"gYearMonth\"")),
				ca(d, te(4, 17, 4, 27, "\"hexBinary\"")),
				ca(d, te(4, 17, 4, 27, "\"int\"")),
				ca(d, te(4, 17, 4, 27, "\"integer\"")),
				ca(d, te(4, 17, 4, 27, "\"language\"")),
				ca(d, te(4, 17, 4, 27, "\"long\"")),
				ca(d, te(4, 17, 4, 27, "\"negativeInteger\"")),
				ca(d, te(4, 17, 4, 27, "\"nonNegativeInteger\"")),
				ca(d, te(4, 17, 4, 27, "\"nonPositiveInteger\"")),
				ca(d, te(4, 17, 4, 27, "\"normalizedString\"")),
				ca(d, te(4, 17, 4, 27, "\"positiveInteger\"")),
				ca(d, te(4, 17, 4, 27, "\"short\"")),
				ca(d, te(4, 17, 4, 27, "\"time\"")),
				ca(d, te(4, 17, 4, 27, "\"unsignedByte\"")),
				ca(d, te(4, 17, 4, 27, "\"unsignedInt\"")),
				ca(d, te(4, 17, 4, 27, "\"unsignedLong\"")),
				ca(d, te(4, 17, 4, 27, "\"unsignedShort\"")),
				ca(d, te(4, 17, 4, 27, "\"untypedAtomic\"")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				// builtin types
				caSnippet(d, sne(4, 17, 4, 27, "\"string\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"token\"")),
				// XSD types (sorted alphabetically)
				caSnippet(d, sne(4, 17, 4, 27, "\"ENTITIES\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"ENTITY\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"ID\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"IDREF\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"IDREFS\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"NCName\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"NMTOKEN\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"NMTOKENS\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"NOTATION\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"Name\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"QName\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"anyAtomicType\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"anyURI\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"base64Binary\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"boolean\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"byte\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"date\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"dateTime\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"decimal\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"double\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"duration\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"float\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"gDay\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"gMonth\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"gMonthDay\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"gYear\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"gYearMonth\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"hexBinary\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"int\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"integer\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"language\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"long\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"negativeInteger\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"nonNegativeInteger\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"nonPositiveInteger\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"normalizedString\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"positiveInteger\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"short\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"time\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"unsignedByte\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"unsignedInt\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"unsignedLong\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"unsignedShort\"")),
				caSnippet(d, sne(4, 17, 4, 27, "\"untypedAtomic\"")));
	}

	// ---------- Helpers ----------

	private static void testDiagnosticsFor(String xml, Diagnostic... expected) {
		XMLAssert.testDiagnosticsFor(xml, null, null, null, true, expected);
	}

	private static void testCodeActionsFor(String xml, Diagnostic d, org.eclipse.lsp4j.CodeAction... expected)
			throws Exception {
		XMLAssert.testCodeActionsFor(xml, d, expected);
	}

	private static void testCodeActionsFor(String xml, Diagnostic d,
			org.eclipse.lemminx.settings.SharedSettings settings, org.eclipse.lsp4j.CodeAction... expected)
			throws Exception {
		XMLAssert.testCodeActionsFor(xml, d, settings, expected);
	}
}
