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
package org.eclipse.lemminx.extensions.relaxng.xml.codeaction;

import static java.lang.System.lineSeparator;
import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.caSnippet;
import static org.eclipse.lemminx.XMLAssert.d;
import static org.eclipse.lemminx.XMLAssert.sne;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.eclipse.lemminx.XMLAssert.testCodeActionsFor;
import static org.eclipse.lemminx.XMLAssert.testDiagnosticsFor;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.relaxng.xml.validator.RelaxNGErrorCode;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

/**
 * Test for code actions: remove undeclared attribute, replace invalid element,
 * and replace invalid attribute value for RelaxNG.
 */
public class ReplaceCodeActionTest extends AbstractCacheBasedTest {

	// ---------- Remove attribute ----------

	/**
	 * Use case: element 'card' in addressBook_v1.rng defines no attributes.
	 * Adding any attribute produces no_attributes_allowed.
	 * Code action: remove the undeclared attribute.
	 */
	@Test
	public void no_attributes_allowed_removeAttribute() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v1.rng\" ?>" + lineSeparator() + //
				"<addressBook>" + lineSeparator() + //
				"  <card XXXX=\"\">" + lineSeparator() + //
				"    <name>John</name>" + lineSeparator() + //
				"    <email>john@example.com</email>" + lineSeparator() + //
				"  </card>" + lineSeparator() + //
				"</addressBook>";
		Diagnostic d = d(2, 8, 12, RelaxNGErrorCode.no_attributes_allowed);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(2, 7, 2, 15, "")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(2, 7, 2, 15, "$0")));
	}

	/**
	 * Use case: element 'name' in addressBook_v1.rng defines no attributes.
	 * Adding attribute 'foo' produces no_attributes_allowed.
	 * Code action: remove the undeclared attribute.
	 */
	@Test
	public void no_attributes_allowed_removeAttribute_name() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v1.rng\" ?>" + lineSeparator() + //
				"<addressBook>" + lineSeparator() + //
				"  <card>" + lineSeparator() + //
				"    <name foo=\"bar\">John</name>" + lineSeparator() + //
				"    <email>john@example.com</email>" + lineSeparator() + //
				"  </card>" + lineSeparator() + //
				"</addressBook>";
		Diagnostic d = d(3, 10, 13, RelaxNGErrorCode.no_attributes_allowed);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(3, 9, 3, 19, "")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(3, 9, 3, 19, "$0")));
	}

	/**
	 * Use case: element 'item' in simple-enum.rng defines attributes 'status'
	 * and optional 'priority'. Adding 'badattr' produces invalid_attribute_name
	 * because the element does define some attributes.
	 * Code action: remove the undeclared attribute.
	 */
	@Test
	public void invalid_attribute_name_removeAttribute_withEnum() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/simple-enum.rng\" ?>" + lineSeparator() + //
				"<root>" + lineSeparator() + //
				"  <item status=\"active\" badattr=\"x\">text</item>" + lineSeparator() + //
				"</root>";
		Diagnostic d = d(2, 24, 31, RelaxNGErrorCode.invalid_attribute_name);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(2, 23, 2, 35, "")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(2, 23, 2, 35, "$0")));
	}

	// ---------- Replace element ----------

	/**
	 * Use case: element 'nameXXX' inside 'card' is not defined in
	 * addressBook_v1.rng. Validator reports unknown_element.
	 * 'email' after it reports unexpected_element_required_element_missing.
	 * Code action on unknown_element: Did you mean 'name'?
	 */
	@Test
	public void unknown_element_replaceElement() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v1.rng\" ?>" + lineSeparator() + //
				"<addressBook>" + lineSeparator() + //
				"  <card>" + lineSeparator() + //
				"    <nameXXX>John</nameXXX>" + lineSeparator() + //
				"    <email>john@example.com</email>" + lineSeparator() + //
				"  </card>" + lineSeparator() + //
				"</addressBook>";
		Diagnostic d = d(3, 5, 12, RelaxNGErrorCode.unknown_element);
		testDiagnosticsFor(xml, d,
				d(4, 5, 10, RelaxNGErrorCode.unexpected_element_required_element_missing));
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(3, 5, 3, 12, "name"), te(3, 19, 3, 26, "name")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(3, 5, 3, 12, "name$0"), sne(3, 19, 3, 26, "name$0")));
	}

	// ---------- Replace attribute value ----------

	/**
	 * Use case: attribute 'status' on 'item' has value 'activ' which is close
	 * to 'active' and 'inactive' in the enumeration (active|inactive|pending).
	 * Code action: Did you mean 'active'? / Did you mean 'inactive'?
	 */
	@Test
	public void invalid_attribute_value_replaceValue() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/simple-enum.rng\" ?>" + lineSeparator() + //
				"<root>" + lineSeparator() + //
				"  <item status=\"activ\">text</item>" + lineSeparator() + //
				"</root>";
		Diagnostic d = d(2, 15, 22, RelaxNGErrorCode.invalid_attribute_value);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(2, 16, 2, 21, "active")),
				ca(d, te(2, 16, 2, 21, "inactive")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(2, 16, 2, 21, "active$0")),
				caSnippet(d, sne(2, 16, 2, 21, "inactive$0")));
	}

	/**
	 * Use case: attribute 'status' on 'item' has value 'xxxx' which matches
	 * no enumeration value. All possible values are offered as replacements.
	 * Code action: Replace with 'active', 'inactive', 'pending'.
	 */
	@Test
	public void invalid_attribute_value_replaceValue_noSimilar() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/simple-enum.rng\" ?>" + lineSeparator() + //
				"<root>" + lineSeparator() + //
				"  <item status=\"xxxx\">text</item>" + lineSeparator() + //
				"</root>";
		Diagnostic d = d(2, 15, 21, RelaxNGErrorCode.invalid_attribute_value);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(2, 16, 2, 20, "active")),
				ca(d, te(2, 16, 2, 20, "inactive")),
				ca(d, te(2, 16, 2, 20, "pending")));
		// With snippet support
		testCodeActionsFor(xml, d, XMLAssert.createSnippetEditSettings(),
				caSnippet(d, sne(2, 16, 2, 20, "active$0")),
				caSnippet(d, sne(2, 16, 2, 20, "inactive$0")),
				caSnippet(d, sne(2, 16, 2, 20, "pending$0")));
	}
}
