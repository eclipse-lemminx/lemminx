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
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.assertFormat;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.settings.RNCFormattingSettings;
import org.junit.jupiter.api.Test;

public class RNCFormattingTest {

	@Test
	public void testFormatSimpleDefinition() throws BadLocationException {
		String unformatted = "start=element root{text}";
		String expected = "start = element root {" + LS +
				"  text" + LS +
				"}";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatMultipleDefinitions() throws BadLocationException {
		String unformatted = "start = element root {" + LS +
				"  text" + LS +
				"}" + LS +
				"inline = element span {" + LS +
				"  text" + LS +
				"}";
		String expected = "start = element root {" + LS +
				"  text" + LS +
				"}" + LS +
				LS +
				"inline = element span {" + LS +
				"  text" + LS +
				"}";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatNamespaceDeclaration() throws BadLocationException {
		String unformatted = "namespace a=\"http://example.com\"";
		String expected = "namespace a = \"http://example.com\"";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatAlreadyFormatted() throws BadLocationException {
		String value = "namespace a = \"http://example.com\"" + LS +
				LS +
				"start = element root {" + LS +
				"  text" + LS +
				"}";
		assertFormat(value, value);
	}

	@Test
	public void testFormatChoiceOperator() throws BadLocationException {
		String unformatted = "start = element root {" + LS +
				"  a|b|c" + LS +
				"}";
		String expected = "start = element root {" + LS +
				"  a | b | c" + LS +
				"}";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatPostfixOperator() throws BadLocationException {
		String unformatted = "start = element root {" + LS +
				"  text *" + LS +
				"}";
		String expected = "start = element root {" + LS +
				"  text*" + LS +
				"}";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatCommaOperator() throws BadLocationException {
		String unformatted = "start = element root {" + LS +
				"  a,b,c" + LS +
				"}";
		String expected = "start = element root {" + LS +
				"  a, b, c" + LS +
				"}";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatBlankLinesBetweenDefinesZero() throws BadLocationException {
		RNCFormattingSettings rncSettings = new RNCFormattingSettings();
		rncSettings.setBlankLinesBetweenDefines(0);
		String unformatted = "start = element root {" + LS +
				"  text" + LS +
				"}" + LS +
				LS +
				"inline = element span {" + LS +
				"  text" + LS +
				"}";
		String expected = "start = element root {" + LS +
				"  text" + LS +
				"}" + LS +
				"inline = element span {" + LS +
				"  text" + LS +
				"}";
		assertFormat(unformatted, expected, rncSettings);
	}

	@Test
	public void testFormatBlankLinesBetweenDefinesTwo() throws BadLocationException {
		RNCFormattingSettings rncSettings = new RNCFormattingSettings();
		rncSettings.setBlankLinesBetweenDefines(2);
		String unformatted = "start = element root {" + LS +
				"  text" + LS +
				"}" + LS +
				"inline = element span {" + LS +
				"  text" + LS +
				"}";
		String expected = "start = element root {" + LS +
				"  text" + LS +
				"}" + LS +
				LS +
				LS +
				"inline = element span {" + LS +
				"  text" + LS +
				"}";
		assertFormat(unformatted, expected, rncSettings);
	}

	@Test
	public void testFormatAssignOperators() throws BadLocationException {
		String unformatted = "start=root" + LS +
				LS +
				"content|=text" + LS +
				LS +
				"content&=text";
		String expected = "start = root" + LS +
				LS +
				"content |= text" + LS +
				LS +
				"content &= text";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatEmptyBlock() throws BadLocationException {
		String unformatted = "start = element root { }";
		assertFormat(unformatted, unformatted);
	}

	@Test
	public void testFormatNestedElements() throws BadLocationException {
		String unformatted = "start=element root{element child{text}}";
		String expected = "start = element root {" + LS +
				"  element child {" + LS +
				"    text" + LS +
				"  }" + LS +
				"}";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatAmpersandOperator() throws BadLocationException {
		String unformatted = "start = element root {" + LS +
				"  a&b" + LS +
				"}";
		String expected = "start = element root {" + LS +
				"  a & b" + LS +
				"}";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatComment() throws BadLocationException {
		String unformatted = "start = element root {" + LS +
				"# a comment" + LS +
				"  text" + LS +
				"}";
		String expected = "start = element root {" + LS +
				"  # a comment" + LS +
				"  text" + LS +
				"}";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatDiv() throws BadLocationException {
		String unformatted = "div{foo=text}";
		String expected = "div {" + LS +
				"  foo = text" + LS +
				"}";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatInclude() throws BadLocationException {
		String unformatted = "start = text" + LS +
				"include \"other.rnc\"";
		String expected = "start = text" + LS +
				LS +
				"include \"other.rnc\"";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatNamespaceAndStart() throws BadLocationException {
		String unformatted = "namespace a=\"http://example.com\"" + LS +
				"start=element root{text}";
		String expected = "namespace a = \"http://example.com\"" + LS +
				LS +
				"start = element root {" + LS +
				"  text" + LS +
				"}";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatDatatypesDecl() throws BadLocationException {
		String unformatted = "datatypes xsd=\"http://www.w3.org/2001/XMLSchema-datatypes\"";
		String expected = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatMixedContent() throws BadLocationException {
		String unformatted = "start = element root {" + LS +
				"  mixed{text}" + LS +
				"}";
		String expected = "start = element root {" + LS +
				"  mixed {" + LS +
				"    text" + LS +
				"  }" + LS +
				"}";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatDefaultBlankLinesBetweenDefines() throws BadLocationException {
		RNCFormattingSettings rncSettings = new RNCFormattingSettings();
		// Default is 1, same as the default behavior
		String value = "start = text" + LS +
				LS +
				"foo = text";
		assertFormat(value, value, rncSettings);
	}

	@Test
	public void testFormatAttributeAndElement() throws BadLocationException {
		String unformatted = "start=element root{attribute id{text},element child{text}}";
		String expected = "start = element root {" + LS +
				"  attribute id {" + LS +
				"    text" + LS +
				"  }, element child {" + LS +
				"    text" + LS +
				"  }" + LS +
				"}";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatDefaultNamespace() throws BadLocationException {
		String unformatted = "default namespace=\"http://example.com\"";
		String expected = "default namespace = \"http://example.com\"";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatDefaultNamespaceWithPrefix() throws BadLocationException {
		String unformatted = "default namespace rng=\"http://relaxng.org/ns/structure/1.0\"";
		String expected = "default namespace rng = \"http://relaxng.org/ns/structure/1.0\"";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatConsecutiveDeclarations() throws BadLocationException {
		String value = "default namespace = \"http://example.com\"" + LS +
				"datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				LS +
				"start = element root {" + LS +
				"  text" + LS +
				"}";
		assertFormat(value, value);
	}

	@Test
	public void testFormatConsecutiveDeclarationsNoBlankLine() throws BadLocationException {
		String unformatted = "default namespace = \"http://example.com\"" + LS +
				LS +
				"namespace foo = \"http://foo.com\"" + LS +
				LS +
				"datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"";
		String expected = "default namespace = \"http://example.com\"" + LS +
				"namespace foo = \"http://foo.com\"" + LS +
				"datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatNameClassAlternatives() throws BadLocationException {
		String value = "start = element a|b|c {" + LS +
				"  text" + LS +
				"}";
		assertFormat(value, value);
	}

	@Test
	public void testFormatNameClassWildcard() throws BadLocationException {
		String value = "start = element * {" + LS +
				"  text" + LS +
				"}";
		assertFormat(value, value);
	}

	@Test
	public void testFormatNameClassWildcardExclusion() throws BadLocationException {
		String value = "namespace rng = \"http://relaxng.org/ns/structure/1.0\"" + LS +
				LS +
				"start = element * - rng:* {" + LS +
				"  text" + LS +
				"}";
		assertFormat(value, value);
	}

	@Test
	public void testFormatKeywordAsElementName() throws BadLocationException {
		String value = "start = element div {" + LS +
				"  text" + LS +
				"}";
		assertFormat(value, value);
	}

	@Test
	public void testFormatMultiplePostfixOperators() throws BadLocationException {
		String unformatted = "start = element root {" + LS +
				"  a ?, b *, c +" + LS +
				"}";
		String expected = "start = element root {" + LS +
				"  a?, b*, c+" + LS +
				"}";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatSingleLineBlock() throws BadLocationException {
		String value = "start = element root { text }";
		assertFormat(value, value);
	}

	@Test
	public void testFormatSingleLineBlockWithComma() throws BadLocationException {
		String value = "start = element root { attribute id { text }, text }";
		assertFormat(value, value);
	}

	@Test
	public void testFormatMultiLineAssign() throws BadLocationException {
		String value = "pattern =" + LS +
				"  element foo { text }" + LS +
				"  | element bar { text }";
		assertFormat(value, value);
	}

	@Test
	public void testFormatMultiLineComma() throws BadLocationException {
		String value = "commonAttributes =" + LS +
				"  attribute ns { text }?," + LS +
				"  attribute datatypeLibrary { text }?";
		assertFormat(value, value);
	}

	@Test
	public void testFormatBlankLinesBetweenDefinesPreserve() throws BadLocationException {
		RNCFormattingSettings rncSettings = new RNCFormattingSettings();
		rncSettings.setBlankLinesBetweenDefines(-1);
		String value = "start = text" + LS +
				LS +
				"foo = text" + LS +
				"bar = text";
		assertFormat(value, value, rncSettings);
	}

	@Test
	public void testFormatChoiceAlternativeMultiLine() throws BadLocationException {
		String value = "element response {" + LS +
				"  element result { text }" + LS +
				"  | element error {" + LS +
				"      attribute code { text }," + LS +
				"      text" + LS +
				"    }" + LS +
				"}";
		assertFormat(value, value);
	}

	@Test
	public void testFormatChoiceAlternativeMultiLineUnformatted() throws BadLocationException {
		String unformatted = "element response {" + LS +
				"  element result { text }" + LS +
				"  | element error {" + LS +
				"    attribute code { text }," + LS +
				"    text" + LS +
				"  }" + LS +
				"}";
		String expected = "element response {" + LS +
				"  element result { text }" + LS +
				"  | element error {" + LS +
				"      attribute code { text }," + LS +
				"      text" + LS +
				"    }" + LS +
				"}";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatTopLevelChoiceAlternativeMultiLine() throws BadLocationException {
		String value = "start = inline" + LS +
				LS +
				"inline =" + LS +
				"  text" + LS +
				"  | element span { inline* }" + LS +
				"  | element a {" + LS +
				"      attribute href { text }," + LS +
				"      inline*" + LS +
				"    }";
		assertFormat(value, value);
	}

	@Test
	public void testFormatTopLevelChoiceAlternativeMultiLineUnformatted() throws BadLocationException {
		String unformatted = "start = inline" + LS +
				LS +
				"inline =" + LS +
				"  text" + LS +
				"  | element span { inline* }" + LS +
				"  | element a {" + LS +
				"    attribute href { text }," + LS +
				"    inline*" + LS +
				"  }";
		String expected = "start = inline" + LS +
				LS +
				"inline =" + LS +
				"  text" + LS +
				"  | element span { inline* }" + LS +
				"  | element a {" + LS +
				"      attribute href { text }," + LS +
				"      inline*" + LS +
				"    }";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatCommentBeforeFirstDefinition() throws BadLocationException {
		String value = "# A comment at the top" + LS +
				"start = element root { text }";
		assertFormat(value, value);
	}

	@Test
	public void testFormatCommentBeforeFirstDefinitionNoBlankLine() throws BadLocationException {
		String unformatted = "# A comment at the top" + LS +
				LS +
				"start = element root { text }";
		String expected = "# A comment at the top" + LS +
				"start = element root { text }";
		assertFormat(unformatted, expected);
	}

	@Test
	public void testFormatRelaxNGXMLSyntaxSpecIdempotent() throws BadLocationException {
		RNCFormattingSettings rncSettings = new RNCFormattingSettings();
		rncSettings.setBlankLinesBetweenDefines(-1);
		String value = "# RELAX NG XML syntax specified in compact syntax." + LS +
				LS +
				"default namespace rng = \"http://relaxng.org/ns/structure/1.0\"" + LS +
				"namespace local = \"\"" + LS +
				"datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				LS +
				"start = pattern" + LS +
				LS +
				"pattern =" + LS +
				"  element element { (nameQName | nameClass), (common & pattern+) }" + LS +
				"  | element attribute { (nameQName | nameClass), (common & pattern?) }" + LS +
				"  | element group|interleave|choice|optional" + LS +
				"            |zeroOrMore|oneOrMore|list|mixed { common & pattern+ }" + LS +
				"  | element ref|parentRef { nameNCName, common }" + LS +
				"  | element empty|notAllowed|text { common }" + LS +
				"  | element data { type, param*, (common & exceptPattern?) }" + LS +
				"  | element value { commonAttributes, type?, xsd:string }" + LS +
				"  | element externalRef { href, common }" + LS +
				"  | element grammar { common & grammarContent* }" + LS +
				LS +
				"param = element param { commonAttributes, nameNCName, xsd:string }" + LS +
				LS +
				"exceptPattern = element except { common & pattern+ }" + LS +
				LS +
				"grammarContent =" + LS +
				"  definition" + LS +
				"  | element div { common & grammarContent* }" + LS +
				"  | element include { href, (common & includeContent*) }" + LS +
				LS +
				"includeContent =" + LS +
				"  definition" + LS +
				"  | element div { common & includeContent* }" + LS +
				LS +
				"definition =" + LS +
				"  element start { combine?, (common & pattern+) }" + LS +
				"  | element define { nameNCName, combine?, (common & pattern+) }" + LS +
				LS +
				"combine = attribute combine { \"choice\" | \"interleave\" }" + LS +
				LS +
				"nameClass =" + LS +
				"  element name { commonAttributes, xsd:QName }" + LS +
				"  | element anyName { common & exceptNameClass? }" + LS +
				"  | element nsName { common & exceptNameClass? }" + LS +
				"  | element choice { common & nameClass+ }" + LS +
				LS +
				"exceptNameClass = element except { common & nameClass+ }" + LS +
				LS +
				"nameQName = attribute name { xsd:QName }" + LS +
				"nameNCName = attribute name { xsd:NCName }" + LS +
				"href = attribute href { xsd:anyURI }" + LS +
				"type = attribute type { xsd:NCName }" + LS +
				LS +
				"common = commonAttributes, foreignElement*" + LS +
				LS +
				"commonAttributes =" + LS +
				"  attribute ns { xsd:string }?," + LS +
				"  attribute datatypeLibrary { xsd:anyURI }?," + LS +
				"  foreignAttribute*" + LS +
				LS +
				"foreignElement = element * - rng:* { (anyAttribute | text | anyElement)* }" + LS +
				"foreignAttribute = attribute * - (rng:*|local:*) { text }" + LS +
				"anyElement = element * { (anyAttribute | text | anyElement)* }" + LS +
				"anyAttribute = attribute * { text }";
		assertFormat(value, value, rncSettings);
	}
}
