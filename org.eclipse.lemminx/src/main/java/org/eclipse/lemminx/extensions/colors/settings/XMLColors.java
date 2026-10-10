/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.colors.settings;

import java.util.List;

import org.eclipse.lemminx.settings.DocumentMatcher;

/**
 * XML colors which stores list of {@link XMLColorExpression} applied
 * for a give pattern.
 * 
 * @author Angelo ZERR
 *
 */
public class XMLColors extends DocumentMatcher {

	private List<XMLColorExpression> expressions;

	/**
	 * Returns list of XML color expressions.
	 * 
	 * @return list of XML color expressions.
	 */
	public List<XMLColorExpression> getExpressions() {
		return expressions;
	}

	/**
	 * Set list of XML color expressions.
	 * 
	 * @param expressions list of XML color expressions.
	 */
	public void setExpressions(List<XMLColorExpression> expressions) {
		this.expressions = expressions;
	}
}
