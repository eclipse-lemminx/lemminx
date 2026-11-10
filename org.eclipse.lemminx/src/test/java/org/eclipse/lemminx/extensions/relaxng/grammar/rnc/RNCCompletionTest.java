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
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.assertResolveCompl;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testCompletionFor;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testFileUri;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.NEW_RNC_FILE_SNIPPETS;
import static org.eclipse.lemminx.XMLAssert.c;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.junit.jupiter.api.Test;

public class RNCCompletionTest {

	// --- Top-level completions ---

	@Test
	public void testCompletionTopLevel() throws BadLocationException {
		String value = "|";
		// Without snippet support: 7 declarations + new file snippets
		testCompletionFor(value, 7 + NEW_RNC_FILE_SNIPPETS,
				c("namespace", te(0, 0, 0, 0, "namespace"), "namespace"),
				c("default", te(0, 0, 0, 0, "default"), "default"),
				c("datatypes", te(0, 0, 0, 0, "datatypes"), "datatypes"),
				c("start", te(0, 0, 0, 0, "start"), "start"),
				c("include", te(0, 0, 0, 0, "include"), "include"),
				c("div", te(0, 0, 0, 0, "div"), "div"),
				c("element", te(0, 0, 0, 0, "element"), "element"));
		// With snippet support: 10 declarations (include splits into 2, datatypes into 3: generic+xsd+compat) + new file snippets
		testCompletionFor(value, true, 10 + NEW_RNC_FILE_SNIPPETS,
				c("namespace", te(0, 0, 0, 0, "namespace ${1:prefix} = \"${2:uri}\""), "namespace"),
				c("default namespace", te(0, 0, 0, 0, "default namespace = \"${1:uri}\""), "default namespace"),
				c("datatypes", te(0, 0, 0, 0, "datatypes ${1:prefix} = \"${2:uri}\""), "datatypes"),
				c("start", te(0, 0, 0, 0, "start = ${1|element,external,grammar|}"), "start"),
				c("include", te(0, 0, 0, 0, "include \"$1\"$0"), "include"),
				c("include { }", te(0, 0, 0, 0, "include \"$0\" {" + LS +
						"  " + LS +
						"}"), "include"),
				c("div", te(0, 0, 0, 0, "div {" + LS +
						"  $0" + LS +
						"}"), "div"),
				c("element", te(0, 0, 0, 0, "element ${1:name} {" + LS +
						"  $0" + LS +
						"}"), "element"));
	}

	// --- After assignment operator ---

	@Test
	public void testCompletionAfterStartAssign() throws BadLocationException {
		// start = | → only element, external, grammar (3 patterns)
		String value = "start = |";
		testCompletionFor(value, 3,
				c("element", te(0, 8, 0, 8, "element"), "element"),
				c("external", te(0, 8, 0, 8, "external"), "external"),
				c("grammar", te(0, 8, 0, 8, "grammar"), "grammar"));
		// With snippet support: element/external/grammar as snippets
		testCompletionFor(value, true, 3,
				c("element", te(0, 8, 0, 8, "element ${1:name} {" + LS +
						"  $0" + LS +
						"}"), "element"),
				c("external", te(0, 8, 0, 8, "external \"$1\"$0"), "external"),
				c("grammar", te(0, 8, 0, 8, "grammar {" + LS +
						"  $0" + LS +
						"}"), "grammar"));
	}

	@Test
	public void testCompletionAfterDefineAssign() throws BadLocationException {
		// foo = | → 12 patterns (including attribute) + 1 define ref = 13
		String value = "foo = |";
		testCompletionFor(value, 13,
				c("element", te(0, 6, 0, 6, "element"), "element"),
				c("attribute", te(0, 6, 0, 6, "attribute"), "attribute"),
				c("text", te(0, 6, 0, 6, "text"), "text"),
				c("empty", te(0, 6, 0, 6, "empty"), "empty"),
				c("foo", te(0, 6, 0, 6, "foo"), "foo"));
		// With snippet support
		testCompletionFor(value, true, 13,
				c("element", te(0, 6, 0, 6, "element ${1:name} {" + LS +
						"  $0" + LS +
						"}"), "element"),
				c("attribute", te(0, 6, 0, 6, "attribute ${1:name} { ${2:text} }"), "attribute"));
	}

	@Test
	public void testCompletionAfterChoiceAssign() throws BadLocationException {
		// Use case: "content |= |" → 12 pattern keywords + 1 define ref = 13
		testCompletionFor("content |= |", 13,
				c("element", te(0, 11, 0, 11, "element"), "element"),
				c("text", te(0, 11, 0, 11, "text"), "text"),
				c("content", te(0, 11, 0, 11, "content"), "content"));
	}

	@Test
	public void testCompletionAfterInterleaveAssign() throws BadLocationException {
		// Use case: "content &= |" → 12 pattern keywords + 1 define ref = 13
		testCompletionFor("content &= |", 13,
				c("element", te(0, 11, 0, 11, "element"), "element"),
				c("content", te(0, 11, 0, 11, "content"), "content"));
	}

	// --- Inside pattern ---

	@Test
	public void testCompletionInPattern() throws BadLocationException {
		// Use case: inside braces → 12 pattern keywords, no define refs
		String value = "element root {" + LS +
				"  |" + LS +
				"}";
		testCompletionFor(value, 12,
				c("element", te(1, 2, 1, 2, "element"), "element"),
				c("attribute", te(1, 2, 1, 2, "attribute"), "attribute"),
				c("text", te(1, 2, 1, 2, "text"), "text"));
	}

