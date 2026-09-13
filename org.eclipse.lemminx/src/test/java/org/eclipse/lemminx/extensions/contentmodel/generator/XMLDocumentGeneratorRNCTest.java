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

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link XMLDocumentGenerator} with RelaxNG Compact (.rnc) grammars.
 *
 * <p>
 * Each test generates an XML document from a RelaxNG Compact file and verifies
 * the exact generated content. The generated XML uses {@code <?xml-model?>} for
 * grammar binding. Tests cover:
 * </p>
 * <ul>
 * <li>Simple element structures</li>
 * <li>Elements with attributes</li>
 * <li>Complex structures (addressBook)</li>
 * <li>Unknown root element (empty result)</li>
 * </ul>
 */
public class XMLDocumentGeneratorRNCTest extends AbstractXMLDocumentGeneratorTest {

	private static final String GENERATOR_RNC_PATH = "src/test/resources/generator/rnc/";

	// -- Simple element tests --

	/**
	 * Tests generation from a simple RNC with two child elements.
	 * <p>
	 * RNC defines: root -> (item1, item2).
	 * The generated XML should include a {@code <?xml-model?>} processing
	 * instruction.
	 * </p>
	 */
	@Test
	public void simpleElement() {
		String grammarURI = getFileURI(GENERATOR_RNC_PATH + "simpleElement.rnc");
		String result = generateFromURI(grammarURI, "root");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<root>" + ls +
				"  <item1>" + ls +
				"  </item1>" + ls +
				"  <item2>" + ls +
				"  </item2>" + ls +
				"</root>" + ls, result);
	}

	// -- Attribute tests --

	/**
	 * Tests generation from a RNC with attributes.
	 * <p>
	 * RNC defines: entry with id and name attributes.
	 * </p>
	 */
	@Test
	public void attributes() {
		String grammarURI = getFileURI(GENERATOR_RNC_PATH + "attributes.rnc");
		String result = generateFromURI(grammarURI, "entry");
		// RNC empty is not detected as truly empty by the generator
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<entry id=\"\" name=\"\">" + ls +
				"</entry>" + ls, result);
	}

	// -- Complex RNC tests (using existing resources) --

	/**
	 * Tests generation from the addressBook RNC.
	 * <p>
	 * RNC defines: addressBook -> card* -> (name, email, age*, @id*).
	 * </p>
	 */
	@Test
	public void addressBook() {
		String grammarURI = getFileURI("src/test/resources/relaxng/addressBook.rnc");
		String result = generateFromURI(grammarURI, "addressBook");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<addressBook>" + ls +
				"  <card>" + ls +
				"    <name>" + ls +
				"    </name>" + ls +
				"    <email>" + ls +
				"    </email>" + ls +
				"    <age>" + ls +
				"    </age>" + ls +
				"  </card>" + ls +
				"</addressBook>" + ls, result);
	}

	// -- Optional element tests --

	@Test
	public void optional() {
		String grammarURI = getFileURI(GENERATOR_RNC_PATH + "optional.rnc");
		String result = generateFromURI(grammarURI, "config");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<config>" + ls +
				"  <required1>" + ls +
				"  </required1>" + ls +
				"  <optional1>" + ls +
				"  </optional1>" + ls +
				"  <required2>" + ls +
				"  </required2>" + ls +
				"</config>" + ls, result);
	}

	// -- Nested elements tests --

	@Test
	public void nestedElements() {
		String grammarURI = getFileURI(GENERATOR_RNC_PATH + "nestedElements.rnc");
		String result = generateFromURI(grammarURI, "catalog");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<catalog>" + ls +
				"  <category>" + ls +
				"    <item>" + ls +
				"      <name>" + ls +
				"      </name>" + ls +
				"      <price>" + ls +
				"      </price>" + ls +
				"    </item>" + ls +
				"  </category>" + ls +
				"</catalog>" + ls, result);
	}

	// -- Choice tests --

	@Test
	public void choiceElements() {
		String grammarURI = getFileURI(GENERATOR_RNC_PATH + "choice.rnc");
		String result = generateFromURI(grammarURI, "container");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<container>" + ls +
				"  <optionA>" + ls +
				"  </optionA>" + ls +
				"</container>" + ls, result);
	}

	// -- Settings tests --

	@Test
	public void settingsMaxDepth1() {
		String grammarURI = getFileURI(GENERATOR_RNC_PATH + "nestedElements.rnc");
		XMLGenerationSettings settings = new XMLGenerationSettings();
		settings.setMaxDepth(1);
		String result = generateFromURI(grammarURI, "catalog", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<catalog>" + ls +
				"  <category></category>" + ls +
				"</catalog>" + ls, result);
	}

	@Test
	public void settingsRequiredOnly() {
		String grammarURI = getFileURI(GENERATOR_RNC_PATH + "optional.rnc");
		XMLGenerationSettings settings = new XMLGenerationSettings();
		settings.setOptionalElements(false);
		String result = generateFromURI(grammarURI, "config", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<config>" + ls +
				"  <required1>" + ls +
				"  </required1>" + ls +
				"  <required2>" + ls +
				"  </required2>" + ls +
				"</config>" + ls, result);
	}

	// -- Edge case tests --

	/**
	 * Tests that an unknown root element returns an empty string.
	 */
	@Test
	public void unknownRootElement() {
		String grammarURI = getFileURI(GENERATOR_RNC_PATH + "simpleElement.rnc");
		String result = generateFromURI(grammarURI, "unknown");
		assertEquals("", result);
	}
}
