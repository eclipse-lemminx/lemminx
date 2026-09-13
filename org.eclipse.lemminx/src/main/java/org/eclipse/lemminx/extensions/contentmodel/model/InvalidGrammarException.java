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
package org.eclipse.lemminx.extensions.contentmodel.model;

import java.util.List;

/**
 * Exception thrown when a grammar file (XSD, DTD, etc.) cannot be loaded
 * because it contains errors.
 */
public class InvalidGrammarException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final List<String> errors;

	public InvalidGrammarException(List<String> errors) {
		super(formatErrors(errors));
		this.errors = errors;
	}

	public List<String> getErrors() {
		return errors;
	}

	private static String formatErrors(List<String> errors) {
		if (errors.size() == 1) {
			return errors.get(0);
		}
		return String.join("\n", errors);
	}
}
