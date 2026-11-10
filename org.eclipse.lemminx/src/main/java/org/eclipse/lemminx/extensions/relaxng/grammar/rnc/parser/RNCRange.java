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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser;

import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;

/**
 * Represents a range (start/end offset pair) within an RNC document, with
 * access to the owning {@link RNCDocument}.
 *
 * <p>Use case: implemented by all RNC AST node types ({@link org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNode}
 * and subclasses) to provide offset-based positioning for language features
 * such as diagnostics, code actions, folding, and highlighting.</p>
 *
 * @author Angelo ZERR
 */
public interface RNCRange {

	/**
	 * Returns the start offset of the node.
	 *
	 * @return the start offset of the node.
	 */
	int getStart();

	/**
	 * Returns the end offset of the node.
	 *
	 * @return the end offset of the node.
	 */
	int getEnd();

	/**
	 * Returns the owner document.
	 *
	 * @return the owner document.
	 */
	RNCDocument getOwnerDocument();
}
