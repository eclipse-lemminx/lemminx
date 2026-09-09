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

import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.d;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.eclipse.lemminx.XMLAssert.testCodeActionsFor;
import static org.eclipse.lemminx.XMLAssert.testDiagnosticsFor;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.extensions.relaxng.xml.validator.RelaxNGErrorCode;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

/**
 * Test for code action to insert missing required attributes for RelaxNG.
 */
public class MissingAttributeCodeActionTest extends AbstractCacheBasedTest {

	@Test
	public void required_attribute_missing() throws Exception {
		// <key> is missing both required attributes "name" and "type"
		String xml = "<?xml-model href=\"src/test/resources/relaxng/choiceAttributeGroup.rng\" ?>\r\n" + //
				"<root>\r\n" + //
				"  <key></key>\r\n" + //
				"</root>";
		Diagnostic d = d(2, 3, 2, 6, RelaxNGErrorCode.required_attributes_missing);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(2, 6, 2, 6, " name=\"\" type=\"\"")));
	}

	@Test
	public void required_attribute_missing_with_existing() throws Exception {
		// <key> has "name" but is missing "type"
		String xml = "<?xml-model href=\"src/test/resources/relaxng/choiceAttributeGroup.rng\" ?>\r\n" + //
				"<root>\r\n" + //
				"  <key name=\"foo\"></key>\r\n" + //
				"</root>";
		Diagnostic d = d(2, 3, 2, 6, RelaxNGErrorCode.required_attribute_missing);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(2, 17, 2, 17, " type=\"\"")));
	}

	@Test
	public void required_attribute_missing_choice_branch() throws Exception {
		// <Item Type="Bar"> selects the Bar branch, making BarAttr required in context
		String xml = "<?xml-model href=\"src/test/resources/relaxng/choiceAttributeGroupDistinct.rng\" ?>\r\n" + //
				"<Items>\r\n" + //
				"  <Item Type=\"Bar\"></Item>\r\n" + //
				"</Items>";
		Diagnostic d = d(2, 3, 2, 7, RelaxNGErrorCode.required_attribute_missing);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(2, 18, 2, 18, " BarAttr=\"\"")));
	}

	@Test
	public void required_attribute_missing_TEI_tagUsage() throws Exception {
		// TEI <tagUsage> is missing required attribute "gi"
		String xml = "<?xml-model href=\"src/test/resources/relaxng/tei_all.rng\" ?>\r\n" + //
				"<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\r\n" + //
				"	<teiHeader>\r\n" + //
				"		<fileDesc>\r\n" + //
				"			<titleStmt>\r\n" + //
				"				<title></title>\r\n" + //
				"			</titleStmt>\r\n" + //
				"			<publicationStmt>\r\n" + //
				"				<ab></ab>\r\n" + //
				"			</publicationStmt>\r\n" + //
				"			<sourceDesc>\r\n" + //
				"				<ab></ab>\r\n" + //
				"			</sourceDesc>\r\n" + //
				"		</fileDesc>\r\n" + //
				"		<encodingDesc>\r\n" + //
				"			<tagsDecl>\r\n" + //
				"				<namespace name=\"http://www.tei-c.org/ns/1.0\">\r\n" + //
				"					<tagUsage></tagUsage>\r\n" + //
				"				</namespace>\r\n" + //
				"			</tagsDecl>\r\n" + //
				"		</encodingDesc>\r\n" + //
				"	</teiHeader>\r\n" + //
				"	<text>\r\n" + //
				"		<body>\r\n" + //
				"			<div></div>\r\n" + //
				"		</body>\r\n" + //
				"	</text>\r\n" + //
				"</TEI>";
		Diagnostic d = d(17, 6, 17, 14, RelaxNGErrorCode.required_attribute_missing);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				ca(d, te(17, 14, 17, 14, " gi=\"\"")));
	}
}