	@Test
	public void testCompletionAfterComma() throws BadLocationException {
		// Use case: after comma separator → 12 pattern keywords, no define refs
		String value = "element root {" + LS +
				"  attribute id { text }," + LS +
				"  |" + LS +
				"}";
		testCompletionFor(value, 12,
				c("element", te(2, 2, 2, 2, "element"), "element"),
				c("text", te(2, 2, 2, 2, "text"), "text"));
	}

	@Test
	public void testCompletionAfterPipe() throws BadLocationException {
		// Use case: after | operator → 12 pattern keywords, no define refs
		testCompletionFor("start = a | |", 12,
				c("element", te(0, 12, 0, 12, "element"), "element"),
				c("text", te(0, 12, 0, 12, "text"), "text"));
	}

	// --- Define references ---

	@Test
	public void testCompletionIncludesDefineRefs() throws BadLocationException {
		// Use case: defines are proposed as references after "start ="
		// 3 start patterns + 2 define refs = 5
		String value = "root = element root { content }" + LS +
				"content = text" + LS +
				"start = |";
		testCompletionFor(value, 5,
				c("root", te(2, 8, 2, 8, "root"), "root"),
				c("content", te(2, 8, 2, 8, "content"), "content"));
	}

	@Test
	public void testCompletionDefineRefsHaveReferenceKind() throws BadLocationException {
		// Use case: define refs have kind=Reference and detail="define"
		// 3 start patterns (element, external, grammar) + 1 define ref = 4
		String value = "myDefine = text" + LS +
				"start = |";
		CompletionList list = testCompletionFor(value, 4,
				c("myDefine", te(1, 8, 1, 8, "myDefine"), "myDefine"));
		CompletionItem myDefine = list.getItems().stream()
				.filter(i -> "myDefine".equals(i.getLabel()))
				.findFirst().orElseThrow();
		assertEquals(CompletionItemKind.Reference, myDefine.getKind());
		assertEquals("define", myDefine.getDetail());
	}

	@Test
	public void testCompletionDefineRefsInPattern() throws BadLocationException {
		// Use case: inside braces, defines are proposed as references
		// 12 patterns + 1 define ref = 13
		String value = "start = element root { | }" + LS +
				"inline = element span { text }";
		testCompletionFor(value, 13,
				c("inline", te(0, 23, 0, 23, "inline"), "inline"));
	}

	@Test
	public void testCompletionDefineRefsAfterCursor() throws BadLocationException {
		// Use case: defines declared AFTER cursor position are still proposed
		// 3 start patterns (element, external, grammar) + 2 define refs = 5
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				LS +
				"start = |" + LS +
				LS +
				"fooContent = element foo {" + LS +
				"  barContent," + LS +
				"  attribute attr { xsd:integer }" + LS +
				"}" + LS +
				"barContent = element bar { empty }";
		testCompletionFor(value, 5,
				c("fooContent", te(2, 8, 2, 8, "fooContent"), "fooContent"),
				c("barContent", te(2, 8, 2, 8, "barContent"), "barContent"));
	}

	@Test
	public void testCompletionAfterPipeWithDefines() throws BadLocationException {
		// Use case: after "|" operator, defines are proposed
		// 12 patterns + 2 define refs = 14
		String value = "start = a | |" + LS +
				"a = text" + LS +
				"b = element b { empty }";
		testCompletionFor(value, 14,
				c("a", te(0, 12, 0, 12, "a"), "a"),
				c("b", te(0, 12, 0, 12, "b"), "b"));
	}

	// --- Namespace / datatypes URI ---

	@Test
	public void testCompletionAfterNamespaceAssign() throws BadLocationException {
		// Use case: "namespace foo = |" → 4 well-known namespace URIs
		testCompletionFor("namespace foo = |", 4,
				c("\"http://www.w3.org/XML/1998/namespace\"",
						te(0, 16, 0, 16, "\"http://www.w3.org/XML/1998/namespace\""),
						"\"http://www.w3.org/XML/1998/namespace\""),
				c("\"http://www.w3.org/1999/xhtml\"",
						te(0, 16, 0, 16, "\"http://www.w3.org/1999/xhtml\""),
						"\"http://www.w3.org/1999/xhtml\""),
				c("\"http://relaxng.org/ns/compatibility/annotations/1.0\"",
						te(0, 16, 0, 16, "\"http://relaxng.org/ns/compatibility/annotations/1.0\""),
						"\"http://relaxng.org/ns/compatibility/annotations/1.0\""),
				c("\"http://relaxng.org/ns/structure/1.0\"",
						te(0, 16, 0, 16, "\"http://relaxng.org/ns/structure/1.0\""),
						"\"http://relaxng.org/ns/structure/1.0\""));
	}

	@Test
	public void testCompletionAfterDatatypesAssign() throws BadLocationException {
		// Use case: "datatypes xsd = |" → 2 datatype library URIs (from JSON)
		testCompletionFor("datatypes xsd = |", 2,
				c("\"http://www.w3.org/2001/XMLSchema-datatypes\"",
						te(0, 16, 0, 16, "\"http://www.w3.org/2001/XMLSchema-datatypes\""),
						"\"http://www.w3.org/2001/XMLSchema-datatypes\""),
				c("\"http://relaxng.org/ns/compatibility/datatypes/1.0\"",
						te(0, 16, 0, 16, "\"http://relaxng.org/ns/compatibility/datatypes/1.0\""),
						"\"http://relaxng.org/ns/compatibility/datatypes/1.0\""));
	}

