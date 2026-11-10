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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.hover;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDatatypeLibrary;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.IRNCDocumentProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCInclude;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNode;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCIncludeUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCKeywords;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Provides hover information for RNC (RELAX NG Compact Syntax) identifiers and
 * keywords.
 *
 * <p>
 * Two types of hover are supported:
 * </p>
 * <ul>
 * <li><b>Define hover</b> — when hovering over a define reference, the full
 * definition text is displayed in a fenced code block.</li>
 * <li><b>Keyword hover</b> — when hovering over an RNC keyword, detailed
 * documentation from the RELAX NG specification is displayed, loaded from a
 * bundled JSON resource file.</li>
 * </ul>
 */
public class RNCHover {

	private static final Logger LOGGER = Logger.getLogger(RNCHover.class.getName());

	private final Map<String, KeywordDoc> keywordDocs;
	private final IRNCDocumentProvider documentProvider;

	public RNCHover(IRNCDocumentProvider documentProvider) {
		this.documentProvider = documentProvider;
		keywordDocs = loadKeywordDocs();
	}

	/**
	 * Computes hover information for the word at the given position.
	 *
	 * <p>Checks, in order: define references (showing the define text), datatype
	 * references (e.g. {@code xsd:integer}), include keywords (showing definitions
	 * from the included file), and RNC keywords (showing specification docs).</p>
	 *
	 * @param rncDocument    the parsed RNC document.
	 * @param position       the cursor position where hover was triggered.
	 * @param sharedSettings settings used to determine Markdown support.
	 * @param cancelChecker  used to check if the operation has been canceled.
	 * @return a {@link Hover} with documentation content, or {@code null} if
	 *         no hover information is available.
	 */
	public Hover doHover(RNCDocument rncDocument, Position position, SharedSettings sharedSettings,
			CancelChecker cancelChecker) {
		try {
			RNCHoverRequest request = new RNCHoverRequest(rncDocument, position, sharedSettings);

			String word = request.getWord();
			if (word == null || word.isEmpty()) {
				return null;
			}

			int offset = request.getOffset();
			TextDocument textDocument = request.getTextDocument();
			boolean supportsMarkdown = request.canSupportMarkdown();

			RNCDefine define = rncDocument.findDefine(word);
			if (define != null) {
				String defineText = define.getText();
				if (defineText != null) {
					String markdown;
					String originalText = findOriginalDefineText(define, word, rncDocument);
					if (originalText != null) {
						markdown = "**Override:**\n```rnc\n" + defineText.trim() + "\n```"
								+ "\n\n**Original:**\n```rnc\n" + originalText.trim() + "\n```";
					} else {
						markdown = "```rnc\n" + defineText.trim() + "\n```";
					}
					MarkupContent content = new MarkupContent(
							supportsMarkdown ? MarkupKind.MARKDOWN : MarkupKind.PLAINTEXT,
							supportsMarkdown ? markdown : defineText.trim());
					Range wordRange = RNCPositionUtils.getWordRange(textDocument, offset, word);
					return new Hover(content, wordRange);
				}
			}

			// Datatype hover: xsd:integer → show XSD integer documentation
			RNCDatatypeLibrary.Entry datatypeEntry = findDatatypeEntry(rncDocument, request.getText(), offset, word);
			if (datatypeEntry != null) {
				MarkupContent content = datatypeEntry.getDocumentation(supportsMarkdown);
				if (content != null) {
					Range wordRange = RNCPositionUtils.getWordRange(textDocument, offset, word);
					return new Hover(content, wordRange);
				}
			}

			// Include hover: show definitions from the included file
			if (RNCKeywords.INCLUDE.equals(word)) {
				RNCInclude include = findIncludeAt(rncDocument, offset);
				if (include != null) {
					String includeHover = buildIncludeHover(include, rncDocument, supportsMarkdown);
					if (includeHover != null) {
						MarkupContent content = new MarkupContent(
								supportsMarkdown ? MarkupKind.MARKDOWN : MarkupKind.PLAINTEXT,
								includeHover);
						Range wordRange = RNCPositionUtils.getWordRange(textDocument, offset, word);
						return new Hover(content, wordRange);
					}
					// File not resolved, fall back to keyword doc with context
					String keywordKey = include.hasBody() ? "include-override" : "include-reference";
					KeywordDoc contextDoc = keywordDocs.get(keywordKey);
					if (contextDoc != null) {
						MarkupContent content = contextDoc.toMarkupContent(supportsMarkdown);
						Range wordRange = RNCPositionUtils.getWordRange(textDocument, offset, word);
						return new Hover(content, wordRange);
					}
				}
			}

			KeywordDoc doc = keywordDocs.get(word);
			if (doc != null) {
				MarkupContent content = doc.toMarkupContent(supportsMarkdown);
				Range wordRange = RNCPositionUtils.getWordRange(textDocument, offset, word);
				return new Hover(content, wordRange);
			}
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "Error computing RNC hover", e);
		}
		return null;
	}

	/**
	 * Returns the documentation for the given keyword as a {@link MarkupContent},
	 * selecting Markdown or plaintext based on client capability.
	 *
	 * @param keyword           the RNC keyword name.
	 * @param supportsMarkdown  whether the client supports Markdown.
	 * @return the documentation content, or {@code null} if the keyword is unknown.
	 */
	public MarkupContent getKeywordDocumentation(String keyword, boolean supportsMarkdown) {
		KeywordDoc doc = keywordDocs.get(keyword);
		if (doc == null) {
			return null;
		}
		return doc.toMarkupContent(supportsMarkdown);
	}

	private static Map<String, KeywordDoc> loadKeywordDocs() {
		Map<String, KeywordDoc> docs = new HashMap<>();
		try (InputStream is = RNCHover.class.getResourceAsStream("rnc-keywords.json")) {
			if (is == null) {
				LOGGER.warning("rnc-keywords.json resource not found");
				return docs;
			}
			Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
			JsonObject root = new Gson().fromJson(reader, JsonObject.class);
			for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
				JsonObject obj = entry.getValue().getAsJsonObject();
				String markdown = obj.has("markdown") ? obj.get("markdown").getAsString() : null;
				String plaintext = obj.has("plaintext") ? obj.get("plaintext").getAsString() : null;
				if (markdown != null || plaintext != null) {
					docs.put(entry.getKey(), new KeywordDoc(markdown, plaintext));
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error loading RNC keyword documentation", e);
		}
		return docs;
	}

	private static RNCInclude findIncludeAt(RNCDocument document, int offset) {
		for (RNCInclude include : document.getIncludes()) {
			if (include.getStart() <= offset && offset < include.getEnd()) {
				return include;
			}
		}
		return null;
	}

	private String buildIncludeHover(RNCInclude include, RNCDocument rncDocument,
			boolean supportsMarkdown) {
		String uri = include.getUri();
		if (uri == null || uri.isEmpty()) {
			return null;
		}
		String resolvedUri = RNCIncludeUtils.resolveUri(include, rncDocument.getUri());
		RNCDocument includedDoc = documentProvider.getDocument(resolvedUri);
		if (includedDoc == null) {
			return null;
		}
		boolean hasStart = includedDoc.getStartDecl() != null;
		List<RNCDefine> includedDefines = includedDoc.getDefines();
		if (!hasStart && includedDefines.isEmpty()) {
			return null;
		}

		Set<String> overriddenNames = include.getOverriddenNames();

		StringBuilder sb = new StringBuilder();
		if (supportsMarkdown) {
			sb.append("**include** `").append(uri).append("`\n\n");
			sb.append("**Definitions from included file:**\n\n");
			if (hasStart) {
				appendDefinitionLine(sb, RNCKeywords.START, overriddenNames, true);
			}
			for (RNCDefine def : includedDefines) {
				String name = def.getName();
				if (name != null) {
					appendDefinitionLine(sb, name, overriddenNames, true);
				}
			}
		} else {
			sb.append("include \"").append(uri).append("\"\n\n");
			sb.append("Definitions from included file:\n\n");
			if (hasStart) {
				appendDefinitionLine(sb, RNCKeywords.START, overriddenNames, false);
			}
			for (RNCDefine def : includedDefines) {
				String name = def.getName();
				if (name != null) {
					appendDefinitionLine(sb, name, overriddenNames, false);
				}
			}
		}
		return sb.toString();
	}

	private static void appendDefinitionLine(StringBuilder sb, String name,
			Set<String> overriddenNames, boolean markdown) {
		boolean overridden = overriddenNames.contains(name);
		if (markdown) {
			if (overridden) {
				sb.append("- ~~`").append(name).append("`~~ — *overridden*\n");
			} else {
				sb.append("- `").append(name).append("` — inherited\n");
			}
		} else {
			sb.append("- ").append(name);
			sb.append(overridden ? " — overridden\n" : " — inherited\n");
		}
	}

	private String findOriginalDefineText(RNCDefine override, String name,
			RNCDocument rncDocument) {
		RNCNode parent = override.getParent();
		if (!(parent instanceof RNCInclude)) {
			return null;
		}
		RNCInclude include = (RNCInclude) parent;
		String resolvedUri = RNCIncludeUtils.resolveUri(include, rncDocument.getUri());
		RNCDocument includedDoc = documentProvider.getDocument(resolvedUri);
		if (includedDoc == null) {
			return null;
		}
		RNCDefine original = includedDoc.findDefine(name);
		if (original != null) {
			return original.getText();
		}
		return null;
	}

	private static RNCDatatypeLibrary.Entry findDatatypeEntry(RNCDocument rncDocument,
			CharSequence text, int offset, String word) {
		String prefix = "";
		String name = word;
		int prefixEnd = word.indexOf(':');
		if (prefixEnd != -1) {
			prefix = word.substring(0, prefixEnd);
			name = word.substring(prefixEnd + 1);
		}		
		RNCDatatypeLibrary library = RNCDatatypeLibrary.resolve(rncDocument, prefix);
		if (library == null) {
			return null;
		}
		return library.getEntry(name);
	}

	private static class KeywordDoc {
		final String markdown;
		final String plaintext;

		KeywordDoc(String markdown, String plaintext) {
			this.markdown = markdown;
			this.plaintext = plaintext;
		}

		MarkupContent toMarkupContent(boolean supportsMarkdown) {
			if (supportsMarkdown && markdown != null) {
				return new MarkupContent(MarkupKind.MARKDOWN, markdown);
			}
			if (plaintext != null) {
				return new MarkupContent(MarkupKind.PLAINTEXT, plaintext);
			}
			if (markdown != null) {
				return new MarkupContent(MarkupKind.MARKDOWN, markdown);
			}
			return null;
		}
	}
}
