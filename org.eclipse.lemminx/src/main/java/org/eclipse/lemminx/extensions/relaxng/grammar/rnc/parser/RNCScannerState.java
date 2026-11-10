/*******************************************************************************
* Copyright (c) 2024 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser;

/**
 * Enumerates the internal states of the {@link RNCScanner} state machine.
 *
 * <p>Currently the scanner uses a single state ({@link #WithinContent}) because
 * RNC has no multi-line constructs that require state tracking across tokens
 * (unlike XML, which has separate states for tags, attributes, CDATA, etc.).
 * The enum exists for consistency with the XML scanner pattern and to support
 * future extensions (e.g., multi-line string handling or annotation blocks).</p>
 */
public enum RNCScannerState {

	/** The default scanning state — processing RNC content (declarations, patterns, operators). */
	WithinContent
}
