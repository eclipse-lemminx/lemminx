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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

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
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCTextDocumentService;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.validator.RNCErrorCode;

/**
 * Parser for RNC (RELAX NG Compact Syntax) files that produces an {@link RNCDocument} AST.
 *
 * <p>The parser uses the {@link RNCScanner} to tokenize the input and builds a tree
 * of {@link RNCNode} subclasses representing the document structure: namespace
 * declarations, datatypes declarations, start patterns, defines, elements,
 * attributes, includes, and divs.</p>
 *
 * <p>Use case: called by {@link RNCTextDocumentService} on every document open/change
 * to produce the parsed model that all language features operate on.</p>
 *
 * <p>Example: parsing the text {@code start = element root { text }} produces:</p>
 * <pre>
 * RNCDocument
 *   ├─ RNCStart (assignType=ASSIGN)
 *   └─ RNCElement (name="root")
 * </pre>
 *
 * <p>This parser also detects structural errors (unclosed blocks, missing names,
 * missing braces, missing assign operators, missing URIs) and records them as
 * {@link RNCDocument.RNCError} entries for diagnostics.</p>
 */
public class RNCParser {

	private static final RNCParser INSTANCE = new RNCParser();

	/**
	 * Returns the singleton parser instance.
	 *
	 * @return the parser instance.
	 */
	public static RNCParser getInstance() {
		return INSTANCE;
	}

	private RNCParser() {
	}

