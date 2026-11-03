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

import java.util.List;

import org.eclipse.lemminx.settings.DocumentMatcher;

/**
 * XML embedded languages which stores list of
 * {@link XMLEmbeddedLanguageExpression} applied for a given document matcher.
 */
public class XMLEmbeddedLanguages extends DocumentMatcher {

	private List<XMLEmbeddedLanguageExpression> expressions;

	public List<XMLEmbeddedLanguageExpression> getExpressions() {
		return expressions;
	}

	public void setExpressions(List<XMLEmbeddedLanguageExpression> expressions) {
		this.expressions = expressions;
	}
}
