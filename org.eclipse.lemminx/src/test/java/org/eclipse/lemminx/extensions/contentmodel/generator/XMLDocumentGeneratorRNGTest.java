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
 * Tests for {@link XMLDocumentGenerator} with RelaxNG (.rng) grammars.
 *
 * <p>
 * Each test generates an XML document from a RelaxNG file and verifies the
 * exact generated content. The generated XML uses {@code <?xml-model?>} for
 * grammar binding. Tests cover:
 * </p>
 * <ul>
 * <li>Simple element structures</li>
 * <li>Elements with attributes</li>
 * <li>Nested element structures</li>
 * <li>Choice content model</li>
 * <li>Unknown root element (empty result)</li>
 * </ul>
 */
public class XMLDocumentGeneratorRNGTest extends AbstractXMLDocumentGeneratorTest {

	private static final String GENERATOR_RNG_PATH = "src/test/resources/generator/rng/";

	// -- Simple element tests --

	/**
	 * Tests generation from a simple RelaxNG with two child elements.
	 * <p>
	 * RNG defines: root -> (item1, item2).
	 * The generated XML should include a {@code <?xml-model?>} processing
	 * instruction.
	 * </p>
	 */
	@Test
	public void simpleElement() {
		String grammarURI = getFileURI(GENERATOR_RNG_PATH + "simpleElement.rng");
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
	 * Tests generation from a RelaxNG with attributes.
	 * <p>
	 * RNG defines: entry with id and name attributes.
	 * </p>
	 */
	@Test
	public void attributes() {
		String grammarURI = getFileURI(GENERATOR_RNG_PATH + "attributes.rng");
		String result = generateFromURI(grammarURI, "entry");
		// RelaxNG <empty/> is not detected as truly empty by the generator
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<entry id=\"\" name=\"\">" + ls +
				"</entry>" + ls, result);
	}

	// -- Nested elements tests --

	/**
	 * Tests generation of nested elements from a RelaxNG (3 levels deep).
	 * <p>
	 * RNG defines: catalog -> category -> item -> name.
	 * </p>
	 */
	@Test
	public void nestedElements() {
		String grammarURI = getFileURI(GENERATOR_RNG_PATH + "nestedElements.rng");
		String result = generateFromURI(grammarURI, "catalog");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<catalog>" + ls +
				"  <category>" + ls +
				"    <item>" + ls +
				"      <name>" + ls +
				"      </name>" + ls +
				"    </item>" + ls +
				"  </category>" + ls +
				"</catalog>" + ls, result);
	}

	// -- Complex RNG tests (using existing resources) --

	/**
	 * Tests generation from the addressBook RelaxNG.
	 * <p>
	 * RNG defines: addressBook -> card+ -> (name, email).
	 * </p>
	 */
	@Test
	public void addressBook() {
		String grammarURI = getFileURI("src/test/resources/relaxng/addressBook_v1.rng");
		String result = generateFromURI(grammarURI, "addressBook");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<addressBook>" + ls +
				"  <card>" + ls +
				"    <name>" + ls +
				"    </name>" + ls +
				"    <email>" + ls +
				"    </email>" + ls +
				"  </card>" + ls +
				"</addressBook>" + ls, result);
	}

	/**
	 * Tests generation from a RelaxNG with attribute choices and namespace
	 * declarations.
	 * <p>
	 * Uses simple.rng: rootelt(@xml:lang, @lmx:type) -> child(@vx:type).
	 * </p>
	 */
	@Test
	public void attributesWithNamespaces() {
		String grammarURI = getFileURI("src/test/resources/relaxng/simple.rng");
		String result = generateFromURI(grammarURI, "rootelt");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<rootelt xmlns:lmx=\"https://github.com/eclipse/lemminx\" xml:lang=\"en\" lmx:type=\"dtd\">" + ls +
				"  <child xmlns:vx=\"https://github.com/redhat-developer/vscode-xml\" vx:type=\"java\">" + ls +
				"  </child>" + ls +
				"</rootelt>" + ls, result);
	}

	/**
	 * Tests generation from a RelaxNG with choice content model.
	 * <p>
	 * Uses article.rng: article -> (title | title/line).
	 * </p>
	 */
	@Test
	public void choice() {
		String grammarURI = getFileURI("src/test/resources/relaxng/article.rng");
		String result = generateFromURI(grammarURI, "article");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<article>" + ls +
				"  <title>" + ls +
				"  </title>" + ls +
				"</article>" + ls, result);
	}

	// -- Optional element tests --

	@Test
	public void optional() {
		String grammarURI = getFileURI(GENERATOR_RNG_PATH + "optional.rng");
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

	// -- Choice tests --

	@Test
	public void choiceElements() {
		String grammarURI = getFileURI(GENERATOR_RNG_PATH + "choice.rng");
		String result = generateFromURI(grammarURI, "container");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<container>" + ls +
				"  <optionA>" + ls +
				"  </optionA>" + ls +
				"</container>" + ls, result);
	}

	// -- Interleave tests --

	@Test
	public void interleave() {
		String grammarURI = getFileURI(GENERATOR_RNG_PATH + "interleave.rng");
		String result = generateFromURI(grammarURI, "record");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<record>" + ls +
				"  <field1>" + ls +
				"  </field1>" + ls +
				"  <field2>" + ls +
				"  </field2>" + ls +
				"</record>" + ls, result);
	}

	// -- Grammar ref tests --

	@Test
	public void grammarRef() {
		String grammarURI = getFileURI(GENERATOR_RNG_PATH + "grammarRef.rng");
		String result = generateFromURI(grammarURI, "book");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<book>" + ls +
				"  <chapter>" + ls +
				"    <title>" + ls +
				"    </title>" + ls +
				"  </chapter>" + ls +
				"</book>" + ls, result);
	}

	// -- Namespace tests --

	/**
	 * Tests generation from a RelaxNG with ns="https://google.ca".
	 * <p>
	 * The generated XML should include xmlns="https://google.ca" on the root
	 * element.
	 * </p>
	 */
	@Test
	public void namespace() {
		String grammarURI = getFileURI(GENERATOR_RNG_PATH + "namespace.rng");
		String result = generateFromURI(grammarURI, "root-element");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + grammarURI + "\"?>" + ls +
				"<root-element xmlns=\"https://google.ca\">" + ls +
				"  <child-element my-attr=\"en\">" + ls +
				"  </child-element>" + ls +
				"</root-element>" + ls, result);
	}

	// -- Settings tests --

	@Test
	public void settingsMaxDepth1() {
		String grammarURI = getFileURI(GENERATOR_RNG_PATH + "nestedElements.rng");
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
		String grammarURI = getFileURI(GENERATOR_RNG_PATH + "optional.rng");
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
		String grammarURI = getFileURI(GENERATOR_RNG_PATH + "simpleElement.rng");
		String result = generateFromURI(grammarURI, "unknown");
		assertEquals("", result);
	}
}