	@Test
	public void testCompletionAfterDatatypesDecl() throws BadLocationException {
		// Use case: after "datatypes xsd = "..."", "start = |" should propose
		// defines not URIs (datatypes context must be reset by the string literal)
		// 3 start patterns (element, external, grammar) + 1 define ref = 4
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				LS +
				"start = |" + LS +
				LS +
				"fooContent = element foo { text }";
		testCompletionFor(value, 4,
				c("fooContent", te(2, 8, 2, 8, "fooContent"), "fooContent"));
	}

	// --- Intelligent start completion ---

	@Test
	public void testCompletionStartWithoutDefines() throws BadLocationException {
		String value = "|";
		// Without snippet support
		testCompletionFor(value,
				c("start", te(0, 0, 0, 0, "start"), "start"));
		// With snippet support (no triggerSuggest): choices with patterns
		testCompletionFor(value, true,
				c("start", te(0, 0, 0, 0,
						"start = ${1|element,external,grammar|}"), "start"));
		// With triggerSuggest: empty after =, cursor at $0
		testCompletionFor(value, true, true, null,
				c("start", te(0, 0, 0, 0, "start = $0"), "start"));
	}

	@Test
	public void testCompletionStartWithDefineChoice() throws BadLocationException {
		String value = "|" + LS +
				"fooContent = element foo { text }" + LS +
				"barContent = element bar { empty }";
		// Without snippet support: 7 decls + 4 define extends + 2 define refs = 13
		testCompletionFor(value, 13,
				c("start", te(0, 0, 0, 0, "start"), "start"),
				c("fooContent |=", te(0, 0, 0, 0, "fooContent |="), "fooContent |="),
				c("barContent &=", te(0, 0, 0, 0, "barContent &="), "barContent &="));
		// With snippet support: 10 decls (include+datatypes+compat) + 4 define extends + 2 define refs = 16
		// Choices include element + define names + patterns
		testCompletionFor(value, true, 16,
				c("start", te(0, 0, 0, 0,
						"start = ${1|element,fooContent,barContent,external,grammar|}"), "start"));
	}

	@Test
	public void testCompletionStartAlreadyDefined() throws BadLocationException {
		String value = "start = foo" + LS +
				"|" + LS +
				"foo = element foo { text }" + LS +
				"bar = element bar { empty }";
		// Without snippet support: 7 decls + 4 define extends + 2 define refs = 13
		testCompletionFor(value, 13,
				c("start |=", te(1, 0, 1, 0, "start |="), "start |="));
		// With snippet support: 10 decls (datatypes+compat) + 4 define extends + 2 define refs = 16
		testCompletionFor(value, true, 16,
				c("start |=", te(1, 0, 1, 0,
						"start |= ${1|element,foo,bar,external,grammar|}"), "start |="));
	}

	// --- New file snippets ---

	@Test
	public void testCompletionNewFileSnippets() throws BadLocationException {
		// Use case: empty file → new file snippets offered with text edits
		testCompletionFor("|", true,
				c("New RelaxNG Compact", te(0, 0, 0, 0,
						"start = element ${1:root} {" + LS + "  ${0}" + LS + "}"), "start"),
				c("New RelaxNG Compact with define", te(0, 0, 0, 0,
						"start = ${1:root}" + LS + LS + "${1:root} = element ${1:root} {" + LS + "  ${0}" + LS + "}"), "start"),
				c("New RelaxNG Compact with namespace", te(0, 0, 0, 0,
						"default namespace = \"${1:http://example.com}\"" + LS +
						"datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS + LS +
						"start = element ${2:root} {" + LS + "  ${0}" + LS + "}"), "default"),
				c("New RelaxNG Compact with namespace and define", te(0, 0, 0, 0,
						"default namespace = \"${1:http://example.com}\"" + LS +
						"datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS + LS +
						"start = ${2:root}" + LS + LS +
						"${2:root} = element ${2:root} {" + LS + "  ${0}" + LS + "}"), "default"));
		// Non-empty document → snippets NOT offered
		String value = "start = element root { text }" + LS + "|";
		CompletionList list = testCompletionFor(value, true);
		assertTrue(list.getItems().stream().noneMatch(i -> i.getLabel().startsWith("New RelaxNG")),
				"New file snippets should not be proposed in non-empty documents");
	}

	// --- Intelligent default namespace ---

	@Test
	public void testCompletionDefaultNamespace() throws BadLocationException {
		String value = "namespace foo = \"http://example.com\"" + LS + "|";
		// Without snippet support
		testCompletionFor(value,
				c("default", te(1, 0, 1, 0, "default"), "default"));
		// With snippet support
		testCompletionFor(value, true,
				c("default namespace", te(1, 0, 1, 0, "default namespace = \"${1:uri}\""), "default namespace"));
		// Already declared → NOT proposed
		String valueDeclared = "default namespace = \"http://example.com\"" + LS + "|";
		CompletionList list = testCompletionFor(valueDeclared, true);
		assertTrue(list.getItems().stream().noneMatch(i -> "default namespace".equals(i.getLabel())),
				"default namespace should not be proposed when already declared");
	}

	// --- Intelligent define extend (|= and &=) ---

