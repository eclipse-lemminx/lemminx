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
package org.eclipse.lemminx.extensions.embeddedLanguages.settings;

import org.eclipse.lemminx.xpath.matcher.XPathMatcher;
import org.w3c.dom.Node;

/**
 * XML embedded language expression.
 *
 * <code>
 * {
 *   "xpath": "select/text()",
 *   "languageId": "sql"
 * }
 * </code>
 */
public class XMLEmbeddedLanguageExpression {

	private transient XPathMatcher pathMatcher;

	private String xpath;

	private String languageId;

	private String grammarPath;

	private String scopeName;

	public String getXPath() {
		return xpath;
	}

	public void setXPath(String xpath) {
		this.xpath = xpath;
	}

	public String getLanguageId() {
		return languageId;
	}

	public void setLanguageId(String languageId) {
		this.languageId = languageId;
	}

	public String getGrammarPath() {
		return grammarPath;
	}

	public void setGrammarPath(String grammarPath) {
		this.grammarPath = grammarPath;
	}

	public String getScopeName() {
		return scopeName;
	}

	public void setScopeName(String scopeName) {
		this.scopeName = scopeName;
	}

	public boolean match(final Node node) {
		if (xpath == null) {
			return false;
		}
		if (pathMatcher == null) {
			pathMatcher = new XPathMatcher(xpath);
		}
		return pathMatcher.match(node);
	}
}
