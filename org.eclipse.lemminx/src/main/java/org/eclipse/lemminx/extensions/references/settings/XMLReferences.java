/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.references.settings;

import java.util.List;

import org.eclipse.lemminx.settings.DocumentMatcher;

/**
 * XML references which stores list of {@link XMLReferenceExpression} applied
 * for a give pattern.
 * 
 * @author Angelo ZERR
 *
 */
public class XMLReferences extends DocumentMatcher {

	private transient boolean updated;

	private String prefix;

	private Boolean multiple;

	private List<XMLReferenceExpression> expressions;

	/**
	 * Returns list of XML reference expressions.
	 * 
	 * @return list of XML reference expressions.
	 */
	public List<XMLReferenceExpression> getExpressions() {
		if (!updated) {
			if (prefix != null || multiple != null) {
				// Update expressions with global settings
				for (XMLReferenceExpression expression : expressions) {
					if (expression.getPrefix() == null) {
						expression.setPrefix(prefix);
					}
					if (expression.getMultiple() == null) {
						expression.setMultiple(multiple);
					}
				}
			}
			updated = true;
		}
		return expressions;
	}

	/**
	 * Set list of XML reference expressions.
	 * 
	 * @param expressions list of XML reference expressions.
	 */
	public void setExpressions(List<XMLReferenceExpression> expressions) {
		this.expressions = expressions;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public Boolean getMultiple() {
		return multiple;
	}

	public void setMultiple(Boolean multiple) {
		this.multiple = multiple;
	}

}
