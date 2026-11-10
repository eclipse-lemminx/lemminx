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
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.assertHover;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testFileUri;
import static org.eclipse.lemminx.XMLAssert.r;

import org.eclipse.lemminx.commons.BadLocationException;
import org.junit.jupiter.api.Test;

public class RNCHoverTest {

	@Test
	public void testHoverOnDefineRef() throws BadLocationException {
		String value = "root = element root { con|tent }" + LS + "content = text";
		assertHover(value, "```rnc\n" + //
				"content = text\n" + //
				"```", r(0, 22, 0, 29));
	}

	@Test
	public void testHoverOnDefineRefShowsMarkdown() throws BadLocationException {
		String value = "start = ro|ot" + LS + "root = element root { text }";
		assertHover(value, "```rnc\n" + //
				"root = element root { text }\n" + //
				"```", r(0, 8, 0, 12));
	}

	@Test
	public void testHoverOnKeywordElement() throws BadLocationException {
		assertHover("ele|ment root { text }", //
				"**element** *nameClass* `{` *pattern* `}` — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2814100)\n"
						+ //
						"\n" + //
						"Defines an element pattern that matches an XML element. The *nameClass* specifies which element names match, and the *pattern* describes the allowed content.\n"
						+ //
						"\n" + //
						"A *nameClass* can be a simple name (`element root { ... }`), a choice (`element (a | b) { ... }`), a wildcard (`element * { ... }`), or a wildcard with exclusions (`element * - local:* { ... }`).\n"
						+ //
						"\n" + //
						"**Examples:**\n" + //
						"```rnc\n" + //
						"element root { text }\n" + //
						"element name { xsd:string }\n" + //
						"element (bold | italic) { text }\n" + //
						"```",
				r(0, 0, 0, 7));
	}

	@Test
	public void testHoverOnKeywordAttribute() throws BadLocationException {
		assertHover("attr|ibute id { text }", //
				"**attribute** *nameClass* `{` *pattern* `}` — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2814218)\n"
						+ //
						"\n" + //
						"Defines an attribute pattern that matches an XML attribute. The *nameClass* specifies which attribute names match, and the *pattern* describes the allowed value.\n"
						+ //
						"\n" + //
						"By default, attributes are required. Use `?` to make them optional: `attribute id { text }?`.\n"
						+ //
						"\n" + //
						"**Examples:**\n" + //
						"```rnc\n" + //
						"attribute href { xsd:anyURI }\n" + //
						"attribute type { \"simple\" | \"complex\" }\n" + //
						"attribute id { text }?\n" + //
						"```",
				r(0, 0, 0, 9));
	}

	@Test
	public void testHoverOnKeywordEmpty() throws BadLocationException {
		assertHover("em|pty", "**empty** — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2819693)\n" + //
				"\n" + //
				"Matches empty content: no text, no child elements. An element with the `empty` pattern may only contain attributes (if specified separately).\n"
				+ //
				"\n" + //
				"**Examples:**\n" + //
				"```rnc\n" + //
				"element br { empty }\n" + //
				"element img { attribute src { xsd:anyURI }, empty }\n" + //
				"```", r(0, 0, 0, 5));
	}

	@Test
	public void testHoverOnKeywordNotAllowed() throws BadLocationException {
		assertHover("not|Allowed", //
				"**notAllowed** — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2819693)\n" + //
						"\n" + //
						"A pattern that never matches. Used as the identity element for choice (`|`) — combining anything with `notAllowed` via choice yields the other pattern unchanged.\n"
						+ //
						"\n" + //
						"Commonly used for extensibility: a define starts as `notAllowed` and is extended via `|=` in including grammars.\n"
						+ //
						"\n" + //
						"**Examples:**\n" + //
						"```rnc\n" + //
						"extra.content = notAllowed\n" + //
						"extra.content |= element note { text }\n" + //
						"```",
				r(0, 0, 0, 10));
	}