	@Test
	public void testCompletionDefineExtend() throws BadLocationException {
		String value = "start = root" + LS +
				"root = element root { content }" + LS +
				"content = text" + LS +
				"|";
		// Without snippet support
		testCompletionFor(value,
				c("content |=", te(3, 0, 3, 0, "content |="), "content |="),
				c("content &=", te(3, 0, 3, 0, "content &="), "content &="),
				c("root |=", te(3, 0, 3, 0, "root |="), "root |="),
				c("root &=", te(3, 0, 3, 0, "root &="), "root &="));
		// With snippet support
		testCompletionFor(value, true,
				c("content |=", te(3, 0, 3, 0, "content |= ${1:pattern}"), "content |="),
				c("content &=", te(3, 0, 3, 0, "content &= ${1:pattern}"), "content &="));
	}

	// --- Namespace / datatypes / include always proposed ---

	@Test
	public void testCompletionNamespaceAlwaysProposed() throws BadLocationException {
		// Use case: namespace can always be added (multiple prefixes allowed)
		String value = "namespace foo = \"http://example.com\"" + LS + "|";
		testCompletionFor(value,
				c("namespace", te(1, 0, 1, 0, "namespace"), "namespace"));
	}

	@Test
	public void testCompletionDatatypesAlwaysProposed() throws BadLocationException {
		// Use case: datatypes can always be added (multiple prefixes allowed)
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS + "|";
		testCompletionFor(value,
				c("datatypes", te(1, 0, 1, 0, "datatypes"), "datatypes"));
	}

	@Test
	public void testCompletionIncludeAlwaysProposed() throws BadLocationException {
		// Use case: include can always be added (multiple includes allowed)
		String value = "include \"other.rnc\"" + LS + "|";
		testCompletionFor(value,
				c("include", te(1, 0, 1, 0, "include"), "include"));
	}

	// --- Attribute not proposed at top level ---

	@Test
	public void testCompletionAttributeNotAtTopLevel() throws BadLocationException {
		String value = "|";
		CompletionList list = testCompletionFor(value);
		assertTrue(list.getItems().stream().noneMatch(i -> "attribute".equals(i.getLabel())),
				"attribute should not be proposed at top level");
	}

	@Test
	public void testCompletionAttributeAfterDefineAssign() throws BadLocationException {
		// attribute IS proposed after = in a define (valid)
		testCompletionFor("foo = |",
				c("attribute", te(0, 6, 0, 6, "attribute"), "attribute"));
	}

	@Test
	public void testCompletionAttributeInPattern() throws BadLocationException {
		// attribute IS proposed inside element body
		String value = "start = element root {" + LS +
				"  |" + LS +
				"}";
		testCompletionFor(value,
				c("attribute", te(1, 2, 1, 2, "attribute"), "attribute"));
	}

	// --- Completion while typing (partial identifier) ---

	@Test
	public void testCompletionWhileTypingAfterAssign() throws BadLocationException {
		// Use case: typing "f" after "start = " → still proposes defines
		String value = "start = f|" + LS +
				"fooContent = element foo { empty }";
		testCompletionFor(value,
				c("fooContent", te(0, 8, 0, 9, "fooContent"), "fooContent"));
	}

	@Test
	public void testCompletionWhileTypingInPattern() throws BadLocationException {
		// Use case: typing inside braces → still proposes defines
		String value = "start = element root {" + LS +
				"  f|" + LS +
				"}" + LS +
				"foo = text";
		testCompletionFor(value,
				c("foo", te(1, 2, 1, 3, "foo"), "foo"));
	}

	// --- Element completion generates define form at top-level in non-empty file ---

	@Test
	public void testCompletionElementInlineInPatternForm() throws BadLocationException {
		// Use case: file with bare element (no declarations) → inline form
		String value = "element root { text }" + LS + "|";
		testCompletionFor(value, true,
				c("element", te(1, 0, 1, 0, "element ${1:name} {" + LS +
						"  $0" + LS +
						"}"), "element"));
	}

	@Test
	public void testCompletionElementDefineFormAfterDefine() throws BadLocationException {
		// Use case: file has defines → element at top-level generates define form
		String value = "start = root" + LS +
				"root = element root { text }" + LS +
				"|";
		testCompletionFor(value, true,
				c("element", te(2, 0, 2, 0, "${1:name} = element ${1:name} {" + LS +
						"  $0" + LS +
						"}"), "element"));
	}

	@Test
	public void testCompletionElementInlineAfterAssign() throws BadLocationException {
		// Use case: after '=' → element stays inline (not define form)
		String value = "root = element root { text }" + LS +
				"child = |";
		testCompletionFor(value, true,
				c("element", te(1, 8, 1, 8, "element ${1:name} {" + LS +
						"  $0" + LS +
						"}"), "element"));
	}

	@Test
	public void testCompletionElementInlineInPattern() throws BadLocationException {
		// Use case: inside braces → element stays inline (not define form)
		String value = "start = element root {" + LS +
				"  |" + LS +
				"}";
		testCompletionFor(value, true,
				c("element", te(1, 2, 1, 2, "element ${1:name} {" + LS +
						"  $0" + LS +
						"}"), "element"));
	}

	@Test
	public void testCompletionElementInlineEmptyFile() throws BadLocationException {
		// Use case: empty file → element stays inline (not define form)
		testCompletionFor("|", true,
				c("element", te(0, 0, 0, 0, "element ${1:name} {" + LS +
						"  $0" + LS +
						"}"), "element"));
	}

