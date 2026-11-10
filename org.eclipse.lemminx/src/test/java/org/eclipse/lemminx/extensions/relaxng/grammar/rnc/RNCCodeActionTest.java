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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.LS;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.ca;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.d;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testCodeActionsFor;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testCodeActionProducesValidRNC;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testFileUri;
import static org.eclipse.lemminx.XMLAssert.te;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

public class RNCCodeActionTest {

	@Test
	public void testCodeActionCloseBlock() throws BadLocationException {
		testCodeActionsFor("element root {",
				d(0, 8, 0, 12, "unclosed-block"),
				ca(d(0, 8, 0, 12, "unclosed-block"),
						te(0, 14, 0, 14, LS + "}")));
	}

	@Test
	public void testCodeActionCloseBlockBeforeDefine() throws BadLocationException {
		String value = "start = bar" + LS
				+ LS
				+ "foo = element foo {" + LS
				+ "  text" + LS
				+ LS
				+ "bar = element bar {" + LS
				+ "  text" + LS
				+ "}";
		testCodeActionsFor(value,
				d(2, 14, 2, 17, "unclosed-block"),
				ca(d(2, 14, 2, 17, "unclosed-block"),
						te(5, 0, 5, 0, "}" + LS)));
	}

	@Test
	public void testCodeActionNoDiagnostics() throws BadLocationException {
		String value = "|element root { text }";
		testCodeActionsFor(value, null,
				ca("Extract define 'root'",
						te(0, 0, 0, 21, "start = root"),
						te(0, 21, 0, 21, LS + LS + "root = element root { text }" + LS)));
		// With snippet support: cursor on extracted name
		testCodeActionsFor(value, true, null,
				ca("Extract define 'root'",
						te(0, 0, 0, 21, "start = root"),
						te(0, 21, 0, 21, LS + LS + "${0:root} = element root { text }" + LS)));
	}

	@Test
	public void testCodeActionAddOpenBrace() throws BadLocationException {
		String value = "element root text";
		testCodeActionsFor(value,
				d(0, 8, 0, 12, "missing-open-brace"),
				ca(d(0, 8, 0, 12, "missing-open-brace"),
						te(0, 12, 0, 12, " { }")));
	}

	@Test
	public void testCodeActionAddOpenBraceWithClosingBrace() throws BadLocationException {
		String value = "element age text }";
		testCodeActionsFor(value,
				d(0, 8, 0, 11, "missing-open-brace"),
				ca(d(0, 8, 0, 11, "missing-open-brace"),
						te(0, 11, 0, 11, " {")));
	}

	@Test
	public void testCodeActionAddAssign() throws BadLocationException {
		String value = "start root";
		// Without snippet support
		testCodeActionsFor(value,
				d(0, 0, 0, 5, "missing-assign"),
				ca(d(0, 0, 0, 5, "missing-assign"),
						te(0, 5, 0, 5, " =")));
		// With snippet support
		testCodeActionsFor(value, true,
				d(0, 0, 0, 5, "missing-assign"),
				ca(d(0, 0, 0, 5, "missing-assign"),
						te(0, 5, 0, 5, " =$0")));
	}

	@Test
	public void testCodeActionAddAssignWithElement() throws BadLocationException {
		String value = "start element root { text }";
		// Without snippet support
		testCodeActionsFor(value,
				d(0, 0, 0, 5, "missing-assign"),
				ca(d(0, 0, 0, 5, "missing-assign"),
						te(0, 5, 0, 5, " =")));
		// With snippet support
		testCodeActionsFor(value, true,
				d(0, 0, 0, 5, "missing-assign"),
				ca(d(0, 0, 0, 5, "missing-assign"),
						te(0, 5, 0, 5, " =$0")));
	}

	@Test
	public void testCodeActionAddUri() throws BadLocationException {
		String value = "include";
		// Without snippet support
		testCodeActionsFor(value,
				d(0, 0, 0, 7, "missing-uri"),
				ca(d(0, 0, 0, 7, "missing-uri"),
						te(0, 7, 0, 7, " \"\"")));
		// With snippet support
		testCodeActionsFor(value, true,
				d(0, 0, 0, 7, "missing-uri"),
				ca(d(0, 0, 0, 7, "missing-uri"),
						te(0, 7, 0, 7, " \"$1\"$0")));
	}

	@Test
	public void testCodeActionAddName() throws BadLocationException {
		String value = "element";
		// Without snippet support
		testCodeActionsFor(value,
				d(0, 0, 0, 7, "missing-name"),
				ca(d(0, 0, 0, 7, "missing-name"),
						te(0, 7, 0, 7, " name")));
		// With snippet support
		testCodeActionsFor(value, true,
				d(0, 0, 0, 7, "missing-name"),
				ca(d(0, 0, 0, 7, "missing-name"),
						te(0, 7, 0, 7, " ${1:name}")));
	}