	@Test
	public void testHoverOnKeywordMixed() throws BadLocationException {
		assertHover("mi|xed { text }",
				"**mixed** `{` *pattern* `}` — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2816748)\n" + //
						"\n" + //
						"Allows mixed content: text interspersed with child elements matching the inner *pattern*. Equivalent to `(text | pattern)*`.\n"
						+ //
						"\n" + //
						"**Examples:**\n" + //
						"```rnc\n" + //
						"element p {\n" + //
						"  mixed {\n" + //
						"    element b { text }*\n" + //
						"    & element i { text }*\n" + //
						"  }\n" + //
						"}\n" + //
						"```",
				r(0, 0, 0, 5));
	}

	@Test
	public void testHoverOnKeywordList() throws BadLocationException {
		assertHover("li|st { text }",
				"**list** `{` *pattern* `}` — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2816748)\n" + //
						"\n" + //
						"Matches a whitespace-separated list of tokens. The text content is split on whitespace, and each token must match the inner *pattern*.\n"
						+ //
						"\n" + //
						"**Examples:**\n" + //
						"```rnc\n" + //
						"attribute points { list { xsd:double+ } }\n" + //
						"element classes { list { xsd:token+ } }\n" + //
						"```",
				r(0, 0, 0, 4));
	}

	@Test
	public void testHoverOnKeywordStart() throws BadLocationException {
		assertHover("sta|rt = root",
				"**start** `=` *pattern* — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2814100)\n" + //
						"\n" + //
						"Defines the start pattern of the grammar — the pattern that the root element must match.\n" + //
						"\n" + //
						"Every grammar must have exactly one `start` declaration. Multiple `start` declarations can be combined using `|=` (choice) or `&=` (interleave).\n"
						+ //
						"\n" + //
						"**Examples:**\n" + //
						"```rnc\n" + //
						"start = element root { text }\n" + //
						"start = myDefine\n" + //
						"start |= element alternate { text }\n" + //
						"```",
				r(0, 0, 0, 5));
	}

