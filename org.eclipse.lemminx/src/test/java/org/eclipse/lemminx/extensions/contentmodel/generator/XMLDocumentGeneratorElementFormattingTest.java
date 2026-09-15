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
package org.eclipse.lemminx.extensions.contentmodel.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link XMLDocumentGenerator} with element formatting settings.
 *
 * <p>
 * Verifies that element generation respects formatting settings such as tab
 * size, insert spaces vs tabs, etc.
 * </p>
 */
public class XMLDocumentGeneratorElementFormattingTest extends AbstractXMLDocumentGeneratorTest {

	private static final String GENERATOR_XSD_PATH = "src/test/resources/generator/xsd/";

	// -- Tab size tests --

	@Test
	public void tabSize4() {
		SharedSettings settings = createSettings(4, true);
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "nestedElements.xsd");
		String result = generateFromURI(grammarURI, "catalog", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<catalog xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"    <category type=\"\">" + ls +
				"        <item>" + ls +
				"            <name>" + ls +
				"            </name>" + ls +
				"            <price>" + ls +
				"            </price>" + ls +
				"        </item>" + ls +
				"    </category>" + ls +
				"</catalog>" + ls, result);
	}

	@Test
	public void tabSize1() {
		SharedSettings settings = createSettings(1, true);
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "nestedElements.xsd");
		String result = generateFromURI(grammarURI, "catalog", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<catalog xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				" <category type=\"\">" + ls +
				"  <item>" + ls +
				"   <name>" + ls +
				"   </name>" + ls +
				"   <price>" + ls +
				"   </price>" + ls +
				"  </item>" + ls +
				" </category>" + ls +
				"</catalog>" + ls, result);
	}

	// -- Insert spaces vs tabs tests --

	@Test
	public void useTabs() {
		SharedSettings settings = createSettings(1, false);
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "simpleSequence.xsd");
		String result = generateFromURI(grammarURI, "root", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"\t<item1>" + ls +
				"\t</item1>" + ls +
				"\t<item2>" + ls +
				"\t</item2>" + ls +
				"</root>" + ls, result);
	}

	@Test
	public void useTabsNested() {
		SharedSettings settings = createSettings(1, false);
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "nestedElements.xsd");
		String result = generateFromURI(grammarURI, "catalog", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<catalog xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"\t<category type=\"\">" + ls +
				"\t\t<item>" + ls +
				"\t\t\t<name>" + ls +
				"\t\t\t</name>" + ls +
				"\t\t\t<price>" + ls +
				"\t\t\t</price>" + ls +
				"\t\t</item>" + ls +
				"\t</category>" + ls +
				"</catalog>" + ls, result);
	}

	// -- Helpers --

	private SharedSettings createSettings(int tabSize, boolean insertSpaces) {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(tabSize);
		settings.getFormattingSettings().setInsertSpaces(insertSpaces);
		settings.getFormattingSettings().setMaxLineWidth(0);
		return settings;
	}
}
