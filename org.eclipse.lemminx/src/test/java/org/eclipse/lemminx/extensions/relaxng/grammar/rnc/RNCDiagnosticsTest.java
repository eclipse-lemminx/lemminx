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
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.d;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testDiagnosticsFor;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testFileContent;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testFileUri;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testNoParserErrors;

import java.io.IOException;

import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.validator.RNCErrorCode;
import org.eclipse.lsp4j.DiagnosticTag;
import org.junit.jupiter.api.Test;

public class RNCDiagnosticsTest {

	// -------------------- Parser Diagnostics --------------------

	@Test
	public void testDiagnosticsUnclosedElement() {
		testDiagnosticsFor("element root {",
				d(0, 8, 0, 12, "unclosed-block"));
	}

	@Test
	public void testDiagnosticsValidDocument() {
		testDiagnosticsFor("start = element root { text }");
	}

	@Test
	public void testDiagnosticsEmptyDocument() {
		testNoParserErrors("");
	}

	@Test
	public void testDiagnosticsValidFullSchema() {
		String value = "default namespace = \"http://example.com\"" + LS
				+ "start = root" + LS
				+ "root = element root {" + LS
				+ "  attribute id { text }," + LS
				+ "  name+" + LS
				+ "}" + LS
				+ "name = element name { text }";
		testDiagnosticsFor(value);
	}

	@Test
	public void testDiagnosticsUnclosedAttribute() {
		testDiagnosticsFor("attribute id {",
				d(0, 10, 0, 12, "unclosed-block"));
	}

	@Test
	public void testDiagnosticsUnclosedDiv() {
		testDiagnosticsFor("div {",
				d(0, 0, 0, 5, "unclosed-block"));
	}

	@Test
	public void testDiagnosticsUnclosedInclude() {
		testDiagnosticsFor("include \"other.rnc\" {",
				d(0, 0, 0, 21, "unclosed-block"));
	}

	@Test
	public void testDiagnosticsClosedBlocksAreOk() {
		String value = "element root { text }" + LS +
				"attribute id { text }" + LS +
				"div { }";
		testNoParserErrors(value);
	}

	// --- Missing name ---

	@Test
	public void testDiagnosticsMissingElementName() {
		testDiagnosticsFor("element {",
				d(0, 0, 0, 7, "missing-name"));
	}

	@Test
	public void testDiagnosticsMissingAttributeName() {
		testDiagnosticsFor("attribute {",
				d(0, 0, 0, 9, "missing-name"));
	}

	@Test
	public void testDiagnosticsMissingElementNameEOS() {
		testDiagnosticsFor("element",
				d(0, 0, 0, 7, "missing-name"));
	}

	@Test
	public void testDiagnosticsMissingAttributeNameEOS() {
		testDiagnosticsFor("attribute",
				d(0, 0, 0, 9, "missing-name"));
	}

	// --- Missing open brace ---

	@Test
	public void testDiagnosticsMissingElementOpenBrace() {
		testDiagnosticsFor("element root text",
				d(0, 8, 0, 12, "missing-open-brace"));
	}

	@Test
	public void testDiagnosticsMissingAttributeOpenBrace() {
		testDiagnosticsFor("attribute id text",
				d(0, 10, 0, 12, "missing-open-brace"));
	}

	@Test
	public void testDiagnosticsMissingDivOpenBrace() {
		testDiagnosticsFor("div text",
				d(0, 0, 0, 3, "missing-open-brace"));
	}

	// --- Missing assign ---

	@Test
	public void testDiagnosticsMissingStartAssign() {
		testDiagnosticsFor("start root",
				d(0, 0, 0, 5, "missing-assign"));
	}

	@Test
	public void testDiagnosticsMissingNamespaceAssign() {
		testDiagnosticsFor("namespace foo \"uri\"",
				d(0, 0, 0, 13, "missing-assign"));
	}

	@Test
	public void testDiagnosticsMissingDatatypesAssign() {
		testDiagnosticsFor("datatypes xsd \"uri\"",
				d(0, 0, 0, 13, "missing-assign"));
	}