	@Test
	public void testCodeActionAddDivOpenBrace() throws BadLocationException {
		testCodeActionsFor("div text",
				d(0, 0, 0, 3, "missing-open-brace"),
				ca(d(0, 0, 0, 3, "missing-open-brace"),
						te(0, 3, 0, 3, " { }")));
	}

	@Test
	public void testCodeActionCreateDefine() throws BadLocationException {
		String value = "start = foo" + LS;
		Diagnostic diag = d(0, 8, 0, 11, "undefined-pattern", "reference to undefined pattern \"foo\"");
		// Without snippet support: 3 code actions (element, attribute, free form)
		testCodeActionsFor(value, diag,
				ca(diag, te(1, 0, 1, 0, "foo = element foo {" + LS + "  " + LS + "}" + LS)),
				ca(diag, te(1, 0, 1, 0, "foo = attribute foo { text }" + LS)),
				ca(diag, te(1, 0, 1, 0, "foo = " + LS)));
		// With snippet support
		testCodeActionsFor(value, true, diag,
				ca(diag, te(1, 0, 1, 0, "foo = element foo {" + LS + "  $0" + LS + "}" + LS)),
				ca(diag, te(1, 0, 1, 0, "foo = attribute foo { ${1:text} }" + LS)),
				ca(diag, te(1, 0, 1, 0, "foo = ${1:pattern}" + LS)));
	}

	@Test
	public void testCodeActionDidYouMean() throws BadLocationException {
		// Use case: "start = conten" when "content" exists → "Did you mean 'content'?" first
		String value = "start = conten" + LS +
				"content = element content { text }" + LS;
		Diagnostic diag = d(0, 8, 0, 14, "undefined-pattern", "reference to undefined pattern \"conten\"");
		// "Did you mean" appears before "Create define" actions
		testCodeActionsFor(value, diag,
				ca(diag, te(0, 8, 0, 14, "content")),
				ca(diag, te(2, 0, 2, 0, "conten = element conten {" + LS + "  " + LS + "}" + LS)),
				ca(diag, te(2, 0, 2, 0, "conten = attribute conten { text }" + LS)),
				ca(diag, te(2, 0, 2, 0, "conten = " + LS)));
		// With snippet support
		testCodeActionsFor(value, true, diag,
				ca(diag, te(0, 8, 0, 14, "content")),
				ca(diag, te(2, 0, 2, 0, "conten = element conten {" + LS + "  $0" + LS + "}" + LS)),
				ca(diag, te(2, 0, 2, 0, "conten = attribute conten { ${1:text} }" + LS)),
				ca(diag, te(2, 0, 2, 0, "conten = ${1:pattern}" + LS)));
	}

	@Test
	public void testCodeActionDidYouMeanMultiple() throws BadLocationException {
		// Use case: multiple similar defines → multiple "Did you mean" actions
		String value = "start = bod" + LS +
				"body = element body { text }" + LS +
				"bold = element b { text }" + LS;
		Diagnostic diag = d(0, 8, 0, 11, "undefined-pattern", "reference to undefined pattern \"bod\"");
		testCodeActionsFor(value, diag,
				ca(diag, te(0, 8, 0, 11, "body")),
				ca(diag, te(0, 8, 0, 11, "bold")),
				ca(diag, te(3, 0, 3, 0, "bod = element bod {" + LS + "  " + LS + "}" + LS)),
				ca(diag, te(3, 0, 3, 0, "bod = attribute bod { text }" + LS)),
				ca(diag, te(3, 0, 3, 0, "bod = " + LS)));
		// With snippet support
		testCodeActionsFor(value, true, diag,
				ca(diag, te(0, 8, 0, 11, "body")),
				ca(diag, te(0, 8, 0, 11, "bold")),
				ca(diag, te(3, 0, 3, 0, "bod = element bod {" + LS + "  $0" + LS + "}" + LS)),
				ca(diag, te(3, 0, 3, 0, "bod = attribute bod { ${1:text} }" + LS)),
				ca(diag, te(3, 0, 3, 0, "bod = ${1:pattern}" + LS)));
	}

	@Test
	public void testCodeActionMissingPattern() throws BadLocationException {
		String value = "start = element root { }";
		Diagnostic diag = d(0, 16, 0, 20, "missing-pattern");
		// Without snippet support: 4 code actions (text, empty, element, attribute)
		testCodeActionsFor(value, diag,
				ca(diag, te(0, 22, 0, 23, " text ")),
				ca(diag, te(0, 22, 0, 23, " empty ")),
				ca(diag, te(0, 22, 0, 23, LS + "  element name { }" + LS)),
				ca(diag, te(0, 22, 0, 23, LS + "  attribute name { text }" + LS)));
		// With snippet support
		testCodeActionsFor(value, true, diag,
				ca(diag, te(0, 22, 0, 23, " text ")),
				ca(diag, te(0, 22, 0, 23, " empty ")),
				ca(diag, te(0, 22, 0, 23, LS + "  element ${1:name} {" + LS + "    $0" + LS + "  }" + LS)),
				ca(diag, te(0, 22, 0, 23, LS + "  attribute ${1:name} { ${2:text} }" + LS)));
	}

