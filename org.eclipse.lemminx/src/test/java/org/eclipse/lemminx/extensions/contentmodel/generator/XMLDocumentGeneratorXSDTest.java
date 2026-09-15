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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link XMLDocumentGenerator} with XSD grammars.
 *
 * <p>
 * Each test generates an XML document from an XSD file and verifies the exact
 * generated content. Tests cover:
 * </p>
 * <ul>
 * <li>Simple sequences (required children only)</li>
 * <li>Sequences with optional elements (only required elements are
 * generated)</li>
 * <li>Namespaced elements (xmlns + xsi:schemaLocation)</li>
 * <li>Required attributes</li>
 * <li>Nested elements (multi-level depth)</li>
 * <li>Empty elements (self-closing tags)</li>
 * <li>Enumeration values (first enum value as default)</li>
 * <li>Abstract types with xsi:type (derived type selection)</li>
 * <li>xs:choice content model</li>
 * <li>xs:all content model</li>
 * <li>xs:group references</li>
 * <li>Unknown root element (empty result)</li>
 * </ul>
 */
public class XMLDocumentGeneratorXSDTest extends AbstractXMLDocumentGeneratorTest {

	private static final String GENERATOR_XSD_PATH = "src/test/resources/generator/xsd/";

	// -- Simple sequence tests --