	@Test
	public void testCompletionElementInlineWhileTypingInEmptyFile() throws BadLocationException {
		// Use case: typing "ele" in an empty file → element stays inline (not define form)
		testCompletionFor("ele|", true,
				c("element", te(0, 0, 0, 3, "element ${1:name} {" + LS +
						"  $0" + LS +
						"}"), "element"));
	}

	@Test
	public void testCompletionElementDefineFormWithNamespace() throws BadLocationException {
		// Use case: file has namespace → non-empty → element generates define form
		String value = "default namespace = \"http://example.com\"" + LS + "|";
		testCompletionFor(value, true,
				c("element", te(1, 0, 1, 0, "${1:name} = element ${1:name} {" + LS +
						"  $0" + LS +
						"}"), "element"));
	}

	// --- Grammar keyword completion ---

	@Test
	public void testCompletionGrammarSnippet() throws BadLocationException {
		// grammar generates { } snippet after start =
		String value = "start = |";
		testCompletionFor(value, true,
				c("grammar", te(0, 8, 0, 8, "grammar {" + LS +
						"  $0" + LS +
						"}"), "grammar"));
	}

	// --- External keyword completion ---

	@Test
	public void testCompletionExternalSnippet() throws BadLocationException {
		String value = "foo = |";
		testCompletionFor(value, true,
				c("external", te(0, 6, 0, 6, "external \"$1\"$0"), "external"));
	}

	@Test
	public void testCompletionExternalKeywordOnly() throws BadLocationException {
		String value = "foo = |";
		testCompletionFor(value, false,
				c("external", te(0, 6, 0, 6, "external"), "external"));
	}

	// --- Include body override completion ---

	@Test
	public void testCompletionIncludeBodyOverrides() throws BadLocationException {
		// include-base.rnc has: start = element doc { paragraph+ } and paragraph = element p { text }
		// Inside include body, should propose both as overrides with original text
		String fileUri = testFileUri("include-valid.rnc");
		String value = "include \"include-base.rnc\" {" + LS +
				"  |" + LS +
				"}";
		testCompletionFor(value, fileUri, true, null,
				c("start", te(1, 2, 1, 2, "start = element doc { paragraph+ }"), "start"),
				c("paragraph", te(1, 2, 1, 2, "paragraph = element p { text }"), "paragraph"));
	}

	@Test
	public void testCompletionIncludeBodyAlreadyOverridden() throws BadLocationException {
		// paragraph is already overridden, should NOT be proposed
		String fileUri = testFileUri("include-valid.rnc");
		String value = "include \"include-base.rnc\" {" + LS +
				"  paragraph = element p { text }" + LS +
				"  |" + LS +
				"}";
		CompletionList list = testCompletionFor(value, fileUri, true, null);
		assertTrue(list.getItems().stream().noneMatch(
				i -> "paragraph".equals(i.getLabel()) && "Override from included file".equals(i.getDetail())),
				"paragraph should not be proposed since it's already overridden");
		// start should still be proposed with original text
		testCompletionFor(value, fileUri, true, null,
				c("start", te(2, 2, 2, 2, "start = element doc { paragraph+ }"), "start"));
	}

	// --- Completion Resolve ---

	@Test
	public void testResolveDefineRef() throws BadLocationException {
		String value = "start = |" + LS +
				"root = element root { text }";
		assertResolveCompl(value, "root", "root = element root { text }");
	}

	@Test
	public void testResolveKeyword() throws BadLocationException {
		assertResolveCompl("start = |", "element", "Defines an element pattern");
	}

	@Test
	public void testResolveIncludeOverride() throws BadLocationException {
		String fileUri = testFileUri("include-valid.rnc");
		String value = "include \"include-base.rnc\" {" + LS +
				"  |" + LS +
				"}";
		assertResolveCompl(value, fileUri, "start", "start = element doc { paragraph+ }");
		assertResolveCompl(value, fileUri, "paragraph", "paragraph = element p { text }");
	}

	// --- Grammar and div block completions ---

	@Test
	public void testCompletionInsideGrammarBlock() throws BadLocationException {
		String value = "grammar {" + LS +
				"  |" + LS +
				"}";
		testCompletionFor(value,
				c("start", te(1, 2, 1, 2, "start"), "start"),
				c("include", te(1, 2, 1, 2, "include"), "include"),
				c("div", te(1, 2, 1, 2, "div"), "div"),
				c("element", te(1, 2, 1, 2, "element"), "element"));
	}

	@Test
	public void testCompletionInsideDivBlock() throws BadLocationException {
		String value = "div {" + LS +
				"  |" + LS +
				"}";
		testCompletionFor(value,
				c("start", te(1, 2, 1, 2, "start"), "start"),
				c("include", te(1, 2, 1, 2, "include"), "include"),
				c("div", te(1, 2, 1, 2, "div"), "div"),
				c("element", te(1, 2, 1, 2, "element"), "element"));
	}

	@Test
	public void testCompletionInsideDivBlockWithExistingDefines() throws BadLocationException {
		String value = "start = root" + LS +
				"root = element root { text }" + LS +
				"div {" + LS +
				"  |" + LS +
				"}";
		testCompletionFor(value,
				c("element", te(3, 2, 3, 2, "element"), "element"),
				c("include", te(3, 2, 3, 2, "include"), "include"));
	}

	// --- List and mixed block completions ---

	@Test
	public void testCompletionInsideListBlock() throws BadLocationException {
		String value = "start = element foo {" + LS +
				"  list { | }" + LS +
				"}";
		testCompletionFor(value,
				c("element", te(1, 9, 1, 9, "element"), "element"),
				c("attribute", te(1, 9, 1, 9, "attribute"), "attribute"),
				c("text", te(1, 9, 1, 9, "text"), "text"),
				c("token", te(1, 9, 1, 9, "token"), "token"));
	}

