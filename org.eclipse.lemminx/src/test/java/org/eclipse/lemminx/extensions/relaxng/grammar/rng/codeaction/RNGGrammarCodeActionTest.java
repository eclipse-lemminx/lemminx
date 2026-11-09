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
	 * Use case: {@code <data />} element without required 'type' attribute.
	 *
	 * <pre>
	 * &lt;data /&gt;
	 * </pre>
	 *
	 * Code action: insert {@code type=""} attribute with cursor inside quotes.
	 */
	@Test
	public void missing_type_attribute_insertAttribute() throws Exception {
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
				ca(d, te(5, 11, 5, 11, " type=\"\"")));
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(5, 11, 5, 11, " type=\"$1\"")));
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
