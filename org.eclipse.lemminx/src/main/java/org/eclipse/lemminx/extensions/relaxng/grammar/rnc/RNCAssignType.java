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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

/**
 * The type of assignment operator used in an RNC define or start declaration.
 *
 * <p>RNC supports three assignment operators that control how a pattern is
 * combined with an existing definition of the same name:</p>
 * <ul>
 *   <li>{@code =} — simple assignment (replaces any previous definition)</li>
 *   <li>{@code |=} — choice combination (adds alternatives via {@code |})</li>
 *   <li>{@code &=} — interleave combination (adds interleaved content via {@code &})</li>
 * </ul>
 *
 * <p>Use case: used by {@link RNCDefine} and {@link RNCStart} to record which
 * operator appeared after the name, so code lens and diagnostics can reason
 * about whether a define is being overridden or extended.</p>
 *
 * <p>Example RNC:</p>
 * <pre>
 * root = element root { text }       → ASSIGN
 * root |= element alt-root { text }  → CHOICE
 * root &amp;= attribute id { text }  → INTERLEAVE
 * </pre>
 */
public enum RNCAssignType {

	/** Simple assignment ({@code =}). Example: {@code start = root}. */
	ASSIGN,

	/** Choice combination ({@code |=}). Example: {@code content |= element p { text }}. */
	CHOICE,

	/** Interleave combination ({@code &=}). Example: {@code content &= attribute id { text }}. */
	INTERLEAVE
}
