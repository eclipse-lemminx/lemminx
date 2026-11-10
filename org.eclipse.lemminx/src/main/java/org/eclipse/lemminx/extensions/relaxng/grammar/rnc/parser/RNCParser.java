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
 * <p>Example: parsing the text {@code "start = element root \{ text \}"} produces:</p>
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
	 * attribute, include, div with {@code \{}), {@code curr} descends into the new
	 * node. When {@code \}} is found, {@code curr} ascends back to the parent.</p>
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
		RNCScanner scanner = RNCScanner.createScanner(text);
		RNCDocument document = new RNCDocument(text, uri);

		RNCNode curr = document;
		RNCTokenType token = scanner.scan();

		// Track a pending identifier that might be a define name.
		// When we see Identifier followed by = / |= / &=, it's a define.
		// This avoids peek-ahead that would consume the next token.
		int pendingIdentStart = -1;
		int pendingIdentEnd = -1;

		while (token != RNCTokenType.EOS) {
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
					token = skipWhitespace(scanner);
					if (token == RNCTokenType.KeywordNamespace) {
						curr = parseNamespaceDecl(scanner, document, curr, true);
					}
					token = scanner.scan();
					continue;
				}

				case KeywordNamespace: {
					curr = parseNamespaceDecl(scanner, document, curr, false);
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
					token = skipWhitespace(scanner);
					if (token == RNCTokenType.BraceOpen) {
						List<RNCInclude> includes = document.getIncludes();
						if (!includes.isEmpty()) {
							RNCInclude lastInclude = includes.get(includes.size() - 1);
							if (lastInclude.isClosed() && !lastInclude.hasBody()) {
								lastInclude.setClosed(false);
								lastInclude.setBodyOpenOffset(scanner.getTokenOffset());
								lastInclude.setEnd(document.getSource().length());
								curr = lastInclude;
							}
						}
						token = scanner.scan();
					}
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

				case BraceClose: {
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
				case KeywordGrammar:
				case KeywordParent:
				case KeywordInherit:
				case KeywordString:
				case KeywordToken:
				case BraceOpen:
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
	 * @param isDefault {@code true} if preceded by the {@code default} keyword.
	 */
	private RNCNode parseNamespaceDecl(RNCScanner scanner, RNCDocument document, RNCNode curr, boolean isDefault) {
		int declStart = isDefault ? findDefaultStart(document, scanner.getTokenOffset()) : scanner.getTokenOffset();
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
	private RNCNode parseDatatypesDecl(RNCScanner scanner, RNCDocument document, RNCNode curr) {
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
	private RNCNode parseStart(RNCScanner scanner, RNCDocument document, RNCNode curr) {
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
		}

		startNode.setEnd(scanner.getTokenEnd());
		startNode.setClosed(true);
		addToParent(curr, document, startNode);
		return curr;
	}

	/**
	 * Parses an element pattern: {@code element name \{ pattern \}}.
	 * The name may be namespace-prefixed (e.g. {@code ns:localName}).
	 * Returns the element node as the new current parent if the body is opened.
	 *
	 * <p>Example: {@code element root \{ text \}} or {@code element html:div \{ text \}}</p>
	 */
	private RNCNode parseElement(RNCScanner scanner, RNCDocument document, RNCNode curr) {
		int declStart = scanner.getTokenOffset();
		int keywordEnd = scanner.getTokenEnd();
		RNCElement element = new RNCElement(declStart, document.getSource().length());

		RNCTokenType token = skipWhitespace(scanner);

		// Parse name (may be namespace-prefixed, wildcard *, or multi-alternative)
		if (isNameToken(token) || token == RNCTokenType.Star) {
			int nameStart = scanner.getTokenOffset();
			int nameEnd = scanner.getTokenEnd();

			// Check for namespace prefix (name:local or *:local)
			RNCTokenType next = scanner.scan();
			if (next == RNCTokenType.Colon) {
				next = scanner.scan();
				if (isNameToken(next) || next == RNCTokenType.Star) {
					nameEnd = scanner.getTokenEnd();
				}
				next = scanner.scan();
			}

			element.setName(nameStart, nameEnd);

			// Skip name class modifiers (|alternatives, - exclusions) until '{'
			next = skipNameClassUntilBrace(scanner, next);

			if (next == RNCTokenType.BraceOpen) {
				element.setBodyOpenOffset(scanner.getTokenOffset());
				addToParent(curr, document, element);
				return element;
			}

			document.addError(RNCErrorCode.MissingOpenBrace,
					"Expected '{' after element name", nameStart, nameEnd);
			element.setEnd(nameEnd);
			element.setClosed(true);
			addToParent(curr, document, element);
			return curr;
		}

		document.addError(RNCErrorCode.MissingName,
				"Expected name after 'element'", declStart, keywordEnd);
		element.setEnd(keywordEnd);
		element.setClosed(true);
		addToParent(curr, document, element);
		return curr;
	}

	/**
	 * Parses an attribute pattern: {@code attribute name \{ pattern \}}.
	 * The name may be namespace-prefixed (e.g. {@code xml:lang}).
	 *
	 * <p>Example: {@code attribute id \{ text \}} or {@code attribute xml:lang \{ text \}}</p>
	 */
	private RNCNode parseAttribute(RNCScanner scanner, RNCDocument document, RNCNode curr) {
		int declStart = scanner.getTokenOffset();
		int keywordEnd = scanner.getTokenEnd();
		RNCAttribute attribute = new RNCAttribute(declStart, document.getSource().length());

		RNCTokenType token = skipWhitespace(scanner);

		// Parse name (may be namespace-prefixed, wildcard *, or multi-alternative)
		if (isNameToken(token) || token == RNCTokenType.Star) {
			int nameStart = scanner.getTokenOffset();
			int nameEnd = scanner.getTokenEnd();

			// Check for namespace prefix (name:local or *:local)
			RNCTokenType next = scanner.scan();
			if (next == RNCTokenType.Colon) {
				next = scanner.scan();
				if (isNameToken(next) || next == RNCTokenType.Star) {
					nameEnd = scanner.getTokenEnd();
				}
				next = scanner.scan();
			}

			attribute.setName(nameStart, nameEnd);

			// Skip name class modifiers (|alternatives, - exclusions) until '{'
			next = skipNameClassUntilBrace(scanner, next);

			if (next == RNCTokenType.BraceOpen) {
				attribute.setBodyOpenOffset(scanner.getTokenOffset());
				addToParent(curr, document, attribute);
				return attribute;
			}

			document.addError(RNCErrorCode.MissingOpenBrace,
					"Expected '{' after attribute name", nameStart, nameEnd);
			attribute.setEnd(nameEnd);
			attribute.setClosed(true);
			addToParent(curr, document, attribute);
			return curr;
		}

		document.addError(RNCErrorCode.MissingName,
				"Expected name after 'attribute'", declStart, keywordEnd);
		attribute.setEnd(keywordEnd);
		attribute.setClosed(true);
		addToParent(curr, document, attribute);
		return curr;
	}

	/**
	 * Parses an include declaration: {@code include "uri"} or
	 * {@code include "uri" \{ overrides \}}.
	 *
	 * <p>Example: {@code include "other.rnc"} or {@code include "base.rnc" \{ start = foo \}}</p>
	 */
	private RNCNode parseInclude(RNCScanner scanner, RNCDocument document, RNCNode curr) {
		int declStart = scanner.getTokenOffset();
		int keywordEnd = scanner.getTokenEnd();
		RNCInclude include = new RNCInclude(declStart, document.getSource().length());

		RNCTokenType token = skipWhitespace(scanner);

		// Expect URI literal
		if (token == RNCTokenType.Literal) {
			include.setUri(scanner.getTokenOffset(), scanner.getTokenEnd());
			// Body detection ({ ... }) is handled in the main loop to avoid
			// consuming the next meaningful token when there is no body.
			include.setEnd(scanner.getTokenEnd());
			include.setClosed(true);
			addToParent(curr, document, include);
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
			return curr;
		}
	}

	private void parseExternal(RNCScanner scanner, RNCDocument document, RNCNode curr) {
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
		}
		external.setClosed(true);
		addToParent(curr, document, external);
	}

	/**
	 * Parses a div block: {@code div \{ grammar-content \}}.
	 *
	 * <p>Example: {@code div \{ a = text \\ b = text \}}</p>
	 */
	private RNCNode parseDiv(RNCScanner scanner, RNCDocument document, RNCNode curr) {
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
		return curr;
	}

	/**
	 * Skips name class modifiers between an element/attribute name and the
	 * opening brace. This handles:
	 * <ul>
	 *   <li>{@code |} alternatives: {@code element group|interleave|choice \{ \}}</li>
	 *   <li>{@code -} exclusions: {@code element * - rng:* \{ \}}</li>
	 *   <li>Parenthesized exclusions: {@code attribute * - (rng:*|local:*) \{ \}}</li>
	 * </ul>
	 *
	 * <p>Inside parentheses, all tokens are skipped. Outside parentheses, only
	 * name class tokens (names, {@code |}, {@code -}, {@code :}, {@code *},
	 * whitespace) are skipped. Stops at {@code \{}, {@code \}}, assignment
	 * operators, or end of stream.</p>
	 */
	private static RNCTokenType skipNameClassUntilBrace(RNCScanner scanner, RNCTokenType next) {
		if (next == RNCTokenType.Whitespace) {
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
				if (next != RNCTokenType.Whitespace
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
	private static RNCTokenType skipWhitespace(RNCScanner scanner) {
		RNCTokenType token = scanner.scan();
		while (token == RNCTokenType.Whitespace || token == RNCTokenType.Comment || token == RNCTokenType.DocumentationComment) {
			token = scanner.scan();
		}
		return token;
	}

	/**
	 * Determines whether a token type can serve as a name in a define, element,
	 * or attribute declaration.
	 *
	 * <p>In RNC, keywords can be used as names (e.g., {@code element text \{ ... \}}
	 * is valid — {@code text} is both a keyword and the element name). This method
	 * returns {@code true} for identifiers and all keywords that can appear as names.</p>
	 *
	 * @param token the token type to test.
	 * @return {@code true} if the token can be used as a name.
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
	 * Finds the start offset of the {@code default} keyword preceding a
	 * {@code namespace} keyword for {@code default namespace} declarations.
	 *
	 * <p>Scans backward from the {@code namespace} token to locate the preceding
	 * {@code default} keyword, so the resulting {@link RNCNamespaceDecl} node spans
	 * both words.</p>
	 *
	 * @param document              the document (used to access source text).
	 * @param namespaceTokenOffset  the offset of the {@code namespace} keyword.
	 * @return the start offset of {@code default}, or {@code namespaceTokenOffset}
	 *         if no {@code default} keyword is found.
	 */
	private static int findDefaultStart(RNCDocument document, int namespaceTokenOffset) {
		CharSequence source = document.getSource();
		int pos = namespaceTokenOffset - 1;
		while (pos >= 0 && isWhitespace(Character.codePointAt(source, pos))) {
			pos--;
		}
		int defaultStart = pos - 6;
		if (defaultStart >= 0 && matchesWord(source, defaultStart, "default")) {
			return defaultStart;
		}
		return namespaceTokenOffset;
	}

	private static boolean matchesWord(CharSequence source, int offset, String word) {
		for (int i = 0; i < word.length(); i++) {
			if (source.charAt(offset + i) != word.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Tests whether a character is whitespace (space, tab, newline, carriage return,
	 * or form feed).
	 *
	 * @param ch the character code point.
	 * @return {@code true} if the character is whitespace.
	 */
	private static boolean isWhitespace(int ch) {
		return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '\f';
	}
}
