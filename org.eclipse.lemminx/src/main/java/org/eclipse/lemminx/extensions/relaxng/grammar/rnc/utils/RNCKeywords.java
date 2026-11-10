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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils;

/**
 * Constants for RNC (RELAX NG Compact Syntax) keywords.
 *
 * <p>Use case: code actions, completions, hover, symbols, and the scanner
 * reference these constants instead of duplicating string literals.</p>
 */
public class RNCKeywords {

	// Declaration keywords
	public static final String START = "start";
	public static final String NAMESPACE = "namespace";
	public static final String DATATYPES = "datatypes";
	public static final String INCLUDE = "include";
	public static final String EXTERNAL = "external";
	public static final String DIV = "div";
	public static final String GRAMMAR = "grammar";
	public static final String DEFAULT = "default";
	public static final String INHERIT = "inherit";

	// Pattern keywords
	public static final String ELEMENT = "element";
	public static final String ATTRIBUTE = "attribute";
	public static final String TEXT = "text";
	public static final String EMPTY = "empty";
	public static final String NOT_ALLOWED = "notAllowed";
	public static final String LIST = "list";
	public static final String MIXED = "mixed";
	public static final String PARENT = "parent";
	public static final String STRING = "string";
	public static final String TOKEN = "token";

	public static final String[] BUILTIN_PATTERNS = { TEXT, EMPTY, NOT_ALLOWED, STRING, TOKEN };

	private RNCKeywords() {
	}
}
