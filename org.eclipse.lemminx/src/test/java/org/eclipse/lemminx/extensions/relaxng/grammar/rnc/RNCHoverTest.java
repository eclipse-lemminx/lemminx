/*******************************************************************************
* Copyright (c) 2024 Red Hat Inc. and others.
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
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.assertHover;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testFileUri;
import static org.eclipse.lemminx.XMLAssert.r;

import org.eclipse.lemminx.commons.BadLocationException;
import org.junit.jupiter.api.Test;

public class RNCHoverTest {

	@Test
	public void testHoverOnDefineRef() throws BadLocationException {
		String value = "root = element root { con|tent }" + LS +
				"content = text";
		assertHover(value,
				"content = text", r(0, 22, 0, 29));
	}

	@Test
	public void testHoverOnDefineRefShowsMarkdown() throws BadLocationException {
		String value = "start = ro|ot" + LS +
				"root = element root { text }";
		assertHover(value,
				"```rnc", r(0, 8, 0, 12));
	}

	@Test
	public void testHoverOnKeywordElement() throws BadLocationException {
		assertHover("ele|ment root { text }", //
				"element nameClass { pattern }\n" + //
						"\n" + //
						"Defines an element pattern that matches an XML element. The nameClass specifies which element names match, and the pattern describes the allowed content.\n"
						+ //
						"\n" + //
						"See https://relaxng.org/compact-tutorial-20030326.html#id2814100",
				r(0, 0, 0, 7));
	}

	@Test
	public void testHoverOnKeywordAttribute() throws BadLocationException {
		assertHover("attr|ibute id { text }",
				"Defines an attribute pattern", r(0, 0, 0, 9));
	}

	@Test
	public void testHoverOnKeywordEmpty() throws BadLocationException {
		assertHover("em|pty",
				"empty", r(0, 0, 0, 5));
	}

	@Test
	public void testHoverOnKeywordNotAllowed() throws BadLocationException {
		assertHover("not|Allowed", "notAllowed\n" + //
				"\n" + //
				"A pattern that never matches. Used for extensibility via combine operators.\n" + //
				"\n" + //
				"See https://relaxng.org/compact-tutorial-20030326.html#id2819693", r(0, 0, 0, 10));
	}

	@Test
	public void testHoverOnKeywordMixed() throws BadLocationException {
		assertHover("mi|xed { text }",
				"mixed", r(0, 0, 0, 5));
	}

	@Test
	public void testHoverOnKeywordList() throws BadLocationException {
		assertHover("li|st { text }",
				"list", r(0, 0, 0, 4));
	}

	@Test
	public void testHoverOnKeywordStart() throws BadLocationException {
		assertHover("sta|rt = root",
				"start", r(0, 0, 0, 5));
	}

	@Test
	public void testHoverOnKeywordInclude() throws BadLocationException {
		assertHover("inc|lude \"foo.rnc\"", "include anyURILiteral (reference)\n" + //
				"\n" + //
				"Merges definitions from another RNC grammar file into the current grammar. The included defines become available as references.\n"
				+ //
				"\n" + //
				"See https://relaxng.org/compact-tutorial-20030326.html#id2820475", r(0, 0, 0, 7));
	}

	@Test
	public void testHoverOnKeywordNamespace() throws BadLocationException {
		assertHover("name|space foo = \"uri\"", //
				"**namespace** *prefix* `=` *anyURILiteral* — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2819036)\n"
						+ //
						"\n" + //
						"Declares a namespace prefix for use in element and attribute names. The prefix can then be used with a colon to qualify names: `prefix:localName`.\n"
						+ //
						"\n" + //
						"**Examples:**\n" + //
						"```rnc\n" + //
						"namespace a = \"http://relaxng.org/ns/compatibility/annotations/1.0\"\n" + //
						"namespace html = \"http://www.w3.org/1999/xhtml\"\n" + //
						"start = element html:html { text }\n" + //
						"```",
				r(0, 0, 0, 9));
	}

	@Test
	public void testHoverOnKeywordDatatypes() throws BadLocationException {
		assertHover("data|types xsd = \"uri\"",
				"datatypes", r(0, 0, 0, 9));
	}

	@Test
	public void testHoverOnKeywordExternal() throws BadLocationException {
		assertHover("ext|ernal \"foo.rnc\"",
				"external", r(0, 0, 0, 8));
	}

	@Test
	public void testHoverOnKeywordGrammar() throws BadLocationException {
		assertHover("gra|mmar { }",
				"grammar", r(0, 0, 0, 7));
	}

	@Test
	public void testHoverOnKeywordText() throws BadLocationException {
		assertHover("te|xt",
				"text", r(0, 0, 0, 4));
	}

	@Test
	public void testHoverOnKeywordDiv() throws BadLocationException {
		assertHover("di|v { }", //
				"div { grammarContent }\n" + //
						"\n" + //
						"Groups grammar definitions for organizational purposes. Has no semantic effect.\n" + //
						"\n" + //
						"See https://relaxng.org/compact-tutorial-20030326.html#id2820694",
				r(0, 0, 0, 3));
	}

	@Test
	public void testHoverOnKeywordParent() throws BadLocationException {
		assertHover("par|ent foo",
				"parent", r(0, 0, 0, 6));
	}

	@Test
	public void testHoverOnKeywordInherit() throws BadLocationException {
		assertHover("inh|erit", "inherit\n" + //
				"\n" + //
				"Inherits the default namespace from the including grammar.\n" + //
				"\n" + //
				"See https://relaxng.org/compact-tutorial-20030326.html#id2819036", r(0, 0, 0, 7));
	}

	@Test
	public void testHoverOnKeywordString() throws BadLocationException {
		assertHover("str|ing",
				"**string** [ *params* ] — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2817679)\n" + //
						"\n" + //
						"Built-in datatype that matches a string value with whitespace preserved. This is the default datatype when no library is specified.\n"
						+ //
						"\n" + //
						"**Examples:**\n" + //
						"```rnc\n" + //
						"attribute type { string \"fixed\" }\n" + //
						"element value { string }\n" + //
						"```",
				r(0, 0, 0, 6));
	}

	@Test
	public void testHoverOnKeywordToken() throws BadLocationException {
		assertHover("to|ken",
				"token", r(0, 0, 0, 5));
	}

	// --- Include hover with definitions from included file ---

	@Test
	public void testHoverOnIncludeShowsDefinitions() throws BadLocationException {
		// include-base.rnc has: start = element doc { paragraph+ } and paragraph = element p { text }
		// include-valid.rnc overrides paragraph
		String fileUri = testFileUri("include-valid.rnc");
		String value = "inc|lude \"include-base.rnc\" {" + LS +
				"  paragraph = element p {" + LS +
				"    mixed { inline* }" + LS +
				"  }" + LS +
				"}";
		assertHover(value, fileUri, "start", r(0, 0, 0, 7));
	}

	@Test
	public void testHoverOnIncludeShowsOverridden() throws BadLocationException {
		String fileUri = testFileUri("include-valid.rnc");
		String value = "inc|lude \"include-base.rnc\" {" + LS +
				"  paragraph = element p {" + LS +
				"    mixed { inline* }" + LS +
				"  }" + LS +
				"}";
		assertHover(value, fileUri, "overridden", r(0, 0, 0, 7));
	}

	@Test
	public void testHoverOnIncludeNoFileFallsBackToKeyword() throws BadLocationException {
		assertHover("inc|lude \"nonexistent.rnc\"",
				"include", r(0, 0, 0, 7));
	}

	@Test
	public void testHoverOnDefineRefInDiv() throws BadLocationException {
		String value = "start = meta|data" + LS + //
				"div {" + LS + //
				"  metadata = element metadata {" + LS + //
				"    element title { text }," + LS + //
				"    element author { text }" + LS + //
				"  }" + LS + //
				"}";
		assertHover(value, "metadata = element metadata {\r\n" + //
				"    element title { text },\r\n" + //
				"    element author { text }\r\n" + //
				"  }\r\n" + //
				"}", r(0, 8, 0, 16));
	}

	@Test
	public void testHoverOnDefineInDiv() throws BadLocationException {
		String value = "start = metadata" + LS +
				"div {" + LS +
				"  meta|data = element metadata {" + LS +
				"    element title { text }," + LS +
				"    element author { text }" + LS +
				"  }" + LS +
				"}";
		assertHover(value,
				"metadata = element metadata", r(2, 2, 2, 10));
	}

	@Test
	public void testHoverOnOverrideShowsOverrideLabel() throws BadLocationException {
		String fileUri = testFileUri("include-valid.rnc");
		String value = "include \"include-base.rnc\" {" + LS +
				"  para|graph = element p {" + LS +
				"    mixed { inline* }" + LS +
				"  }" + LS +
				"}";
		assertHover(value, fileUri, "**Override:**", r(1, 2, 1, 11));
	}

	@Test
	public void testHoverOnOverrideShowsOriginal() throws BadLocationException {
		String fileUri = testFileUri("include-valid.rnc");
		String value = "include \"include-base.rnc\" {" + LS +
				"  para|graph = element p {" + LS +
				"    mixed { inline* }" + LS +
				"  }" + LS +
				"}";
		assertHover(value, fileUri, "**Original:**", r(1, 2, 1, 11));
	}

	@Test
	public void testHoverOnOverrideShowsOriginalContent() throws BadLocationException {
		String fileUri = testFileUri("include-valid.rnc");
		String value = "include \"include-base.rnc\" {" + LS + //
				"  para|graph = element p {" + LS + //
				"    mixed { inline* }" + LS + //
				"  }" + LS + //
				"}";
		assertHover(value, fileUri, "**Override:**\n" + //
				"```rnc\n" + //
				"paragraph = element p {\r\n" + //
				"    mixed { inline* }\r\n" + //
				"  }\n" + //
				"```\n" + //
				"\n" + //
				"**Original:**\n" + //
				"```rnc\n" + //
				"paragraph = element p { text }\n" + //
				"```", r(1, 2, 1, 11));
	}

	@Test
	public void testHoverOnOverrideShowsOverrideContent() throws BadLocationException {
		String fileUri = testFileUri("include-valid.rnc");
		String value = "include \"include-base.rnc\" {" + LS +
				"  para|graph = element p {" + LS +
				"    mixed { inline* }" + LS +
				"  }" + LS +
				"}";
		assertHover(value, fileUri, "paragraph = element p {", r(1, 2, 1, 11));
	}

	@Test
	public void testHoverOnNonOverrideDefineNoOverrideLabel() throws BadLocationException {
		String value = "start = con|tent" + LS +
				"content = element content { text }";
		assertHover(value, "content = element content { text }", r(0, 8, 0, 15));
	}

	// --- Datatype hover ---

	@Test
	public void testHoverOnXsdDatatype() throws BadLocationException {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS + //
				"start = element root {" + LS + //
				"  attribute id { xsd:int|eger }" + LS + //
				"}";
		assertHover(value, "integer\n" + //
				"\n" + //
				"Arbitrary-precision integer.\n" + //
				"\n" + //
				"See https://www.w3.org/TR/xmlschema11-2/#integer", r(2, 17, 2, 28));
	}

	@Test
	public void testHoverOnXsdDatatypeShowsLink() throws BadLocationException {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				"start = element root {" + LS +
				"  attribute id { xsd:str|ing }" + LS +
				"}";
		assertHover(value, "XSD Spec", r(2, 17, 2, 27));
	}

	@Test
	public void testHoverOnDatatypeNoDecl() throws BadLocationException {
		// No datatypes declaration → no hover
		String value = "start = element root {" + LS +
				"  attribute id { xsd:int|eger }" + LS +
				"}";
		assertHover(value);
	}

	@Test
	public void testHoverOnCompatDatatype() throws BadLocationException {
		String value = "datatypes compat = \"http://relaxng.org/ns/compatibility/datatypes/1.0\"" + LS +
				"start = element root {" + LS +
				"  attribute id { compat:I|D }" + LS +
				"}";
		assertHover(value, "**ID**", r(2, 17, 2, 26));
	}

	@Test
	public void testHoverOnCompatDatatypeIdref() throws BadLocationException {
		String value = "datatypes compat = \"http://relaxng.org/ns/compatibility/datatypes/1.0\"" + LS +
				"start = element root {" + LS +
				"  attribute ref { compat:IDR|EF }" + LS +
				"}";
		assertHover(value, "**IDREF**", r(2, 18, 2, 30));
	}

	@Test
	public void testHoverOnXsdDatatypeBoolean() throws BadLocationException {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS + //
				"start = element root {" + LS + //
				"  attribute flag { xsd:bool|ean }" + LS + //
				"}";
		assertHover(value, "boolean\n" + //
				"\n" + //
				"Binary-valued logic: true, false, 1, 0.\n" + //
				"\n" + //
				"See https://www.w3.org/TR/xmlschema11-2/#boolean", r(2, 19, 2, 30));
	}

	@Test
	public void testHoverOnWhitespace() throws BadLocationException {
		assertHover("|   start = root");
	}

	@Test
	public void testHoverOnUndefinedRef() throws BadLocationException {
		assertHover("start = unkno|wn");
	}
}
