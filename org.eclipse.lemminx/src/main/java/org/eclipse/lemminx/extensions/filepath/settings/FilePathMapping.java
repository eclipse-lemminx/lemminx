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
package org.eclipse.lemminx.extensions.filepath.settings;

import java.util.List;

import org.eclipse.lemminx.settings.DocumentMatcher;

/**
 * File path mapping which stores list of {@link FilePathExpression} applied
 * for a given input file pattern.
 * 
 * @author Angelo ZERR
 *
 */
public class FilePathMapping extends DocumentMatcher {

	private List<FilePathExpression> expressions;

	/**
	 * Returns list of file path expressions.
	 * 
	 * @return list of file path expressions.
	 */
	public List<FilePathExpression> getExpressions() {
		return expressions;
	}

	/**
	 * Set list of file path expressions.
	 * 
	 * @param expressions list of file path expressions.
	 */
	public void setExpressions(List<FilePathExpression> expressions) {
		this.expressions = expressions;
	}
}
