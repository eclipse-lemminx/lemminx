/*******************************************************************************
 * Copyright (c) 2026 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.lemminx.converter;

/**
 * Provides read-only access to the HTML attributes accumulated while scanning
 * the current open tag. {@link HtmlConverter} implements this interface so that
 * the converter itself can be passed to {@link TagHandler#open(HtmlAttributes, StringBuilder)}
 * without exposing the full converter API.
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>{@code <a href="…">} — {@link #getHref()} returns the link destination</li>
 *   <li>{@code <img src="…" alt="…">} — {@link #getSrc()} and {@link #getAlt()} return
 *       the image source and alternative text</li>
 *   <li>{@code <td colspan="3">} — {@link #getColspan()} returns the column span (defaults to 1)</li>
 * </ul>
 *
 * @see TagHandler
 * @see HtmlConverter
 */
public interface HtmlAttributes {

	/**
	 * Returns the value of the {@code href} attribute, or {@code null} if absent.
	 *
	 * <p>Use case: {@code <a href="https://example.com">link</a>}
	 * → returns {@code "https://example.com"}.</p>
	 */
	String getHref();

	/**
	 * Returns the value of the {@code src} attribute, or {@code null} if absent.
	 *
	 * <p>Use case: {@code <img src="logo.png" />}
	 * → returns {@code "logo.png"}.</p>
	 */
	String getSrc();

	/**
	 * Returns the value of the {@code alt} attribute, or {@code null} if absent.
	 *
	 * <p>Use case: {@code <img alt="Company logo" />}
	 * → returns {@code "Company logo"}.</p>
	 */
	String getAlt();

	/**
	 * Returns the value of the {@code colspan} attribute, or {@code 1} if absent
	 * or invalid.
	 *
	 * <p>Use case: {@code <td colspan="3">merged</td>}
	 * → returns {@code 3}.</p>
	 */
	int getColspan();

	/**
	 * Returns the value of the {@code rowspan} attribute, or {@code 1} if absent
	 * or invalid.
	 *
	 * <p>Use case: {@code <td rowspan="2">spans two rows</td>}
	 * → returns {@code 2}.</p>
	 */
	int getRowspan();
}