	// --- Missing URI ---

	@Test
	public void testDiagnosticsMissingNamespaceUri() {
		testDiagnosticsFor("namespace foo =",
				d(0, 0, 0, 15, "missing-uri"));
	}

	@Test
	public void testDiagnosticsMissingDatatypesUri() {
		testDiagnosticsFor("datatypes xsd =",
				d(0, 0, 0, 15, "missing-uri"));
	}

	@Test
	public void testDiagnosticsMissingIncludeUri() {
		testDiagnosticsFor("include",
				d(0, 0, 0, 7, "missing-uri"));
	}

	@Test
	public void testDiagnosticsMissingIncludeUriWithOther() {
		String value = "include" + LS +
				"start = root";
		testDiagnosticsFor(value,
				d(0, 0, 0, 7, "missing-uri"));
	}

	@Test
	public void testDiagnosticsMissingExternalUri() {
		testDiagnosticsFor("start = external",
				d(0, 8, 0, 16, "missing-uri"));
	}

	@Test
	public void testDiagnosticsValidExternal() {
		testNoParserErrors("start = external \"other.rnc\"");
	}

	@Test
	public void testDiagnosticsValidExternalInsideDefine() {
		testNoParserErrors("start = root" + LS +
				"root = external \"content.rnc\"");
	}

	// --- Valid cases (no errors) ---

	@Test
	public void testDiagnosticsValidNamespace() {
		testNoParserErrors("namespace foo = \"http://example.com\"");
	}

	@Test
	public void testDiagnosticsValidDefaultNamespace() {
		testNoParserErrors("default namespace = \"http://example.com\"");
	}