	@Test
	public void testCompletionInsideMixedBlock() throws BadLocationException {
		String value = "start = element foo {" + LS +
				"  mixed { | }" + LS +
				"}";
		testCompletionFor(value,
				c("element", te(1, 10, 1, 10, "element"), "element"),
				c("attribute", te(1, 10, 1, 10, "attribute"), "attribute"),
				c("text", te(1, 10, 1, 10, "text"), "text"));
	}

	@Test
	public void testCompletionListSnippet() throws BadLocationException {
		// list generates { } snippet in snippet mode
		String value = "start = element foo {" + LS +
				"  |" + LS +
				"}";
		testCompletionFor(value, true,
				c("list", te(1, 2, 1, 2, "list {" + LS +
						"  $0" + LS +
						"}"), "list"));
	}

	@Test
	public void testCompletionMixedSnippet() throws BadLocationException {
		// mixed generates { } snippet in snippet mode
		String value = "start = element foo {" + LS +
				"  |" + LS +
				"}";
		testCompletionFor(value, true,
				c("mixed", te(1, 2, 1, 2, "mixed {" + LS +
						"  $0" + LS +
						"}"), "mixed"));
	}

	@Test
	public void testCompletionListKeywordWithoutSnippets() throws BadLocationException {
		// list is plain keyword without snippet support
		String value = "start = element foo {" + LS +
				"  |" + LS +
				"}";
		testCompletionFor(value, false,
				c("list", te(1, 2, 1, 2, "list"), "list"));
	}

	@Test
	public void testCompletionMixedKeywordWithoutSnippets() throws BadLocationException {
		// mixed is plain keyword without snippet support
		String value = "start = element foo {" + LS +
				"  |" + LS +
				"}";
		testCompletionFor(value, false,
				c("mixed", te(1, 2, 1, 2, "mixed"), "mixed"));
	}

	// --- Nested element completions ---

	@Test
	public void testCompletionNestedElement() throws BadLocationException {
		String value = "start = element root {" + LS +
				"  element child {" + LS +
				"    |" + LS +
				"  }" + LS +
				"}";
		testCompletionFor(value, 12,
				c("element", te(2, 4, 2, 4, "element"), "element"),
				c("attribute", te(2, 4, 2, 4, "attribute"), "attribute"),
				c("text", te(2, 4, 2, 4, "text"), "text"),
				c("empty", te(2, 4, 2, 4, "empty"), "empty"));
	}

	@Test
	public void testCompletionNestedElementWithDefineRefs() throws BadLocationException {
		String value = "start = element root {" + LS +
				"  element child {" + LS +
				"    |" + LS +
				"  }" + LS +
				"}" + LS +
				"inline = text";
		testCompletionFor(value, 13,
				c("element", te(2, 4, 2, 4, "element"), "element"),
				c("inline", te(2, 4, 2, 4, "inline"), "inline"));
	}

	// --- After interleave operator ---

	@Test
	public void testCompletionAfterInterleaveOperator() throws BadLocationException {
		String value = "start = element root {" + LS +
				"  element a { text } & |" + LS +
				"}";
		testCompletionFor(value, 12,
				c("element", te(1, 23, 1, 23, "element"), "element"),
				c("text", te(1, 23, 1, 23, "text"), "text"));
	}

	@Test
	public void testCompletionAfterInterleaveOperatorWithDefines() throws BadLocationException {
		String value = "start = element root {" + LS +
				"  element a { text } & |" + LS +
				"}" + LS +
				"foo = text";
		testCompletionFor(value, 13,
				c("element", te(1, 23, 1, 23, "element"), "element"),
				c("foo", te(1, 23, 1, 23, "foo"), "foo"));
	}

	// --- After choice operator ---

	@Test
	public void testCompletionAfterChoiceOperatorInPattern() throws BadLocationException {
		String value = "start = element root {" + LS +
				"  text | |" + LS +
				"}";
		testCompletionFor(value, 12,
				c("element", te(1, 9, 1, 9, "element"), "element"),
				c("empty", te(1, 9, 1, 9, "empty"), "empty"));
	}

	// --- After group (comma) operator ---

	@Test
	public void testCompletionAfterCommaWithDefineRefs() throws BadLocationException {
		String value = "start = element root {" + LS +
				"  attribute id { text }," + LS +
				"  |" + LS +
				"}" + LS +
				"content = text";
		testCompletionFor(value, 13,
				c("element", te(2, 2, 2, 2, "element"), "element"),
				c("content", te(2, 2, 2, 2, "content"), "content"));
	}

	// --- Combine operator completions ---

	@Test
	public void testCompletionAfterStartChoiceAssign() throws BadLocationException {
		// start |= | → AFTER_START_ASSIGN (only element, external, grammar + define refs)
		String value = "start = foo" + LS +
				"foo = element foo { text }" + LS +
				"bar = element bar { empty }" + LS +
				"start |= |";
		testCompletionFor(value, 5,
				c("foo", te(3, 9, 3, 9, "foo"), "foo"),
				c("bar", te(3, 9, 3, 9, "bar"), "bar"),
				c("element", te(3, 9, 3, 9, "element"), "element"),
				c("external", te(3, 9, 3, 9, "external"), "external"),
				c("grammar", te(3, 9, 3, 9, "grammar"), "grammar"));
		CompletionList list = testCompletionFor(value);
		assertTrue(list.getItems().stream().noneMatch(i -> "attribute".equals(i.getLabel())),
				"attribute should not be proposed after start |=");
		assertTrue(list.getItems().stream().noneMatch(i -> "text".equals(i.getLabel())),
				"text should not be proposed after start |=");
	}

