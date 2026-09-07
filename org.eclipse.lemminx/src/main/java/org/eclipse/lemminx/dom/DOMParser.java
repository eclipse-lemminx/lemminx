/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Initial code from https://github.com/Microsoft/vscode-html-languageservice
 * Initial copyright Copyright (C) Microsoft Corporation. All rights reserved.
 * Initial license: MIT
 *
 * Contributors:
 *  - Microsoft Corporation: Initial code, written in TypeScript, licensed under MIT license
 *  - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 */
package org.eclipse.lemminx.dom;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.green.GreenDocument;
import org.eclipse.lemminx.dom.green.GreenTreeBuilder;
import org.eclipse.lemminx.dom.green.IncrementalParser;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Tolerant XML parser.
 *
 */
public class DOMParser {

	private static final DOMParser INSTANCE = new DOMParser();

	public static DOMParser getInstance() {
		return INSTANCE;
	}

	private DOMParser() {

	}

	public DOMDocument parse(String text, String uri, URIResolverExtensionManager resolverExtensionManager) {
		return parse(new TextDocument(text, uri), resolverExtensionManager);
	}

	public DOMDocument parse(String text, String uri, URIResolverExtensionManager resolverExtensionManager,
			boolean ignoreWhitespaceContent) {
		return parse(new TextDocument(text, uri), resolverExtensionManager, ignoreWhitespaceContent);
	}

	public DOMDocument parse(TextDocument document, URIResolverExtensionManager resolverExtensionManager) {
		return parse(document, resolverExtensionManager, true);
	}

	public DOMDocument parse(TextDocument document, URIResolverExtensionManager resolverExtensionManager,
			boolean ignoreWhitespaceContent) {
		return parse(document, resolverExtensionManager, ignoreWhitespaceContent, null);
	}

	public DOMDocument parse(TextDocument document, URIResolverExtensionManager resolverExtensionManager,
			boolean ignoreWhitespaceContent, CancelChecker monitor) {
		CharSequence text = document.getTextSequence();
		String uri = document.getUri();
		GreenDocument greenDoc = GreenTreeBuilder.parse(text, uri, monitor);
		return buildDocument(greenDoc, document, resolverExtensionManager, ignoreWhitespaceContent, monitor);
	}

	public DOMDocument parseIncremental(TextDocument document,
			GreenDocument previousGreenDoc, int editStart, int deleteLength, int insertLength,
			URIResolverExtensionManager resolverExtensionManager,
			boolean ignoreWhitespaceContent, CancelChecker monitor) {
		CharSequence text = document.getTextSequence();
		String uri = document.getUri();
		GreenDocument greenDoc = IncrementalParser.incrementalParse(
				previousGreenDoc, text, editStart, deleteLength, insertLength, uri, monitor);
		DOMDocument xmlDocument = RedTreeBuilder.buildLazy(greenDoc, document, resolverExtensionManager);
		xmlDocument.setGreenDocument(greenDoc);
		xmlDocument.setCancelChecker(monitor);
		return xmlDocument;
	}

	private static DOMDocument buildDocument(GreenDocument greenDoc, TextDocument document,
			URIResolverExtensionManager resolverExtensionManager,
			boolean ignoreWhitespaceContent, CancelChecker monitor) {
		DOMDocument xmlDocument = ignoreWhitespaceContent
				? RedTreeBuilder.buildLazy(greenDoc, document, resolverExtensionManager)
				: RedTreeBuilder.build(greenDoc, document, resolverExtensionManager, ignoreWhitespaceContent);
		xmlDocument.setGreenDocument(greenDoc);
		xmlDocument.setCancelChecker(monitor);
		return xmlDocument;
	}
}