	@Test
	public void testCodeActionMissingPatternInAttribute() throws BadLocationException {
		String value = "start = element root {" + LS
				+ "  attribute id { }" + LS
				+ "}";
		Diagnostic diag = d(1, 12, 1, 14, "missing-pattern");
		testCodeActionsFor(value, diag,
				ca(diag, te(1, 16, 1, 17, " text ")),
				ca(diag, te(1, 16, 1, 17, " empty ")),
				ca(diag, te(1, 16, 1, 17, LS + "    element name { }" + LS + "  ")),
				ca(diag, te(1, 16, 1, 17, LS + "    attribute name { text }" + LS + "  ")));
		// With snippet support
		testCodeActionsFor(value, true, diag,
				ca(diag, te(1, 16, 1, 17, " text ")),
				ca(diag, te(1, 16, 1, 17, " empty ")),
				ca(diag, te(1, 16, 1, 17, LS + "    element ${1:name} {" + LS + "      $0" + LS + "    }" + LS + "  ")),
				ca(diag, te(1, 16, 1, 17, LS + "    attribute ${1:name} { ${2:text} }" + LS + "  ")));
	}

	@Test
	public void testCodeActionMissingPatternNested() throws BadLocationException {
		String value = "foo = element parent {" + LS
				+ "  element child { }" + LS
				+ "}";
		Diagnostic diag = d(1, 10, 1, 15, "missing-pattern");
		testCodeActionsFor(value, diag,
				ca(diag, te(1, 17, 1, 18, " text ")),
				ca(diag, te(1, 17, 1, 18, " empty ")),
				ca(diag, te(1, 17, 1, 18, LS + "    element name { }" + LS + "  ")),
				ca(diag, te(1, 17, 1, 18, LS + "    attribute name { text }" + LS + "  ")));
		// With snippet support
		testCodeActionsFor(value, true, diag,
				ca(diag, te(1, 17, 1, 18, " text ")),
				ca(diag, te(1, 17, 1, 18, " empty ")),
				ca(diag, te(1, 17, 1, 18, LS + "    element ${1:name} {" + LS + "      $0" + LS + "    }" + LS + "  ")),
				ca(diag, te(1, 17, 1, 18, LS + "    attribute ${1:name} { ${2:text} }" + LS + "  ")));
	}

	@Test
	public void testCodeActionDidYouMeanBuiltinKeyword() throws BadLocationException {
		String value = "start = element root {" + LS +
				"  tex" + LS +
				"}" + LS;
		Diagnostic diag = d(1, 2, 1, 5, "undefined-pattern", "reference to undefined pattern \"tex\"");
		testCodeActionsFor(value, diag,
				ca(diag, te(1, 2, 1, 5, "text")),
				ca(diag, te(3, 0, 3, 0, "tex = element tex {" + LS + "  " + LS + "}" + LS)),
				ca(diag, te(3, 0, 3, 0, "tex = attribute tex { text }" + LS)),
				ca(diag, te(3, 0, 3, 0, "tex = " + LS)));
		// With snippet support
		testCodeActionsFor(value, true, diag,
				ca(diag, te(1, 2, 1, 5, "text")),
				ca(diag, te(3, 0, 3, 0, "tex = element tex {" + LS + "  $0" + LS + "}" + LS)),
				ca(diag, te(3, 0, 3, 0, "tex = attribute tex { ${1:text} }" + LS)),
				ca(diag, te(3, 0, 3, 0, "tex = ${1:pattern}" + LS)));
	}

	@Test
	public void testCodeActionDidYouMeanBuiltinKeywordInline() throws BadLocationException {
		// Top-level pattern form: only "Did you mean" (no "Create define")
		String value = "element email { tex }" + LS;
		Diagnostic diag = d(0, 16, 0, 19, "undefined-pattern", "reference to undefined pattern \"tex\"");
		testCodeActionsFor(value, diag,
				ca(diag, te(0, 16, 0, 19, "text")));
	}

	@Test
	public void testCodeActionNoCreateDefineInPatternForm() throws BadLocationException {
		// Top-level pattern form (no start =): only "Did you mean", no "Create define"
		String value = "element addressBook { tex }" + LS;
		Diagnostic diag = d(0, 22, 0, 25, "undefined-pattern", "reference to undefined pattern \"tex\"");
		testCodeActionsFor(value, diag,
				ca(diag, te(0, 22, 0, 25, "text")));
	}