	@Test
	public void testCompletionAfterStartInterleaveAssign() throws BadLocationException {
		// start &= | → AFTER_START_ASSIGN (only element, external, grammar + define refs)
		String value = "start = foo" + LS +
				"foo = element foo { text }" + LS +
				"bar = element bar { empty }" + LS +
				"start &= |";
		testCompletionFor(value, 5,
				c("foo", te(3, 9, 3, 9, "foo"), "foo"),
				c("bar", te(3, 9, 3, 9, "bar"), "bar"),
				c("element", te(3, 9, 3, 9, "element"), "element"),
				c("external", te(3, 9, 3, 9, "external"), "external"),
				c("grammar", te(3, 9, 3, 9, "grammar"), "grammar"));
		CompletionList list = testCompletionFor(value);
		assertTrue(list.getItems().stream().noneMatch(i -> "attribute".equals(i.getLabel())),
				"attribute should not be proposed after start &=");
		assertTrue(list.getItems().stream().noneMatch(i -> "text".equals(i.getLabel())),
				"text should not be proposed after start &=");
	}

	// --- No completion in comments ---

	@Test
	public void testNoCompletionInComment() throws BadLocationException {
		testCompletionFor("# |", 0);
	}

	@Test
	public void testNoCompletionInCommentAfterCode() throws BadLocationException {
		String value = "start = element root { text }" + LS + "# a comment |";
		testCompletionFor(value, 0);
	}

	@Test
	public void testNoCompletionInCommentInsideBlock() throws BadLocationException {
		String value = "start = element root {" + LS + "  # comment |" + LS + "  text" + LS + "}";
		testCompletionFor(value, 0);
	}

	@Test
	public void testNoCompletionInDocumentationComment() throws BadLocationException {
		testCompletionFor("## |", 0);
	}

	@Test
	public void testCompletionAfterComment() throws BadLocationException {
		String value = "# comment" + LS + "|";
		testCompletionFor(value, 7 + NEW_RNC_FILE_SNIPPETS);
	}

	// --- TriggerSuggest command tests ---

	@Test
	public void testCompletionStartTriggerSuggest() throws BadLocationException {
		// With triggerSuggest: start = $0 and command attached
		String value = "|";
		CompletionList list = testCompletionFor(value, true, true, null,
				c("start", te(0, 0, 0, 0, "start = $0"), "start"));
		CompletionItem startItem = list.getItems().stream()
				.filter(i -> "start".equals(i.getLabel()))
				.findFirst().orElseThrow();
		assertNotNull(startItem.getCommand(), "start should have triggerSuggest command");
	}

	@Test
	public void testCompletionStartAlreadyDefinedTriggerSuggest() throws BadLocationException {
		// start |= with triggerSuggest
		String value = "start = foo" + LS +
				"|" + LS +
				"foo = element foo { text }";
		CompletionList list = testCompletionFor(value, true, true, null,
				c("start |=", te(1, 0, 1, 0, "start |= $0"), "start |="));
		CompletionItem startItem = list.getItems().stream()
				.filter(i -> "start |=".equals(i.getLabel()))
				.findFirst().orElseThrow();
		assertNotNull(startItem.getCommand(), "start |= should have triggerSuggest command");
	}

	@Test
	public void testCompletionIncludeTriggerSuggest() throws BadLocationException {
		// include and include { } should both have triggerSuggest command
		String value = "|";
		CompletionList list = testCompletionFor(value, true, true, null);
		CompletionItem includeItem = list.getItems().stream()
				.filter(i -> "include".equals(i.getLabel()))
				.findFirst().orElseThrow();
		assertNotNull(includeItem.getCommand(), "include should have triggerSuggest command");

		CompletionItem includeBodyItem = list.getItems().stream()
				.filter(i -> "include { }".equals(i.getLabel()))
				.findFirst().orElseThrow();
		assertNotNull(includeBodyItem.getCommand(), "include { } should have triggerSuggest command");
	}

	@Test
	public void testCompletionNoTriggerSuggestWithoutSupport() throws BadLocationException {
		// Without triggerSuggest: no commands attached
		String value = "|";
		CompletionList list = testCompletionFor(value, true);
		CompletionItem startItem = list.getItems().stream()
				.filter(i -> "start".equals(i.getLabel()))
				.findFirst().orElseThrow();
		assertNull(startItem.getCommand(), "start should not have command without triggerSuggest");
	}

	// --- Include with body completion ---

	@Test
	public void testCompletionIncludeWithBody() throws BadLocationException {
		// In snippet mode: both include items are proposed
		String value = "|";
		testCompletionFor(value, true,
				c("include", te(0, 0, 0, 0, "include \"$1\"$0"), "include"),
				c("include { }", te(0, 0, 0, 0, "include \"$0\" {" + LS +
						"  " + LS +
						"}"), "include"));
	}

	@Test
	public void testCompletionIncludeNoBodyWithoutSnippets() throws BadLocationException {
		// Without snippets: only one include keyword (no body variant)
		String value = "|";
		CompletionList list = testCompletionFor(value);
		long includeCount = list.getItems().stream()
				.filter(i -> i.getLabel() != null && i.getLabel().startsWith("include"))
				.count();
		assertEquals(1, includeCount, "Only one include item without snippets");
	}