	/**
	 * Parses RNC source text into an {@link RNCDocument} AST.
	 *
	 * <p>The parser scans tokens and builds a tree by tracking the current parent
	 * node ({@code curr}). When a block-opening construct is encountered (element,
	 * attribute, include, div with an opening brace), {@code curr} descends into
	 * the new node. When the matching closing brace is found, {@code curr} ascends
	 * back to the parent.</p>
	 *
	 * <p>Braces that do not belong to a node (<code>mixed &#123; &#125;</code>,
	 * <code>list &#123; &#125;</code>, <code>grammar &#123; &#125;</code>, datatype
	 * parameters, malformed names) are tracked on a stack of anonymous blocks, so
	 * that each closing brace is matched with its own opening brace.</p>
	 *
	 * <p>Example usage:</p>
	 * <pre>
	 * RNCDocument doc = RNCParser.getInstance().parse("start = element root { text }", "test.rnc");
	 * List&lt;RNCDefine&gt; defines = doc.getDefines(); // empty - no defines, just inline
	 * RNCStart start = doc.getStartDecl(); // RNCStart node
	 * </pre>
	 *
	 * @param text the RNC source text to parse.
	 * @param uri  the document URI (stored in the resulting document).
	 * @return the parsed {@link RNCDocument} with all AST nodes and errors.
	 */
	public RNCDocument parse(CharSequence text, String uri) {
		TokenReader scanner = new TokenReader(RNCScanner.createScanner(text));
		RNCDocument document = new RNCDocument(text, uri);

		RNCNode curr = document;
		RNCTokenType token = scanner.scan();

		// Track a pending identifier that might be a define name.
		// When we see Identifier followed by = / |= / &=, it's a define.
		// This avoids peek-ahead that would consume the next token.
		int pendingIdentStart = -1;
		int pendingIdentEnd = -1;

		// Open '{' that don't belong to a node (mixed, list, grammar, datatype
		// params, malformed names...). The most recent one is on top.
		Deque<AnonymousBlock> openBlocks = new ArrayDeque<>();
		// true when the previous meaningful token was the 'grammar' keyword
		boolean grammarKeywordSeen = false;

		while (token != RNCTokenType.EOS) {
			if (token != RNCTokenType.Whitespace && token != RNCTokenType.Comment
					&& token != RNCTokenType.DocumentationComment
					&& token != RNCTokenType.KeywordGrammar
					&& token != RNCTokenType.BraceOpen) {
				grammarKeywordSeen = false;
			}

			// When we see a meaningful (non-whitespace) token that is NOT
			// Identifier or Assign, clear the pending identifier — it was
			// just a reference, not a define declaration.
			if (token != RNCTokenType.Whitespace && token != RNCTokenType.Comment
					&& token != RNCTokenType.DocumentationComment
					&& token != RNCTokenType.Identifier
					&& token != RNCTokenType.Assign && token != RNCTokenType.AssignChoice
					&& token != RNCTokenType.AssignInterleave) {
				if (pendingIdentStart >= 0
						&& (curr == document || curr instanceof RNCDiv)
						&& !isInNonGrammarBlock(openBlocks, curr)
						&& isPatternKeyword(token)) {
					document.addError(RNCErrorCode.MissingAssign,
							"Expected '=' after define name", pendingIdentStart, pendingIdentEnd);
					RNCDefine define = new RNCDefine(pendingIdentStart, document.getSource().length());
					define.setName(pendingIdentStart, pendingIdentEnd);
					define.setEnd(pendingIdentEnd);
					define.setClosed(true);
					addToParent(curr, document, define);
				}
				pendingIdentStart = -1;
			}

			switch (token) {
				case KeywordDefault: {
					int defaultStart = scanner.getTokenOffset();
					token = skipWhitespace(scanner);
					if (token == RNCTokenType.KeywordNamespace) {
						curr = parseNamespaceDecl(scanner, document, curr, defaultStart, true);
						token = scanner.scan();
					}
					// otherwise 'token' is the token following 'default': process it
					// as usual instead of swallowing it.
					continue;
				}

				case KeywordNamespace: {
					curr = parseNamespaceDecl(scanner, document, curr, scanner.getTokenOffset(), false);
					token = scanner.scan();
					continue;
				}

				case KeywordDatatypes: {
					curr = parseDatatypesDecl(scanner, document, curr);
					token = scanner.scan();
					continue;
				}

				case KeywordStart: {
					curr = parseStart(scanner, document, curr);
					token = scanner.scan();
					continue;
				}

				case KeywordInclude: {
					curr = parseInclude(scanner, document, curr);
					token = scanner.scan();
					continue;
				}

				case KeywordExternal: {
					parseExternal(scanner, document, curr);
					token = scanner.scan();
					continue;
				}

				case KeywordDiv: {
					curr = parseDiv(scanner, document, curr);
					token = scanner.scan();
					continue;
				}

				case Identifier: {
					pendingIdentStart = scanner.getTokenOffset();
					pendingIdentEnd = scanner.getTokenEnd();
					break;
				}

				case Assign:
				case AssignChoice:
				case AssignInterleave: {
					if (pendingIdentStart >= 0) {
						// Inside datatype parameters / list / mixed, 'name = value'
						// is not a define.
						if (!isInNonGrammarBlock(openBlocks, curr)) {
							RNCDefine define = new RNCDefine(pendingIdentStart, document.getSource().length());
							define.setName(pendingIdentStart, pendingIdentEnd);
							if (token == RNCTokenType.AssignChoice) {
								define.setAssignType(RNCAssignType.CHOICE);
							} else if (token == RNCTokenType.AssignInterleave) {
								define.setAssignType(RNCAssignType.INTERLEAVE);
							}
							define.setEnd(scanner.getTokenEnd());
							define.setClosed(true);
							addToParent(curr, document, define);
						}
						pendingIdentStart = -1;
					}
					break;
				}

				case KeywordElement: {
					curr = parseElement(scanner, document, curr);
					token = scanner.scan();
					continue;
				}

				case KeywordAttribute: {
					curr = parseAttribute(scanner, document, curr);
					token = scanner.scan();
					continue;
				}

				case KeywordGrammar: {
					grammarKeywordSeen = true;
					break;
				}

				case BraceOpen: {
					// Opening brace of an anonymous block: the braces of element,
					// attribute, include and div are consumed by their parseXxx method.
					openBlocks.push(new AnonymousBlock(scanner.getTokenOffset(), grammarKeywordSeen, curr));
					grammarKeywordSeen = false;
					break;
				}

				case BraceClose: {
					AnonymousBlock top = openBlocks.peek();
					if (top != null && top.owner == curr) {
						// Matches an anonymous '{', not the body of curr.
						openBlocks.pop();
						break;
					}
					if (curr != document) {
						curr.setEnd(scanner.getTokenEnd());
						curr.setClosed(true);
						if (curr instanceof RNCElement) {
							((RNCElement) curr).setBodyCloseOffset(scanner.getTokenOffset());
						} else if (curr instanceof RNCAttribute) {
							((RNCAttribute) curr).setBodyCloseOffset(scanner.getTokenOffset());
						} else if (curr instanceof RNCInclude) {
							((RNCInclude) curr).setBodyCloseOffset(scanner.getTokenOffset());
						} else if (curr instanceof RNCDiv) {
							((RNCDiv) curr).setBodyCloseOffset(scanner.getTokenOffset());
						}
						curr = curr.getParent() != null ? curr.getParent() : document;
					}
					break;
				}

				case DocumentationComment:
				case Comment:
				case Whitespace:
				case Comma:
				case Pipe:
				case Ampersand:
				case QuestionMark:
				case Star:
				case Plus:
				case Minus:
				case ParenOpen:
				case ParenClose:
					break;

				case BracketOpen: {
					int depth = 1;
					while (depth > 0) {
						token = scanner.scan();
						if (token == RNCTokenType.EOS) {
							break;
						}
						if (token == RNCTokenType.BracketOpen) {
							depth++;
						} else if (token == RNCTokenType.BracketClose) {
							depth--;
						}
					}
					break;
				}
				case BracketClose:
				case Literal:
				case Colon:
				case Tilde:
				case Backslash:
				case KeywordText:
				case KeywordEmpty:
				case KeywordNotAllowed:
				case KeywordList:
				case KeywordMixed:
				case KeywordParent:
				case KeywordInherit:
				case KeywordString:
				case KeywordToken:
				case Unknown:
					break;

				default:
					break;
			}
			token = scanner.scan();
		}

		// Close any unclosed nodes and report errors
		while (curr != document && curr.getParent() != null) {
			curr.setEnd(text.length());
			int errorStart = getUnclosedBlockErrorStart(curr);
			int errorEnd = getUnclosedBlockErrorEnd(curr);
			document.addError(RNCErrorCode.UnclosedBlock, "Missing closing '}'",
					errorStart, errorEnd);
			curr = curr.getParent();
		}

		// Report unclosed anonymous blocks, in source order
		Iterator<AnonymousBlock> unclosed = openBlocks.descendingIterator();
		while (unclosed.hasNext()) {
			AnonymousBlock block = unclosed.next();
			document.addError(RNCErrorCode.UnclosedBlock, "Missing closing '}'",
					block.offset, block.offset + 1);
		}

		return document;
	}

