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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssignType;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAttribute;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDatatypesDecl;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDiv;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCElement;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCExternal;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCInclude;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNamespaceDecl;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNode;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCStart;
import org.junit.jupiter.api.Test;

public class RNCParserTest {

	// --- Namespace declarations ---

	@Test
	public void testNamespaceDecl() {
		RNCDocument doc = parse("namespace foo = \"http://example.com\"");
		List<RNCNamespaceDecl> nsDecls = doc.getNamespaceDecls();
		assertEquals(1, nsDecls.size());

		RNCNamespaceDecl ns = nsDecls.get(0);
		assertFalse(ns.isDefault());
		assertEquals("foo", ns.getPrefix());
		assertEquals("http://example.com", ns.getUri());
		assertTrue(ns.isClosed());
	}

	@Test
	public void testDefaultNamespaceDecl() {
		RNCDocument doc = parse("default namespace = \"http://example.com\"");
		List<RNCNamespaceDecl> nsDecls = doc.getNamespaceDecls();
		assertEquals(1, nsDecls.size());

		RNCNamespaceDecl ns = nsDecls.get(0);
		assertTrue(ns.isDefault());
		assertNull(ns.getPrefix());
		assertEquals("http://example.com", ns.getUri());
		assertTrue(ns.isClosed());
	}

	@Test
	public void testDefaultNamespaceDeclWithPrefix() {
		RNCDocument doc = parse("default namespace db = \"http://docbook.org\"");
		List<RNCNamespaceDecl> nsDecls = doc.getNamespaceDecls();
		assertEquals(1, nsDecls.size());

		RNCNamespaceDecl ns = nsDecls.get(0);
		assertTrue(ns.isDefault());
		assertEquals("db", ns.getPrefix());
		assertEquals("http://docbook.org", ns.getUri());
		assertTrue(ns.isClosed());
		assertEquals(0, ns.getStart());
		assertTrue(doc.getErrors().isEmpty());
	}

	@Test
	public void testMultipleNamespaceDecls() {
		RNCDocument doc = parse("namespace foo = \"http://foo.com\"\nnamespace bar = \"http://bar.com\"");
		List<RNCNamespaceDecl> nsDecls = doc.getNamespaceDecls();
		assertEquals(2, nsDecls.size());

		assertEquals("foo", nsDecls.get(0).getPrefix());
		assertEquals("http://foo.com", nsDecls.get(0).getUri());
		assertEquals("bar", nsDecls.get(1).getPrefix());
		assertEquals("http://bar.com", nsDecls.get(1).getUri());
	}

	// --- Datatypes declarations ---

