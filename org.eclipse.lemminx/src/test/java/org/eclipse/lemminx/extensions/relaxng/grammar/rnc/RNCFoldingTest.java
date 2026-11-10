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
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.fr;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testFoldingFor;

import org.eclipse.lsp4j.FoldingRangeKind;
import org.junit.jupiter.api.Test;

public class RNCFoldingTest {

	@Test
	public void testFoldingBraces() {
		String value = "element root {" + LS +
				"  attribute id { text }," + LS +
				"  text" + LS +
				"}";
		testFoldingFor(value,
				fr(0, 3, FoldingRangeKind.Region));
	}

	@Test
	public void testFoldingNestedBraces() {
		String value = "element root {" + LS +
				"  element child {" + LS +
				"    text" + LS +
				"  }" + LS +
				"}";
		testFoldingFor(value,
				fr(0, 4, FoldingRangeKind.Region),
				fr(1, 3, FoldingRangeKind.Region));
	}

	@Test
	public void testFoldingNoSingleLineBraces() {
		testFoldingFor("attribute id { text }");
	}

	@Test
	public void testFoldingComments() {
		String value = "# Line 1" + LS +
				"# Line 2" + LS +
				"# Line 3" + LS +
				"start = root";
		testFoldingFor(value,
				fr(0, 2, FoldingRangeKind.Comment));
	}

	@Test
	public void testFoldingDocComments() {
		String value = "## Doc line 1" + LS +
				"## Doc line 2" + LS +
				"start = root";
		testFoldingFor(value,
				fr(0, 1, FoldingRangeKind.Comment));
	}

	@Test
	public void testFoldingSingleLineComment() {
		String value = "# Just one line" + LS +
				"start = root";
		testFoldingFor(value);
	}

	@Test
	public void testFoldingEmptyDocument() {
		testFoldingFor("");
	}

	@Test
	public void testFoldingDiv() {
		String value = "div {" + LS +
				"  a = text" + LS +
				"  b = text" + LS +
				"}";
		testFoldingFor(value,
				fr(0, 3, FoldingRangeKind.Region));
	}

	@Test
	public void testFoldingIncludeWithBody() {
		String value = "include \"other.rnc\" {" + LS +
				"  start = foo" + LS +
				"}";
		testFoldingFor(value,
				fr(0, 2, FoldingRangeKind.Region));
	}

	@Test
	public void testFoldingDeclarations() {
		String value = "default namespace = \"http://example.com\"" + LS +
				"namespace foo = \"http://foo.com\"" + LS +
				"datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				LS +
				"start = element root {" + LS +
				"  text" + LS +
				"}";
		testFoldingFor(value,
				fr(0, 2, FoldingRangeKind.Region),
				fr(4, 6, FoldingRangeKind.Region));
	}

	@Test
	public void testFoldingSingleDeclaration() {
		String value = "namespace foo = \"http://example.com\"" + LS +
				LS +
				"start = element root {" + LS +
				"  text" + LS +
				"}";
		testFoldingFor(value,
				fr(2, 4, FoldingRangeKind.Region));
	}

	@Test
	public void testFoldingMultiLineDefine() {
		String value = "pattern =" + LS +
				"  element element { text }" + LS +
				"  | element attribute { text }";
		testFoldingFor(value,
				fr(0, 2, FoldingRangeKind.Region));
	}

	@Test
	public void testFoldingRelaxNGXMLSyntaxSpec() {
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
		testFoldingFor(value,
				fr(2, 4, FoldingRangeKind.Region),   // declarations group
				fr(8, 18, FoldingRangeKind.Region),   // pattern
				fr(24, 27, FoldingRangeKind.Region),  // grammarContent
				fr(29, 31, FoldingRangeKind.Region),  // includeContent
				fr(33, 35, FoldingRangeKind.Region),  // definition
				fr(39, 43, FoldingRangeKind.Region),  // nameClass
				fr(54, 57, FoldingRangeKind.Region)); // commonAttributes
	}

	@Test
	public void testFoldingMultipleSections() {
		String value = "# Header" + LS +
				"# comment" + LS +
				LS +
				"element root {" + LS +
				"  text" + LS +
				"}" + LS +
				LS +
				"element child {" + LS +
				"  text" + LS +
				"}";
		testFoldingFor(value,
				fr(0, 1, FoldingRangeKind.Comment),
				fr(3, 5, FoldingRangeKind.Region),
				fr(7, 9, FoldingRangeKind.Region));
	}
}