	private static int getUnclosedBlockErrorStart(RNCNode node) {
		if (node instanceof RNCElement) {
			RNCElement el = (RNCElement) node;
			if (el.getNameStart() != -1) {
				return el.getNameStart();
			}
		} else if (node instanceof RNCAttribute) {
			RNCAttribute attr = (RNCAttribute) node;
			if (attr.getNameStart() != -1) {
				return attr.getNameStart();
			}
		}
		return node.getStart();
	}

	private static int getUnclosedBlockErrorEnd(RNCNode node) {
		if (node instanceof RNCElement) {
			RNCElement el = (RNCElement) node;
			if (el.getNameEnd() != -1) {
				return el.getNameEnd();
			}
		} else if (node instanceof RNCAttribute) {
			RNCAttribute attr = (RNCAttribute) node;
			if (attr.getNameEnd() != -1) {
				return attr.getNameEnd();
			}
		}
		return node.getEnd();
	}

	/**
	 * Parses a namespace declaration: {@code namespace prefix = "uri"} or
	 * {@code default namespace = "uri"}.
	 *
	 * <p>Expected token sequence: {@code [prefix] = literal|inherit}</p>
	 *
	 * @param declStart start offset of the declaration (the {@code default}
	 *                  keyword when {@code isDefault} is {@code true}, otherwise
	 *                  the {@code namespace} keyword).
	 * @param isDefault {@code true} if preceded by the {@code default} keyword.
	 */
	private RNCNode parseNamespaceDecl(TokenReader scanner, RNCDocument document, RNCNode curr, int declStart,
			boolean isDefault) {
		int keywordEnd = scanner.getTokenEnd();
		RNCNamespaceDecl nsDecl = new RNCNamespaceDecl(declStart, document.getSource().length());
		nsDecl.setDefault(isDefault);

		RNCTokenType token = skipWhitespace(scanner);

		// Parse prefix (identifier) — required for non-default, optional for default
		if (isNameToken(token)) {
			nsDecl.setPrefix(scanner.getTokenOffset(), scanner.getTokenEnd());
			token = skipWhitespace(scanner);
		} else if (!isDefault && token != RNCTokenType.Assign) {
			document.addError(RNCErrorCode.MissingAssign,
					"Expected prefix identifier after 'namespace'", declStart, keywordEnd);
			nsDecl.setEnd(keywordEnd);
			nsDecl.setClosed(true);
			addToParent(curr, document, nsDecl);
			scanner.pushBack(token);
			return curr;
		}

		// Expect '='
		if (token == RNCTokenType.Assign) {
			token = skipWhitespace(scanner);
		} else {
			int errorEnd = nsDecl.getPrefixEnd() != -1 ? nsDecl.getPrefixEnd() : keywordEnd;
			document.addError(RNCErrorCode.MissingAssign,
					"Expected '=' in namespace declaration", declStart, errorEnd);
			nsDecl.setEnd(errorEnd);
			nsDecl.setClosed(true);
			addToParent(curr, document, nsDecl);
			scanner.pushBack(token);
			return curr;
		}

		// Expect URI literal or inherit
		if (token == RNCTokenType.Literal) {
			nsDecl.setUri(scanner.getTokenOffset(), scanner.getTokenEnd());
			nsDecl.setEnd(scanner.getTokenEnd());
		} else if (token == RNCTokenType.KeywordInherit) {
			nsDecl.setEnd(scanner.getTokenEnd());
		} else {
			document.addError(RNCErrorCode.MissingUri,
					"Expected URI string in namespace declaration", declStart, scanner.getTokenOffset());
			nsDecl.setEnd(scanner.getTokenOffset());
			scanner.pushBack(token);
		}

		nsDecl.setClosed(true);
		addToParent(curr, document, nsDecl);
		return curr;
	}

