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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class RNCScannerTest {

	private RNCScanner scanner;

	// --- Keywords ---

	@Test
	public void testKeywordElement() {
		scanner = RNCScanner.createScanner("element");
		assertOffsetAndToken(0, RNCTokenType.KeywordElement, "element");
		assertOffsetAndToken(7, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordAttribute() {
		scanner = RNCScanner.createScanner("attribute");
		assertOffsetAndToken(0, RNCTokenType.KeywordAttribute, "attribute");
		assertOffsetAndToken(9, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordText() {
		scanner = RNCScanner.createScanner("text");
		assertOffsetAndToken(0, RNCTokenType.KeywordText, "text");
		assertOffsetAndToken(4, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordEmpty() {
		scanner = RNCScanner.createScanner("empty");
		assertOffsetAndToken(0, RNCTokenType.KeywordEmpty, "empty");
		assertOffsetAndToken(5, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordNotAllowed() {
		scanner = RNCScanner.createScanner("notAllowed");
		assertOffsetAndToken(0, RNCTokenType.KeywordNotAllowed, "notAllowed");
		assertOffsetAndToken(10, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordStart() {
		scanner = RNCScanner.createScanner("start");
		assertOffsetAndToken(0, RNCTokenType.KeywordStart, "start");
		assertOffsetAndToken(5, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordInclude() {
		scanner = RNCScanner.createScanner("include");
		assertOffsetAndToken(0, RNCTokenType.KeywordInclude, "include");
		assertOffsetAndToken(7, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordNamespace() {
		scanner = RNCScanner.createScanner("namespace");
		assertOffsetAndToken(0, RNCTokenType.KeywordNamespace, "namespace");
		assertOffsetAndToken(9, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordDefault() {
		scanner = RNCScanner.createScanner("default");
		assertOffsetAndToken(0, RNCTokenType.KeywordDefault, "default");
		assertOffsetAndToken(7, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordDatatypes() {
		scanner = RNCScanner.createScanner("datatypes");
		assertOffsetAndToken(0, RNCTokenType.KeywordDatatypes, "datatypes");
		assertOffsetAndToken(9, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordList() {
		scanner = RNCScanner.createScanner("list");
		assertOffsetAndToken(0, RNCTokenType.KeywordList, "list");
		assertOffsetAndToken(4, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordMixed() {
		scanner = RNCScanner.createScanner("mixed");
		assertOffsetAndToken(0, RNCTokenType.KeywordMixed, "mixed");
		assertOffsetAndToken(5, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordExternal() {
		scanner = RNCScanner.createScanner("external");
		assertOffsetAndToken(0, RNCTokenType.KeywordExternal, "external");
		assertOffsetAndToken(8, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordGrammar() {
		scanner = RNCScanner.createScanner("grammar");
		assertOffsetAndToken(0, RNCTokenType.KeywordGrammar, "grammar");
		assertOffsetAndToken(7, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordDiv() {
		scanner = RNCScanner.createScanner("div");
		assertOffsetAndToken(0, RNCTokenType.KeywordDiv, "div");
		assertOffsetAndToken(3, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordParent() {
		scanner = RNCScanner.createScanner("parent");
		assertOffsetAndToken(0, RNCTokenType.KeywordParent, "parent");
		assertOffsetAndToken(6, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordInherit() {
		scanner = RNCScanner.createScanner("inherit");
		assertOffsetAndToken(0, RNCTokenType.KeywordInherit, "inherit");
		assertOffsetAndToken(7, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordString() {
		scanner = RNCScanner.createScanner("string");
		assertOffsetAndToken(0, RNCTokenType.KeywordString, "string");
		assertOffsetAndToken(6, RNCTokenType.EOS);
	}

	@Test
	public void testKeywordToken() {
		scanner = RNCScanner.createScanner("token");
		assertOffsetAndToken(0, RNCTokenType.KeywordToken, "token");
		assertOffsetAndToken(5, RNCTokenType.EOS);
	}

	// --- Identifiers ---

	@Test
	public void testIdentifier() {
		scanner = RNCScanner.createScanner("myName");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "myName");
		assertOffsetAndToken(6, RNCTokenType.EOS);
	}

	@Test
	public void testIdentifierWithDash() {
		scanner = RNCScanner.createScanner("my-name");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "my-name");
		assertOffsetAndToken(7, RNCTokenType.EOS);
	}

	@Test
	public void testIdentifierWithDot() {
		scanner = RNCScanner.createScanner("my.name");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "my.name");
		assertOffsetAndToken(7, RNCTokenType.EOS);
	}

	@Test
	public void testIdentifierWithUnderscore() {
		scanner = RNCScanner.createScanner("_myName");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "_myName");
		assertOffsetAndToken(7, RNCTokenType.EOS);
	}

	@Test
	public void testIdentifierWithDigits() {
		scanner = RNCScanner.createScanner("name123");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "name123");
		assertOffsetAndToken(7, RNCTokenType.EOS);
	}

	@Test
	public void testEscapedKeyword() {
		scanner = RNCScanner.createScanner("\\element");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "\\element");
		assertOffsetAndToken(8, RNCTokenType.EOS);
	}

	@Test
	public void testEscapedKeywordText() {
		scanner = RNCScanner.createScanner("\\text");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "\\text");
		assertOffsetAndToken(5, RNCTokenType.EOS);
	}

	// --- Literals ---

	@Test
	public void testDoubleQuotedLiteral() {
		scanner = RNCScanner.createScanner("\"hello\"");
		assertOffsetAndToken(0, RNCTokenType.Literal, "\"hello\"");
		assertOffsetAndToken(7, RNCTokenType.EOS);
	}

	@Test
	public void testSingleQuotedLiteral() {
		scanner = RNCScanner.createScanner("'hello'");
		assertOffsetAndToken(0, RNCTokenType.Literal, "'hello'");
		assertOffsetAndToken(7, RNCTokenType.EOS);
	}

	@Test
	public void testEmptyDoubleQuotedLiteral() {
		scanner = RNCScanner.createScanner("\"\"");
		assertOffsetAndToken(0, RNCTokenType.Literal, "\"\"");
		assertOffsetAndToken(2, RNCTokenType.EOS);
	}

	@Test
	public void testEmptySingleQuotedLiteral() {
		scanner = RNCScanner.createScanner("''");
		assertOffsetAndToken(0, RNCTokenType.Literal, "''");
		assertOffsetAndToken(2, RNCTokenType.EOS);
	}

	@Test
	public void testTripleQuotedLiteral() {
		scanner = RNCScanner.createScanner("\"\"\"hello\nworld\"\"\"");
		assertOffsetAndToken(0, RNCTokenType.Literal, "\"\"\"hello\nworld\"\"\"");
		assertOffsetAndToken(17, RNCTokenType.EOS);
	}

	@Test
	public void testTripleSingleQuotedLiteral() {
		scanner = RNCScanner.createScanner("'''hello\nworld'''");
		assertOffsetAndToken(0, RNCTokenType.Literal, "'''hello\nworld'''");
		assertOffsetAndToken(17, RNCTokenType.EOS);
	}

	@Test
	public void testUnterminatedLiteral() {
		scanner = RNCScanner.createScanner("\"hello");
		assertOffsetAndToken(0, RNCTokenType.Literal, "\"hello");
		assertOffsetAndToken(6, RNCTokenType.EOS);
	}

	@Test
	public void testUnterminatedLiteralAtNewline() {
		scanner = RNCScanner.createScanner("\"hello\nworld\"");
		assertOffsetAndToken(0, RNCTokenType.Literal, "\"hello");
		assertOffsetAndToken(6, RNCTokenType.Whitespace, "\n");
		assertOffsetAndToken(7, RNCTokenType.Identifier, "world");
		assertOffsetAndToken(12, RNCTokenType.Literal, "\"");
		assertOffsetAndToken(13, RNCTokenType.EOS);
	}

	@Test
	public void testLiteralWithUri() {
		scanner = RNCScanner.createScanner("\"http://www.w3.org/2001/XMLSchema-datatypes\"");
		assertOffsetAndToken(0, RNCTokenType.Literal, "\"http://www.w3.org/2001/XMLSchema-datatypes\"");
		assertOffsetAndToken(44, RNCTokenType.EOS);
	}

	// --- Comments ---

	@Test
	public void testComment() {
		scanner = RNCScanner.createScanner("# this is a comment");
		assertOffsetAndToken(0, RNCTokenType.Comment, "# this is a comment");
		assertOffsetAndToken(19, RNCTokenType.EOS);
	}

	@Test
	public void testCommentWithNewline() {
		scanner = RNCScanner.createScanner("# comment\nstart");
		assertOffsetAndToken(0, RNCTokenType.Comment, "# comment");
		assertOffsetAndToken(9, RNCTokenType.Whitespace, "\n");
		assertOffsetAndToken(10, RNCTokenType.KeywordStart, "start");
		assertOffsetAndToken(15, RNCTokenType.EOS);
	}

	@Test
	public void testDocumentationComment() {
		scanner = RNCScanner.createScanner("## documentation");
		assertOffsetAndToken(0, RNCTokenType.DocumentationComment, "## documentation");
		assertOffsetAndToken(16, RNCTokenType.EOS);
	}

	@Test
	public void testDocumentationCommentWithNewline() {
		scanner = RNCScanner.createScanner("## doc\nelement");
		assertOffsetAndToken(0, RNCTokenType.DocumentationComment, "## doc");
		assertOffsetAndToken(6, RNCTokenType.Whitespace, "\n");
		assertOffsetAndToken(7, RNCTokenType.KeywordElement, "element");
		assertOffsetAndToken(14, RNCTokenType.EOS);
	}

	// --- Operators ---

	@Test
	public void testAssign() {
		scanner = RNCScanner.createScanner("=");
		assertOffsetAndToken(0, RNCTokenType.Assign, "=");
		assertOffsetAndToken(1, RNCTokenType.EOS);
	}

	@Test
	public void testAssignChoice() {
		scanner = RNCScanner.createScanner("|=");
		assertOffsetAndToken(0, RNCTokenType.AssignChoice, "|=");
		assertOffsetAndToken(2, RNCTokenType.EOS);
	}

	@Test
	public void testAssignInterleave() {
		scanner = RNCScanner.createScanner("&=");
		assertOffsetAndToken(0, RNCTokenType.AssignInterleave, "&=");
		assertOffsetAndToken(2, RNCTokenType.EOS);
	}

	@Test
	public void testPipe() {
		scanner = RNCScanner.createScanner("|");
		assertOffsetAndToken(0, RNCTokenType.Pipe, "|");
		assertOffsetAndToken(1, RNCTokenType.EOS);
	}

	@Test
	public void testAmpersand() {
		scanner = RNCScanner.createScanner("&");
		assertOffsetAndToken(0, RNCTokenType.Ampersand, "&");
		assertOffsetAndToken(1, RNCTokenType.EOS);
	}

	@Test
	public void testComma() {
		scanner = RNCScanner.createScanner(",");
		assertOffsetAndToken(0, RNCTokenType.Comma, ",");
		assertOffsetAndToken(1, RNCTokenType.EOS);
	}

	@Test
	public void testQuestionMark() {
		scanner = RNCScanner.createScanner("?");
		assertOffsetAndToken(0, RNCTokenType.QuestionMark, "?");
		assertOffsetAndToken(1, RNCTokenType.EOS);
	}

	@Test
	public void testStar() {
		scanner = RNCScanner.createScanner("*");
		assertOffsetAndToken(0, RNCTokenType.Star, "*");
		assertOffsetAndToken(1, RNCTokenType.EOS);
	}

	@Test
	public void testPlus() {
		scanner = RNCScanner.createScanner("+");
		assertOffsetAndToken(0, RNCTokenType.Plus, "+");
		assertOffsetAndToken(1, RNCTokenType.EOS);
	}

	@Test
	public void testMinus() {
		scanner = RNCScanner.createScanner("-");
		assertOffsetAndToken(0, RNCTokenType.Minus, "-");
		assertOffsetAndToken(1, RNCTokenType.EOS);
	}

	@Test
	public void testTilde() {
		scanner = RNCScanner.createScanner("~");
		assertOffsetAndToken(0, RNCTokenType.Tilde, "~");
		assertOffsetAndToken(1, RNCTokenType.EOS);
	}

	// --- Delimiters ---

	@Test
	public void testBraces() {
		scanner = RNCScanner.createScanner("{}");
		assertOffsetAndToken(0, RNCTokenType.BraceOpen, "{");
		assertOffsetAndToken(1, RNCTokenType.BraceClose, "}");
		assertOffsetAndToken(2, RNCTokenType.EOS);
	}

	@Test
	public void testParens() {
		scanner = RNCScanner.createScanner("()");
		assertOffsetAndToken(0, RNCTokenType.ParenOpen, "(");
		assertOffsetAndToken(1, RNCTokenType.ParenClose, ")");
		assertOffsetAndToken(2, RNCTokenType.EOS);
	}

	@Test
	public void testBrackets() {
		scanner = RNCScanner.createScanner("[]");
		assertOffsetAndToken(0, RNCTokenType.BracketOpen, "[");
		assertOffsetAndToken(1, RNCTokenType.BracketClose, "]");
		assertOffsetAndToken(2, RNCTokenType.EOS);
	}

	@Test
	public void testColon() {
		scanner = RNCScanner.createScanner(":");
		assertOffsetAndToken(0, RNCTokenType.Colon, ":");
		assertOffsetAndToken(1, RNCTokenType.EOS);
	}

	// --- Whitespace ---

	@Test
	public void testWhitespace() {
		scanner = RNCScanner.createScanner("  \t\n  ");
		assertOffsetAndToken(0, RNCTokenType.Whitespace, "  \t\n  ");
		assertOffsetAndToken(6, RNCTokenType.EOS);
	}

	// --- Combined expressions ---

	@Test
	public void testNamespaceDecl() {
		scanner = RNCScanner.createScanner("namespace foo = \"http://example.com\"");
		assertOffsetAndToken(0, RNCTokenType.KeywordNamespace, "namespace");
		assertOffsetAndToken(9, RNCTokenType.Whitespace);
		assertOffsetAndToken(10, RNCTokenType.Identifier, "foo");
		assertOffsetAndToken(13, RNCTokenType.Whitespace);
		assertOffsetAndToken(14, RNCTokenType.Assign);
		assertOffsetAndToken(15, RNCTokenType.Whitespace);
		assertOffsetAndToken(16, RNCTokenType.Literal, "\"http://example.com\"");
		assertOffsetAndToken(36, RNCTokenType.EOS);
	}

	@Test
	public void testDefaultNamespaceDecl() {
		scanner = RNCScanner.createScanner("default namespace = \"http://example.com\"");
		assertOffsetAndToken(0, RNCTokenType.KeywordDefault, "default");
		assertOffsetAndToken(7, RNCTokenType.Whitespace);
		assertOffsetAndToken(8, RNCTokenType.KeywordNamespace, "namespace");
		assertOffsetAndToken(17, RNCTokenType.Whitespace);
		assertOffsetAndToken(18, RNCTokenType.Assign);
		assertOffsetAndToken(19, RNCTokenType.Whitespace);
		assertOffsetAndToken(20, RNCTokenType.Literal, "\"http://example.com\"");
		assertOffsetAndToken(40, RNCTokenType.EOS);
	}

	@Test
	public void testDatatypesDecl() {
		scanner = RNCScanner.createScanner("datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"");
		assertOffsetAndToken(0, RNCTokenType.KeywordDatatypes, "datatypes");
		assertOffsetAndToken(9, RNCTokenType.Whitespace);
		assertOffsetAndToken(10, RNCTokenType.Identifier, "xsd");
		assertOffsetAndToken(13, RNCTokenType.Whitespace);
		assertOffsetAndToken(14, RNCTokenType.Assign);
		assertOffsetAndToken(15, RNCTokenType.Whitespace);
		assertOffsetAndToken(16, RNCTokenType.Literal, "\"http://www.w3.org/2001/XMLSchema-datatypes\"");
		assertOffsetAndToken(60, RNCTokenType.EOS);
	}

	@Test
	public void testStartPattern() {
		scanner = RNCScanner.createScanner("start = root");
		assertOffsetAndToken(0, RNCTokenType.KeywordStart, "start");
		assertOffsetAndToken(5, RNCTokenType.Whitespace);
		assertOffsetAndToken(6, RNCTokenType.Assign);
		assertOffsetAndToken(7, RNCTokenType.Whitespace);
		assertOffsetAndToken(8, RNCTokenType.Identifier, "root");
		assertOffsetAndToken(12, RNCTokenType.EOS);
	}

	@Test
	public void testSimpleElement() {
		scanner = RNCScanner.createScanner("element name { text }");
		assertOffsetAndToken(0, RNCTokenType.KeywordElement, "element");
		assertOffsetAndToken(7, RNCTokenType.Whitespace);
		assertOffsetAndToken(8, RNCTokenType.Identifier, "name");
		assertOffsetAndToken(12, RNCTokenType.Whitespace);
		assertOffsetAndToken(13, RNCTokenType.BraceOpen);
		assertOffsetAndToken(14, RNCTokenType.Whitespace);
		assertOffsetAndToken(15, RNCTokenType.KeywordText, "text");
		assertOffsetAndToken(19, RNCTokenType.Whitespace);
		assertOffsetAndToken(20, RNCTokenType.BraceClose);
		assertOffsetAndToken(21, RNCTokenType.EOS);
	}

	@Test
	public void testElementWithAttribute() {
		scanner = RNCScanner.createScanner("element root {\n  attribute id { text },\n  text\n}");
		assertOffsetAndToken(0, RNCTokenType.KeywordElement, "element");
		assertOffsetAndToken(7, RNCTokenType.Whitespace);
		assertOffsetAndToken(8, RNCTokenType.Identifier, "root");
		assertOffsetAndToken(12, RNCTokenType.Whitespace);
		assertOffsetAndToken(13, RNCTokenType.BraceOpen);
		assertOffsetAndToken(14, RNCTokenType.Whitespace);
		assertOffsetAndToken(17, RNCTokenType.KeywordAttribute, "attribute");
		assertOffsetAndToken(26, RNCTokenType.Whitespace);
		assertOffsetAndToken(27, RNCTokenType.Identifier, "id");
		assertOffsetAndToken(29, RNCTokenType.Whitespace);
		assertOffsetAndToken(30, RNCTokenType.BraceOpen);
		assertOffsetAndToken(31, RNCTokenType.Whitespace);
		assertOffsetAndToken(32, RNCTokenType.KeywordText, "text");
		assertOffsetAndToken(36, RNCTokenType.Whitespace);
		assertOffsetAndToken(37, RNCTokenType.BraceClose);
		assertOffsetAndToken(38, RNCTokenType.Comma);
		assertOffsetAndToken(39, RNCTokenType.Whitespace);
		assertOffsetAndToken(42, RNCTokenType.KeywordText, "text");
		assertOffsetAndToken(46, RNCTokenType.Whitespace);
		assertOffsetAndToken(47, RNCTokenType.BraceClose);
		assertOffsetAndToken(48, RNCTokenType.EOS);
	}

	@Test
	public void testDefineWithAssign() {
		scanner = RNCScanner.createScanner("name = element name { text }");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "name");
		assertOffsetAndToken(4, RNCTokenType.Whitespace);
		assertOffsetAndToken(5, RNCTokenType.Assign);
		assertOffsetAndToken(6, RNCTokenType.Whitespace);
		assertOffsetAndToken(7, RNCTokenType.KeywordElement, "element");
		assertOffsetAndToken(14, RNCTokenType.Whitespace);
		assertOffsetAndToken(15, RNCTokenType.Identifier, "name");
		assertOffsetAndToken(19, RNCTokenType.Whitespace);
		assertOffsetAndToken(20, RNCTokenType.BraceOpen);
		assertOffsetAndToken(21, RNCTokenType.Whitespace);
		assertOffsetAndToken(22, RNCTokenType.KeywordText, "text");
		assertOffsetAndToken(26, RNCTokenType.Whitespace);
		assertOffsetAndToken(27, RNCTokenType.BraceClose);
		assertOffsetAndToken(28, RNCTokenType.EOS);
	}

	@Test
	public void testDefineWithChoiceAssign() {
		scanner = RNCScanner.createScanner("content |= element img { empty }");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "content");
		assertOffsetAndToken(7, RNCTokenType.Whitespace);
		assertOffsetAndToken(8, RNCTokenType.AssignChoice, "|=");
		assertOffsetAndToken(10, RNCTokenType.Whitespace);
		assertOffsetAndToken(11, RNCTokenType.KeywordElement, "element");
		assertOffsetAndToken(18, RNCTokenType.Whitespace);
		assertOffsetAndToken(19, RNCTokenType.Identifier, "img");
		assertOffsetAndToken(22, RNCTokenType.Whitespace);
		assertOffsetAndToken(23, RNCTokenType.BraceOpen);
		assertOffsetAndToken(24, RNCTokenType.Whitespace);
		assertOffsetAndToken(25, RNCTokenType.KeywordEmpty, "empty");
		assertOffsetAndToken(30, RNCTokenType.Whitespace);
		assertOffsetAndToken(31, RNCTokenType.BraceClose);
		assertOffsetAndToken(32, RNCTokenType.EOS);
	}

	@Test
	public void testChoicePattern() {
		scanner = RNCScanner.createScanner("\"high\" | \"low\"");
		assertOffsetAndToken(0, RNCTokenType.Literal, "\"high\"");
		assertOffsetAndToken(6, RNCTokenType.Whitespace);
		assertOffsetAndToken(7, RNCTokenType.Pipe);
		assertOffsetAndToken(8, RNCTokenType.Whitespace);
		assertOffsetAndToken(9, RNCTokenType.Literal, "\"low\"");
		assertOffsetAndToken(14, RNCTokenType.EOS);
	}

	@Test
	public void testOptionalPattern() {
		scanner = RNCScanner.createScanner("name?");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "name");
		assertOffsetAndToken(4, RNCTokenType.QuestionMark);
		assertOffsetAndToken(5, RNCTokenType.EOS);
	}

	@Test
	public void testZeroOrMorePattern() {
		scanner = RNCScanner.createScanner("name*");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "name");
		assertOffsetAndToken(4, RNCTokenType.Star);
		assertOffsetAndToken(5, RNCTokenType.EOS);
	}

	@Test
	public void testOneOrMorePattern() {
		scanner = RNCScanner.createScanner("name+");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "name");
		assertOffsetAndToken(4, RNCTokenType.Plus);
		assertOffsetAndToken(5, RNCTokenType.EOS);
	}

	@Test
	public void testGroupedChoice() {
		scanner = RNCScanner.createScanner("(a | b)+");
		assertOffsetAndToken(0, RNCTokenType.ParenOpen);
		assertOffsetAndToken(1, RNCTokenType.Identifier, "a");
		assertOffsetAndToken(2, RNCTokenType.Whitespace);
		assertOffsetAndToken(3, RNCTokenType.Pipe);
		assertOffsetAndToken(4, RNCTokenType.Whitespace);
		assertOffsetAndToken(5, RNCTokenType.Identifier, "b");
		assertOffsetAndToken(6, RNCTokenType.ParenClose);
		assertOffsetAndToken(7, RNCTokenType.Plus);
		assertOffsetAndToken(8, RNCTokenType.EOS);
	}

	@Test
	public void testNamespacePrefix() {
		scanner = RNCScanner.createScanner("xsd:integer");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "xsd");
		assertOffsetAndToken(3, RNCTokenType.Colon);
		assertOffsetAndToken(4, RNCTokenType.Identifier, "integer");
		assertOffsetAndToken(11, RNCTokenType.EOS);
	}

	@Test
	public void testIncludeDecl() {
		scanner = RNCScanner.createScanner("include \"other.rnc\"");
		assertOffsetAndToken(0, RNCTokenType.KeywordInclude, "include");
		assertOffsetAndToken(7, RNCTokenType.Whitespace);
		assertOffsetAndToken(8, RNCTokenType.Literal, "\"other.rnc\"");
		assertOffsetAndToken(19, RNCTokenType.EOS);
	}

	@Test
	public void testIncludeWithOverrides() {
		scanner = RNCScanner.createScanner("include \"other.rnc\" {\n  start = foo\n}");
		assertOffsetAndToken(0, RNCTokenType.KeywordInclude, "include");
		assertOffsetAndToken(7, RNCTokenType.Whitespace);
		assertOffsetAndToken(8, RNCTokenType.Literal, "\"other.rnc\"");
		assertOffsetAndToken(19, RNCTokenType.Whitespace);
		assertOffsetAndToken(20, RNCTokenType.BraceOpen);
		assertOffsetAndToken(21, RNCTokenType.Whitespace);
		assertOffsetAndToken(24, RNCTokenType.KeywordStart, "start");
		assertOffsetAndToken(29, RNCTokenType.Whitespace);
		assertOffsetAndToken(30, RNCTokenType.Assign);
		assertOffsetAndToken(31, RNCTokenType.Whitespace);
		assertOffsetAndToken(32, RNCTokenType.Identifier, "foo");
		assertOffsetAndToken(35, RNCTokenType.Whitespace);
		assertOffsetAndToken(36, RNCTokenType.BraceClose);
		assertOffsetAndToken(37, RNCTokenType.EOS);
	}

	@Test
	public void testAnnotation() {
		scanner = RNCScanner.createScanner("[a:documentation]");
		assertOffsetAndToken(0, RNCTokenType.BracketOpen);
		assertOffsetAndToken(1, RNCTokenType.Identifier, "a");
		assertOffsetAndToken(2, RNCTokenType.Colon);
		assertOffsetAndToken(3, RNCTokenType.Identifier, "documentation");
		assertOffsetAndToken(16, RNCTokenType.BracketClose);
		assertOffsetAndToken(17, RNCTokenType.EOS);
	}

	@Test
	public void testMixedPattern() {
		scanner = RNCScanner.createScanner("mixed { text }");
		assertOffsetAndToken(0, RNCTokenType.KeywordMixed, "mixed");
		assertOffsetAndToken(5, RNCTokenType.Whitespace);
		assertOffsetAndToken(6, RNCTokenType.BraceOpen);
		assertOffsetAndToken(7, RNCTokenType.Whitespace);
		assertOffsetAndToken(8, RNCTokenType.KeywordText, "text");
		assertOffsetAndToken(12, RNCTokenType.Whitespace);
		assertOffsetAndToken(13, RNCTokenType.BraceClose);
		assertOffsetAndToken(14, RNCTokenType.EOS);
	}

	@Test
	public void testListPattern() {
		scanner = RNCScanner.createScanner("list { xsd:integer+ }");
		assertOffsetAndToken(0, RNCTokenType.KeywordList, "list");
		assertOffsetAndToken(4, RNCTokenType.Whitespace);
		assertOffsetAndToken(5, RNCTokenType.BraceOpen);
		assertOffsetAndToken(6, RNCTokenType.Whitespace);
		assertOffsetAndToken(7, RNCTokenType.Identifier, "xsd");
		assertOffsetAndToken(10, RNCTokenType.Colon);
		assertOffsetAndToken(11, RNCTokenType.Identifier, "integer");
		assertOffsetAndToken(18, RNCTokenType.Plus);
		assertOffsetAndToken(19, RNCTokenType.Whitespace);
		assertOffsetAndToken(20, RNCTokenType.BraceClose);
		assertOffsetAndToken(21, RNCTokenType.EOS);
	}

	@Test
	public void testExternalRef() {
		scanner = RNCScanner.createScanner("external \"foo.rnc\"");
		assertOffsetAndToken(0, RNCTokenType.KeywordExternal, "external");
		assertOffsetAndToken(8, RNCTokenType.Whitespace);
		assertOffsetAndToken(9, RNCTokenType.Literal, "\"foo.rnc\"");
		assertOffsetAndToken(18, RNCTokenType.EOS);
	}

	@Test
	public void testInterleaveOperator() {
		scanner = RNCScanner.createScanner("a & b");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "a");
		assertOffsetAndToken(1, RNCTokenType.Whitespace);
		assertOffsetAndToken(2, RNCTokenType.Ampersand);
		assertOffsetAndToken(3, RNCTokenType.Whitespace);
		assertOffsetAndToken(4, RNCTokenType.Identifier, "b");
		assertOffsetAndToken(5, RNCTokenType.EOS);
	}

	@Test
	public void testSequenceOperator() {
		scanner = RNCScanner.createScanner("a, b, c");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "a");
		assertOffsetAndToken(1, RNCTokenType.Comma);
		assertOffsetAndToken(2, RNCTokenType.Whitespace);
		assertOffsetAndToken(3, RNCTokenType.Identifier, "b");
		assertOffsetAndToken(4, RNCTokenType.Comma);
		assertOffsetAndToken(5, RNCTokenType.Whitespace);
		assertOffsetAndToken(6, RNCTokenType.Identifier, "c");
		assertOffsetAndToken(7, RNCTokenType.EOS);
	}

	@Test
	public void testDatatypeWithParams() {
		scanner = RNCScanner.createScanner("xsd:string { maxLength = \"255\" }");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "xsd");
		assertOffsetAndToken(3, RNCTokenType.Colon);
		assertOffsetAndToken(4, RNCTokenType.KeywordString, "string");
		assertOffsetAndToken(10, RNCTokenType.Whitespace);
		assertOffsetAndToken(11, RNCTokenType.BraceOpen);
		assertOffsetAndToken(12, RNCTokenType.Whitespace);
		assertOffsetAndToken(13, RNCTokenType.Identifier, "maxLength");
		assertOffsetAndToken(22, RNCTokenType.Whitespace);
		assertOffsetAndToken(23, RNCTokenType.Assign);
		assertOffsetAndToken(24, RNCTokenType.Whitespace);
		assertOffsetAndToken(25, RNCTokenType.Literal, "\"255\"");
		assertOffsetAndToken(30, RNCTokenType.Whitespace);
		assertOffsetAndToken(31, RNCTokenType.BraceClose);
		assertOffsetAndToken(32, RNCTokenType.EOS);
	}

	// --- Full schema example ---

	@Test
	public void testFullSchema() {
		String schema = "default namespace = \"http://example.com\"\n"
				+ "\n"
				+ "start = root\n"
				+ "\n"
				+ "root = element root {\n"
				+ "  attribute id { text },\n"
				+ "  name+\n"
				+ "}\n"
				+ "\n"
				+ "name = element name { text }";

		scanner = RNCScanner.createScanner(schema);

		// default namespace = "http://example.com"
		assertOffsetAndToken(0, RNCTokenType.KeywordDefault, "default");
		assertOffsetAndToken(7, RNCTokenType.Whitespace);
		assertOffsetAndToken(8, RNCTokenType.KeywordNamespace, "namespace");
		assertOffsetAndToken(17, RNCTokenType.Whitespace);
		assertOffsetAndToken(18, RNCTokenType.Assign);
		assertOffsetAndToken(19, RNCTokenType.Whitespace);
		assertOffsetAndToken(20, RNCTokenType.Literal, "\"http://example.com\"");
		assertOffsetAndToken(40, RNCTokenType.Whitespace); // \n\n

		// start = root
		assertOffsetAndToken(42, RNCTokenType.KeywordStart, "start");
		assertOffsetAndToken(47, RNCTokenType.Whitespace);
		assertOffsetAndToken(48, RNCTokenType.Assign);
		assertOffsetAndToken(49, RNCTokenType.Whitespace);
		assertOffsetAndToken(50, RNCTokenType.Identifier, "root");
		assertOffsetAndToken(54, RNCTokenType.Whitespace); // \n\n

		// root = element root {
		assertOffsetAndToken(56, RNCTokenType.Identifier, "root");
		assertOffsetAndToken(60, RNCTokenType.Whitespace);
		assertOffsetAndToken(61, RNCTokenType.Assign);
		assertOffsetAndToken(62, RNCTokenType.Whitespace);
		assertOffsetAndToken(63, RNCTokenType.KeywordElement, "element");
		assertOffsetAndToken(70, RNCTokenType.Whitespace);
		assertOffsetAndToken(71, RNCTokenType.Identifier, "root");
		assertOffsetAndToken(75, RNCTokenType.Whitespace);
		assertOffsetAndToken(76, RNCTokenType.BraceOpen);
		assertOffsetAndToken(77, RNCTokenType.Whitespace);

		//   attribute id { text },
		assertOffsetAndToken(80, RNCTokenType.KeywordAttribute, "attribute");
		assertOffsetAndToken(89, RNCTokenType.Whitespace);
		assertOffsetAndToken(90, RNCTokenType.Identifier, "id");
		assertOffsetAndToken(92, RNCTokenType.Whitespace);
		assertOffsetAndToken(93, RNCTokenType.BraceOpen);
		assertOffsetAndToken(94, RNCTokenType.Whitespace);
		assertOffsetAndToken(95, RNCTokenType.KeywordText, "text");
		assertOffsetAndToken(99, RNCTokenType.Whitespace);
		assertOffsetAndToken(100, RNCTokenType.BraceClose);
		assertOffsetAndToken(101, RNCTokenType.Comma);
		assertOffsetAndToken(102, RNCTokenType.Whitespace);

		//   name+
		assertOffsetAndToken(105, RNCTokenType.Identifier, "name");
		assertOffsetAndToken(109, RNCTokenType.Plus);
		assertOffsetAndToken(110, RNCTokenType.Whitespace);

		// }
		assertOffsetAndToken(111, RNCTokenType.BraceClose);
		assertOffsetAndToken(112, RNCTokenType.Whitespace); // \n\n

		// name = element name { text }
		assertOffsetAndToken(114, RNCTokenType.Identifier, "name");
		assertOffsetAndToken(118, RNCTokenType.Whitespace);
		assertOffsetAndToken(119, RNCTokenType.Assign);
		assertOffsetAndToken(120, RNCTokenType.Whitespace);
		assertOffsetAndToken(121, RNCTokenType.KeywordElement, "element");
		assertOffsetAndToken(128, RNCTokenType.Whitespace);
		assertOffsetAndToken(129, RNCTokenType.Identifier, "name");
		assertOffsetAndToken(133, RNCTokenType.Whitespace);
		assertOffsetAndToken(134, RNCTokenType.BraceOpen);
		assertOffsetAndToken(135, RNCTokenType.Whitespace);
		assertOffsetAndToken(136, RNCTokenType.KeywordText, "text");
		assertOffsetAndToken(140, RNCTokenType.Whitespace);
		assertOffsetAndToken(141, RNCTokenType.BraceClose);
		assertOffsetAndToken(142, RNCTokenType.EOS);
	}

	// --- Fault tolerance tests ---

	@Test
	public void testEmptyInput() {
		scanner = RNCScanner.createScanner("");
		assertOffsetAndToken(0, RNCTokenType.EOS);
	}

	@Test
	public void testBackslashAlone() {
		scanner = RNCScanner.createScanner("\\");
		assertOffsetAndToken(0, RNCTokenType.Backslash, "\\");
		assertOffsetAndToken(1, RNCTokenType.EOS);
	}

	@Test
	public void testUnknownChar() {
		scanner = RNCScanner.createScanner(";");
		assertOffsetAndToken(0, RNCTokenType.Unknown, ";");
		assertOffsetAndToken(1, RNCTokenType.EOS);
	}

	@Test
	public void testUnterminatedTripleLiteral() {
		scanner = RNCScanner.createScanner("\"\"\"hello");
		assertOffsetAndToken(0, RNCTokenType.Literal, "\"\"\"hello");
		assertOffsetAndToken(8, RNCTokenType.EOS);
	}

	@Test
	public void testScanFromOffset() {
		scanner = RNCScanner.createScanner("  element foo", 2);
		assertOffsetAndToken(2, RNCTokenType.KeywordElement, "element");
		assertOffsetAndToken(9, RNCTokenType.Whitespace);
		assertOffsetAndToken(10, RNCTokenType.Identifier, "foo");
		assertOffsetAndToken(13, RNCTokenType.EOS);
	}

	@Test
	public void testDocCommentBeforeElement() {
		scanner = RNCScanner.createScanner("## The root element\nelement root { text }");
		assertOffsetAndToken(0, RNCTokenType.DocumentationComment, "## The root element");
		assertOffsetAndToken(19, RNCTokenType.Whitespace);
		assertOffsetAndToken(20, RNCTokenType.KeywordElement, "element");
		assertOffsetAndToken(27, RNCTokenType.Whitespace);
		assertOffsetAndToken(28, RNCTokenType.Identifier, "root");
		assertOffsetAndToken(32, RNCTokenType.Whitespace);
		assertOffsetAndToken(33, RNCTokenType.BraceOpen);
		assertOffsetAndToken(34, RNCTokenType.Whitespace);
		assertOffsetAndToken(35, RNCTokenType.KeywordText, "text");
		assertOffsetAndToken(39, RNCTokenType.Whitespace);
		assertOffsetAndToken(40, RNCTokenType.BraceClose);
		assertOffsetAndToken(41, RNCTokenType.EOS);
	}

	// --- DatatypeWithParams but "string" is standalone keyword ---

	@Test
	public void testStandaloneStringKeyword() {
		scanner = RNCScanner.createScanner("string");
		assertOffsetAndToken(0, RNCTokenType.KeywordString, "string");
		assertOffsetAndToken(6, RNCTokenType.EOS);
	}

	@Test
	public void testStandaloneTokenKeyword() {
		scanner = RNCScanner.createScanner("token");
		assertOffsetAndToken(0, RNCTokenType.KeywordToken, "token");
		assertOffsetAndToken(5, RNCTokenType.EOS);
	}

	// --- Grammar construct tests ---

	@Test
	public void testNsName() {
		scanner = RNCScanner.createScanner("foo:*");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "foo");
		assertOffsetAndToken(3, RNCTokenType.Colon);
		assertOffsetAndToken(4, RNCTokenType.Star);
		assertOffsetAndToken(5, RNCTokenType.EOS);
	}

	@Test
	public void testLiteralConcatenation() {
		scanner = RNCScanner.createScanner("\"foo\" ~ \"bar\"");
		assertOffsetAndToken(0, RNCTokenType.Literal, "\"foo\"");
		assertOffsetAndToken(5, RNCTokenType.Whitespace);
		assertOffsetAndToken(6, RNCTokenType.Tilde);
		assertOffsetAndToken(7, RNCTokenType.Whitespace);
		assertOffsetAndToken(8, RNCTokenType.Literal, "\"bar\"");
		assertOffsetAndToken(13, RNCTokenType.EOS);
	}

	@Test
	public void testAssignInterleaveInContext() {
		scanner = RNCScanner.createScanner("content &= element img { empty }");
		assertOffsetAndToken(0, RNCTokenType.Identifier, "content");
		assertOffsetAndToken(7, RNCTokenType.Whitespace);
		assertOffsetAndToken(8, RNCTokenType.AssignInterleave);
		assertOffsetAndToken(10, RNCTokenType.Whitespace);
		assertOffsetAndToken(11, RNCTokenType.KeywordElement, "element");
		assertOffsetAndToken(18, RNCTokenType.Whitespace);
		assertOffsetAndToken(19, RNCTokenType.Identifier, "img");
		assertOffsetAndToken(22, RNCTokenType.Whitespace);
		assertOffsetAndToken(23, RNCTokenType.BraceOpen);
		assertOffsetAndToken(24, RNCTokenType.Whitespace);
		assertOffsetAndToken(25, RNCTokenType.KeywordEmpty, "empty");
		assertOffsetAndToken(30, RNCTokenType.Whitespace);
		assertOffsetAndToken(31, RNCTokenType.BraceClose);
		assertOffsetAndToken(32, RNCTokenType.EOS);
	}

	@Test
	public void testGrammarBlock() {
		scanner = RNCScanner.createScanner("grammar { start = text }");
		assertOffsetAndToken(0, RNCTokenType.KeywordGrammar, "grammar");
		assertOffsetAndToken(7, RNCTokenType.Whitespace);
		assertOffsetAndToken(8, RNCTokenType.BraceOpen);
		assertOffsetAndToken(9, RNCTokenType.Whitespace);
		assertOffsetAndToken(10, RNCTokenType.KeywordStart, "start");
		assertOffsetAndToken(15, RNCTokenType.Whitespace);
		assertOffsetAndToken(16, RNCTokenType.Assign);
		assertOffsetAndToken(17, RNCTokenType.Whitespace);
		assertOffsetAndToken(18, RNCTokenType.KeywordText, "text");
		assertOffsetAndToken(22, RNCTokenType.Whitespace);
		assertOffsetAndToken(23, RNCTokenType.BraceClose);
		assertOffsetAndToken(24, RNCTokenType.EOS);
	}

	@Test
	public void testParentRef() {
		scanner = RNCScanner.createScanner("parent foo");
		assertOffsetAndToken(0, RNCTokenType.KeywordParent, "parent");
		assertOffsetAndToken(6, RNCTokenType.Whitespace);
		assertOffsetAndToken(7, RNCTokenType.Identifier, "foo");
		assertOffsetAndToken(10, RNCTokenType.EOS);
	}

	@Test
	public void testExternalWithInherit() {
		scanner = RNCScanner.createScanner("external \"foo.rnc\" inherit = ns");
		assertOffsetAndToken(0, RNCTokenType.KeywordExternal, "external");
		assertOffsetAndToken(8, RNCTokenType.Whitespace);
		assertOffsetAndToken(9, RNCTokenType.Literal, "\"foo.rnc\"");
		assertOffsetAndToken(18, RNCTokenType.Whitespace);
		assertOffsetAndToken(19, RNCTokenType.KeywordInherit, "inherit");
		assertOffsetAndToken(26, RNCTokenType.Whitespace);
		assertOffsetAndToken(27, RNCTokenType.Assign);
		assertOffsetAndToken(28, RNCTokenType.Whitespace);
		assertOffsetAndToken(29, RNCTokenType.Identifier, "ns");
		assertOffsetAndToken(31, RNCTokenType.EOS);
	}

	@Test
	public void testDatatypeValue() {
		scanner = RNCScanner.createScanner("string \"fixed\"");
		assertOffsetAndToken(0, RNCTokenType.KeywordString, "string");
		assertOffsetAndToken(6, RNCTokenType.Whitespace);
		assertOffsetAndToken(7, RNCTokenType.Literal, "\"fixed\"");
		assertOffsetAndToken(14, RNCTokenType.EOS);
	}

	@Test
	public void testExceptPattern() {
		scanner = RNCScanner.createScanner("* - foo");
		assertOffsetAndToken(0, RNCTokenType.Star);
		assertOffsetAndToken(1, RNCTokenType.Whitespace);
		assertOffsetAndToken(2, RNCTokenType.Minus);
		assertOffsetAndToken(3, RNCTokenType.Whitespace);
		assertOffsetAndToken(4, RNCTokenType.Identifier, "foo");
		assertOffsetAndToken(7, RNCTokenType.EOS);
	}

	// --- Helper methods ---

	private void assertOffsetAndToken(int expectedOffset, RNCTokenType expectedType) {
		RNCTokenType token = scanner.scan();
		assertEquals(expectedOffset, scanner.getTokenOffset(),
				"Token offset for " + expectedType + " (got " + token + ")");
		assertEquals(expectedType, token,
				"Token type at offset " + expectedOffset);
	}

	private void assertOffsetAndToken(int expectedOffset, RNCTokenType expectedType, String expectedText) {
		RNCTokenType token = scanner.scan();
		assertEquals(expectedOffset, scanner.getTokenOffset(),
				"Token offset for " + expectedType + " (got " + token + ")");
		assertEquals(expectedType, token,
				"Token type at offset " + expectedOffset);
		assertEquals(expectedText, scanner.getTokenText(),
				"Token text at offset " + expectedOffset);
	}
}