	@Test
	public void testCodeActionCreateDefineInGrammarForm() throws BadLocationException {
		// Grammar form (with start =): "Did you mean" + "Create define"
		String value = "start = element root { tex }" + LS;
		Diagnostic diag = d(0, 23, 0, 26, "undefined-pattern", "reference to undefined pattern \"tex\"");
		testCodeActionsFor(value, diag,
				ca(diag, te(0, 23, 0, 26, "text")),
				ca(diag, te(1, 0, 1, 0, "tex = element tex {" + LS + "  " + LS + "}" + LS)),
				ca(diag, te(1, 0, 1, 0, "tex = attribute tex { text }" + LS)),
				ca(diag, te(1, 0, 1, 0, "tex = " + LS)));
		// With snippet support
		testCodeActionsFor(value, true, diag,
				ca(diag, te(0, 23, 0, 26, "text")),
				ca(diag, te(1, 0, 1, 0, "tex = element tex {" + LS + "  $0" + LS + "}" + LS)),
				ca(diag, te(1, 0, 1, 0, "tex = attribute tex { ${1:text} }" + LS)),
				ca(diag, te(1, 0, 1, 0, "tex = ${1:pattern}" + LS)));
	}

	@Test
	public void testCodeActionNoDidYouMean() throws BadLocationException {
		// Use case: no similar defines → only "Create define" actions (no "Did you mean")
		String value = "start = xyz" + LS +
				"content = element content { text }" + LS;
		Diagnostic diag = d(0, 8, 0, 11, "undefined-pattern", "reference to undefined pattern \"xyz\"");
		testCodeActionsFor(value, diag,
				ca(diag, te(2, 0, 2, 0, "xyz = element xyz {" + LS + "  " + LS + "}" + LS)),
				ca(diag, te(2, 0, 2, 0, "xyz = attribute xyz { text }" + LS)),
				ca(diag, te(2, 0, 2, 0, "xyz = " + LS)));
		// With snippet support
		testCodeActionsFor(value, true, diag,
				ca(diag, te(2, 0, 2, 0, "xyz = element xyz {" + LS + "  $0" + LS + "}" + LS)),
				ca(diag, te(2, 0, 2, 0, "xyz = attribute xyz { ${1:text} }" + LS)),
				ca(diag, te(2, 0, 2, 0, "xyz = ${1:pattern}" + LS)));
	}

	// -------------------- Missing start element --------------------

	@Test
	public void testCodeActionMissingStartElement() throws BadLocationException {
		String value = "root = element root { text }";
		Diagnostic diag = d(0, 0, 0, 4, "missing-start-element", "missing \"start\" element");
		testCodeActionsFor(value, diag,
				ca(diag, te(0, 0, 0, 0, "start = root" + LS + LS)));
	}

	@Test
	public void testCodeActionMissingStartElementMultipleDefines() throws BadLocationException {
		String value = "root = element root {" + LS +
				"  inline" + LS +
				"}" + LS +
				"inline = element span { text }";
		Diagnostic diag = d(0, 0, 0, 4, "missing-start-element", "missing \"start\" element");
		testCodeActionsFor(value, diag,
				ca(diag, te(0, 0, 0, 0, "start = root" + LS + LS)),
				ca(diag, te(0, 0, 0, 0, "start = inline" + LS + LS)));
	}

	// -------------------- Tests from error files with snippet support --------------------

	@Test
	public void testCodeActionAddNameFromFile() throws BadLocationException {
		String value = "start = element {" + LS
				+ "  text" + LS
				+ "}";
		testCodeActionsFor(value, true,
				d(0, 8, 0, 15, "missing-name"),
				ca(d(0, 8, 0, 15, "missing-name"),
						te(0, 15, 0, 15, " ${1:name}")));
	}

	@Test
	public void testCodeActionAddOpenBraceFromFile() throws BadLocationException {
		String value = "start = element root" + LS
				+ "  text" + LS
				+ "}";
		testCodeActionsFor(value, true,
				d(0, 16, 0, 20, "missing-open-brace"),
				ca(d(0, 16, 0, 20, "missing-open-brace"),
						te(0, 20, 0, 20, " { ")));
	}

	@Test
	public void testCodeActionAddUriFromFile() throws BadLocationException {
		testCodeActionsFor("include", true,
				d(0, 0, 0, 7, "missing-uri"),
				ca(d(0, 0, 0, 7, "missing-uri"),
						te(0, 7, 0, 7, " \"$1\"$0")));
	}

	@Test
	public void testCodeActionCloseBlockFromFile() throws BadLocationException {
		String value = "start = element root {" + LS
				+ "  element child {" + LS
				+ "    text" + LS
				+ "}";
		testCodeActionsFor(value,
				d(0, 16, 0, 20, "unclosed-block"),
				ca(d(0, 16, 0, 20, "unclosed-block"),
						te(3, 1, 3, 1, LS + "}")));
	}