	/**
	 * Parses a datatypes declaration: {@code datatypes prefix = "uri"}.
	 *
	 * <p>Example: {@code datatypes xsd = "http://www.w3.org/2001/XMLSchema-datatypes"}</p>
	 */
	private RNCNode parseDatatypesDecl(TokenReader scanner, RNCDocument document, RNCNode curr) {
		int declStart = scanner.getTokenOffset();
		int keywordEnd = scanner.getTokenEnd();
		RNCDatatypesDecl dtDecl = new RNCDatatypesDecl(declStart, document.getSource().length());

		RNCTokenType token = skipWhitespace(scanner);

		// Parse prefix
		if (token == RNCTokenType.Identifier) {
			dtDecl.setPrefix(scanner.getTokenOffset(), scanner.getTokenEnd());
			token = skipWhitespace(scanner);
		}

		// Expect '='
		if (token == RNCTokenType.Assign) {
			token = skipWhitespace(scanner);
		} else {
			int errorEnd = dtDecl.getPrefixEnd() != -1 ? dtDecl.getPrefixEnd() : keywordEnd;
			document.addError(RNCErrorCode.MissingAssign,
					"Expected '=' in datatypes declaration", declStart, errorEnd);
			dtDecl.setEnd(errorEnd);
			dtDecl.setClosed(true);
			addToParent(curr, document, dtDecl);
			scanner.pushBack(token);
			return curr;
		}

		// Expect URI literal
		if (token == RNCTokenType.Literal) {
			dtDecl.setUri(scanner.getTokenOffset(), scanner.getTokenEnd());
			dtDecl.setEnd(scanner.getTokenEnd());
		} else {
			document.addError(RNCErrorCode.MissingUri,
					"Expected URI string in datatypes declaration", declStart, scanner.getTokenOffset());
			dtDecl.setEnd(scanner.getTokenOffset());
			scanner.pushBack(token);
		}

		dtDecl.setClosed(true);
		addToParent(curr, document, dtDecl);
		return curr;
	}

