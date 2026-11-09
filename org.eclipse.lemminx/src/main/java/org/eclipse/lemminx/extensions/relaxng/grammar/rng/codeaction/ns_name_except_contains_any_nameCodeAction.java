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
 * Code action to fix {@code ns_name_except_contains_any_name} and
 * {@code ns_name_except_contains_ns_name} errors by replacing an invalid
 * element with a valid name class element.
 */
public class ns_name_except_contains_any_nameCodeAction extends RNGReplaceElementCodeAction {

	public ns_name_except_contains_any_nameCodeAction() {
		super(RelaxNGUtils.NSNAME_EXCEPT_ELEMENTS);
	}
}