	@Test
	public void testCodeActionCreateDefineFromFile() throws BadLocationException {
		String value = "start = element root {" + LS
				+ "  content" + LS
				+ "}" + LS;
		Diagnostic diag = d(1, 2, 1, 9, "undefined-pattern", "reference to undefined pattern \"content\"");
		testCodeActionsFor(value, true, diag,
				ca(diag, te(3, 0, 3, 0, "content = element content {" + LS + "  $0" + LS + "}" + LS)),
				ca(diag, te(3, 0, 3, 0, "content = attribute content { ${1:text} }" + LS)),
				ca(diag, te(3, 0, 3, 0, "content = ${1:pattern}" + LS)));
	}

	@Test
	public void testCodeActionMissingStartElementFromFile() throws BadLocationException {
		String value = "root = element root { text }" + LS
				+ LS
				+ "child = element child { text }";
		Diagnostic diag = d(0, 0, 0, 4, "missing-start-element", "missing \"start\" element");
		testCodeActionsFor(value, diag,
				ca(diag, te(0, 0, 0, 0, "start = root" + LS + LS)),
				ca(diag, te(0, 0, 0, 0, "start = child" + LS + LS)));
	}

	@Test
	public void testCodeActionAddDivOpenBraceWithSnippet() throws BadLocationException {
		testCodeActionsFor("div text", true,
				d(0, 0, 0, 3, "missing-open-brace"),
				ca(d(0, 0, 0, 3, "missing-open-brace"),
						te(0, 3, 0, 3, " { }")));
	}

	@Test
	public void testCodeActionCloseBlockWithSnippet() throws BadLocationException {
		testCodeActionsFor("element root {", true,
				d(0, 8, 0, 12, "unclosed-block"),
				ca(d(0, 8, 0, 12, "unclosed-block"),
						te(0, 14, 0, 14, LS + "}")));
	}

	@Test
	public void testCodeActionMissingPatternWithSnippet() throws BadLocationException {
		String value = "start = element root { }";
		Diagnostic diag = d(0, 16, 0, 20, "missing-pattern");
		testCodeActionsFor(value, true, diag,
				ca(diag, te(0, 22, 0, 23, " text ")),
				ca(diag, te(0, 22, 0, 23, " empty ")),
				ca(diag, te(0, 22, 0, 23, LS + "  element ${1:name} {" + LS + "    $0" + LS + "  }" + LS)),
				ca(diag, te(0, 22, 0, 23, LS + "  attribute ${1:name} { ${2:text} }" + LS)));
	}

	// -------------------- Code Action Integrity Tests --------------------
	// Verify that applying each code action produces valid (parseable) RNC

	@Test
	public void testIntegrityMissingOpenBrace() throws BadLocationException {
		String value = "element root text";
		Diagnostic diag = d(0, 8, 0, 12, "missing-open-brace");
		testCodeActionProducesValidRNC(value, true, diag,
				ca(diag, te(0, 12, 0, 12, " { }")));
	}

	@Test
	public void testIntegrityMissingOpenBraceDiv() throws BadLocationException {
		String value = "div text";
		Diagnostic diag = d(0, 0, 0, 3, "missing-open-brace");
		testCodeActionProducesValidRNC(value, true, diag,
				ca(diag, te(0, 3, 0, 3, " { }")));
	}

	@Test
	public void testIntegrityMissingAssign() throws BadLocationException {
		String value = "start root";
		Diagnostic diag = d(0, 0, 0, 5, "missing-assign");
		testCodeActionProducesValidRNC(value, true, diag,
				ca(diag, te(0, 5, 0, 5, " =$0")));
	}

	@Test
	public void testIntegrityMissingAssignWithElement() throws BadLocationException {
		String value = "start element root { text }";
		Diagnostic diag = d(0, 0, 0, 5, "missing-assign");
		testCodeActionProducesValidRNC(value, true, diag,
				ca(diag, te(0, 5, 0, 5, " =$0")));
	}

	@Test
	public void testIntegrityMissingUri() throws BadLocationException {
		String value = "include";
		Diagnostic diag = d(0, 0, 0, 7, "missing-uri");
		testCodeActionProducesValidRNC(value, true, diag,
				ca(diag, te(0, 7, 0, 7, " \"$1\"$0")));
	}

	@Test
	public void testIntegrityMissingUriNamespace() throws BadLocationException {
		String value = "namespace foo =";
		Diagnostic diag = d(0, 0, 0, 15, "missing-uri");
		testCodeActionProducesValidRNC(value, true, diag,
				ca(diag, te(0, 15, 0, 15, " \"$1\"$0")));
	}

	@Test
	public void testIntegrityUnclosedBlock() throws BadLocationException {
		String value = "element root {";
		Diagnostic diag = d(0, 8, 0, 12, "unclosed-block");
		testCodeActionProducesValidRNC(value, diag,
				ca(diag, te(0, 14, 0, 14, LS + "}")));
	}