	/**
	 * Tests generation from an XSD with a simple sequence of required elements.
	 * <p>
	 * XSD defines: root -> (item1, item2)
	 * </p>
	 */
	@Test
	public void simpleSequence() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "simpleSequence.xsd");
		String result = generateFromURI(grammarURI, "root");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <item1>" + ls +
				"  </item1>" + ls +
				"  <item2>" + ls +
				"  </item2>" + ls +
				"</root>" + ls, result);
	}

	/**
	 * Tests that all elements (required and optional) are generated.
	 * <p>
	 * XSD defines: config -> (required1, optional1?, required2, optional2?)
	 * </p>
	 */
	@Test
	public void sequenceWithOptional() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "sequenceWithOptional.xsd");
		String result = generateFromURI(grammarURI, "config");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <required1>" + ls +
				"  </required1>" + ls +
				"  <optional1>" + ls +
				"  </optional1>" + ls +
				"  <required2>" + ls +
				"  </required2>" + ls +
				"  <optional2>" + ls +
				"  </optional2>" + ls +
				"</config>" + ls, result);
	}

	// -- Namespace tests --

	/**
	 * Tests generation from an XSD with a target namespace.
	 * <p>
	 * The generated XML should include xmlns, xmlns:xsi and xsi:schemaLocation.
	 * XSD defines: project -> (name, version) in namespace http://example.com/ns
	 * </p>
	 */
	@Test
	public void namespace() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "namespace.xsd");
		String result = generateFromURI(grammarURI, "project");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<project xmlns=\"http://example.com/ns\"" +
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:schemaLocation=\"http://example.com/ns " + grammarURI + "\">" + ls +
				"  <name>" + ls +
				"  </name>" + ls +
				"  <version>" + ls +
				"  </version>" + ls +
				"</project>" + ls, result);
	}

	// -- Attribute tests --

	/**
	 * Tests generation from an XSD with required attributes and no children.
	 * <p>
	 * XSD defines: entry with required id, name and optional "optional" attribute.
	 * Only required attributes should be generated.
	 * </p>
	 */
	@Test
	public void requiredAttributes() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "attributes.xsd");
		String result = generateFromURI(grammarURI, "entry");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<entry xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\"" +
				" id=\"\" name=\"\" />" + ls, result);
	}

	// -- Nested elements tests --

	/**
	 * Tests generation of deeply nested elements (3 levels).
	 * <p>
	 * XSD defines: catalog -> category(@type) -> item -> (name, price)
	 * </p>
	 */
	@Test
	public void nestedElements() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "nestedElements.xsd");
		String result = generateFromURI(grammarURI, "catalog");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<catalog xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
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

	// -- Empty element tests --

	/**
	 * Tests generation of an element with an empty complex type.
	 * <p>
	 * XSD defines: marker with empty complexType, should self-close.
	 * </p>
	 */
	@Test
	public void emptyElement() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "emptyElement.xsd");
		String result = generateFromURI(grammarURI, "marker");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<marker xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\" />" + ls, result);
	}

	// -- Enumeration tests --

	/**
	 * Tests generation of elements with enumeration values.
	 * <p>
	 * XSD defines: settings -> (level{low,medium,high}, enabled).
	 * The first enumeration value should be used as default.
	 * </p>
	 */
	@Test
	public void enumerationValues() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "enumeration.xsd");
		String result = generateFromURI(grammarURI, "settings");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<settings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <level>low</level>" + ls +
				"  <enabled>true</enabled>" + ls +
				"</settings>" + ls, result);
	}

	// -- Abstract/derived type tests --

	/**
	 * Tests generation with abstract types and xsi:type attribute.
	 * <p>
	 * XSD defines: root -> Character (abstract, derived as Teacher/Student).
	 * The generated XML should pick one of the derived types for xsi:type.
	 * </p>
	 */
	@Test
	public void derivedType() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "derivedType.xsd");
		String result = generateFromURI(grammarURI, "root");
		// Xerces may return derived types in any order
		assertTrue(result.contains("xsi:type=\"Teacher\"") || result.contains("xsi:type=\"Student\""),
				"Expected xsi:type to be Teacher or Student, but got: " + result);
		assertTrue(result.contains("Name=\"\""), "Expected Name attribute");
		assertTrue(result.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""),
				"Expected xmlns:xsi declaration");
		assertTrue(result.contains("xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\""),
				"Expected xsi:noNamespaceSchemaLocation");
	}

	// -- xs:choice tests --

	/**
	 * Tests generation with xs:choice content model.
	 * <p>
	 * Uses the existing choice.xsd which defines: person -> (employee | member).
	 * </p>
	 */
	@Test
	public void choice() {
		String grammarURI = getFileURI("src/test/resources/xsd/choice.xsd");
		String result = generateFromURI(grammarURI, "person");
		// choice has maxOccurs="3", so all alternatives are generated
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <employee>" + ls +
				"  </employee>" + ls +
				"  <member>" + ls +
				"  </member>" + ls +
				"</person>" + ls, result);
	}

	// -- xs:all tests --

	/**
	 * Tests generation with xs:all content model where all elements are optional.
	 * <p>
	 * Uses the existing all.xsd: Demo -> (Hello?, World?).
	 * Both optional elements are generated.
	 * </p>
	 */
	@Test
	public void allOptional() {
		String grammarURI = getFileURI("src/test/resources/xsd/all.xsd");
		String result = generateFromURI(grammarURI, "Demo");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<Demo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <Hello>" + ls +
				"  </Hello>" + ls +
				"  <World>" + ls +
				"  </World>" + ls +
				"</Demo>" + ls, result);
	}

	// -- xs:group tests --

	/**
	 * Tests generation with xs:group references and namespace.
	 * <p>
	 * Uses the existing group.xsd: observation -> (resultTime, procedure, result)
	 * via group ref, in namespace http://group-test.
	 * </p>
	 */
	@Test
	public void groupWithNamespace() {
		String grammarURI = getFileURI("src/test/resources/xsd/group.xsd");
		String result = generateFromURI(grammarURI, "observation");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<observation xmlns=\"http://group-test\"" +
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:schemaLocation=\"http://group-test " + grammarURI + "\">" + ls +
				"  <resultTime>2026-01-01T00:00:00</resultTime>" + ls +
				"  <procedure>" + ls +
				"  </procedure>" + ls +
				"  <result>" + ls +
				"  </result>" + ls +
				"</observation>" + ls, result);
	}

	// -- Complex nested structure tests --

	/**
	 * Tests generation of a complex invoice structure with nested types.
	 * <p>
	 * Uses the existing invoice.xsd: invoice -> (date, number, products, payments).
	 * Products contains product with required attributes.
	 * </p>
	 */
	@Test
	public void invoice() {
		String grammarURI = getFileURI("src/test/resources/xsd/invoice.xsd");
		String result = generateFromURI(grammarURI, "invoice");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <date>2026-01-01</date>" + ls +
				"  <number>0</number>" + ls +
				"  <products>" + ls +
				"    <product price=\"0\" description=\"\" />" + ls +
				"  </products>" + ls +
				"  <payments>" + ls +
				"    <payment amount=\"0\" method=\"credit\" />" + ls +
				"  </payments>" + ls +
				"</invoice>" + ls, result);
	}

	/**
	 * Tests generation of bookstore XSD with substitution groups.
	 * <p>
	 * Uses the existing bookstore.xsd: BookStore -> (Name, Publication*).
	 * Substitution groups expand to include Book, Magazine, etc.
	 * </p>
	 */
	@Test
	public void bookstore() {
		String grammarURI = getFileURI("src/test/resources/xsd/bookstore.xsd");
		String result = generateFromURI(grammarURI, "BookStore");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<BookStore xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <StoreName>" + ls +
				"  </StoreName>" + ls +
				"  <Name>" + ls +
				"  </Name>" + ls +
				"  <Book>" + ls +
				"    <Title>" + ls +
				"    </Title>" + ls +
				"    <Author>" + ls +
				"    </Author>" + ls +
				"    <Date>2026</Date>" + ls +
				"    <ISBN>" + ls +
				"    </ISBN>" + ls +
				"    <Publisher>" + ls +
				"    </Publisher>" + ls +
				"  </Book>" + ls +
				"  <Magazine>" + ls +
				"    <Title>" + ls +
				"    </Title>" + ls +
				"    <Date>2026</Date>" + ls +
				"  </Magazine>" + ls +
				"  <Publication>" + ls +
				"    <Title>" + ls +
				"    </Title>" + ls +
				"    <Author>" + ls +
				"    </Author>" + ls +
				"    <Date>2026</Date>" + ls +
				"  </Publication>" + ls +
				"</BookStore>" + ls, result);
	}

	// -- Attribute enumeration tests --

	@Test
	public void attributeEnumeration() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "attributeEnumeration.xsd");
		String result = generateFromURI(grammarURI, "config");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\"" +
				" mode=\"debug\" />" + ls, result);
	}

	// -- Fixed/default value tests --

	@Test
	public void fixedValue() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "fixedValue.xsd");
		String result = generateFromURI(grammarURI, "document");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<document xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <title>" + ls +
				"  </title>" + ls +
				"</document>" + ls, result);
	}

	@Test
	public void defaultValue() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "defaultValue.xsd");
		String result = generateFromURI(grammarURI, "form");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<form xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <name>" + ls +
				"  </name>" + ls +
				"</form>" + ls, result);
	}

	// -- Mixed content tests --

	@Test
	public void mixedContent() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "mixedContent.xsd");
		String result = generateFromURI(grammarURI, "paragraph");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<paragraph xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <bold>" + ls +
				"  </bold>" + ls +
				"  <italic>" + ls +
				"  </italic>" + ls +
				"</paragraph>" + ls, result);
	}

	// -- Recursive element tests --

	@Test
	public void recursive() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "recursive.xsd");
		String result = generateFromURI(grammarURI, "folder");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<folder xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <name>" + ls +
				"  </name>" + ls +
				"</folder>" + ls, result);
	}

	// -- SimpleContent tests --

	@Test
	public void simpleContent() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "simpleContent.xsd");
		String result = generateFromURI(grammarURI, "price");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<price xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\"" +
				" currency=\"\">0</price>" + ls, result);
	}

	// -- AttributeGroup tests --

	@Test
	public void attributeGroup() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "attributeGroup.xsd");
		String result = generateFromURI(grammarURI, "item");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<item xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\"" +
				" id=\"\" name=\"\">" + ls +
				"  <description>" + ls +
				"  </description>" + ls +
				"</item>" + ls, result);
	}

	// -- Element ref tests --

	@Test
	public void elementRef() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "elementRef.xsd");
		String result = generateFromURI(grammarURI, "person");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <firstName>" + ls +
				"  </firstName>" + ls +
				"  <lastName>" + ls +
				"  </lastName>" + ls +
				"</person>" + ls, result);
	}

	// -- Extension tests --

	@Test
	public void extension() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "extension.xsd");
		String result = generateFromURI(grammarURI, "employee");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<employee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\"" +
				" role=\"\">" + ls +
				"  <name>" + ls +
				"  </name>" + ls +
				"  <department>" + ls +
				"  </department>" + ls +
				"</employee>" + ls, result);
	}

	// -- Nillable tests --

	@Test
	public void nillable() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "nillable.xsd");
		String result = generateFromURI(grammarURI, "record");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<record xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <name>" + ls +
				"  </name>" + ls +
				"  <value>" + ls +
				"  </value>" + ls +
				"</record>" + ls, result);
	}

	// -- Imported element tests --

	/**
	 * Tests generation with a root element that references elements from an
	 * imported schema. The imported elements should be generated with namespace
	 * prefixes, and the xsi:schemaLocation must use the main schema's namespace.
	 * <p>
	 * main.xsd (targetNamespace=http://example.com/main) imports product.xsd
	 * (targetNamespace=http://example.com/product) which defines QuestionValue.
	 * </p>
	 */
	@Test
	public void importedElement() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "importedElement/main.xsd");
		String result = generateFromURI(grammarURI, "root");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<root xmlns=\"http://example.com/main\"" +
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xmlns:product=\"http://example.com/product\"" +
				" xsi:schemaLocation=\"http://example.com/main " + grammarURI + "\">" + ls +
				"  <title>" + ls +
				"  </title>" + ls +
				"  <product:QuestionValue name=\"\" />" + ls +
				"</root>" + ls, result);
	}

	/**
	 * Tests generation directly from the imported schema (product.xsd).
	 * When generating from product.xsd, QuestionValue is a valid root element.
	 */
	@Test
	public void importedSchemaDirectly() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "importedElement/product.xsd");
		String result = generateFromURI(grammarURI, "QuestionValue");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<QuestionValue xmlns=\"http://example.com/product\"" +
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:schemaLocation=\"http://example.com/product " + grammarURI + "\"" +
				" name=\"\" />" + ls, result);
	}

	// -- Data type tests --

	/**
	 * Tests generation of type-aware default values for XSD built-in types.
	 * <p>
	 * Elements and attributes with XSD types (date, dateTime, time, integer,
	 * decimal, float, double, boolean, etc.) should generate valid default
	 * values instead of empty content.
	 * </p>
	 */
	@Test
	public void dataTypes() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "dataTypes.xsd");
		String result = generateFromURI(grammarURI, "record");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<record xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\"" +
				" id=\"0\" timestamp=\"2026-01-01T00:00:00\" enabled=\"false\">" + ls +
				"  <name>" + ls +
				"  </name>" + ls +
				"  <active>true</active>" + ls +
				"  <count>0</count>" + ls +
				"  <score>0</score>" + ls +
				"  <ratio>0</ratio>" + ls +
				"  <precision>0</precision>" + ls +
				"  <birthDate>2026-01-01</birthDate>" + ls +
				"  <createdAt>2026-01-01T00:00:00</createdAt>" + ls +
				"  <startTime>00:00:00</startTime>" + ls +
				"  <age>0</age>" + ls +
				"  <code>0</code>" + ls +
				"  <rank>1</rank>" + ls +
				"  <offset>-1</offset>" + ls +
				"  <duration>P1D</duration>" + ls +
				"</record>" + ls, result);
	}

	// -- Choice tests --

	/**
	 * Tests generation from an XSD with a simple xs:choice.
	 * Only the first alternative should be generated (not all).
	 */
	@Test
	public void simpleChoice() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "choice.xsd");
		String result = generateFromURI(grammarURI, "payment");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<payment xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <cash>" + ls +
				"  </cash>" + ls +
				"</payment>" + ls, result);
	}

	/**
	 * Tests generation from an XSD with xs:choice inside xs:sequence.
	 * The sequence elements should all be generated, but the choice
	 * should only produce its first alternative.
	 */
	@Test
	public void choiceInSequence() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "choice.xsd");
		String result = generateFromURI(grammarURI, "order");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<order xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <id>0</id>" + ls +
				"  <pickup>" + ls +
				"  </pickup>" + ls +
				"  <total>0</total>" + ls +
				"</order>" + ls, result);
	}

	/**
	 * Tests generation from an XSD with nested choice containing a sequence.
	 * The first alternative of the choice is a sequence (email + subject),
	 * so both elements from that sequence should be generated.
	 */
	@Test
	public void nestedChoiceWithSequence() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "choice.xsd");
		String result = generateFromURI(grammarURI, "notification");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<notification xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <email>" + ls +
				"  </email>" + ls +
				"  <subject>" + ls +
				"  </subject>" + ls +
				"</notification>" + ls, result);
	}

	/**
	 * Tests generation from an XSD with xs:choice maxOccurs="unbounded".
	 * All alternatives should be generated since the choice is repeatable.
	 */
	@Test
	public void choiceUnbounded() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "choice.xsd");
		String result = generateFromURI(grammarURI, "container");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<container xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <name>" + ls +
				"  </name>" + ls +
				"  <itemA>" + ls +
				"  </itemA>" + ls +
				"  <itemB>" + ls +
				"  </itemB>" + ls +
				"  <itemC>" + ls +
				"  </itemC>" + ls +
				"</container>" + ls, result);
	}

	/**
	 * Tests generation from an XSD with xs:choice and optionalElements=false.
	 * Only the first alternative of the choice should be generated.
	 */
	@Test
	public void simpleChoiceRequiredOnly() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "choice.xsd");
		XMLGenerationSettings settings = new XMLGenerationSettings();
		settings.setOptionalElements(false);
		String result = generateFromURI(grammarURI, "payment", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<payment xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <cash>" + ls +
				"  </cash>" + ls +
				"</payment>" + ls, result);
	}

	/**
	 * Tests generation from an XSD with xs:choice inside xs:sequence
	 * and optionalElements=false.
	 */
	@Test
	public void choiceInSequenceRequiredOnly() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "choice.xsd");
		XMLGenerationSettings settings = new XMLGenerationSettings();
		settings.setOptionalElements(false);
		String result = generateFromURI(grammarURI, "order", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<order xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <id>0</id>" + ls +
				"  <pickup>" + ls +
				"  </pickup>" + ls +
				"  <total>0</total>" + ls +
				"</order>" + ls, result);
	}

	// -- Settings tests --

	/**
	 * Tests that maxDepth=1 limits generation to only the root element's
	 * direct children (no grandchildren).
	 */
	@Test
	public void settingsMaxDepth1() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "nestedElements.xsd");
		XMLGenerationSettings settings = new XMLGenerationSettings();
		settings.setMaxDepth(1);
		String result = generateFromURI(grammarURI, "catalog", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<catalog xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <category type=\"\"></category>" + ls +
				"</catalog>" + ls, result);
	}

	/**
	 * Tests that optionalElements=false generates only required elements.
	 */
	@Test
	public void settingsRequiredOnly() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "sequenceWithOptional.xsd");
		XMLGenerationSettings settings = new XMLGenerationSettings();
		settings.setOptionalElements(false);
		String result = generateFromURI(grammarURI, "config", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\">" + ls +
				"  <required1>" + ls +
				"  </required1>" + ls +
				"  <required2>" + ls +
				"  </required2>" + ls +
				"</config>" + ls, result);
	}

	/**
	 * Tests that typeDefaults=false suppresses type-aware default values.
	 */
	@Test
	public void settingsTypeDefaultsDisabled() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "dataTypes.xsd");
		XMLGenerationSettings settings = new XMLGenerationSettings();
		settings.setTypeDefaults(false);
		String result = generateFromURI(grammarURI, "record", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<record xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:noNamespaceSchemaLocation=\"" + grammarURI + "\"" +
				" id=\"\" timestamp=\"\" enabled=\"true\">" + ls +
				"  <name>" + ls +
				"  </name>" + ls +
				"  <active>true</active>" + ls +
				"  <count>" + ls +
				"  </count>" + ls +
				"  <score>" + ls +
				"  </score>" + ls +
				"  <ratio>" + ls +
				"  </ratio>" + ls +
				"  <precision>" + ls +
				"  </precision>" + ls +
				"  <birthDate>" + ls +
				"  </birthDate>" + ls +
				"  <createdAt>" + ls +
				"  </createdAt>" + ls +
				"  <startTime>" + ls +
				"  </startTime>" + ls +
				"  <age>" + ls +
				"  </age>" + ls +
				"  <code>" + ls +
				"  </code>" + ls +
				"  <rank>" + ls +
				"  </rank>" + ls +
				"  <offset>" + ls +
				"  </offset>" + ls +
				"  <duration>" + ls +
				"  </duration>" + ls +
				"</record>" + ls, result);
	}

	// -- Maven POM tests --

	private static final String MAVEN_POM_XSD_PATH = "src/test/resources/xsd/maven-4.0.0.xsd";

	/**
	 * Tests generating a Maven POM with optionalElements=false.
	 * Since all children of the Model type are optional (minOccurs="0"),
	 * only the empty root element is generated.
	 */
	@Test
	public void mavenPomRequiredOnly() {
		String grammarURI = getFileURI(MAVEN_POM_XSD_PATH);
		XMLGenerationSettings settings = new XMLGenerationSettings();
		settings.setOptionalElements(false);
		String result = generateFromURI(grammarURI, "project", settings);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\"" +
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 " + grammarURI + "\">" + ls +
				"</project>" + ls, result);
	}

	/**
	 * Tests generating a Maven POM with maxDepth=1.
	 * All first-level children (modelVersion, groupId, etc.) are generated
	 * but none of their nested children (e.g., parent/relativePath).
	 */
	@Test
	public void mavenPomMaxDepth1() {
		String grammarURI = getFileURI(MAVEN_POM_XSD_PATH);
		XMLGenerationSettings settings = new XMLGenerationSettings();
		settings.setMaxDepth(1);
		settings.setOptionalElements(true);
		String result = generateFromURI(grammarURI, "project", settings);
		// First-level children are present
		assertTrue(result.contains("<modelVersion>"), "Expected modelVersion");
		assertTrue(result.contains("<groupId>"), "Expected groupId");
		assertTrue(result.contains("<artifactId>"), "Expected artifactId");
		assertTrue(result.contains("<version>"), "Expected version");
		assertTrue(result.contains("<dependencies>"), "Expected dependencies");
		assertTrue(result.contains("<build>"), "Expected build");
		// Nested children are NOT present (maxDepth=1 stops recursion)
		assertFalse(result.contains("<relativePath>"));
		assertFalse(result.contains("<dependency>"));
		assertFalse(result.contains("<plugin>"));
	}

	/**
	 * Tests generating a Maven POM with maxDepth=2.
	 * First-level children are expanded (e.g., parent has groupId, artifactId)
	 * but third-level children are not (e.g., dependency/exclusions is empty).
	 */
	@Test
	public void mavenPomMaxDepth2() {
		String grammarURI = getFileURI(MAVEN_POM_XSD_PATH);
		XMLGenerationSettings settings = new XMLGenerationSettings();
		settings.setMaxDepth(2);
		settings.setOptionalElements(true);
		String result = generateFromURI(grammarURI, "project", settings);
		// Second-level children are present
		assertTrue(result.contains("<dependency>"));
		assertTrue(result.contains("<relativePath>" + ls));
		// Third-level children are NOT present
		assertFalse(result.contains("<exclusion>"));
	}

	// -- Edge case tests --

	/**
	 * Tests that an unknown root element returns an empty string.
	 */
	@Test
	public void unknownRootElement() {
		String grammarURI = getFileURI(GENERATOR_XSD_PATH + "simpleSequence.xsd");
		String result = generateFromURI(grammarURI, "unknown");
		assertEquals("", result);
	}
}