	@Test
	public void testDatatypesDecl() {
		RNCDocument doc = parse("datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"");
		List<RNCDatatypesDecl> dtDecls = doc.getDatatypesDecls();
		assertEquals(1, dtDecls.size());

		RNCDatatypesDecl dt = dtDecls.get(0);
		assertEquals("xsd", dt.getPrefix());
		assertEquals("http://www.w3.org/2001/XMLSchema-datatypes", dt.getUri());
		assertTrue(dt.isClosed());
	}

	// --- Start declarations ---

	@Test
	public void testStartDecl() {
		RNCDocument doc = parse("start = root");
		RNCStart start = doc.getStartDecl();
		assertNotNull(start);
		assertEquals(RNCAssignType.ASSIGN, start.getAssignType());
	}

	@Test
	public void testStartDeclWithChoice() {
		RNCDocument doc = parse("start |= root");
		RNCStart start = doc.getStartDecl();
		assertNotNull(start);
		assertEquals(RNCAssignType.CHOICE, start.getAssignType());
	}

	// --- Define declarations ---

	@Test
	public void testDefine() {
		RNCDocument doc = parse("myPattern = element foo { text }");
		List<RNCDefine> defines = doc.getDefines();
		assertEquals(1, defines.size());

		RNCDefine define = defines.get(0);
		assertEquals("myPattern", define.getName());
		assertEquals(RNCAssignType.ASSIGN, define.getAssignType());
	}

	@Test
	public void testDefineWithChoiceAssign() {
		RNCDocument doc = parse("content |= element img { empty }");
		List<RNCDefine> defines = doc.getDefines();
		assertEquals(1, defines.size());

		RNCDefine define = defines.get(0);
		assertEquals("content", define.getName());
		assertEquals(RNCAssignType.CHOICE, define.getAssignType());
	}

	@Test
	public void testDefineWithInterleaveAssign() {
		RNCDocument doc = parse("content &= element extra { text }");
		List<RNCDefine> defines = doc.getDefines();
		assertEquals(1, defines.size());

		RNCDefine define = defines.get(0);
		assertEquals("content", define.getName());
		assertEquals(RNCAssignType.INTERLEAVE, define.getAssignType());
	}

	@Test
	public void testFindDefine() {
		RNCDocument doc = parse("a = element a { text }\nb = element b { text }");
		assertNotNull(doc.findDefine("a"));
		assertNotNull(doc.findDefine("b"));
		assertNull(doc.findDefine("c"));
	}

	// --- Element patterns ---

	@Test
	public void testElementPattern() {
		RNCDocument doc = parse("element root { text }");
		List<RNCNode> children = doc.getChildren();
		assertEquals(1, children.size());
		assertTrue(children.get(0) instanceof RNCElement);

		RNCElement element = (RNCElement) children.get(0);
		assertEquals("root", element.getName());
		assertTrue(element.isClosed());
	}

	@Test
	public void testNestedElements() {
		RNCDocument doc = parse("element root {\n  element child { text }\n}");
		List<RNCNode> children = doc.getChildren();
		assertEquals(1, children.size());

		RNCElement root = (RNCElement) children.get(0);
		assertEquals("root", root.getName());
		assertTrue(root.isClosed());

		List<RNCNode> rootChildren = root.getChildren();
		assertEquals(1, rootChildren.size());
		assertTrue(rootChildren.get(0) instanceof RNCElement);

		RNCElement child = (RNCElement) rootChildren.get(0);
		assertEquals("child", child.getName());
		assertTrue(child.isClosed());
	}

	@Test
	public void testElementWithAttribute() {
		RNCDocument doc = parse("element root {\n  attribute id { text },\n  text\n}");
		List<RNCNode> children = doc.getChildren();
		assertEquals(1, children.size());

		RNCElement root = (RNCElement) children.get(0);
		assertEquals("root", root.getName());

		List<RNCNode> rootChildren = root.getChildren();
		assertEquals(1, rootChildren.size());
		assertTrue(rootChildren.get(0) instanceof RNCAttribute);

		RNCAttribute attr = (RNCAttribute) rootChildren.get(0);
		assertEquals("id", attr.getName());
	}

	// --- Include ---

	@Test
	public void testInclude() {
		RNCDocument doc = parse("include \"other.rnc\"");
		List<RNCInclude> includes = doc.getIncludes();
		assertEquals(1, includes.size());

		RNCInclude include = includes.get(0);
		assertEquals("other.rnc", include.getUri());
		assertFalse(include.hasBody());
		assertTrue(include.isClosed());
	}

	@Test
	public void testIncludeWithBody() {
		RNCDocument doc = parse("include \"other.rnc\" {\n  start = foo\n}");
		List<RNCInclude> includes = doc.getIncludes();
		assertEquals(1, includes.size());

		RNCInclude include = includes.get(0);
		assertEquals("other.rnc", include.getUri());
		assertTrue(include.hasBody());
		assertTrue(include.isClosed());
	}

	// --- Div ---

	@Test
	public void testDiv() {
		RNCDocument doc = parse("div {\n  a = element a { text }\n}");
		List<RNCNode> children = doc.getChildren();
		assertEquals(1, children.size());
		assertTrue(children.get(0) instanceof RNCDiv);

		RNCDiv div = (RNCDiv) children.get(0);
		assertTrue(div.isClosed());
	}

	// --- Full schema ---

	@Test
	public void testFullSchema() {
		String schema = "default namespace = \"http://example.com\"\n"
				+ "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"\n"
				+ "\n"
				+ "start = root\n"
				+ "\n"
				+ "root = element root {\n"
				+ "  attribute id { text },\n"
				+ "  name+\n"
				+ "}\n"
				+ "\n"
				+ "name = element name { text }";

		RNCDocument doc = parse(schema);

		// Namespace declaration
		assertEquals(1, doc.getNamespaceDecls().size());
		assertTrue(doc.getNamespaceDecls().get(0).isDefault());
		assertEquals("http://example.com", doc.getNamespaceDecls().get(0).getUri());

		// Datatypes declaration
		assertEquals(1, doc.getDatatypesDecls().size());
		assertEquals("xsd", doc.getDatatypesDecls().get(0).getPrefix());

		// Start
		assertNotNull(doc.getStartDecl());

		// Defines
		List<RNCDefine> defines = doc.getDefines();
		assertTrue(defines.size() >= 2);
		assertNotNull(doc.findDefine("root"));
		assertNotNull(doc.findDefine("name"));
	}

	// --- Fault tolerance ---

	@Test
	public void testEmptyDocument() {
		RNCDocument doc = parse("");
		assertTrue(doc.getChildren().isEmpty());
	}

	@Test
	public void testUnclosedElement() {
		RNCDocument doc = parse("element root {");
		List<RNCNode> children = doc.getChildren();
		assertEquals(1, children.size());
		assertTrue(children.get(0) instanceof RNCElement);

		RNCElement element = (RNCElement) children.get(0);
		assertEquals("root", element.getName());
		// Element should not be closed since '}' is missing
		assertFalse(element.isClosed());
	}

	@Test
	public void testPartialNamespace() {
		RNCDocument doc = parse("namespace foo =");
		List<RNCNamespaceDecl> nsDecls = doc.getNamespaceDecls();
		assertEquals(1, nsDecls.size());
		assertEquals("foo", nsDecls.get(0).getPrefix());
		assertNull(nsDecls.get(0).getUri());
	}

	@Test
	public void testPartialDefine() {
		RNCDocument doc = parse("myPattern =");
		List<RNCDefine> defines = doc.getDefines();
		assertEquals(1, defines.size());
		assertEquals("myPattern", defines.get(0).getName());
	}

	@Test
	public void testCommentsAreSkipped() {
		RNCDocument doc = parse("# This is a comment\nstart = root");
		RNCStart start = doc.getStartDecl();
		assertNotNull(start);
	}

	@Test
	public void testDocCommentBeforeDefine() {
		RNCDocument doc = parse("## The root element\nroot = element root { text }");
		List<RNCDefine> defines = doc.getDefines();
		assertEquals(1, defines.size());
		assertEquals("root", defines.get(0).getName());
	}

	// --- Grammar block ---

	@Test
	public void testGrammarBlock() {
		RNCDocument doc = parse("grammar {\n  start = text\n}");
		assertNotNull(doc.getStartDecl());
		assertTrue(doc.getErrors().isEmpty());
	}

	@Test
	public void testGrammarBlockWithDefines() {
		RNCDocument doc = parse("grammar {\n  start = root\n  root = element root { text }\n}");
		assertNotNull(doc.getStartDecl());
		assertNotNull(doc.findDefine("root"));
		assertTrue(doc.getErrors().isEmpty());
	}

	// --- Div block ---

	@Test
	public void testDivWithDefines() {
		RNCDocument doc = parse("start = root\ndiv {\n  root = element root { text }\n  child = element child { text }\n}");
		List<RNCNode> children = doc.getChildren();
		boolean hasDiv = false;
		for (RNCNode child : children) {
			if (child instanceof RNCDiv) {
				hasDiv = true;
				assertTrue(child.isClosed());
			}
		}
		assertTrue(hasDiv);
		assertTrue(doc.getErrors().isEmpty());
	}

	// --- Include with override body ---

	@Test
	public void testIncludeWithOverrideDefine() {
		RNCDocument doc = parse("include \"base.rnc\" {\n  start = element doc { text }\n}");
		List<RNCInclude> includes = doc.getIncludes();
		assertEquals(1, includes.size());

		RNCInclude include = includes.get(0);
		assertEquals("base.rnc", include.getUri());
		assertTrue(include.hasBody());
		assertTrue(include.isClosed());
	}

	// --- Element with attribute and content ---

	@Test
	public void testElementWithMultipleAttributes() {
		RNCDocument doc = parse("element img {\n  attribute src { text },\n  attribute alt { text }?,\n  empty\n}");
		List<RNCNode> children = doc.getChildren();
		assertEquals(1, children.size());

		RNCElement element = (RNCElement) children.get(0);
		assertEquals("img", element.getName());
		assertTrue(element.isClosed());

		List<RNCNode> elementChildren = element.getChildren();
		assertEquals(2, elementChildren.size());
		assertTrue(elementChildren.get(0) instanceof RNCAttribute);
		assertTrue(elementChildren.get(1) instanceof RNCAttribute);
		assertEquals("src", ((RNCAttribute) elementChildren.get(0)).getName());
		assertEquals("alt", ((RNCAttribute) elementChildren.get(1)).getName());
	}

	// --- Nested element and attribute ---

	@Test
	public void testDeeplyNestedElements() {
		RNCDocument doc = parse("element a {\n  element b {\n    element c { text }\n  }\n}");
		RNCElement a = (RNCElement) doc.getChildren().get(0);
		assertEquals("a", a.getName());
		assertTrue(a.isClosed());

		RNCElement b = (RNCElement) a.getChildren().get(0);
		assertEquals("b", b.getName());
		assertTrue(b.isClosed());

		RNCElement c = (RNCElement) b.getChildren().get(0);
		assertEquals("c", c.getName());
		assertTrue(c.isClosed());
		assertTrue(doc.getErrors().isEmpty());
	}

	// --- External reference ---

	@Test
	public void testExternal() {
		RNCDocument doc = parse("start = external \"other.rnc\"");
		List<RNCExternal> externals = doc.getExternals();
		assertEquals(1, externals.size());
		assertEquals("other.rnc", externals.get(0).getUri());
		assertTrue(doc.getErrors().isEmpty());
	}

	@Test
	public void testExternalWithInherit() {
		RNCDocument doc = parse("start = external \"other.rnc\" inherit = ns");
		List<RNCExternal> externals = doc.getExternals();
		assertEquals(1, externals.size());
		assertEquals("other.rnc", externals.get(0).getUri());
		assertTrue(doc.getErrors().isEmpty());
	}

	// --- notAllowed pattern ---

	@Test
	public void testNotAllowed() {
		RNCDocument doc = parse("start = notAllowed");
		assertNotNull(doc.getStartDecl());
		assertTrue(doc.getErrors().isEmpty());
	}

	// --- Multiple namespace and datatypes declarations ---

	@Test
	public void testMultipleNamespaceAndDatatypes() {
		String schema = "namespace a = \"http://a.com\"\n"
				+ "namespace b = \"http://b.com\"\n"
				+ "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"\n"
				+ "start = element root { text }";
		RNCDocument doc = parse(schema);
		assertEquals(2, doc.getNamespaceDecls().size());
		assertEquals(1, doc.getDatatypesDecls().size());
		assertNotNull(doc.getStartDecl());
		assertTrue(doc.getErrors().isEmpty());
	}

	// --- Escaped identifiers ---

	@Test
	public void testEscapedIdentifier() {
		RNCDocument doc = parse("\\element = element foo { text }");
		List<RNCDefine> defines = doc.getDefines();
		assertEquals(1, defines.size());
		assertTrue(doc.getErrors().isEmpty());
	}

	// --- Fault tolerance: more cases ---

	@Test
	public void testPartialInclude() {
		RNCDocument doc = parse("include");
		List<RNCInclude> includes = doc.getIncludes();
		assertEquals(1, includes.size());
		assertNull(includes.get(0).getUri());
		assertFalse(doc.getErrors().isEmpty());
	}

	@Test
	public void testPartialExternal() {
		RNCDocument doc = parse("start = external");
		List<RNCExternal> externals = doc.getExternals();
		assertEquals(1, externals.size());
		assertNull(externals.get(0).getUri());
		assertFalse(doc.getErrors().isEmpty());
	}

	@Test
	public void testUnclosedDiv() {
		RNCDocument doc = parse("div {\n  a = element a { text }");
		assertFalse(doc.getErrors().isEmpty());
	}

	@Test
	public void testUnclosedIncludeBody() {
		RNCDocument doc = parse("include \"base.rnc\" {\n  start = foo");
		List<RNCInclude> includes = doc.getIncludes();
		assertEquals(1, includes.size());
		assertTrue(includes.get(0).hasBody());
		assertFalse(includes.get(0).isClosed());
		assertFalse(doc.getErrors().isEmpty());
	}

	@Test
	public void testStartWithElementPattern() {
		RNCDocument doc = parse("start = element root {\n  attribute id { text },\n  element child { text }*\n}");
		assertNotNull(doc.getStartDecl());
		assertTrue(doc.getErrors().isEmpty());
	}

	@Test
	public void testMultipleDefinesAndStart() {
		String schema = "start = root\n"
				+ "root = element root { head, body }\n"
				+ "head = element head { title }\n"
				+ "title = element title { text }\n"
				+ "body = element body { text }";
		RNCDocument doc = parse(schema);
		assertNotNull(doc.getStartDecl());
		assertEquals(4, doc.getDefines().size());
		assertTrue(doc.getErrors().isEmpty());
	}

	@Test
	public void testCommentOnlyDocument() {
		RNCDocument doc = parse("# Just a comment\n# Another comment");
		assertTrue(doc.getChildren().isEmpty());
		assertTrue(doc.getErrors().isEmpty());
	}

	@Test
	public void testDocCommentOnly() {
		RNCDocument doc = parse("## Documentation only");
		assertTrue(doc.getErrors().isEmpty());
	}

	// --- Anonymous brace blocks: each '}' matches its own '{' ---

	@Test
	public void testMixedBlockInsideElement() {
		String text = "element a { mixed { text } }";
		RNCDocument doc = parse(text);
		assertEquals(1, doc.getChildren().size());

		RNCElement a = (RNCElement) doc.getChildren().get(0);
		assertEquals("a", a.getName());
		assertTrue(a.isClosed());
		// the last '}' closes 'a', not the one of 'mixed'
		assertEquals(text.length(), a.getEnd());
		assertTrue(a.getChildren().isEmpty());
		assertTrue(doc.getErrors().isEmpty());
	}

	@Test
	public void testElementAfterMixedBlockStaysInParent() {
		RNCDocument doc = parse("element a { mixed { text }, element b { text } }");
		assertEquals(1, doc.getChildren().size());

		RNCElement a = (RNCElement) doc.getChildren().get(0);
		assertTrue(a.isClosed());
		assertEquals(1, a.getChildren().size());

		RNCElement b = (RNCElement) a.getChildren().get(0);
		assertEquals("b", b.getName());
		assertTrue(b.isClosed());
		assertSame(a, b.getParent());
		assertTrue(doc.getErrors().isEmpty());
	}

	@Test
	public void testListBlockInsideAttribute() {
		RNCDocument doc = parse("element a { attribute x { list { text } }, element b { text } }");
		assertEquals(1, doc.getChildren().size());

		RNCElement a = (RNCElement) doc.getChildren().get(0);
		assertTrue(a.isClosed());
		assertEquals(2, a.getChildren().size());
		assertTrue(a.getChildren().get(0) instanceof RNCAttribute);
		assertTrue(a.getChildren().get(0).isClosed());
		assertTrue(a.getChildren().get(1) instanceof RNCElement);
		assertTrue(doc.getErrors().isEmpty());
	}

	@Test
	public void testNestedAnonymousBlocks() {
		String text = "element a { mixed { list { text } } }";
		RNCDocument doc = parse(text);
		assertEquals(1, doc.getChildren().size());

		RNCElement a = (RNCElement) doc.getChildren().get(0);
		assertTrue(a.isClosed());
		assertEquals(text.length(), a.getEnd());
		assertTrue(doc.getErrors().isEmpty());
	}

	@Test
	public void testGrammarBlockInsideElement() {
		String text = "element a { grammar { start = text } }";
		RNCDocument doc = parse(text);
		assertEquals(1, doc.getChildren().size());

		RNCElement a = (RNCElement) doc.getChildren().get(0);
		assertTrue(a.isClosed());
		assertEquals(text.length(), a.getEnd());
		assertTrue(doc.getErrors().isEmpty());
	}

	@Test
	public void testUnclosedAnonymousBlockAtRoot() {
		RNCDocument doc = parse("grammar {");
		assertEquals(1, doc.getErrors().size());
	}

	@Test
	public void testUnclosedElementAfterClosedMixedBlock() {
		RNCDocument doc = parse("element a { mixed { text }");
		RNCElement a = (RNCElement) doc.getChildren().get(0);
		assertFalse(a.isClosed());
		// only 'a' is unclosed, the mixed block is closed
		assertEquals(1, doc.getErrors().size());
	}

	@Test
	public void testUnclosedElementAndUnclosedMixedBlock() {
		RNCDocument doc = parse("element a { mixed { text");
		RNCElement a = (RNCElement) doc.getChildren().get(0);
		assertFalse(a.isClosed());
		// one error for 'a', one for the 'mixed' block
		assertEquals(2, doc.getErrors().size());
	}

	@Test
	public void testExtraClosingBraceAtRootIsIgnored() {
		RNCDocument doc = parse("element a { text } }");
		assertEquals(1, doc.getChildren().size());
		assertTrue(doc.getChildren().get(0).isClosed());
	}

	@Test
	public void testParenthesizedNameClassKeepsBracesBalanced() {
		// the name class is not supported yet (reports an error),
		// but its braces must not close or open any other node
		RNCDocument doc = parse("element (a|b) { text }\nx = element x { text }");
		assertNotNull(doc.findDefine("x"));
	}

	// --- Defines inside anonymous blocks ---

	@Test
	public void testDatatypeParametersInsideElementAreNotDefines() {
		RNCDocument doc = parse("element a { xsd:string { maxLength = \"5\" } }");
		assertEquals(1, doc.getChildren().size());

		RNCElement a = (RNCElement) doc.getChildren().get(0);
		assertTrue(a.isClosed());
		assertTrue(a.getChildren().isEmpty());
		assertTrue(doc.getErrors().isEmpty());
	}

	@Test
	public void testDatatypeParametersAtRootAreNotDefines() {
		RNCDocument doc = parse("t = xsd:string { maxLength = \"5\" }\nu = element u { text }");
		assertEquals(2, doc.getDefines().size());
		assertNotNull(doc.findDefine("t"));
		assertNotNull(doc.findDefine("u"));
		assertNull(doc.findDefine("maxLength"));
		assertTrue(doc.getErrors().isEmpty());
	}

	@Test
	public void testDefinesInsideGrammarBlockInsideElementAreKept() {
		RNCDocument doc = parse("element a { grammar { b = text } }");
		RNCElement a = (RNCElement) doc.getChildren().get(0);
		assertTrue(a.isClosed());
		assertEquals(1, a.getChildren().size());
		assertTrue(a.getChildren().get(0) instanceof RNCDefine);
	}

	@Test
	public void testDefineInsideNestedGrammarAtRoot() {
		RNCDocument doc = parse("x = grammar {\n  start = a\n  a = element a { text }\n}");
		assertNotNull(doc.findDefine("x"));
		assertNotNull(doc.findDefine("a"));
		assertTrue(doc.getErrors().isEmpty());
	}

	// --- An unexpected token must not be swallowed ---

	@Test
	public void testElementMissingOpenBraceKeepsNextToken() {
		RNCDocument doc = parse("x = element a\ny = element b { text }");
		assertNotNull(doc.findDefine("x"));
		assertNotNull(doc.findDefine("y"));
		assertFalse(doc.getErrors().isEmpty());
	}

	@Test
	public void testElementMissingNameKeepsClosingBrace() {
		RNCDocument doc = parse("element a { element }");
		RNCElement a = (RNCElement) doc.getChildren().get(0);
		// the '}' after the incomplete 'element' closes 'a'
		assertTrue(a.isClosed());
		assertEquals(1, doc.getErrors().size());
	}

	@Test
	public void testAttributeMissingNameKeepsClosingBrace() {
		RNCDocument doc = parse("element a { attribute }");
		RNCElement a = (RNCElement) doc.getChildren().get(0);
		assertTrue(a.isClosed());
		assertEquals(1, doc.getErrors().size());
	}

	@Test
	public void testStartMissingAssignKeepsNextToken() {
		RNCDocument doc = parse("start element root { text }");
		assertFalse(doc.getErrors().isEmpty());
		boolean hasElement = false;
		for (RNCNode child : doc.getChildren()) {
			if (child instanceof RNCElement) {
				hasElement = true;
			}
		}
		assertTrue(hasElement);
	}

	@Test
	public void testDivMissingBraceKeepsNextToken() {
		RNCDocument doc = parse("div\nx = text");
		assertFalse(doc.getErrors().isEmpty());
		assertNotNull(doc.findDefine("x"));
	}

	@Test
	public void testExternalMissingUriKeepsNextToken() {
		RNCDocument doc = parse("start = external\nx = text");
		assertFalse(doc.getErrors().isEmpty());
		assertNotNull(doc.findDefine("x"));
	}

	@Test
	public void testNamespaceMissingUriKeepsNextToken() {
		RNCDocument doc = parse("namespace foo =\nx = text");
		assertFalse(doc.getErrors().isEmpty());
		assertNotNull(doc.findDefine("x"));
	}

	@Test
	public void testDatatypesMissingUriKeepsNextToken() {
		RNCDocument doc = parse("datatypes xsd =\nx = text");
		assertFalse(doc.getErrors().isEmpty());
		assertNotNull(doc.findDefine("x"));
	}

	@Test
	public void testIncludeMissingUriKeepsNextToken() {
		RNCDocument doc = parse("include\nx = text");
		assertFalse(doc.getErrors().isEmpty());
		assertNotNull(doc.findDefine("x"));
	}

	@Test
	public void testElementNameWithTrailingColonKeepsBrace() {
		RNCDocument doc = parse("element a: { text }");
		RNCElement a = (RNCElement) doc.getChildren().get(0);
		assertEquals("a", a.getName());
		assertTrue(a.isClosed());
	}

	@Test
	public void testCommentBetweenElementNameAndBrace() {
		RNCDocument doc = parse("element a # comment\n{ text }");
		RNCElement a = (RNCElement) doc.getChildren().get(0);
		assertTrue(a.isClosed());
		assertTrue(doc.getErrors().isEmpty());
	}

	// --- default namespace ---

	@Test
	public void testDefaultNamespaceStartsAtDefaultKeyword() {
		RNCDocument doc = parse("default namespace = \"http://example.com\"");
		assertEquals(0, doc.getNamespaceDecls().get(0).getStart());
	}

	@Test
	public void testDefaultNamespaceWithCommentBetweenKeywords() {
		RNCDocument doc = parse("default # comment\nnamespace = \"http://example.com\"");
		assertEquals(1, doc.getNamespaceDecls().size());
		assertTrue(doc.getNamespaceDecls().get(0).isDefault());
		assertEquals(0, doc.getNamespaceDecls().get(0).getStart());
	}

	@Test
	public void testDefaultNotFollowedByNamespaceKeepsNextToken() {
		RNCDocument doc = parse("default\nx = text");
		assertTrue(doc.getNamespaceDecls().isEmpty());
		assertNotNull(doc.findDefine("x"));
	}

	// --- include body detection ---

	@Test
	public void testIncludeWithoutBodyFollowedByDefine() {
		String include = "include \"a.rnc\"";
		RNCDocument doc = parse(include + "\nx = text");
		assertEquals(1, doc.getIncludes().size());

		RNCInclude inc = doc.getIncludes().get(0);
		assertFalse(inc.hasBody());
		assertTrue(inc.isClosed());
		assertEquals(include.length(), inc.getEnd());
		assertNotNull(doc.findDefine("x"));
		assertTrue(doc.getErrors().isEmpty());
	}

	@Test
	public void testIncludeWithCommentBeforeBody() {
		RNCDocument doc = parse("include \"a.rnc\" # comment\n{ x = text }");
		assertEquals(1, doc.getIncludes().size());
		assertTrue(doc.getIncludes().get(0).hasBody());
		assertTrue(doc.getIncludes().get(0).isClosed());
		assertTrue(doc.getErrors().isEmpty());
	}

	@Test
	public void testIncludeWithBodyInsideDiv() {
		RNCDocument doc = parse("div { include \"a.rnc\" { x = text } }");
		assertEquals(1, doc.getChildren().size());

		RNCDiv div = (RNCDiv) doc.getChildren().get(0);
		assertTrue(div.isClosed());
		assertEquals(1, div.getChildren().size());

		RNCInclude include = (RNCInclude) div.getChildren().get(0);
		assertTrue(include.hasBody());
		assertTrue(include.isClosed());
		assertTrue(doc.getErrors().isEmpty());
	}

	// --- hasNoContent ---

	@Test
	public void testHasNoContentEmptyString() {
		RNCDocument doc = parse("");
		assertTrue(doc.hasNoContent());
	}

	@Test
	public void testHasNoContentWhitespaceOnly() {
		RNCDocument doc = parse("   \n\t  \n  ");
		assertTrue(doc.hasNoContent());
	}

	@Test
	public void testHasNoContentCommentsOnly() {
		RNCDocument doc = parse("# This is a comment\n# Another comment\n");
		assertTrue(doc.hasNoContent());
	}

	@Test
	public void testHasNoContentDocCommentOnly() {
		RNCDocument doc = parse("## Documentation comment\n");
		assertTrue(doc.hasNoContent());
	}

	@Test
	public void testHasNoContentWhitespaceAndComments() {
		RNCDocument doc = parse("  \n# comment\n  \n## doc\n  ");
		assertTrue(doc.hasNoContent());
	}

	@Test
	public void testHasNoContentWithStart() {
		RNCDocument doc = parse("start = element root { text }");
		assertFalse(doc.hasNoContent());
	}

	@Test
	public void testHasNoContentWithNamespace() {
		RNCDocument doc = parse("namespace ns = \"http://example.com\"");
		assertFalse(doc.hasNoContent());
	}

	@Test
	public void testHasNoContentWithDatatypes() {
		RNCDocument doc = parse("datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"");
		assertFalse(doc.hasNoContent());
	}

	@Test
	public void testHasNoContentWithDefine() {
		RNCDocument doc = parse("root = element root { text }");
		assertFalse(doc.hasNoContent());
	}

	@Test
	public void testHasNoContentWithInclude() {
		RNCDocument doc = parse("include \"other.rnc\"");
		assertFalse(doc.hasNoContent());
	}

	// --- Helper ---

	private RNCDocument parse(String text) {
		return RNCParser.getInstance().parse(text, "test.rnc");
	}
}