	@Test
	public void testIntegrityUnclosedBlockNested() throws BadLocationException {
		String value = "start = element root {" + LS +
				"  element child {" + LS +
				"    text" + LS +
				"}";
		Diagnostic diag = d(0, 16, 0, 20, "unclosed-block");
		testCodeActionProducesValidRNC(value, diag,
				ca(diag, te(3, 1, 3, 1, LS + "}")));
	}

	@Test
	public void testIntegrityCreateDefine() throws BadLocationException {
		String value = "start = foo" + LS;
		Diagnostic diag = d(0, 8, 0, 11, "undefined-pattern", "reference to undefined pattern \"foo\"");
		testCodeActionProducesValidRNC(value, true, diag,
				ca(diag, te(1, 0, 1, 0, "foo = element foo {" + LS + "  $0" + LS + "}" + LS)),
				ca(diag, te(1, 0, 1, 0, "foo = attribute foo { ${1:text} }" + LS)),
				ca(diag, te(1, 0, 1, 0, "foo = ${1:pattern}" + LS)));
	}

	@Test
	public void testIntegrityCreateDefineInComplexSchema() throws BadLocationException {
		String value = "start = element root {" + LS +
				"  header, content, footer" + LS +
				"}" + LS;
		Diagnostic diag = d(1, 2, 1, 8, "undefined-pattern", "reference to undefined pattern \"header\"");
		testCodeActionProducesValidRNC(value, true, diag,
				ca(diag, te(3, 0, 3, 0, "header = element header {" + LS + "  $0" + LS + "}" + LS)),
				ca(diag, te(3, 0, 3, 0, "header = attribute header { ${1:text} }" + LS)),
				ca(diag, te(3, 0, 3, 0, "header = ${1:pattern}" + LS)));
	}

	@Test
	public void testIntegrityMissingPattern() throws BadLocationException {
		String value = "start = element root { }";
		Diagnostic diag = d(0, 16, 0, 20, "missing-pattern");
		testCodeActionProducesValidRNC(value, true, diag,
				ca(diag, te(0, 22, 0, 23, " text ")),
				ca(diag, te(0, 22, 0, 23, " empty ")),
				ca(diag, te(0, 22, 0, 23, LS + "  element ${1:name} {" + LS + "    $0" + LS + "  }" + LS)),
				ca(diag, te(0, 22, 0, 23, LS + "  attribute ${1:name} { ${2:text} }" + LS)));
	}

	@Test
	public void testIntegrityMissingPatternNested() throws BadLocationException {
		String value = "start = element root {" + LS +
				"  element child { }" + LS +
				"}";
		Diagnostic diag = d(1, 10, 1, 15, "missing-pattern");
		testCodeActionProducesValidRNC(value, true, diag,
				ca(diag, te(1, 17, 1, 18, " text ")),
				ca(diag, te(1, 17, 1, 18, " empty ")),
				ca(diag, te(1, 17, 1, 18, LS + "    element ${1:name} {" + LS + "      $0" + LS + "    }" + LS + "  ")),
				ca(diag, te(1, 17, 1, 18, LS + "    attribute ${1:name} { ${2:text} }" + LS + "  ")));
	}

	@Test
	public void testIntegrityMissingStartElement() throws BadLocationException {
		String value = "root = element root { text }";
		Diagnostic diag = d(0, 0, 0, 4, "missing-start-element", "missing \"start\" element");
		testCodeActionProducesValidRNC(value, diag,
				ca(diag, te(0, 0, 0, 0, "start = root" + LS + LS)));
	}

	@Test
	public void testIntegrityMissingStartElementMultipleDefines() throws BadLocationException {
		String value = "root = element root { text }" + LS +
				LS +
				"child = element child { text }";
		Diagnostic diag = d(0, 0, 0, 4, "missing-start-element", "missing \"start\" element");
		testCodeActionProducesValidRNC(value, diag,
				ca(diag, te(0, 0, 0, 0, "start = root" + LS + LS)),
				ca(diag, te(0, 0, 0, 0, "start = child" + LS + LS)));
	}

	@Test
	public void testIntegrityDidYouMean() throws BadLocationException {
		String value = "start = conten" + LS +
				"content = element content { text }" + LS;
		Diagnostic diag = d(0, 8, 0, 14, "undefined-pattern", "reference to undefined pattern \"conten\"");
		testCodeActionProducesValidRNC(value, diag,
				ca(diag, te(0, 8, 0, 14, "content")),
				ca(diag, te(2, 0, 2, 0, "conten = element conten {" + LS + "  " + LS + "}" + LS)),
				ca(diag, te(2, 0, 2, 0, "conten = attribute conten { text }" + LS)),
				ca(diag, te(2, 0, 2, 0, "conten = " + LS)));
	}