	// --- Define references from included files ---

	@Test
	public void testCompletionDefineRefFromIncludedFile() throws BadLocationException {
		// include-base.rnc defines: start, paragraph
		// "paragraph" should appear as a define reference completion
		String fileUri = testFileUri("include-valid.rnc");
		String value = "include \"include-base.rnc\"" + LS
				+ LS
				+ "start = element doc {" + LS
				+ "  |" + LS
				+ "}";
		testCompletionFor(value, fileUri, false, null,
				c("paragraph", te(3, 2, 3, 2, "paragraph"), "paragraph"));
	}

	// -------------------- Pattern completion with datatypes declaration --------------------

	@Test
	public void testCompletionInsideElementBlockWithDatatypes() throws BadLocationException {
		// Use case: cursor inside element body after datatypes declaration
		// Should still get pattern completions (element, attribute, text, etc.)
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				"start = element foo {" + LS +
				"  |" + LS +
				"}";
		testCompletionFor(value,
				c("element", te(2, 2, 2, 2, "element"), "element"),
				c("attribute", te(2, 2, 2, 2, "attribute"), "attribute"),
				c("text", te(2, 2, 2, 2, "text"), "text"),
				c("xsd:integer", te(2, 2, 2, 2, "xsd:integer"), "xsd:integer"),
				c("xsd:string", te(2, 2, 2, 2, "xsd:string"), "xsd:string"),
				c("xsd:boolean", te(2, 2, 2, 2, "xsd:boolean"), "xsd:boolean"));
	}

	// -------------------- XSD Datatype Completion --------------------

	@Test
	public void testCompletionXsdDatatypeAfterColon() throws BadLocationException {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				"start = element root {" + LS +
				"  attribute id { xsd:| }" + LS +
				"}";
		testCompletionFor(value,
				c("xsd:integer", te(2, 17, 2, 21, "xsd:integer"), "xsd:integer"),
				c("xsd:string", te(2, 17, 2, 21, "xsd:string"), "xsd:string"),
				c("xsd:boolean", te(2, 17, 2, 21, "xsd:boolean"), "xsd:boolean"));
	}

	@Test
	public void testCompletionXsdDatatypeWithPartial() throws BadLocationException {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				"start = element root {" + LS +
				"  attribute id { xsd:in| }" + LS +
				"}";
		testCompletionFor(value,
				c("xsd:integer", te(2, 17, 2, 23, "xsd:integer"), "xsd:integer"),
				c("xsd:int", te(2, 17, 2, 23, "xsd:int"), "xsd:int"));
	}

	@Test
	public void testCompletionNoDatatypeWithoutDecl() throws BadLocationException {
		String value = "start = element root {" + LS +
				"  attribute id { xsd:| }" + LS +
				"}";
		testCompletionFor(value, 0);
	}

	// -------------------- Datatype Library Snippets --------------------

	@Test
	public void testCompletionDatatypesXsdSnippet() throws BadLocationException {
		// Use case: "datatypes xsd" snippet is proposed with placeholder for prefix
		// filterText is "datatypes" so all variants appear when typing "datatypes"
		testCompletionFor("|", true,
				c("datatypes xsd",
						te(0, 0, 0, 0, "datatypes ${1:xsd} = \"http://www.w3.org/2001/XMLSchema-datatypes\"$0"),
						"datatypes"));
	}

	@Test
	public void testCompletionDatatypesCompatSnippet() throws BadLocationException {
		// Use case: "datatypes compat" snippet is proposed with placeholder for prefix
		testCompletionFor("|", true,
				c("datatypes compat",
						te(0, 0, 0, 0, "datatypes ${1:compat} = \"http://relaxng.org/ns/compatibility/datatypes/1.0\"$0"),
						"datatypes"));
	}

	@Test
	public void testCompletionDatatypesGenericSnippet() throws BadLocationException {
		// Use case: generic "datatypes" snippet is also proposed
		testCompletionFor("|", true,
				c("datatypes",
						te(0, 0, 0, 0, "datatypes ${1:prefix} = \"${2:uri}\""),
						"datatypes"));
	}

	@Test
	public void testCompletionDatatypesUriDetail() throws BadLocationException {
		// Use case: datatypes URI completion shows description as detail
		testCompletionFor("datatypes xsd = |", 2);
	}

	// -------------------- Compatibility Datatype Completion --------------------

	@Test
	public void testCompletionCompatDatatypeAfterColon() throws BadLocationException {
		String value = "datatypes compat = \"http://relaxng.org/ns/compatibility/datatypes/1.0\"" + LS +
				"start = element root {" + LS +
				"  attribute id { compat:| }" + LS +
				"}";
		testCompletionFor(value,
				c("compat:ID", te(2, 17, 2, 24, "compat:ID"), "compat:ID"),
				c("compat:IDREF", te(2, 17, 2, 24, "compat:IDREF"), "compat:IDREF"),
				c("compat:IDREFS", te(2, 17, 2, 24, "compat:IDREFS"), "compat:IDREFS"));
	}

	// -------------------- Resolve Datatype Documentation --------------------

	@Test
	public void testResolveDatatypeDoc() throws BadLocationException {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				"start = element root {" + LS +
				"  attribute id { xsd:| }" + LS +
				"}";
		assertResolveCompl(value, "xsd:integer", "integer");
	}
}
