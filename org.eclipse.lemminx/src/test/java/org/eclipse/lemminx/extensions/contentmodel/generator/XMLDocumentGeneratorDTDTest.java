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
 * Tests for {@link XMLDocumentGenerator} with DTD grammars.
 *
 * <p>
 * Each test generates an XML document from a DTD file and verifies the exact
 * generated content. Tests cover:
 * </p>
 * <ul>
 * <li>Simple element sequences</li>
 * <li>Elements with REQUIRED and IMPLIED attributes</li>
 * <li>Nested element structures</li>
 * <li>Mixed content elements</li>
 * <li>EMPTY elements</li>
 * <li>Complex DTD structures (web-app)</li>
 * <li>Unknown root element (empty result)</li>
 * </ul>
 */
public class XMLDocumentGeneratorDTDTest extends AbstractXMLDocumentGeneratorTest {

	private static final String GENERATOR_DTD_PATH = "src/test/resources/generator/dtd/";

	// -- Simple sequence tests --

	/**
	 * Tests generation from a DTD with a simple element sequence.
	 * <p>
	 * DTD defines: root -> (item1, item2).
	 * The generated XML should include a DOCTYPE declaration.
	 * </p>
	 */
	@Test
	public void simpleSequence() {
		String grammarURI = getFileURI(GENERATOR_DTD_PATH + "simpleSequence.dtd");
		String result = generateFromURI(grammarURI, "root");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<!DOCTYPE root SYSTEM \"" + grammarURI + "\">" + ls +
				"<root>" + ls +
				"  <item1>" + ls +
				"  </item1>" + ls +
				"  <item2>" + ls +
				"  </item2>" + ls +
				"</root>" + ls, result);
	}

	// -- Attribute tests --

	/**
	 * Tests generation from a DTD with REQUIRED and IMPLIED attributes.
	 * <p>
	 * DTD defines: entry (EMPTY) with id #REQUIRED, name #REQUIRED,
	 * optional #IMPLIED. Only required attributes should be generated.
	 * </p>
	 */
	@Test
	public void requiredAttributes() {
		String grammarURI = getFileURI(GENERATOR_DTD_PATH + "attributes.dtd");
		String result = generateFromURI(grammarURI, "entry");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<!DOCTYPE entry SYSTEM \"" + grammarURI + "\">" + ls +
				"<entry id=\"\" name=\"\" />" + ls, result);
	}

	// -- Nested elements tests --

	/**
	 * Tests generation of nested elements from a DTD (3 levels deep).
	 * <p>
	 * DTD defines: catalog -> category -> item -> (name, price).
	 * </p>
	 */
	@Test
	public void nestedElements() {
		String grammarURI = getFileURI(GENERATOR_DTD_PATH + "nestedElements.dtd");
		String result = generateFromURI(grammarURI, "catalog");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<!DOCTYPE catalog SYSTEM \"" + grammarURI + "\">" + ls +
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

	// -- Mixed content tests --

	/**
	 * Tests generation of a mixed content element from a DTD.
	 * <p>
	 * DTD defines: paragraph (#PCDATA | bold | italic)*.
	 * Mixed content elements should generate a simple open/close tag.
	 * </p>
	 */
	@Test
	public void mixedContent() {
		String grammarURI = getFileURI(GENERATOR_DTD_PATH + "mixedContent.dtd");
		String result = generateFromURI(grammarURI, "paragraph");
		// DTD mixed content (#PCDATA | bold | italic)* generates all child elements
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<!DOCTYPE paragraph SYSTEM \"" + grammarURI + "\">" + ls +
				"<paragraph>" + ls +
				"  <bold>" + ls +
				"  </bold>" + ls +
				"  <italic>" + ls +
				"  </italic>" + ls +
				"</paragraph>" + ls, result);
	}

	// -- Empty element tests --

	/**
	 * Tests generation of an EMPTY element from a DTD.
	 * <p>
	 * DTD defines: marker EMPTY. Should self-close.
	 * </p>
	 */
	@Test
	public void emptyElement() {
		String grammarURI = getFileURI(GENERATOR_DTD_PATH + "emptyElement.dtd");
		String result = generateFromURI(grammarURI, "marker");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<!DOCTYPE marker SYSTEM \"" + grammarURI + "\">" + ls +
				"<marker />" + ls, result);
	}

	// -- Complex DTD tests --

	/**
	 * Tests generation from the note.dtd with simple child elements.
	 * <p>
	 * DTD defines: note -> (to, from, body).
	 * </p>
	 */
	@Test
	public void note() {
		String grammarURI = getFileURI("src/test/resources/dtd/note.dtd");
		String result = generateFromURI(grammarURI, "note");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<!DOCTYPE note SYSTEM \"" + grammarURI + "\">" + ls +
				"<note>" + ls +
				"  <to>" + ls +
				"  </to>" + ls +
				"  <from>" + ls +
				"  </from>" + ls +
				"  <body>" + ls +
				"  </body>" + ls +
				"</note>" + ls, result);
	}

	/**
	 * Tests generation from a complex web-app DTD.
	 * <p>
	 * Uses the existing web-app_2_3.dtd with many nested elements.
	 * Only verifies the beginning of the generated XML (the full output is large).
	 * </p>
	 */
	@Test
	public void webApp() {
		String grammarURI = getFileURI("src/test/resources/dtd/web-app_2_3.dtd");
		String result = generateFromURI(grammarURI, "web-app");
		// The full web-app is very large; verify structure
		String expectedStart = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<!DOCTYPE web-app SYSTEM \"" + grammarURI + "\">" + ls +
				"<web-app>" + ls +
				"  <icon>" + ls +
				"    <small-icon>" + ls +
				"    </small-icon>" + ls +
				"    <large-icon>" + ls +
				"    </large-icon>" + ls +
				"  </icon>" + ls +
				"  <display-name>" + ls +
				"  </display-name>" + ls +
				"  <description>" + ls +
				"  </description>" + ls +
				"  <distributable />" + ls;
		assertEquals(expectedStart, result.substring(0, expectedStart.length()));
	}

	// -- Attribute enumeration tests --

	@Test
	public void attributeEnumeration() {
		String grammarURI = getFileURI(GENERATOR_DTD_PATH + "attributeEnumeration.dtd");
		String result = generateFromURI(grammarURI, "config");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<!DOCTYPE config SYSTEM \"" + grammarURI + "\">" + ls +
				"<config mode=\"debug\" />" + ls, result);
	}

	// -- Fixed attribute tests --

	@Test
	public void fixedAttribute() {
		String grammarURI = getFileURI(GENERATOR_DTD_PATH + "fixedAttribute.dtd");
		String result = generateFromURI(grammarURI, "document");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<!DOCTYPE document SYSTEM \"" + grammarURI + "\">" + ls +
				"<document>" + ls +
				"  <title>" + ls +
				"  </title>" + ls +
				"</document>" + ls, result);
	}

	// -- ANY content tests --

	@Test
	public void anyContent() {
		String grammarURI = getFileURI(GENERATOR_DTD_PATH + "anyContent.dtd");
		String result = generateFromURI(grammarURI, "container");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<!DOCTYPE container SYSTEM \"" + grammarURI + "\">" + ls +
				"<container>" + ls +
				"</container>" + ls, result);
	}

	// -- Recursive element tests --

	@Test
	public void recursive() {
		String grammarURI = getFileURI(GENERATOR_DTD_PATH + "recursive.dtd");
		String result = generateFromURI(grammarURI, "folder");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<!DOCTYPE folder SYSTEM \"" + grammarURI + "\">" + ls +
				"<folder>" + ls +
				"  <name>" + ls +
				"  </name>" + ls +
				"</folder>" + ls, result);
	}

	// -- Edge case tests --

	// -- Settings tests --

	@Test
	public void settingsMaxDepth1() {
		String grammarURI = getFileURI(GENERATOR_DTD_PATH + "nestedElements.dtd");
		XMLGenerationSettings settings = new XMLGenerationSettings();
		settings.setMaxDepth(1);
		String result = generateFromURI(grammarURI, "catalog", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<!DOCTYPE catalog SYSTEM \"" + grammarURI + "\">" + ls +
				"<catalog>" + ls +
				"  <category></category>" + ls +
				"</catalog>" + ls, result);
	}

	/**
	 * Tests that an unknown root element returns an empty string.
	 */
	@Test
	public void unknownRootElement() {
		String grammarURI = getFileURI(GENERATOR_DTD_PATH + "simpleSequence.dtd");
		String result = generateFromURI(grammarURI, "unknown");
		assertEquals("", result);
	}
}