	// -------------------- Code Action in complex contexts --------------------

	@Test
	public void testCodeActionMissingOpenBraceAfterElement() throws BadLocationException {
		String value = "start = element root" + LS +
				"  text" + LS +
				"}";
		testCodeActionsFor(value, true,
				d(0, 16, 0, 20, "missing-open-brace"),
				ca(d(0, 16, 0, 20, "missing-open-brace"),
						te(0, 20, 0, 20, " { ")));
	}

	@Test
	public void testCodeActionCloseBlockMultipleDefines() throws BadLocationException {
		String value = "start = element root {" + LS +
				"  text";
		testCodeActionsFor(value,
				d(0, 16, 0, 20, "unclosed-block"),
				ca(d(0, 16, 0, 20, "unclosed-block"),
						te(1, 6, 1, 6, LS + "}")));
	}

	@Test
	public void testCodeActionAddAssignDefine() throws BadLocationException {
		String value = "root element root { text }";
		testCodeActionsFor(value,
				d(0, 0, 0, 4, "missing-assign"),
				ca(d(0, 0, 0, 4, "missing-assign"),
						te(0, 4, 0, 4, " =")));
	}

	@Test
	public void testCodeActionAddAssignNamespace() throws BadLocationException {
		String value = "namespace foo \"uri\"";
		testCodeActionsFor(value,
				d(0, 0, 0, 13, "missing-assign"),
				ca(d(0, 0, 0, 13, "missing-assign"),
						te(0, 13, 0, 13, " =")));
	}

	@Test
	public void testCodeActionAddAssignDatatypes() throws BadLocationException {
		String value = "datatypes xsd \"uri\"";
		testCodeActionsFor(value,
				d(0, 0, 0, 13, "missing-assign"),
				ca(d(0, 0, 0, 13, "missing-assign"),
						te(0, 13, 0, 13, " =")));
	}

	@Test
	public void testCodeActionAddUriDatatypes() throws BadLocationException {
		String value = "datatypes xsd =";
		testCodeActionsFor(value, true,
				d(0, 0, 0, 15, "missing-uri"),
				ca(d(0, 0, 0, 15, "missing-uri"),
						te(0, 15, 0, 15, " \"$1\"$0")));
	}

	@Test
	public void testCodeActionMissingPatternInDeeplyNested() throws BadLocationException {
		String value = "start = element root {" + LS +
				"  element child {" + LS +
				"    element grandchild { }" + LS +
				"  }" + LS +
				"}";
		Diagnostic diag = d(2, 8, 2, 18, "missing-pattern");
		testCodeActionsFor(value, diag,
				ca(diag, te(2, 24, 2, 25, " text ")),
				ca(diag, te(2, 24, 2, 25, " empty ")),
				ca(diag, te(2, 24, 2, 25, LS + "      element name { }" + LS + "    ")),
				ca(diag, te(2, 24, 2, 25, LS + "      attribute name { text }" + LS + "    ")));
		// With snippet support
		testCodeActionsFor(value, true, diag,
				ca(diag, te(2, 24, 2, 25, " text ")),
				ca(diag, te(2, 24, 2, 25, " empty ")),
				ca(diag, te(2, 24, 2, 25, LS + "      element ${1:name} {" + LS + "        $0" + LS + "      }" + LS + "    ")),
				ca(diag, te(2, 24, 2, 25, LS + "      attribute ${1:name} { ${2:text} }" + LS + "    ")));
	}

	// --- Extract define refactoring ---

	@Test
	public void testExtractDefineNestedElement() throws BadLocationException {
		String value = "start = element foo {" + LS
				+ "  element |bar { text }" + LS
				+ "}";
		testCodeActionsFor(value, null,
				ca("Extract define 'bar'",
						te(1, 2, 1, 22, "bar"),
						te(2, 1, 2, 1, LS + LS + "bar = element bar { text }" + LS)));
		// With snippet support: cursor on extracted name
		testCodeActionsFor(value, true, null,
				ca("Extract define 'bar'",
						te(1, 2, 1, 22, "bar"),
						te(2, 1, 2, 1, LS + LS + "${0:bar} = element bar { text }" + LS)));
	}

	@Test
	public void testExtractDefineNestedAttribute() throws BadLocationException {
		String value = "start = element foo {" + LS
				+ "  attribute |id { text }" + LS
				+ "}";
		testCodeActionsFor(value, null,
				ca("Extract define 'id'",
						te(1, 2, 1, 23, "id"),
						te(2, 1, 2, 1, LS + LS + "id = attribute id { text }" + LS)));
		// With snippet support
		testCodeActionsFor(value, true, null,
				ca("Extract define 'id'",
						te(1, 2, 1, 23, "id"),
						te(2, 1, 2, 1, LS + LS + "${0:id} = attribute id { text }" + LS)));
	}