	@Test
	public void testDiagnosticsValidDatatypes() {
		testNoParserErrors("datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"");
	}

	@Test
	public void testDiagnosticsValidInclude() {
		testDiagnosticsFor("include \"base.rnc\"");
	}

	@Test
	public void testDiagnosticsValidIncludeWithBody() {
		String value = "include \"other.rnc\" {" + LS +
				"  start = foo" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testDiagnosticsNoMissingStartWithInclude() {
		String value = "include \"base.rnc\" {" + LS +
				"  paragraph = element p { text }" + LS +
				"}" + LS +
				LS +
				"inline = element bold { text }";
		testDiagnosticsFor(value, testFileUri("base.rnc"));
	}

	@Test
	public void testDiagnosticsValidDiv() {
		String value = "div {" + LS +
				"  a = text" + LS +
				"}";
		testNoParserErrors(value);
	}

	@Test
	public void testDiagnosticsValidStart() {
		testNoParserErrors("start = root");
	}

	@Test
	public void testDiagnosticsValidStartChoice() {
		testNoParserErrors("start |= root");
	}

	// --- Missing pattern ---

	@Test
	public void testDiagnosticsMissingPatternInElement() {
		testDiagnosticsFor("start = element root { }",
				d(0, 16, 0, 20, "missing-pattern"));
	}

	@Test
	public void testDiagnosticsMissingPatternInAttribute() {
		testDiagnosticsFor("start = element root { attribute id { } }",
				d(0, 33, 0, 35, "missing-pattern"));
	}

	// -------------------- Jing Validation Diagnostics --------------------

	@Test
	public void testDiagnosticsUndefinedPattern() {
		testDiagnosticsFor("start = foo",
				d(0, 8, 0, 11, "undefined-pattern"));
	}

	@Test
	public void testDiagnosticsDuplicateDefinition() {
		String value = "start = a" + LS +
				"a = text" + LS +
				"a = element x { empty }";
		testDiagnosticsFor(value,
				d(2, 0, 2, 1, "duplicate-definition"));
	}

	@Test
	public void testDiagnosticsMissingStartElement() {
		String value = "root = element root { text }";
		testDiagnosticsFor(value,
				d(0, 0, 0, 4, "missing-start-element"));
	}

	@Test
	public void testDiagnosticsValidSchema() {
		testDiagnosticsFor(
				"start = element root { text }");
	}

	@Test
	public void testDiagnosticsValidSchemaWithDefines() {
		String value = "start = rootContent" + LS +
				"rootContent = element root {" + LS +
				"  barContent" + LS +
				"}" + LS +
				"barContent = element bar { empty }";
		testDiagnosticsFor(value);
	}

	@Test
	public void testDiagnosticsValidSchemaWithDatatypes() {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				"start = element root { attribute id { xsd:integer } }";
		testDiagnosticsFor(value);
	}

	// -------------------- No false positives on complex RNC --------------------

	@Test
	public void testNoFalsePositiveDefaultNamespaceWithPrefix() {
		testNoParserErrors("default namespace rng = \"http://relaxng.org/ns/structure/1.0\"");
	}

	@Test
	public void testNoFalsePositiveWildcardElementName() {
		testNoParserErrors("start = element * { text }");
	}

	@Test
	public void testNoFalsePositiveWildcardAttributeName() {
		testNoParserErrors("start = element root { attribute * { text } }");
	}

	@Test
	public void testNoFalsePositiveMultiNameAlternatives() {
		String value = "start = element root {" + LS +
				"  element a|b|c { text }" + LS +
				"}";
		testNoParserErrors(value);
	}

	@Test
	public void testNoFalsePositiveMultiNameAlternativesWithKeywords() {
		String value = "start = element root {" + LS +
				"  element group|interleave|choice|optional|zeroOrMore|oneOrMore|list|mixed { text }" + LS +
				"}";
		testNoParserErrors(value);
	}

	@Test
	public void testNoFalsePositiveWildcardExclusion() {
		String value = "namespace ns = \"http://example.com\"" + LS +
				"start = element * - ns:foo { text }";
		testNoParserErrors(value);
	}

	@Test
	public void testNoFalsePositiveWildcardExclusionParenthesized() {
		String value = "namespace rng = \"http://relaxng.org/ns/structure/1.0\"" + LS +
				"namespace local = \"\"" + LS +
				"start = element * - rng:* { text }";
		testNoParserErrors(value);
	}

	@Test
	public void testNoFalsePositiveAttributeWildcardExclusionParenthesized() {
		String value = "namespace rng = \"http://relaxng.org/ns/structure/1.0\"" + LS +
				"namespace local = \"\"" + LS +
				"start = element root {" + LS +
				"  attribute * - (rng:*|local:*) { text }" + LS +
				"}";
		testNoParserErrors(value);
	}

	@Test
	public void testNoFalsePositiveNamespacePrefixedWildcard() {
		String value = "namespace rng = \"http://relaxng.org/ns/structure/1.0\"" + LS +
				"start = element rng:* { text }";
		testNoParserErrors(value);
	}

	@Test
	public void testNoFalsePositiveRelaxNGCompactSyntaxSpec() {
		String value = "default namespace rng = \"http://relaxng.org/ns/structure/1.0\"" + LS +
				"namespace local = \"\"" + LS +
				"datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				LS +
				"start = pattern" + LS +
				LS +
				"pattern =" + LS +
				"  element element { text }" + LS +
				"  | element attribute { text }" + LS +
				"  | element group|interleave|choice|optional" + LS +
				"            |zeroOrMore|oneOrMore|list|mixed { text }" + LS +
				"  | element ref|parentRef { text }" + LS +
				"  | element empty|notAllowed|text { text }" + LS +
				"  | element data { text }" + LS +
				"  | element value { text }" + LS +
				"  | element externalRef { text }" + LS +
				"  | element grammar { text }" + LS +
				LS +
				"nameClass =" + LS +
				"  element name { text }" + LS +
				"  | element anyName { text }" + LS +
				"  | element nsName { text }" + LS +
				"  | element choice { text }" + LS +
				LS +
				"foreignElement = element * - rng:* { text }" + LS +
				"foreignAttribute = attribute * - (rng:*|local:*) { text }" + LS +
				"anyElement = element * { text }" + LS +
				"anyAttribute = attribute * { text }";
		testNoParserErrors(value);
	}

	@Test
	public void testNoFalsePositiveKeywordAsElementName() {
		testNoParserErrors("start = element element { text }");
	}

	@Test
	public void testNoFalsePositiveKeywordAsAttributeName() {
		testNoParserErrors("start = element root { attribute attribute { text } }");
	}

	@Test
	public void testNoFalsePositiveDefaultNamespaceNoPrefix() {
		testNoParserErrors("default namespace = \"http://example.com\"");
	}

	@Test
	public void testNoFalsePositiveMultiAlternativeAttributeNames() {
		String value = "start = element root {" + LS +
				"  attribute ns|datatypeLibrary { text }" + LS +
				"}";
		testNoParserErrors(value);
	}

	@Test
	public void testNoFalsePositiveAnnotations() {
		String value = "namespace doc = \"http://www.example.com/documentation\"" + LS +
				LS +
				"[doc:href=\"address.html#addressBook\"]" + LS +
				"element addressBook {" + LS +
				"  [doc:href=\"address.html#card\"]" + LS +
				"  element card {" + LS +
				"    [doc:href=\"address.html#name\"]" + LS +
				"    element name { text }," + LS +
				"    [doc:href=\"address.html#email\"]" + LS +
				"    element email { text }" + LS +
				"  }*" + LS +
				"}";
		testNoParserErrors(value);
	}

	// -------------------- Tests from error files --------------------

	@Test
	public void testDiagnosticsMissingNameFromFile() {
		String value = "start = element {" + LS
				+ "  text" + LS
				+ "}";
		testDiagnosticsFor(value,
				d(0, 8, 0, 15, "missing-name"));
	}

	@Test
	public void testDiagnosticsMissingOpenBraceFromFile() {
		String value = "start = element root" + LS
				+ "  text" + LS
				+ "}";
		testDiagnosticsFor(value,
				d(0, 16, 0, 20, "missing-open-brace"));
	}

	@Test
	public void testDiagnosticsMissingAssignFromFile() {
		testDiagnosticsFor("root element root { text }",
				d(0, 0, 0, 4, "missing-assign"));
	}

	@Test
	public void testDiagnosticsUnclosedBlockFromFile() {
		String value = "start = element root {" + LS
				+ "  element child {" + LS
				+ "    text" + LS
				+ "}";
		testDiagnosticsFor(value,
				d(0, 16, 0, 20, "unclosed-block"));
	}

	@Test
	public void testDiagnosticsUndefinedPatternFromFile() {
		String value = "start = element root {" + LS
				+ "  content" + LS
				+ "}";
		testDiagnosticsFor(value,
				d(1, 2, 1, 9, "undefined-pattern"));
	}

	@Test
	public void testDiagnosticsDuplicateDefinitionFromFile() {
		String value = "start = root" + LS
				+ LS
				+ "root = element root { text }" + LS
				+ LS
				+ "root = element other { text }";
		testDiagnosticsFor(value,
				d(4, 0, 4, 4, "duplicate-definition"));
	}

	@Test
	public void testDiagnosticsMissingStartElementFromFile() {
		String value = "root = element root { text }" + LS
				+ LS
				+ "child = element child { text }";
		testDiagnosticsFor(value,
				d(0, 0, 0, 4, "missing-start-element"));
	}

	@Test
	public void testDiagnosticsMissingOpenBraceNoFalseUnclosed() {
		String value = "start = element root {" + LS
				+ "  element child text" + LS
				+ "}";
		testDiagnosticsFor(value,
				d(1, 10, 1, 15, "missing-open-brace"));
	}

	// -------------------- Include tests with real files --------------------

	@Test
	public void testDiagnosticsValidIncludeWithDefines() throws IOException {
		testDiagnosticsFor(testFileContent("include-valid.rnc"), testFileUri("include-valid.rnc"));
	}

	@Test
	public void testDiagnosticsIncludeNotOverriding() {
		// include-base.rnc defines: start, paragraph
		// "xparagraph" does not exist in base, error should be on "xparagraph" not on "include"
		String fileUri = testFileUri("include-valid.rnc");
		String value = "include \"include-base.rnc\" {" + LS +
				"  xparagraph = element p { text }" + LS +
				"}";
		testDiagnosticsFor(value, fileUri,
				d(1, 2, 1, 12, "include-not-overriding"));
	}

	@Test
	public void testDiagnosticsIncludeFileNotFound() throws IOException {
		String fileUri = testFileUri("include-file-not-found.rnc");
		String value = testFileContent("include-file-not-found.rnc");
		testDiagnosticsFor(value, fileUri,
				d(0, 9, 0, 27, "file-not-found"));
	}

	// -------------------- Spec-based validation tests --------------------
	// Based on RELAX NG specification examples and restrictions

	// --- Valid schemas (no false positives) ---

	@Test
	public void testValidTopLevelPattern() {
		String value = "element addressBook {" + LS +
				"  text" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testDefineWithoutStartReportsMissingStart() {
		// A define without start = is NOT a top-level pattern form,
		// it should report MissingStartElement
		String value = "addressBook = element addressBook {" + LS +
				"  text" + LS +
				"}";
		testDiagnosticsFor(value,
				d(0, 0, 0, 11, "missing-start-element"));
	}

	@Test
	public void testValidSimpleAddressBook() {
		String value = "start = element addressBook {" + LS +
				"  element card {" + LS +
				"    element name { text }," + LS +
				"    element email { text }" + LS +
				"  }*" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidOptionalAttribute() {
		String value = "start = element foo {" + LS +
				"  attribute bar { text }?," + LS +
				"  text" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidZeroOrMoreElements() {
		String value = "start = element foo {" + LS +
				"  element bar { text }*" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidOneOrMoreElements() {
		String value = "start = element foo {" + LS +
				"  element bar { text }+" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidMixedContent() {
		String value = "start = element foo {" + LS +
				"  mixed {" + LS +
				"    element bold { text }*" + LS +
				"  }" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidInterleave() {
		String value = "start = element foo {" + LS +
				"  element bar { text }" + LS +
				"  & element baz { text }" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidListPattern() {
		String value = "start = element foo {" + LS +
				"  list { token }" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidEmpty() {
		String value = "start = element foo { empty }";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidNotAllowed() {
		String value = "start = element foo {" + LS +
				"  element bar { text }" + LS +
				"  | notAllowed" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidGroupWithComma() {
		String value = "start = element person {" + LS +
				"  element firstName { text }," + LS +
				"  element lastName { text }," + LS +
				"  element email { text }?" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidDatatypeXsdString() {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				LS +
				"start = element order {" + LS +
				"  attribute id { xsd:positiveInteger }," + LS +
				"  element date { xsd:date }," + LS +
				"  element quantity { xsd:nonNegativeInteger }" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidRecursivePattern() {
		String value = "start = element tree {" + LS +
				"  node" + LS +
				"}" + LS +
				LS +
				"node = element node {" + LS +
				"  attribute name { text }," + LS +
				"  node*" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidMultipleNamespaces() {
		String value = "namespace a = \"http://example.com/a\"" + LS +
				"namespace b = \"http://example.com/b\"" + LS +
				LS +
				"start = element a:root {" + LS +
				"  element b:child { text }" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidChoiceOfElements() {
		String value = "start = element container {" + LS +
				"  (element a { text }" + LS +
				"   | element b { text }" + LS +
				"   | element c { text })*" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidAttributeGroup() {
		String value = "start = element foo {" + LS +
				"  attribute x { text }," + LS +
				"  attribute y { text }," + LS +
				"  attribute z { text }?," + LS +
				"  text" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidDivWithMultipleDefines() {
		String value = "start = root" + LS +
				LS +
				"div {" + LS +
				"  root = element root { child+ }" + LS +
				"  child = element child { text }" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidCombineChoice() {
		String value = "start = element doc { inline* }" + LS +
				LS +
				"inline = element bold { text }" + LS +
				LS +
				"inline |= element italic { text }" + LS +
				LS +
				"inline |= element code { text }";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidCombineInterleave() {
		String value = "start = element doc { attrs, text }" + LS +
				LS +
				"attrs = attribute id { text }" + LS +
				LS +
				"attrs &= attribute class { text }";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidExternalRef() {
		String value = "start = element doc {" + LS +
				"  element title { text }," + LS +
				"  element body { text }" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidStartChoice() {
		String value = "start = element report { text }" + LS +
				LS +
				"start |= element memo { text }";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidNestedChoice() {
		String value = "start = element doc {" + LS +
				"  (element p { text }" + LS +
				"   | element pre { text }" + LS +
				"   | element blockquote {" + LS +
				"       element p { text }+" + LS +
				"     })+" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidParenthesizedGroup() {
		String value = "start = element root {" + LS +
				"  (element a { text }, element b { text })+" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidCommentsEverywhere() {
		String value = "# File header" + LS +
				"namespace a = \"http://example.com\"" + LS +
				"# Before start" + LS +
				"## Documentation comment" + LS +
				"start = element root {" + LS +
				"  # Inside element" + LS +
				"  text" + LS +
				"}" + LS +
				"# After definition";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidMultipleAttributes() {
		String value = "start = element img {" + LS +
				"  attribute src { text }," + LS +
				"  attribute alt { text }?," + LS +
				"  attribute width { text }?," + LS +
				"  attribute height { text }?," + LS +
				"  empty" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidInheritNamespace() {
		String value = "default namespace = \"http://www.w3.org/1999/xhtml\"" + LS +
				LS +
				"start = element html {" + LS +
				"  element head {" + LS +
				"    element title { text }" + LS +
				"  }," + LS +
				"  element body {" + LS +
				"    element p { text }*" + LS +
				"  }" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidGrammarBlock() {
		String value = "start = grammar {" + LS +
				"  start = element root { text }" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testValidParentIdentifier() {
		String value = "start = element root {" + LS +
				"  grammar {" + LS +
				"    start = element child {" + LS +
				"      parent root" + LS +
				"    }" + LS +
				"  }" + LS +
				"}";
		testNoParserErrors(value);
	}

	@Test
	public void testValidExternalWithInherit() {
		testNoParserErrors("start = external \"foo.rnc\" inherit = myns");
	}

	@Test
	public void testValidQuotedIdentifier() {
		String value = "start = \\element" + LS +
				LS +
				"\\element = element root { text }";
		testNoParserErrors(value);
	}

	@Test
	public void testValidLiteralConcatenation() {
		testNoParserErrors("default namespace = \"http://example\" ~ \".com\"");
	}

	@Test
	public void testValidDatatypeWithParams() {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				LS +
				"start = element root {" + LS +
				"  xsd:string { pattern = \"[A-Z]+\" }" + LS +
				"}";
		testNoParserErrors(value);
	}

	@Test
	public void testValidStringDatatypeName() {
		testNoParserErrors("start = element root { string }");
	}

	@Test
	public void testValidTokenDatatypeName() {
		testNoParserErrors("start = element root { token }");
	}

	@Test
	public void testValidIncludeWithInherit() {
		testNoParserErrors("include \"base.rnc\" inherit = myns");
	}

	@Test
	public void testValidDivInsideInclude() {
		String value = "include \"base.rnc\" {" + LS +
				"  div {" + LS +
				"    start = element root { text }" + LS +
				"  }" + LS +
				"}";
		testNoParserErrors(value);
	}

	@Test
	public void testValidAssignChoice() {
		String value = "start = root" + LS +
				LS +
				"root = element a { text }" + LS +
				"root |= element b { text }";
		testNoParserErrors(value);
	}

	@Test
	public void testValidAssignInterleave() {
		String value = "start = root" + LS +
				LS +
				"root = element a { text }" + LS +
				"root &= element b { text }";
		testNoParserErrors(value);
	}

	// --- Jing semantic validation errors ---

	@Test
	public void testDiagnosticsMultipleUndefinedPatterns() {
		String value = "start = element root {" + LS +
				"  foo, bar, baz" + LS +
				"}";
		testDiagnosticsFor(value,
				d(1, 2, 1, 5, "undefined-pattern"),
				d(1, 7, 1, 10, "undefined-pattern"),
				d(1, 12, 1, 15, "undefined-pattern"));
	}

	@Test
	public void testDiagnosticsDuplicateDefinitionWithBody() {
		String value = "start = element root { a }" + LS +
				LS +
				"a = element a { text }" + LS +
				LS +
				"a = element b { text }";
		testDiagnosticsFor(value,
				d(4, 0, 4, 1, "duplicate-definition"));
	}

	@Test
	public void testDiagnosticsMissingStartWithMultipleDefines() {
		String value = "header = element header { text }" + LS +
				LS +
				"footer = element footer { text }" + LS +
				LS +
				"content = element content { text }";
		testDiagnosticsFor(value,
				d(0, 0, 0, 6, "missing-start-element"));
	}

	@Test
	public void testDiagnosticsUndefinedPatternInNestedElement() {
		String value = "start = element root {" + LS +
				"  element child {" + LS +
				"    unknownRef" + LS +
				"  }" + LS +
				"}";
		testDiagnosticsFor(value,
				d(2, 4, 2, 14, "undefined-pattern"));
	}

	@Test
	public void testDiagnosticsUndefinedPatternInChoice() {
		String value = "start = element root {" + LS +
				"  element a { text }" + LS +
				"  | missingDef" + LS +
				"}";
		testDiagnosticsFor(value,
				d(2, 4, 2, 14, "undefined-pattern"));
	}

	@Test
	public void testDiagnosticsMissingPatternNestedAttribute() {
		String value = "start = element root {" + LS +
				"  attribute id { }," + LS +
				"  text" + LS +
				"}";
		testDiagnosticsFor(value,
				d(1, 12, 1, 14, "missing-pattern"));
	}

	// --- Multiple parser errors ---

	@Test
	public void testDiagnosticsMultipleMissingNames() {
		String value = "start = element {" + LS +
				"  attribute { text }" + LS +
				"}";
		testDiagnosticsFor(value,
				d(0, 8, 0, 15, "missing-name"),
				d(1, 2, 1, 11, "missing-name"));
	}

	@Test
	public void testDiagnosticsNestedUnclosedBlocks() {
		String value = "start = element root {" + LS +
				"  element child {" + LS +
				"    element grandchild {";
		testDiagnosticsFor(value,
				d(0, 16, 0, 20, "unclosed-block"),
				d(1, 10, 1, 15, "unclosed-block"),
				d(2, 12, 2, 22, "unclosed-block"));
	}

	@Test
	public void testDiagnosticsMissingAssignAndUnclosed() {
		String value = "start element root {";
		testDiagnosticsFor(value,
				d(0, 0, 0, 5, "missing-assign"));
	}

	// --- Edge cases ---

	@Test
	public void testDiagnosticsOnlyComments() {
		String value = "# Just a comment" + LS +
				"## Documentation" + LS +
				"# Another comment";
		testDiagnosticsFor(value,
				d(0, 0, 0, 0, "missing-start-element"));
	}

	@Test
	public void testDiagnosticsOnlyNamespace() {
		testDiagnosticsFor("namespace a = \"http://example.com\"",
				d(0, 0, 0, 0, "missing-start-element"));
	}

	@Test
	public void testDiagnosticsStartWithElement() {
		testDiagnosticsFor("start = element root { text }");
	}

	@Test
	public void testDiagnosticsStartWithEmpty() {
		testDiagnosticsFor("start = element root { empty }");
	}

	@Test
	public void testDiagnosticsStartReferencingDefine() {
		String value = "start = root" + LS +
				LS +
				"root = element root {" + LS +
				"  text" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testDiagnosticsComplexSchema() {
		String value = "default namespace = \"http://example.com\"" + LS +
				"datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				LS +
				"start = element catalog {" + LS +
				"  item+" + LS +
				"}" + LS +
				LS +
				"item = element item {" + LS +
				"  attribute id { xsd:positiveInteger }," + LS +
				"  element name { text }," + LS +
				"  element price { xsd:decimal }," + LS +
				"  element description { text }?" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testDiagnosticsIncludeNoStartNotError() {
		String value = "include \"base.rnc\"" + LS +
				LS +
				"extra = element extra { text }";
		testDiagnosticsFor(value);
	}

	@Test
	public void testDiagnosticsIncludeWithOverrideNoStart() {
		String value = "include \"base.rnc\" {" + LS +
				"  foo = element bar { text }" + LS +
				"}" + LS +
				LS +
				"extra = element extra { text }";
		testDiagnosticsFor(value);
	}

	@Test
	public void testDiagnosticsMultipleIncludes() {
		String value = "include \"base.rnc\"" + LS +
				LS +
				"include \"base.rnc\"" + LS +
				LS +
				"extra = element extra { text }";
		testDiagnosticsFor(value);
	}

	@Test
	public void testDiagnosticsValidPatternWithParens() {
		String value = "start = element root {" + LS +
				"  (text | element a { text } | element b { text })*" + LS +
				"}";
		testDiagnosticsFor(value);
	}

	@Test
	public void testDiagnosticsInterleaveContainsText() {
		String value = "start = element doc {" + LS +
				"  mixed { inline* }" + LS +
				"}" + LS +
				"inline = text | element bold { text }";
		testDiagnosticsFor(value,
				d(1, 2, 1, 7, "interleave-contains-text"));
	}

	@Test
	public void testDiagnosticsValidNestedDefines() {
		String value = "start = element doc { block+ }" + LS +
				LS +
				"block =" + LS +
				"  element p { inline* }" + LS +
				"  | element pre { text }" + LS +
				"  | element ul {" + LS +
				"      element li { inline* }+" + LS +
				"    }" + LS +
				LS +
				"inline =" + LS +
				"  text" + LS +
				"  | element em { text }" + LS +
				"  | element strong { text }" + LS +
				"  | element a {" + LS +
				"      attribute href { text }," + LS +
				"      inline*" + LS +
				"    }";
		testDiagnosticsFor(value);
	}

	// -------------------- Unused Define Diagnostics --------------------

	@Test
	public void testDiagnosticsUnusedDefine() {
		String value = "start = element root { text }" + LS +
				LS +
				"unused = element foo { text }";
		testDiagnosticsFor(value,
				d(2, 0, 2, 6, RNCErrorCode.UnusedDefine, DiagnosticTag.Unnecessary));
	}

	@Test
	public void testDiagnosticsUsedDefineNoWarning() {
		String value = "start = root" + LS +
				LS +
				"root = element root { text }";
		testDiagnosticsFor(value);
	}

	@Test
	public void testDiagnosticsMultipleUnusedDefines() {
		String value = "start = element root { text }" + LS +
				LS +
				"unused1 = element a { text }" + LS +
				LS +
				"unused2 = element b { text }";
		testDiagnosticsFor(value,
				d(2, 0, 2, 7, RNCErrorCode.UnusedDefine, DiagnosticTag.Unnecessary),
				d(4, 0, 4, 7, RNCErrorCode.UnusedDefine, DiagnosticTag.Unnecessary));
	}

	@Test
	public void testDiagnosticsUsedDefineInPattern() {
		String value = "start = element root { content }" + LS +
				LS +
				"content = element p { text }";
		testDiagnosticsFor(value);
	}

	@Test
	public void testDiagnosticsUsedDefineWithCombine() {
		String value = "start = element doc { inline* }" + LS +
				LS +
				"inline = element bold { text }" + LS +
				LS +
				"inline |= element italic { text }";
		testDiagnosticsFor(value);
	}

	@Test
	public void testDiagnosticsMixedUsedAndUnused() {
		String value = "start = root" + LS +
				LS +
				"root = element root { content }" + LS +
				LS +
				"content = element p { text }" + LS +
				LS +
				"unused = element extra { text }";
		testDiagnosticsFor(value,
				d(6, 0, 6, 6, RNCErrorCode.UnusedDefine, DiagnosticTag.Unnecessary));
	}

	@Test
	public void testDiagnosticsNoUnusedWithInclude() {
		String value = "include \"base.rnc\"" + LS +
				LS +
				"extra = element extra { text }";
		testDiagnosticsFor(value, testFileUri("base.rnc"));
	}
}