	/**
	 * Parses a start pattern: {@code start = pattern} or {@code start |= pattern}.
	 *
	 * <p>Example: {@code start = root} or {@code start |= altRoot}</p>
	 */
	private RNCNode parseStart(TokenReader scanner, RNCDocument document, RNCNode curr) {
		int declStart = scanner.getTokenOffset();
		int keywordEnd = scanner.getTokenEnd();
		RNCStart startNode = new RNCStart(declStart, document.getSource().length());

		RNCTokenType token = skipWhitespace(scanner);

		// Expect '=' or '|=' or '&='
		if (token == RNCTokenType.Assign) {
			startNode.setAssignType(RNCAssignType.ASSIGN);
		} else if (token == RNCTokenType.AssignChoice) {
			startNode.setAssignType(RNCAssignType.CHOICE);
		} else if (token == RNCTokenType.AssignInterleave) {
			startNode.setAssignType(RNCAssignType.INTERLEAVE);
		} else {
			document.addError(RNCErrorCode.MissingAssign,
					"Expected '=' after 'start'", declStart, keywordEnd);
			startNode.setEnd(keywordEnd);
			startNode.setClosed(true);
			addToParent(curr, document, startNode);
			scanner.pushBack(token);
			return curr;
		}

		startNode.setEnd(scanner.getTokenEnd());
		startNode.setClosed(true);
		addToParent(curr, document, startNode);
		return curr;
	}

	/**
	 * Parses an element pattern: {@code element name { pattern }}.
	 * The name may be namespace-prefixed (e.g. {@code ns:localName}).
	 * Returns the element node as the new current parent if the body is opened.
	 *
	 * <p>Example: {@code element root { text }} or {@code element html:div { text }}</p>
	 */
	private RNCNode parseElement(TokenReader scanner, RNCDocument document, RNCNode curr) {
		int declStart = scanner.getTokenOffset();
		int keywordEnd = scanner.getTokenEnd();
		RNCElement element = new RNCElement(declStart, document.getSource().length());

		RNCTokenType token = skipWhitespace(scanner);

		// Parse name (may be namespace-prefixed, wildcard *, or multi-alternative)
		if (isNameToken(token) || token == RNCTokenType.Star) {
			NameScan name = scanName(scanner);
			element.setName(name.start, name.end);

			if (name.next == RNCTokenType.BraceOpen) {
				element.setBodyOpenOffset(scanner.getTokenOffset());
				addToParent(curr, document, element);
				return element;
			}

			document.addError(RNCErrorCode.MissingOpenBrace,
					"Expected '{' after element name", name.start, name.end);
			element.setEnd(name.end);
			element.setClosed(true);
			addToParent(curr, document, element);
			scanner.pushBack(name.next);
			return curr;
		}

		document.addError(RNCErrorCode.MissingName,
				"Expected name after 'element'", declStart, keywordEnd);
		element.setEnd(keywordEnd);
		element.setClosed(true);
		addToParent(curr, document, element);
		scanner.pushBack(token);
		return curr;
	}

	/**
	 * Parses an attribute pattern: {@code attribute name { pattern }}.
	 * The name may be namespace-prefixed (e.g. {@code xml:lang}).
	 *
	 * <p>Example: {@code attribute id { text }} or {@code attribute xml:lang { text }}</p>
	 */
	private RNCNode parseAttribute(TokenReader scanner, RNCDocument document, RNCNode curr) {
		int declStart = scanner.getTokenOffset();
		int keywordEnd = scanner.getTokenEnd();
		RNCAttribute attribute = new RNCAttribute(declStart, document.getSource().length());

		RNCTokenType token = skipWhitespace(scanner);

		// Parse name (may be namespace-prefixed, wildcard *, or multi-alternative)
		if (isNameToken(token) || token == RNCTokenType.Star) {
			NameScan name = scanName(scanner);
			attribute.setName(name.start, name.end);

			if (name.next == RNCTokenType.BraceOpen) {
				attribute.setBodyOpenOffset(scanner.getTokenOffset());
				addToParent(curr, document, attribute);
				return attribute;
			}

			document.addError(RNCErrorCode.MissingOpenBrace,
					"Expected '{' after attribute name", name.start, name.end);
			attribute.setEnd(name.end);
			attribute.setClosed(true);
			addToParent(curr, document, attribute);
			scanner.pushBack(name.next);
			return curr;
		}

		document.addError(RNCErrorCode.MissingName,
				"Expected name after 'attribute'", declStart, keywordEnd);
		attribute.setEnd(keywordEnd);
		attribute.setClosed(true);
		addToParent(curr, document, attribute);
		scanner.pushBack(token);
		return curr;
	}

