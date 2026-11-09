/**
 * Copyright (c) 2026 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Red Hat Inc. - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel.participants.codeactions;

import java.util.Collection;

import org.eclipse.lemminx.extensions.contentmodel.model.CMAttributeDeclaration;
import org.eclipse.lemminx.services.extensions.codeaction.InsertAttributeCodeAction;

/**
 * Code action to fix DTD MSG_REQUIRED_ATTRIBUTE_NOT_SPECIFIED error by
 * inserting missing required attributes.
 */
public class MSG_REQUIRED_ATTRIBUTE_NOT_SPECIFIEDCodeAction extends InsertAttributeCodeAction {

	@Override
	protected String getCodeActionTitle(Collection<CMAttributeDeclaration> attributes) {
		return "Insert required attributes";
	}
}
