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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link XMLDocumentGenerator} with attribute formatting settings.
 *
 * <p>
 * Verifies that attribute generation respects formatting settings such as
 * {@code splitAttributes}, {@code splitAttributesIndentSize},
 * {@code spaceBeforeEmptyCloseTag}, and {@code closingBracketNewLine}.
 * </p>
 */
public class XMLDocumentGeneratorAttributeFormattingTest extends AbstractXMLDocumentGeneratorTest {

	private static final String GENERATOR_XSD_PATH = "src/test/resources/generator/xsd/";

	// -- splitAttributes tests --

	@Test
	public void splitAttributesNewLine() {
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "attributes.xsd");
		String result = generateFromURI(grammarURI, "entry", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<entry" + ls +
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + ls +
				"    xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\"" + ls +
				"    id=\"\"" + ls +
				"    name=\"\" />" + ls, result);
	}

	@Test
	public void splitAttributesPreserve() {
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.preserve);
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "attributes.xsd");
		String result = generateFromURI(grammarURI, "entry", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<entry xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\"" +
				" id=\"\" name=\"\" />" + ls, result);
	}

	@Test
	public void splitAttributesAlignWithFirstAttr() {
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "attributes.xsd");
		String result = generateFromURI(grammarURI, "entry", settings);
		// alignWithFirstAttr is not yet implemented in generation,
		// behaves like preserve (all attributes on same line)
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<entry xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\"" +
				" id=\"\" name=\"\" />" + ls, result);
	}

	// -- splitAttributesIndentSize tests --

	@Test
	public void splitAttributesIndentSize1() {
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(1);
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "attributes.xsd");
		String result = generateFromURI(grammarURI, "entry", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<entry" + ls +
				"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + ls +
				"  xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\"" + ls +
				"  id=\"\"" + ls +
				"  name=\"\" />" + ls, result);
	}

	// -- spaceBeforeEmptyCloseTag tests --

	@Test
	public void spaceBeforeEmptyCloseTag() {
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSpaceBeforeEmptyCloseTag(true);
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "emptyElement.xsd");
		String result = generateFromURI(grammarURI, "marker", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<marker xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\" />" + ls, result);
	}

	@Test
	public void noSpaceBeforeEmptyCloseTag() {
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSpaceBeforeEmptyCloseTag(false);
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "emptyElement.xsd");
		String result = generateFromURI(grammarURI, "marker", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<marker xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\"/>" + ls, result);
	}

	// -- Combined settings tests --

	@Test
	public void splitAttributesWithNestedElements() {
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "nestedElements.xsd");
		String result = generateFromURI(grammarURI, "catalog", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<catalog" + ls +
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + ls +
				"    xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <category type=\"\">" + ls +
				"    <item>" + ls +
				"      <name>" + ls +
				"      </name>" + ls +
				"      <price>" + ls +
				"      </price>" + ls +
				"    </item>" + ls +
				"  </category>" + ls +
				"</catalog>" + ls, result);
	}

	// -- maxLineWidth tests --

	@Test
	public void maxLineWidthWrapsBindingAttributes() {
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setMaxLineWidth(100);
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "simpleSequence.xsd");
		String result = generateFromURI(grammarURI, "root", settings);
		// With maxLineWidth=100, the binding attributes wrap when the line exceeds 100 chars
		// First attribute stays on same line, subsequent ones wrap with indent level+1
		assertTrue(result.contains("<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + ls));
		assertTrue(result.contains("  xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">"));
	}

	// -- Helpers --

	private SharedSettings createSettings() {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(2);
		settings.getFormattingSettings().setInsertSpaces(true);
		settings.getFormattingSettings().setMaxLineWidth(0);
		return settings;
	}
}
