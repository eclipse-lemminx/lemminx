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

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.lemminx.AbstractTextDocumentService;
import org.eclipse.lemminx.commons.ModelTextDocument;

import org.eclipse.lemminx.extensions.index.GrammarReference;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCScanner;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCTokenType;
import org.eclipse.lemminx.services.extensions.IIndexFileScannerContributor;
import org.eclipse.lemminx.utils.FilesUtils;

/**
 * Workspace index scanner contributor for RELAX NG Compact Syntax (RNC) files.
 *
 * <p>RNC uses a non-XML compact syntax, so the standard XML-based index scanner
 * cannot parse it. This contributor uses {@link RNCScanner} to find
 * {@code include "..."} and {@code external "..."} directives and records them
 * as {@link GrammarReference.Kind#HREF} references in the workspace Bloom index.
 *
 * <p>Example — given {@code include "base.rnc"}, this contributor adds a reference
 * to {@code base.rnc} so that the workspace index can answer "which RNC files
 * include {@code base.rnc}?" without opening any files at query time.
 */
public class RNCIndexFileScannerContributor implements IIndexFileScannerContributor {

	/**
	 * The RNC text-document service, set once from {@link RNCTextDocumentService}
	 * when it is created. Using a static field avoids any registry-lookup or
	 * timing issue — in production there is exactly one contributor instance and
	 * one service instance per server process.
	 */
	private static volatile AbstractTextDocumentService<?> documentService;

	/**
	 * Called by {@link RNCTextDocumentService} at construction time so that
	 * {@link #getOpenDocumentContent} can return live in-memory content for
	 * open RNC files.
	 */
	static void setDocumentService(AbstractTextDocumentService<?> service) {
		RNCIndexFileScannerContributor.documentService = service;
	}

	@Override
	public String getOpenDocumentContent(String uri) {
		AbstractTextDocumentService<?> svc = RNCIndexFileScannerContributor.documentService;
		if (svc == null) {
			return null;
		}
		// Fast path: exact URI match
		ModelTextDocument<?, ?> doc = svc.getDocument(uri);
		if (doc != null) {
			return doc.getText();
		}
		// Path-based fallback: handles URI case mismatches (e.g. file:///C:/ vs file:///c:/ on Windows).
		// Path.equals() on Windows is case-insensitive, so this resolves drive-letter differences.
		Path path = FilesUtils.getPathOrNull(uri);
		if (path != null) {
			for (ModelTextDocument<?, ?> d : svc.allDocuments()) {
				if (path.equals(FilesUtils.getPathOrNull(d.getUri()))) {
					return d.getText();
				}
			}
		}
		return null;
	}

	@Override
	public boolean canScan(Path path) {
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		return name.endsWith(".rnc");
	}

	@Override
	public boolean scan(Path path, int maxChars, List<GrammarReference> refs, Set<String> names) throws IOException {
		// Read up to maxChars chars — RNC grammar files are typically small,
		// but we honour the configured limit to bound scan time.
		StringBuilder sb = new StringBuilder();
		boolean partial = false;
		try (Reader reader = Files.newBufferedReader(path)) {
			char[] buf = new char[8192];
			int read;
			int total = 0;
			while ((read = reader.read(buf)) != -1) {
				int remaining = maxChars - total;
				if (read >= remaining) {
					sb.append(buf, 0, remaining);
					total += remaining;
					partial = true;
					break;
				}
				sb.append(buf, 0, read);
				total += read;
			}
		}

		String content = sb.toString();
		scanContent(content, refs, names);
		return partial;
	}

	@Override
	public void scan(String content, Path path, List<GrammarReference> refs, Set<String> names) {
		scanContent(content, refs, names);
	}

	private static void scanContent(String content, List<GrammarReference> refs, Set<String> names) {
		RNCScanner scanner = RNCScanner.createScanner(content);
		boolean awaitingLiteral = false;

		RNCTokenType token;
		while ((token = scanner.scan()) != RNCTokenType.EOS) {
			switch (token) {
				case KeywordInclude:
				case KeywordExternal:
					awaitingLiteral = true;
					break;

				case Literal:
					if (awaitingLiteral) {
						String literalText = scanner.getTokenText();
						String val = stripQuotes(literalText);
						if (!val.isEmpty()) {
							int litOffset = scanner.getTokenOffset();
							int contentStart = litOffset + quotePrefixLen(literalText);
							int[] lineCol = lineAndCol(content, contentStart);
							addFilePath(names, val);
							refs.add(new GrammarReference(contentStart, lineCol[0], lineCol[1],
									lineCol[1] + val.length(), val, GrammarReference.Kind.HREF));
						}
						awaitingLiteral = false;
					}
					break;

				case Whitespace:
				case Comment:
				case DocumentationComment:
					// these may appear between keyword and literal — keep waiting
					break;

				default:
					// Any other token between keyword and literal means the literal
					// is not a direct file argument; reset.
					awaitingLiteral = false;
					break;
			}
		}
	}

	/** Strips surrounding quotes (single, double, or triple) from a literal token. */
	private static String stripQuotes(String literalText) {
		if (literalText.startsWith("\"\"\"") && literalText.endsWith("\"\"\"") && literalText.length() >= 6) {
			return literalText.substring(3, literalText.length() - 3);
		}
		if (literalText.startsWith("'''") && literalText.endsWith("'''") && literalText.length() >= 6) {
			return literalText.substring(3, literalText.length() - 3);
		}
		if ((literalText.startsWith("\"") || literalText.startsWith("'")) && literalText.length() >= 2) {
			return literalText.substring(1, literalText.length() - 1);
		}
		return literalText;
	}

	/** Returns the length of the opening quote prefix (1 or 3). */
	private static int quotePrefixLen(String literalText) {
		if (literalText.startsWith("\"\"\"") || literalText.startsWith("'''")) {
			return 3;
		}
		return 1;
	}

	/** Computes 0-based [line, col] for the given character offset in content. */
	private static int[] lineAndCol(String content, int offset) {
		int line = 0;
		int col = 0;
		for (int i = 0; i < offset && i < content.length(); i++) {
			if (content.charAt(i) == '\n') {
				line++;
				col = 0;
			} else {
				col++;
			}
		}
		return new int[] { line, col };
	}

	private static void addFilePath(Set<String> names, String path) {
		if (path == null || path.isEmpty()) return;
		names.add(path);
		int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		if (lastSlash >= 0 && lastSlash < path.length() - 1) {
			names.add(path.substring(lastSlash + 1));
		}
	}
}
