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

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.settings.XMLInlayHintSettings;
import org.eclipse.lsp4j.Range;

/**
 * Inlay hint request API.
 */
public interface IInlayHintRequest {

	/**
	 * Returns the DOM document.
	 *
	 * @return the DOM document
	 */
	DOMDocument getDocument();

	/**
	 * Returns the range for which inlay hints are requested.
	 *
	 * @return the range
	 */
	Range getRange();

	/**
	 * Returns the inlay hint settings.
	 *
	 * @return the inlay hint settings
	 */
	XMLInlayHintSettings getInlayHintSettings();
}
