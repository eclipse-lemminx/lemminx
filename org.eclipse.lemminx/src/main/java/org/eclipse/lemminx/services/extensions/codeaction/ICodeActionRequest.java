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
package org.eclipse.lemminx.services.extensions.codeaction;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lemminx.extensions.contentmodel.generator.XMLElementGenerator;
import org.eclipse.lemminx.commons.BadLocationException;

/**
 * Code action request API.
 * 
 * @author Angelo ZERR
 *
 */
public interface ICodeActionRequest extends IBaseCodeActionRequest {

	/**
	 * Returns the diagnostic.
	 * 
	 * @return the diagnostic.
	 */
	Diagnostic getDiagnostic();

	/**
	 * Returns the code action range.
	 * 
	 * @return the code action range.
	 */
	Range getRange();

	XMLElementGenerator getXMLGenerator() throws BadLocationException;

	/**
	 * Returns true if the client can supportcodeAction/resolve and false otherwise.
	 * 
	 * @return true if the client can supportcodeAction/resolve and false otherwise.
	 */
	default boolean canSupportResolve() {
		return getSharedSettings().getCodeActionSettings().canSupportResolve();
	}
}
