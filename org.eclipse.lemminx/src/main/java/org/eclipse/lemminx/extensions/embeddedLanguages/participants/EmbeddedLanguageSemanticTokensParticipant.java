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
package org.eclipse.lemminx.extensions.embeddedLanguages.participants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMText;
import org.eclipse.lemminx.extensions.embeddedLanguages.XMLEmbeddedLanguagesPlugin;
import org.eclipse.lemminx.extensions.embeddedLanguages.settings.XMLEmbeddedLanguageExpression;
import org.eclipse.lemminx.extensions.embeddedLanguages.settings.XMLEmbeddedLanguages;
import org.eclipse.lemminx.extensions.embeddedLanguages.settings.XMLEmbeddedLanguagesSettings;
import org.eclipse.lemminx.services.extensions.semantictokens.ISemanticTokensParticipant;
import org.eclipse.lemminx.services.extensions.semantictokens.ISemanticTokensRequest;
import org.eclipse.lemminx.services.extensions.semantictokens.SemanticTokensData;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Semantic tokens participant for embedded language content in XML.
 *
 * <p>
 * Uses the {@code xml.embeddedLanguages} settings to find text/CDATA nodes that
 * contain embedded code, then tokenizes them using an
 * {@link IEmbeddedLanguageTokenizer} (backed by TM4E when available).
 * </p>
 */
public class EmbeddedLanguageSemanticTokensParticipant implements ISemanticTokensParticipant {

	private static final Logger LOGGER = Logger.getLogger(EmbeddedLanguageSemanticTokensParticipant.class.getName());

	private final XMLEmbeddedLanguagesPlugin plugin;

	public EmbeddedLanguageSemanticTokensParticipant(XMLEmbeddedLanguagesPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void findSemanticTokens(ISemanticTokensRequest request, SemanticTokensData builder,
			CancelChecker cancelChecker) {
		IEmbeddedLanguageTokenizer tokenizer = plugin.getTokenizer();
		if (tokenizer == null) {
			return;
		}
		DOMDocument document = request.getDocument();
		List<EmbeddedRegion> regions = findEmbeddedRegions(document);
		if (regions.isEmpty()) {
			return;
		}
		for (EmbeddedRegion region : regions) {
			cancelChecker.checkCanceled();
			tokenizeRegion(region, tokenizer, builder);
		}
	}

	private List<EmbeddedRegion> findEmbeddedRegions(DOMDocument document) {
		XMLEmbeddedLanguagesSettings settings = plugin.getEmbeddedLanguagesSettings();
		if (settings == null) {
			return Collections.emptyList();
		}
		List<XMLEmbeddedLanguages> embeddedLanguagesDef = settings.getEmbeddedLanguages();
		if (embeddedLanguagesDef == null) {
			return Collections.emptyList();
		}
		List<XMLEmbeddedLanguageExpression> expressions = new ArrayList<>();
		for (XMLEmbeddedLanguages embeddedLanguages : embeddedLanguagesDef) {
			if (embeddedLanguages.matches(document)) {
				if (embeddedLanguages.getExpressions() != null) {
					for (XMLEmbeddedLanguageExpression expr : embeddedLanguages.getExpressions()) {
						if (expr.getGrammarPath() != null) {
							plugin.registerGrammarPath(expr.getLanguageId(), expr.getGrammarPath());
						} else {
							plugin.resolveGrammarIfNeeded(expr.getLanguageId());
						}
						expressions.add(expr);
					}
				}
			}
		}
		if (expressions.isEmpty()) {
			return Collections.emptyList();
		}
		List<EmbeddedRegion> regions = new ArrayList<>();
		collectEmbeddedRegions(document, expressions, regions);
		return regions;
	}

	private void collectEmbeddedRegions(DOMNode node, List<XMLEmbeddedLanguageExpression> expressions,
			List<EmbeddedRegion> regions) {
		if (node.isText() || node.isCDATA()) {
			for (XMLEmbeddedLanguageExpression expression : expressions) {
				if (expression.match(node)) {
					DOMText textNode = (DOMText) node;
					int startContent = textNode.getStartContent();
					int endContent = textNode.getEndContent();
					String content = node.getOwnerDocument().getText().substring(startContent, endContent);
					regions.add(new EmbeddedRegion(content, expression.getLanguageId(), startContent));
					break;
				}
			}
		}
		if (node.hasChildNodes()) {
			for (DOMNode child : node.getChildren()) {
				collectEmbeddedRegions(child, expressions, regions);
			}
		}
	}

	private void tokenizeRegion(EmbeddedRegion region, IEmbeddedLanguageTokenizer tokenizer,
			SemanticTokensData builder) {
		boolean trace = LOGGER.isLoggable(Level.FINEST);
		List<IEmbeddedLanguageTokenizer.EmbeddedToken> tokens = tokenizer.tokenize(region.content,
				region.languageId);
		for (IEmbeddedLanguageTokenizer.EmbeddedToken token : tokens) {
			TextMateScopeMapper.TokenMapping mapping = TextMateScopeMapper.toTokenMapping(token.getScopes());
			if (trace) {
				String text = region.content.substring(token.getStartIndex(), token.getEndIndex());
				LOGGER.finest("[SemanticToken] '" + text + "' scopes=" + token.getScopes()
						+ " -> type=" + (mapping != null ? mapping.tokenType : "none")
						+ ", modifiers=" + (mapping != null ? mapping.tokenModifiers : "none"));
			}
			if (mapping != null) {
				int offset = region.startOffset + token.getStartIndex();
				int length = token.getEndIndex() - token.getStartIndex();
				builder.addToken(offset, length, mapping.tokenType, mapping.tokenModifiers);
			}
		}
	}

	private static class EmbeddedRegion {
		final String content;
		final String languageId;
		final int startOffset;

		EmbeddedRegion(String content, String languageId, int startOffset) {
			this.content = content;
			this.languageId = languageId;
			this.startOffset = startOffset;
		}
	}
}