	/**
	 * Parses an include declaration: {@code include "uri"} or
	 * {@code include "uri" { overrides }}.
	 *
	 * <p>The token following the URI is read to detect the optional body; when it
	 * is not an opening brace it is pushed back so that the main loop processes it.</p>
	 *
	 * <p>Example: {@code include "other.rnc"} or {@code include "base.rnc" { start = foo }}</p>
	 *
	 * @return the include node if its body is opened, otherwise {@code curr}.
	 */
	private RNCNode parseInclude(TokenReader scanner, RNCDocument document, RNCNode curr) {
		int declStart = scanner.getTokenOffset();
		int keywordEnd = scanner.getTokenEnd();
		RNCInclude include = new RNCInclude(declStart, document.getSource().length());

		RNCTokenType token = skipWhitespace(scanner);

		// Expect URI literal
		if (token == RNCTokenType.Literal) {
			include.setUri(scanner.getTokenOffset(), scanner.getTokenEnd());
			int uriEnd = scanner.getTokenEnd();

			token = skipWhitespace(scanner);
			if (token == RNCTokenType.BraceOpen) {
				include.setBodyOpenOffset(scanner.getTokenOffset());
				addToParent(curr, document, include);
				return include;
			}

			include.setEnd(uriEnd);
			include.setClosed(true);
			addToParent(curr, document, include);
			scanner.pushBack(token);
			return curr;
		} else if (token == RNCTokenType.BraceOpen) {
			document.addError(RNCErrorCode.MissingUri,
					"Expected URI string after 'include'", declStart, keywordEnd);
			include.setBodyOpenOffset(scanner.getTokenOffset());
			addToParent(curr, document, include);
			return include;
		} else {
			document.addError(RNCErrorCode.MissingUri,
					"Expected URI string after 'include'", declStart, keywordEnd);
			include.setEnd(keywordEnd);
			include.setClosed(true);
			addToParent(curr, document, include);
			scanner.pushBack(token);
			return curr;
		}
	}

	private void parseExternal(TokenReader scanner, RNCDocument document, RNCNode curr) {
		int declStart = scanner.getTokenOffset();
		int keywordEnd = scanner.getTokenEnd();
		RNCExternal external = new RNCExternal(declStart, document.getSource().length());

		RNCTokenType token = skipWhitespace(scanner);

		if (token == RNCTokenType.Literal) {
			external.setUri(scanner.getTokenOffset(), scanner.getTokenEnd());
			external.setEnd(scanner.getTokenEnd());
		} else {
			document.addError(RNCErrorCode.MissingUri,
					"Expected URI string after 'external'", declStart, keywordEnd);
			external.setEnd(keywordEnd);
			scanner.pushBack(token);
		}
		external.setClosed(true);
		addToParent(curr, document, external);
	}

	/**
	 * Parses a div block: {@code div { grammar-content }}.
	 *
	 * <p>Example: {@code div { a = text }}</p>
	 */
	private RNCNode parseDiv(TokenReader scanner, RNCDocument document, RNCNode curr) {
		int declStart = scanner.getTokenOffset();
		int keywordEnd = scanner.getTokenEnd();
		RNCDiv div = new RNCDiv(declStart, document.getSource().length());

		RNCTokenType token = skipWhitespace(scanner);

		if (token == RNCTokenType.BraceOpen) {
			div.setBodyOpenOffset(scanner.getTokenOffset());
			addToParent(curr, document, div);
			return div;
		}

		document.addError(RNCErrorCode.MissingOpenBrace,
				"Expected '{' after 'div'", declStart, keywordEnd);
		div.setEnd(keywordEnd);
		div.setClosed(true);
		addToParent(curr, document, div);
		scanner.pushBack(token);
		return curr;
	}

