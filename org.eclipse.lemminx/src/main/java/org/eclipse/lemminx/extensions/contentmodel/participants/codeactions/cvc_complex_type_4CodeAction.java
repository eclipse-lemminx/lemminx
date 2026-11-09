/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel.participants.codeactions;

import java.util.Collection;

import org.eclipse.lemminx.extensions.contentmodel.model.CMAttributeDeclaration;
import org.eclipse.lemminx.services.extensions.codeaction.InsertAttributeCodeAction;

/**
 * Code action to fix cvc-complex-type.4 error.
 *
 */
public class cvc_complex_type_4CodeAction extends InsertAttributeCodeAction {

	@Override
	protected String getCodeActionTitle(Collection<CMAttributeDeclaration> attributes) {
		return "Insert required attributes";
	}
}
