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
package org.eclipse.lemminx.extensions.relaxng.grammar.rng.codeaction;

import org.eclipse.lemminx.extensions.relaxng.utils.RelaxNGUtils;

/**
 * Code action to fix {@code expected_define} error by replacing an invalid
 * element with a valid grammar child element.
 */
public class expected_defineCodeAction extends RNGReplaceElementCodeAction {

	public expected_defineCodeAction() {
		super(RelaxNGUtils.GRAMMAR_ELEMENTS);
	}
}