	@Test
	public void testHoverOnKeywordInclude() throws BadLocationException {
		assertHover("inc|lude \"foo.rnc\"", //
				"**include** *anyURILiteral* (reference) — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2820475)\n"
						+ //
						"\n" + //
						"Merges definitions from another RNC grammar file into the current grammar. The included defines become available as references.\n"
						+ //
						"\n" + //
						"**Example:**\n" + //
						"```rnc\n" + //
						"include \"inline.rnc\"\n" + //
						"\n" + //
						"start = element doc {\n" + //
						"  paragraph+\n" + //
						"}\n" + "```",
				r(0, 0, 0, 7));
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
				"**datatypes** *prefix* `=` *anyURILiteral* — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2817679)\n"
						+ //
						"\n" + //
						"Declares a datatypes library prefix. The most common datatype library is XML Schema datatypes (`xsd`), which provides types like `xsd:string`, `xsd:integer`, `xsd:anyURI`, etc.\n"
						+ //
						"\n" + //
						"**Examples:**\n" + //
						"```rnc\n" + //
						"datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"\n" + //
						"attribute age { xsd:positiveInteger }\n" + //
						"```",
				r(0, 0, 0, 9));
	}

	@Test
	public void testHoverOnKeywordExternal() throws BadLocationException {
		assertHover("ext|ernal \"foo.rnc\"",
				"**external** *anyURILiteral* [ *inherit* ] — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2820475)\n"
						+ //
						"\n" + //
						"References an external RELAX NG grammar and uses its start pattern as a sub-pattern. Unlike `include`, which merges definitions, `external` simply uses the other grammar's content.\n"
						+ //
						"\n" + //
						"**Examples:**\n" + //
						"```rnc\n" + //
						"start = external \"other.rnc\"\n" + //
						"foo = element foo { external \"bar-content.rnc\" }\n" + //
						"```",
				r(0, 0, 0, 8));
	}

	@Test
	public void testHoverOnKeywordGrammar() throws BadLocationException {
		assertHover("gra|mmar { }",
				"**grammar** `{` *grammarContent* `}` — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2820694)\n"
						+ //
						"\n" + //
						"Defines an explicit grammar block containing `start` and named definitions. Every RNC file is implicitly a grammar, so `grammar { ... }` is mainly used for nested grammars.\n"
						+ //
						"\n" + //
						"A nested grammar creates a new scope for definitions and has its own `start` pattern. Use `parent` to reference the enclosing grammar's definitions.\n"
						+ //
						"\n" + //
						"**Examples:**\n" + //
						"```rnc\n" + //
						"element root {\n" + //
						"  grammar {\n" + //
						"    start = element child { parent content }\n" + //
						"  }\n" + //
						"}\n" + //
						"```",
				r(0, 0, 0, 7));
	}

	@Test
	public void testHoverOnKeywordText() throws BadLocationException {
		assertHover("te|xt", "**text** — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2814100)\n" + //
				"\n" + //
				"Matches any text content, including the empty string. When used inside an element pattern, it allows the element to contain arbitrary character data.\n"
				+ //
				"\n" + //
				"The `text` pattern is idempotent: `text, text` is equivalent to `text`.\n" + //
				"\n" + //
				"**Examples:**\n" + //
				"```rnc\n" + //
				"element name { text }\n" + //
				"element description { text }\n" + //
				"```", r(0, 0, 0, 4));
	}

	@Test
	public void testHoverOnKeywordDiv() throws BadLocationException {
		assertHover("di|v { }", //
				"**div** `{` *grammarContent* `}` — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2820694)\n"
						+ //
						"\n" + //
						"Groups grammar definitions for organizational purposes. Has no semantic effect — `div` blocks are transparent and their contents are treated as if written directly at the enclosing level.\n"
						+ //
						"\n" + //
						"**Examples:**\n" + //
						"```rnc\n" + //
						"div {\n" + //
						"  heading = element h1 { text }\n" + //
						"  paragraph = element p { text }\n" + //
						"}\n" + //
						"```",
				r(0, 0, 0, 3));
	}

	@Test
	public void testHoverOnKeywordParent() throws BadLocationException {
		assertHover("par|ent foo",
				"**parent** *name* — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2820694)\n" + //
						"\n" + //
						"References a named definition in the parent grammar when used inside a nested `grammar { ... }` block.\n"
						+ //
						"\n" + //
						"**Examples:**\n" + //
						"```rnc\n" + //
						"common = text\n" + //
						"start = element root {\n" + //
						"  grammar {\n" + //
						"    start = element child { parent common }\n" + //
						"  }\n" + //
						"}\n" + //
						"```",
				r(0, 0, 0, 6));
	}

	@Test
	public void testHoverOnKeywordInherit() throws BadLocationException {
		assertHover("inh|erit", //
				"**inherit** — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2819036)\n" + //
						"\n" + //
						"Used in namespace declarations to inherit the default namespace from the including grammar.\n"
						+ //
						"\n" + //
						"**Examples:**\n" + //
						"```rnc\n" + //
						"namespace local = inherit\n" + //
						"include \"other.rnc\" inherit\n" + //
						"```",
				r(0, 0, 0, 7));
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
				"**token** [ *params* ] — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2817679)\n" + //
						"\n" + //
						"Built-in datatype that matches a tokenized string: leading and trailing whitespace is stripped, and internal whitespace is collapsed to single spaces before comparison.\n"
						+ //
						"\n" + //
						"**Examples:**\n" + //
						"```rnc\n" + //
						"attribute class { token \"main\" }\n" + //
						"element name { token }\n" + //
						"```",
				r(0, 0, 0, 5));
	}

	// --- Include hover with definitions from included file ---

	@Test
	public void testHoverOnIncludeShowsDefinitions() throws BadLocationException {
		// include-base.rnc has: start = element doc { paragraph+ } and paragraph = element p { text }
		// include-valid.rnc overrides paragraph
		String fileUri = testFileUri("include-valid.rnc");
		String value = "inc|lude \"include-base.rnc\" {" + LS + //
				"  paragraph = element p {" + LS + //
				"    mixed { inline* }" + LS + //
				"  }" + LS + //
				"}";
		assertHover(value, fileUri, //
				"**include** `include-base.rnc`\n" + //
						"\n" + //
						"**Definitions from included file:**\n" + //
						"\n" + //
						"- `start` — inherited\n" + //
						"- ~~`paragraph`~~ — *overridden*\n",
				r(0, 0, 0, 7));
	}

	@Test
	public void testHoverOnIncludeShowsOverridden() throws BadLocationException {
		String fileUri = testFileUri("include-valid.rnc");
		String value = "inc|lude \"include-base.rnc\" {" + LS + //
				"  paragraph = element p {" + LS + //
				"    mixed { inline* }" + LS + //
				"  }" + LS + //
				"}";
		assertHover(value, fileUri, //
				"**include** `include-base.rnc`\n" + //
						"\n" + //
						"**Definitions from included file:**\n" + //
						"\n" + //
						"- `start` — inherited\n" + //
						"- ~~`paragraph`~~ — *overridden*\n",
				r(0, 0, 0, 7));
	}

	@Test
	public void testHoverOnIncludeNoFileFallsBackToKeyword() throws BadLocationException {
		assertHover("inc|lude \"nonexistent.rnc\"",
				"**include** *anyURILiteral* (reference) — [Spec](https://relaxng.org/compact-tutorial-20030326.html#id2820475)\n"
						+ //
						"\n" + //
						"Merges definitions from another RNC grammar file into the current grammar. The included defines become available as references.\n"
						+ //
						"\n" + //
						"**Example:**\n" + //
						"```rnc\n" + //
						"include \"inline.rnc\"\n" + //
						"\n" + //
						"start = element doc {\n" + //
						"  paragraph+\n" + //
						"}\n" + //
						"```",
				r(0, 0, 0, 7));
	}

	@Test
	public void testHoverOnDefineRefInDiv() throws BadLocationException {
		String value = "start = meta|data" + LS + //
				"div {" + LS + //
				"  metadata = element metadata {" + LS + //
				"    element title { text }," + LS + //
				"    element author { text }" + LS + //
				"  }";
		assertHover(value, "```rnc\n" + //
				"metadata = element metadata {" + LS + //
				"    element title { text }," + LS + //
				"    element author { text }" + LS + //
				"  }\n" + //
				"```", r(0, 8, 0, 16));
	}

	@Test
	public void testHoverOnDefineInDiv() throws BadLocationException {
		String value = "start = metadata" + LS + //
				"div {" + LS + //
				"  meta|data = element metadata {" + LS + //
				"    element title { text }," + LS + //
				"    element author { text }" + LS + //
				"  }" + LS + //
				"}";
		assertHover(value, //
				"```rnc\n" + //
						"metadata = element metadata {" + LS + //
						"    element title { text }," + LS + //
						"    element author { text }" + LS + //
						"  }\n" + //
						"```",
				r(2, 2, 2, 10));
	}

	@Test
	public void testHoverOnOverrideShowsOverrideLabel() throws BadLocationException {
		String fileUri = testFileUri("include-valid.rnc");
		String value = "include \"include-base.rnc\" {" + LS + //
				"  para|graph = element p {" + LS + //
				"    mixed { inline* }" + LS + //
				"  }" + LS + //
				"}";
		assertHover(value, fileUri, //
				"**Override:**\n" + //
						"```rnc\n" + //
						"paragraph = element p {" + LS + //
						"    mixed { inline* }" + LS + //
						"  }\n" + //
						"```\n" + //
						"\n" + //
						"**Original:**\n" + //
						"```rnc\n" + //
						"paragraph = element p { text }\n" + //
						"```",
				r(1, 2, 1, 11));
	}

	@Test
	public void testHoverOnOverrideShowsOriginal() throws BadLocationException {
		String fileUri = testFileUri("include-valid.rnc");
		String value = "include \"include-base.rnc\" {" + LS + //
				"  para|graph = element p {" + LS + //
				"    mixed { inline* }" + LS + //
				"  }" + LS + //
				"}";
		assertHover(value, fileUri, //
				"**Override:**\n" + //
						"```rnc\n" + //
						"paragraph = element p {" + LS + //
						"    mixed { inline* }" + LS + //
						"  }\n" + //
						"```\n" + //
						"\n" + //
						"**Original:**\n" + //
						"```rnc\n" + //
						"paragraph = element p { text }\n" + //
						"```",
				r(1, 2, 1, 11));
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
				"paragraph = element p {" + LS + //
				"    mixed { inline* }" + LS + //
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
		String value = "include \"include-base.rnc\" {" + LS + //
				"  para|graph = element p {" + LS + //
				"    mixed { inline* }" + LS + //
				"  }" + LS + //
				"}";
		assertHover(value, fileUri, //
				"**Override:**\n" + //
						"```rnc\n" + //
						"paragraph = element p {" + LS + //
						"    mixed { inline* }" + LS + //
						"  }\n" + //
						"```\n" + //
						"\n" + //
						"**Original:**\n" + //
						"```rnc\n" + //
						"paragraph = element p { text }\n" + //
						"```",
				r(1, 2, 1, 11));
	}

	@Test
	public void testHoverOnNonOverrideDefineNoOverrideLabel() throws BadLocationException {
		String value = "start = con|tent" + LS + "content = element content { text }";
		assertHover(value, "```rnc\n" + //
				"content = element content { text }\n" + //
				"```", r(0, 8, 0, 15));
	}

	// --- Datatype hover ---

	@Test
	public void testHoverOnXsdDatatype() throws BadLocationException {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS + //
				"start = element root {" + LS + //
				"  attribute id { xsd:int|eger }" + LS + //
				"}";
		assertHover(value, "**integer**\n" + //
				"\n" + //
				"Arbitrary-precision integer (no fractional part).\n" + //
				"\n" + //
				"See [XSD Spec](https://www.w3.org/TR/xmlschema11-2/#integer)", r(2, 17, 2, 28));
	}

	@Test
	public void testHoverOnXsdDatatypeShowsLink() throws BadLocationException {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				"start = element root {" + LS +
				"  attribute id { xsd:str|ing }" + LS +
				"}";
		assertHover(value, //
				"**string**\n" + //
						"\n" + //
						"Finite-length sequence of characters.\n" + //
						"\n" + //
						"See [XSD Spec](https://www.w3.org/TR/xmlschema11-2/#string)",
				r(2, 17, 2, 27));
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
		String value = "datatypes compat = \"http://relaxng.org/ns/compatibility/datatypes/1.0\"" + LS + //
				"start = element root {" + LS + //
				"  attribute id { compat:I|D }" + LS + //
				"}";
		assertHover(value, "**ID**\n" + //
				"\n" + //
				"XML compatibility datatype. Unique identifier for an element.\n" + //
				"\n" + //
				"See [RELAX NG DTD Compatibility](https://relaxng.org/compatibility-20011203.html)", r(2, 17, 2, 26));
	}

	@Test
	public void testHoverOnCompatDatatypeIdref() throws BadLocationException {
		String value = "datatypes compat = \"http://relaxng.org/ns/compatibility/datatypes/1.0\"" + LS + //
				"start = element root {" + LS + //
				"  attribute ref { compat:IDR|EF }" + LS + //
				"}";
		assertHover(value, "**IDREF**\n" + //
				"\n" + //
				"XML compatibility datatype. Reference to an element's ID.\n" + //
				"\n" + //
				"See [RELAX NG DTD Compatibility](https://relaxng.org/compatibility-20011203.html)", r(2, 18, 2, 30));
	}

	@Test
	public void testHoverOnXsdDatatypeBoolean() throws BadLocationException {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS + //
				"start = element root {" + LS + //
				"  attribute flag { xsd:bool|ean }" + LS + //
				"}";
		assertHover(value, "**boolean**\n" + //
				"\n" + //
				"Binary-valued logic: `true`, `false`, `1`, `0`.\n" + //
				"\n" + //
				"See [XSD Spec](https://www.w3.org/TR/xmlschema11-2/#boolean)", r(2, 19, 2, 30));
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
