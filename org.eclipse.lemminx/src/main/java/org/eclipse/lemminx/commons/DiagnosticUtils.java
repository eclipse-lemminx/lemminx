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
package org.eclipse.lemminx.commons;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Utilities for {@link Diagnostic}.
 * 
 * @author Arun Venmany
 *
 */
public class DiagnosticUtils {

	/**
	 * Extracts the diagnostic message as a String from Either<String, MarkupContent>.
	 * <p>
	 * In LSP4J 1.0.0, diagnostic messages can be either plain strings or rich MarkupContent.
	 * This method extracts the plain text representation from either format.
	 * </p>
	 *
	 * @param diagnostic the diagnostic
	 * @return the diagnostic message as a String
	 */
	public static String getDiagnosticMessage(Diagnostic diagnostic) {
		Either<String, MarkupContent> message = diagnostic.getMessage();
		return message.isLeft() ? message.getLeft() : message.getRight().getValue();
	}

	private DiagnosticUtils() {
		// Utility class, no instantiation
	}
}