	/**
	 * Scans an element/attribute name starting at the current token (a name or
	 * {@code *}): an optional {@code :local} part, then the name class modifiers up
	 * to the opening brace.
	 *
	 * @return the name range and the first token that follows the name class (the
	 *         opening brace when well-formed). That token is the last one scanned.
	 */
	private static NameScan scanName(TokenReader scanner) {
		int nameStart = scanner.getTokenOffset();
		int nameEnd = scanner.getTokenEnd();

		// Check for namespace prefix (name:local or *:local)
		RNCTokenType next = scanner.scan();
		if (next == RNCTokenType.Colon) {
			next = scanner.scan();
			if (isNameToken(next) || next == RNCTokenType.Star) {
				nameEnd = scanner.getTokenEnd();
				next = scanner.scan();
			}
		}

		// Skip name class modifiers (|alternatives, - exclusions) until '{'
		next = skipNameClassUntilBrace(scanner, next);
		return new NameScan(nameStart, nameEnd, next);
	}

	/**
	 * Skips name class modifiers between an element/attribute name and the
	 * opening brace. This handles:
	 * <ul>
	 *   <li>{@code |} alternatives: {@code element group|interleave|choice { }}</li>
	 *   <li>{@code -} exclusions: {@code element * - rng:* { }}</li>
	 *   <li>Parenthesized exclusions: {@code attribute * - (rng:*|local:*) { }}</li>
	 * </ul>
	 *
	 * <p>Inside parentheses, all tokens are skipped. Outside parentheses, only
	 * name class tokens (names, {@code |}, {@code -}, {@code :}, {@code *},
	 * whitespace, comments) are skipped. Stops at an opening brace
	 * (<code>&#123;</code>), a closing brace (<code>&#125;</code>), assignment
	 * operators, or end of stream.</p>
	 *
	 * @return the token on which the skipping stopped (the last one scanned).
	 */
	private static RNCTokenType skipNameClassUntilBrace(TokenReader scanner, RNCTokenType next) {
		while (isTrivia(next)) {
			next = scanner.scan();
		}
		if (next != RNCTokenType.Pipe && next != RNCTokenType.Minus
				&& next != RNCTokenType.ParenOpen) {
			return next;
		}
		int parenDepth = 0;
		while (next != RNCTokenType.BraceOpen && next != RNCTokenType.EOS) {
			if (next == RNCTokenType.ParenOpen) {
				parenDepth++;
			} else if (next == RNCTokenType.ParenClose) {
				if (parenDepth > 0) {
					parenDepth--;
				} else {
					break;
				}
			} else if (parenDepth == 0) {
				if (!isTrivia(next)
						&& next != RNCTokenType.Pipe
						&& next != RNCTokenType.Minus
						&& next != RNCTokenType.Colon
						&& next != RNCTokenType.Star
						&& !isNameToken(next)) {
					break;
				}
			}
			next = scanner.scan();
		}
		return next;
	}

	/**
	 * Advances the scanner past whitespace, comments, and documentation comments,
	 * returning the next meaningful token.
	 *
	 * @param scanner the scanner to advance.
	 * @return the first non-whitespace, non-comment token, or {@link RNCTokenType#EOS}.
	 */
	private static RNCTokenType skipWhitespace(TokenReader scanner) {
		RNCTokenType token = scanner.scan();
		while (isTrivia(token)) {
			token = scanner.scan();
		}
		return token;
	}

	private static boolean isTrivia(RNCTokenType token) {
		return token == RNCTokenType.Whitespace || token == RNCTokenType.Comment
				|| token == RNCTokenType.DocumentationComment;
	}

	/**
	 * Determines whether a token type is a keyword that starts a pattern.
	 *
	 * @param token the token type to test.
	 * @return {@code true} if the token starts a pattern.
	 */
	private static boolean isPatternKeyword(RNCTokenType token) {
		return token == RNCTokenType.KeywordElement
				|| token == RNCTokenType.KeywordAttribute
				|| token == RNCTokenType.KeywordText
				|| token == RNCTokenType.KeywordEmpty
				|| token == RNCTokenType.KeywordNotAllowed
				|| token == RNCTokenType.KeywordList
				|| token == RNCTokenType.KeywordMixed
				|| token == RNCTokenType.KeywordExternal
				|| token == RNCTokenType.KeywordGrammar
				|| token == RNCTokenType.KeywordParent
				|| token == RNCTokenType.KeywordString
				|| token == RNCTokenType.KeywordToken;
	}

