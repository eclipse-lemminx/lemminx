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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

import org.eclipse.lemminx.AbstractTextDocumentService;
import org.eclipse.lemminx.ITextDocumentServiceContribution;
import org.eclipse.lemminx.XMLLanguageServer;

/**
 * SPI contribution that registers the {@link RNCTextDocumentService} for
 * handling RNC (RELAX NG Compact Syntax) files.
 */
public class RNCTextDocumentServiceContribution implements ITextDocumentServiceContribution {

	@Override
	public AbstractTextDocumentService<?> createService(XMLLanguageServer server) {
		return new RNCTextDocumentService(server);
	}
}
