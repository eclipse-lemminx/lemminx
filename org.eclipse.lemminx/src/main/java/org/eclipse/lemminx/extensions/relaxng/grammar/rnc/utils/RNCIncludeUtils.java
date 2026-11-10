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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xerces.impl.XMLEntityManager;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCFileReferenceNode;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCParser;

/**
 * Utilities for resolving and parsing included/external RNC files.
 *
 * <p>Use case: when computing hover, completion, definition, or references for
 * an {@code include} or {@code external} directive, the referenced RNC file
 * must be resolved (relative URI → absolute URI) and parsed into an
 * {@link RNCDocument} so that its defines are available for cross-file features.</p>
 *
 * <p>Example: given {@code include "types.rnc"} in {@code /schemas/main.rnc},
 * {@link #resolveUri} expands the relative path to
 * {@code file:///schemas/types.rnc}, and {@link #parseFile} reads and parses it.</p>
 */
public class RNCIncludeUtils {

	private static final Logger LOGGER = Logger.getLogger(RNCIncludeUtils.class.getName());

	private RNCIncludeUtils() {
	}

	/**
	 * Resolves the URI of a file reference and parses the target RNC file.
	 *
	 * <p>Use case: hover over a define reference inside an {@code include}
	 * body needs the included file's AST to show the define's text.</p>
	 *
	 * @param ref         the include or external reference node.
	 * @param documentUri the URI of the document containing the reference
	 *                    (used as base for relative resolution).
	 * @return the parsed {@link RNCDocument}, or {@code null} if the URI
	 *         cannot be resolved or the file cannot be parsed.
	 */
	public static RNCDocument resolveAndParse(RNCFileReferenceNode ref, String documentUri) {
		String resolvedUri = resolveUri(ref, documentUri);
		if (resolvedUri == null) {
			return null;
		}
		return parseFile(resolvedUri);
	}

	/**
	 * Resolves the relative URI from a file reference node against the base
	 * document URI.
	 *
	 * <p>Use case: document links and definition navigation need the absolute
	 * URI to create clickable links or to locate the target file on disk.</p>
	 *
	 * @param ref         the include or external reference node containing
	 *                    the relative URI (e.g. {@code "types.rnc"}).
	 * @param documentUri the absolute URI of the current document.
	 * @return the resolved absolute URI, or {@code null} if resolution fails.
	 */
	public static String resolveUri(RNCFileReferenceNode ref, String documentUri) {
		String refUri = ref.getUri();
		if (refUri == null || refUri.isEmpty()) {
			return null;
		}
		try {
			return XMLEntityManager.expandSystemId(refUri, documentUri, false);
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "Could not resolve included RNC file URI: " + refUri, e);
			return null;
		}
	}

	/**
	 * Reads and parses an RNC file from the filesystem.
	 *
	 * <p>Use case: after resolving an include URI, the file content must be
	 * loaded and parsed to make its defines available for cross-file
	 * completion, hover, references, and rename.</p>
	 *
	 * @param resolvedUri the absolute file URI (e.g. {@code "file:///path/to/types.rnc"}).
	 * @return the parsed {@link RNCDocument}, or {@code null} if the file
	 *         does not exist or cannot be parsed.
	 */
	public static RNCDocument parseFile(String resolvedUri) {
		if (resolvedUri == null) {
			return null;
		}
		try {
			Path path = Paths.get(new URI(resolvedUri));
			if (!Files.isRegularFile(path)) {
				return null;
			}
			String content = Files.readString(path);
			return RNCParser.getInstance().parse(content, resolvedUri);
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "Could not parse RNC file: " + resolvedUri, e);
			return null;
		}
	}
}