	/**
	 * Determines whether a token type can serve as a name in a define, element,
	 * or attribute declaration.
	 *
	 * <p>In RNC, keywords can be used as names (e.g., {@code element text { ... }}
	 * is valid — {@code text} is both a keyword and the element name). This method
	 * returns {@code true} for identifiers and all keywords that can appear as names.</p>
	 *
	 * @param token the token type to test.
	 * @return {@code true} if the token can be used as a name.
	 */
	private static boolean isNameToken(RNCTokenType token) {
		return token == RNCTokenType.Identifier
				|| token == RNCTokenType.KeywordElement
				|| token == RNCTokenType.KeywordAttribute
				|| token == RNCTokenType.KeywordText
				|| token == RNCTokenType.KeywordEmpty
				|| token == RNCTokenType.KeywordNotAllowed
				|| token == RNCTokenType.KeywordStart
				|| token == RNCTokenType.KeywordInclude
				|| token == RNCTokenType.KeywordDiv
				|| token == RNCTokenType.KeywordNamespace
				|| token == RNCTokenType.KeywordDefault
				|| token == RNCTokenType.KeywordDatatypes
				|| token == RNCTokenType.KeywordExternal
				|| token == RNCTokenType.KeywordGrammar
				|| token == RNCTokenType.KeywordList
				|| token == RNCTokenType.KeywordMixed
				|| token == RNCTokenType.KeywordParent
				|| token == RNCTokenType.KeywordInherit
				|| token == RNCTokenType.KeywordString
				|| token == RNCTokenType.KeywordToken;
	}

	/**
	 * Returns {@code true} if the innermost open brace owned by {@code curr} is an
	 * anonymous block that is not a {@code grammar} (list, mixed, datatype
	 * parameters...). Defines are not allowed there.
	 */
	private static boolean isInNonGrammarBlock(Deque<AnonymousBlock> openBlocks, RNCNode curr) {
		AnonymousBlock top = openBlocks.peek();
		return top != null && top.owner == curr && !top.grammar;
	}

	/**
	 * Adds a child node to the current parent context.
	 *
	 * <p>If the current node is the document root, the child becomes a top-level
	 * node; otherwise it is nested inside the current enclosing construct.</p>
	 *
	 * @param curr     the current parent node in the parse tree.
	 * @param document the root document node.
	 * @param child    the child node to add.
	 */
	private static void addToParent(RNCNode curr, RNCDocument document, RNCNode child) {
		if (curr == document) {
			document.addChild(child);
		} else {
			curr.addChild(child);
		}
	}

	/**
	 * An opening brace that does not belong to a node of the AST
	 * ({@code mixed}, {@code list}, {@code grammar}, datatype parameters,
	 * malformed names).
	 */
	private static final class AnonymousBlock {

		private final int offset;
		private final boolean grammar;
		private final RNCNode owner;

		AnonymousBlock(int offset, boolean grammar, RNCNode owner) {
			this.offset = offset;
			this.grammar = grammar;
			this.owner = owner;
		}
	}

	/**
	 * Name range of an element/attribute and the token following its name class.
	 */
	private static final class NameScan {

		private final int start;
		private final int end;
		private final RNCTokenType next;

		NameScan(int start, int end, RNCTokenType next) {
			this.start = start;
			this.end = end;
			this.next = next;
		}
	}

	/**
	 * Wraps the {@link RNCScanner} to allow giving back the last scanned token, so
	 * that a parse method that reads ahead to check what follows does not swallow an
	 * unexpected token. Since nothing is scanned between {@link #scan()} and
	 * {@link #pushBack(RNCTokenType)}, the scanner's token offset/end still describe
	 * the pushed back token when it is returned again by {@link #scan()}.
	 */
	private static final class TokenReader {

		private final RNCScanner scanner;
		private RNCTokenType pushedBack;

		TokenReader(RNCScanner scanner) {
			this.scanner = scanner;
		}

		RNCTokenType scan() {
			if (pushedBack != null) {
				RNCTokenType token = pushedBack;
				pushedBack = null;
				return token;
			}
			return scanner.scan();
		}

		/**
		 * Gives back the last scanned token: it will be returned by the next call to
		 * {@link #scan()}.
		 */
		void pushBack(RNCTokenType token) {
			pushedBack = token;
		}

		int getTokenOffset() {
			return scanner.getTokenOffset();
		}

		int getTokenEnd() {
			return scanner.getTokenEnd();
		}
	}
}