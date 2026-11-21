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
package org.eclipse.lemminx.services.extensions;

import java.util.function.Predicate;

import org.eclipse.lemminx.dom.DOMDocument;

/**
 * Common API for triggering a diagnostics re-publish for a subset of open
 * documents.
 *
 * <p>Implemented by both:
 * <ul>
 *   <li>{@link org.eclipse.lemminx.services.extensions.save.ISaveContext} — the
 *       save-triggered path: documents are accumulated across all participants and
 *       validated in a single pass at the end of the save context.</li>
 *   <li>{@link XMLExtensionsRegistry} — the async path: used by extensions that
 *       need to push diagnostics after a background event (e.g. workspace index
 *       scan completed) with no associated save event.</li>
 * </ul>
 */
public interface IDiagnosticsRefresher {

	/**
	 * Schedules a diagnostics re-publication for all open documents accepted by
	 * {@code filter}.
	 *
	 * @param filter predicate applied to each open {@link DOMDocument}.
	 */
	void refreshDiagnostics(Predicate<DOMDocument> filter);
}
