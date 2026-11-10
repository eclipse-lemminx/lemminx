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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.completion;

import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDatatypeLibrary;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.IRNCDocumentProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCStart;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.hover.RNCHover;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCKeywords;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Handles lazy resolution of completion item documentation for RNC files.
 *
 * <p>At completion time, items are created with a {@code resolveKind} in their
 * data payload. When the client requests full documentation (via
 * {@code completionItem/resolve}), this class loads the appropriate content
 * based on the resolve kind.</p>
 *
 * <p>Use case: the user selects a completion item in the editor and the client
 * requests its full documentation — this class loads it lazily to avoid
 * computing documentation for all items upfront.</p>
 *
 * @see RNCCompletions
 */
public class RNCCompletionResolver {

	static final String RESOLVE_KIND_FIELD = "resolveKind";
	static final String INCLUDE_FILE_URI_FIELD = "includeFileUri";
	static final String RESOLVE_KEYWORD = "keyword";
	static final String RESOLVE_DEFINE_REF = "defineRef";
	static final String RESOLVE_OVERRIDE = "override";
	static final String RESOLVE_DATATYPE = "datatype";
	static final String DATATYPE_LIBRARY_URI_FIELD = "datatypeLibraryUri";

	/**
	 * Creates a JSON data object for a completion item, storing the document
	 * URI and resolve kind for later resolution.
	 *
	 * @param uri         the document URI.
	 * @param resolveKind the kind of resolution to perform.
	 * @return the resolve data object.
	 */
	static JsonObject createResolveData(String uri, String resolveKind) {
		JsonObject data = new JsonObject();
		data.addProperty("uri", uri);
		data.addProperty(RESOLVE_KIND_FIELD, resolveKind);
		return data;
	}

	/**
	 * Extracts the resolve kind from a completion item's data payload.
	 */
	private static String getResolveKind(Object data) {
		if (data instanceof JsonObject) {
			JsonElement el = ((JsonObject) data).get(RESOLVE_KIND_FIELD);
			return el != null ? el.getAsString() : null;
		}
		return null;
	}

	/**
	 * Resolves the documentation for a completion item that was deferred at
	 * completion time.
	 *
	 * <p>The {@code resolveKind} stored in the item's data determines what
	 * documentation to load:</p>
	 * <ul>
	 *   <li>{@code defineRef} — the full text of the referenced define</li>
	 *   <li>{@code override} — the original define text from the included file</li>
	 *   <li>{@code keyword} — the keyword documentation from the JSON registry</li>
	 *   <li>{@code datatype} — the datatype documentation from
	 *       {@link RNCDatatypeLibrary}</li>
	 * </ul>
	 *
	 * @param item           the completion item to resolve.
	 * @param rncDocument    the parsed RNC document.
	 * @param hover          the hover provider (for keyword documentation).
	 * @param sharedSettings the shared settings (for markup kind capabilities).
	 * @param cancelChecker  the cancel checker.
	 * @return the resolved completion item.
	 */
	public static CompletionItem resolveCompletionItem(CompletionItem item, RNCDocument rncDocument,
			RNCHover hover, SharedSettings sharedSettings, IRNCDocumentProvider documentProvider,
			CancelChecker cancelChecker) {
		String resolveKind = getResolveKind(item.getData());
		if (resolveKind == null) {
			return item;
		}

		boolean supportsMarkdown = canSupportMarkdownInCompletion(sharedSettings);

		switch (resolveKind) {
			case RESOLVE_DEFINE_REF: {
				String name = item.getLabel();
				RNCDefine define = rncDocument.findDefine(name);
				if (define != null) {
					String text = define.getText();
					if (text != null) {
						String markdown = "```rnc\n" + text.trim() + "\n```";
						MarkupContent content = new MarkupContent(
								supportsMarkdown ? MarkupKind.MARKDOWN : MarkupKind.PLAINTEXT,
								supportsMarkdown ? markdown : text.trim());
						item.setDocumentation(content);
					}
				}
				break;
			}
			case RESOLVE_OVERRIDE: {
				JsonObject data = (JsonObject) item.getData();
				JsonElement el = data.get(INCLUDE_FILE_URI_FIELD);
				if (el != null) {
					String includeFileUri = el.getAsString();
					RNCDocument includedDoc = documentProvider.getDocument(includeFileUri);
					if (includedDoc != null) {
						String name = item.getLabel();
						String text = null;
						if (RNCKeywords.START.equals(name)) {
							RNCStart start = includedDoc.getStartDecl();
							if (start != null) {
								text = start.getText();
							}
						} else {
							RNCDefine define = includedDoc.findDefine(name);
							if (define != null) {
								text = define.getText();
							}
						}
						if (text != null) {
							String markdown = "```rnc\n" + text.trim() + "\n```";
							MarkupContent content = new MarkupContent(
									supportsMarkdown ? MarkupKind.MARKDOWN : MarkupKind.PLAINTEXT,
									supportsMarkdown ? markdown : text.trim());
							item.setDocumentation(content);
						}
					}
				}
				break;
			}
			case RESOLVE_KEYWORD: {
				String keyword = item.getFilterText();
				MarkupContent content = hover.getKeywordDocumentation(keyword, supportsMarkdown);
				if (content != null) {
					item.setDocumentation(content);
				}
				break;
			}
			case RESOLVE_DATATYPE: {
				JsonObject data = (JsonObject) item.getData();
				JsonElement libEl = data.get(DATATYPE_LIBRARY_URI_FIELD);
				if (libEl != null) {
					RNCDatatypeLibrary library = RNCDatatypeLibrary.get(libEl.getAsString());
					if (library != null) {
						String label = item.getLabel();
						int colonIdx = label.indexOf(':');
						String typeName = colonIdx >= 0 ? label.substring(colonIdx + 1) : label;
						RNCDatatypeLibrary.Entry entry = library.getEntry(typeName);
						if (entry != null) {
							MarkupContent content = entry.getDocumentation(supportsMarkdown);
							if (content != null) {
								item.setDocumentation(content);
							}
						}
					}
				}
				break;
			}
		}
		return item;
	}

	/**
	 * Returns whether the client supports Markdown in completion item
	 * documentation.
	 */
	private static boolean canSupportMarkdownInCompletion(SharedSettings settings) {
		if (settings == null) {
			return true;
		}
		return settings.getCompletionSettings().canSupportMarkupKind(MarkupKind.MARKDOWN);
	}

	private RNCCompletionResolver() {
	}
}
