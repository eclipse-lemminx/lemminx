/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.services.format;

/**
 * Classification of an XML element that determines how the formatter handles
 * its whitespace. The category is computed from the element's content model
 * (children, text, or both) and from {@code xml:space} attributes.
 *
 * <p>
 * The category drives every formatting decision in
 * {@link DOMElementFormatter}: indentation of children, wrapping at
 * {@code maxLineWidth}, whitespace collapsing, and preservation.
 * </p>
 *
 * @author Angelo ZERR
 *
 * @see <a href="https://www.oxygenxml.com/doc/versions/24.0/ug-editorEclipse/topics/format-and-indent-xml.html">
 *      Oxygen XML — Format and Indent</a>
 */
public enum FormatElementCategory {

	/**
	 * Element contains only child elements (no text content).
	 * All whitespace between children is insignificant and is replaced
	 * with newlines and proper indentation.
	 *
	 * <p>Use case:</p>
	 * <pre>
	 * &lt;root&gt;&lt;a&gt;&lt;b/&gt;&lt;/a&gt;&lt;/root&gt;
	 * →
	 * &lt;root&gt;
	 *   &lt;a&gt;
	 *     &lt;b /&gt;
	 *   &lt;/a&gt;
	 * &lt;/root&gt;
	 * </pre>
	 */
	IgnoreSpace,

	/**
	 * Element contains only text (no child elements).
	 * Consecutive whitespace characters are collapsed to a single space.
	 * Line breaks are preserved unless {@code joinContentLines} is enabled.
	 *
	 * <p>Use case:</p>
	 * <pre>
	 * &lt;p&gt;  hello   world  &lt;/p&gt;  →  &lt;p&gt;hello world&lt;/p&gt;
	 * </pre>
	 */
	NormalizeSpace,

	/**
	 * Element contains both text and child elements.
	 * Inline flow is preserved: a single whitespace between text and
	 * elements is significant. Child elements only wrap to new lines
	 * when {@code maxLineWidth} is exceeded or indentation needs
	 * normalizing.
	 *
	 * <p>Use case (no overflow — stays inline):</p>
	 * <pre>
	 * &lt;p&gt;text &lt;b&gt;bold&lt;/b&gt; more&lt;/p&gt;  →  unchanged
	 * </pre>
	 *
	 * <p>Use case (overflow — element wraps):</p>
	 * <pre>
	 * &lt;p&gt;text &lt;a&gt;aaa&lt;/a&gt;&lt;b&gt;bbb&lt;/b&gt;&lt;/p&gt;
	 * →
	 * &lt;p&gt;text &lt;a&gt;aaa&lt;/a&gt;
	 *   &lt;b&gt;bbb&lt;/b&gt;&lt;/p&gt;
	 * </pre>
	 */
	MixedContent,

	/**
	 * Element has {@code xml:space="preserve"} — all whitespace is
	 * significant. No changes are made to spacing within this element.
	 * Child elements may belong to a different category and may be
	 * reformatted independently.
	 *
	 * <p>
	 * Attribute values are always treated as preserve-space regardless
	 * of the element's category.
	 * </p>
	 *
	 * <p>Use case:</p>
	 * <pre>
	 * &lt;pre xml:space="preserve"&gt;  keep  spaces  &lt;/pre&gt;  →  unchanged
	 * </pre>
	 */
	PreserveSpace
}
