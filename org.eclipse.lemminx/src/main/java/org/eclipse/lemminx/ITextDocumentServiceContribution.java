/**
 *  Copyright (c) 2026 Angelo ZERR.
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
package org.eclipse.lemminx;

/**
 * SPI interface for contributing additional {@link AbstractTextDocumentService}
 * implementations that handle non-XML file types (e.g., RNC, DTD).
 *
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader} and
 * registered in {@code META-INF/services/org.eclipse.lemminx.ITextDocumentServiceContribution}.
 * </p>
 */
public interface ITextDocumentServiceContribution {

	/**
	 * Creates the text document service for the contributed file type.
	 *
	 * @param server the language server instance.
	 * @return the text document service.
	 */
	AbstractTextDocumentService<?> createService(XMLLanguageServer server);
}