	@Test
	public void testExtractDefineFromStart() throws BadLocationException {
		// Element in start declaration should be extractable
		String value = "start = element |name { text }";
		testCodeActionsFor(value, null,
				ca("Extract define 'name'",
						te(0, 8, 0, 29, "name"),
						te(0, 29, 0, 29, LS + LS + "name = element name { text }" + LS)));
		// With snippet support
		testCodeActionsFor(value, true, null,
				ca("Extract define 'name'",
						te(0, 8, 0, 29, "name"),
						te(0, 29, 0, 29, LS + LS + "${0:name} = element name { text }" + LS)));
	}

	@Test
	public void testExtractDefineFromStandalone() throws BadLocationException {
		// Standalone top-level element should be extractable, generating start = name
		String value = "element |foo { text }";
		testCodeActionsFor(value, null,
				ca("Extract define 'foo'",
						te(0, 0, 0, 20, "start = foo"),
						te(0, 20, 0, 20, LS + LS + "foo = element foo { text }" + LS)));
		// With snippet support
		testCodeActionsFor(value, true, null,
				ca("Extract define 'foo'",
						te(0, 0, 0, 20, "start = foo"),
						te(0, 20, 0, 20, LS + LS + "${0:foo} = element foo { text }" + LS)));
	}

	@Test
	public void testExtractDefineFromStartWithExistingDefines() throws BadLocationException {
		// Extract element when a define with the same name already exists
		String value = "start = element doc {" + LS
				+ "  element |foo { text }" + LS
				+ "}" + LS
				+ LS
				+ "foo = element bar { text }";
		testCodeActionsFor(value, null,
				ca("Extract define 'foo1'",
						te(1, 2, 1, 22, "foo1"),
						te(4, 26, 4, 26, LS + LS + "foo1 = element foo { text }" + LS)));
		// With snippet support
		testCodeActionsFor(value, true, null,
				ca("Extract define 'foo1'",
						te(1, 2, 1, 22, "foo1"),
						te(4, 26, 4, 26, LS + LS + "${0:foo1} = element foo { text }" + LS)));
	}

	@Test
	public void testExtractDefineNotOfferedWithDiagnostic() throws BadLocationException {
		// When diagnostics are present, extract define should not be offered
		String value = "start = element root {" + LS
				+ "  element child { }" + LS
				+ "}";
		Diagnostic diag = d(1, 10, 1, 15, "missing-pattern");
		testCodeActionsFor(value, diag,
				ca(diag, te(1, 17, 1, 18, " text ")),
				ca(diag, te(1, 17, 1, 18, " empty ")),
				ca(diag, te(1, 17, 1, 18, LS + "    element name { }" + LS + "  ")),
				ca(diag, te(1, 17, 1, 18, LS + "    attribute name { text }" + LS + "  ")));
	}

	@Test
	public void testExtractDefineDedentsMultiline() throws BadLocationException {
		String value = "start = element root {" + LS
				+ "  element |bar {" + LS
				+ "    attribute name { text }" + LS
				+ "  }" + LS
				+ "}";
		testCodeActionsFor(value, null,
				ca("Extract define 'bar'",
						te(1, 2, 3, 3, "bar"),
						te(4, 1, 4, 1, LS + LS + "bar = element bar {" + LS + "  attribute name { text }" + LS + "}" + LS)));
		// With snippet support
		testCodeActionsFor(value, true, null,
				ca("Extract define 'bar'",
						te(1, 2, 3, 3, "bar"),
						te(4, 1, 4, 1, LS + LS + "${0:bar} = element bar {" + LS + "  attribute name { text }" + LS + "}" + LS)));
	}

	@Test
	public void testExtractDefineWithSnippet() throws BadLocationException {
		String value = "start = element foo {" + LS
				+ "  element |bar { text }" + LS
				+ "}";
		testCodeActionsFor(value, true, null,
				ca("Extract define 'bar'",
						te(1, 2, 1, 22, "bar"),
						te(2, 1, 2, 1, LS + LS + "${0:bar} = element bar { text }" + LS)));
	}

	// --- Include not overriding ---

	@Test
	public void testCodeActionIncludeNotOverridingDidYouMean() throws BadLocationException {
		// include-base.rnc defines: start, paragraph
		// "paragrap" is close to "paragraph" → "Did you mean"
		String fileUri = testFileUri("include-valid.rnc");
		String value = "include \"include-base.rnc\" {" + LS +
				"  paragrap = element p { text }" + LS +
				"}";
		Diagnostic diag = d(1, 2, 1, 10, "include-not-overriding",
				"definition of \"paragrap\" in \"include\" does not override anything");
		testCodeActionsFor(value, fileUri, diag,
				ca(diag, te(1, 2, 1, 10, "paragraph")),
				ca(diag, te(1, 2, 1, 10, "start")));
	}
}
