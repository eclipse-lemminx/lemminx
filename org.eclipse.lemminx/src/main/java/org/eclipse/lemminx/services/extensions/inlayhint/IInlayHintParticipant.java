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
package org.eclipse.lemminx.services.extensions.inlayhint;

import java.util.List;

import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Inlay hint participant API.
 */
public interface IInlayHintParticipant {

	void doInlayHint(IInlayHintRequest request, List<InlayHint> inlayHints, CancelChecker cancelChecker);

}
