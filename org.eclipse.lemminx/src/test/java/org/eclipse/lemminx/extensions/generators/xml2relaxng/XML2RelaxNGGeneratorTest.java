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
package org.eclipse.lemminx.extensions.generators.xml2relaxng;

import static java.lang.System.lineSeparator;
import static org.eclipse.lemminx.XMLAssert.assertGrammarGenerator;

import java.io.IOException;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.settings.SharedSettings;
import org.junit.jupiter.api.Test;

/**
 * Tests for generating RelaxNG schema from XML source.
 *
 */
public class XML2RelaxNGGeneratorTest extends AbstractCacheBasedTest {

	@Test
	public void schemaWithLegacyFormatter() throws IOException {
		String xml = "<note>\r\n" + //
				"	<to>Tove</to>\r\n" + //
				"	<from>Jani</from>\r\n" + //
				"	<heading>Reminder</heading>\r\n" + //
				"	<body>Don't forget me this weekend!</body>\r\n" + //
				"</note>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"noteContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"noteContent\">" + lineSeparator() + //
				"    <element name=\"note\">" + lineSeparator() + //
				"      <ref name=\"toContent\" />" + lineSeparator() + //
				"      <ref name=\"fromContent\" />" + lineSeparator() + //
				"      <ref name=\"headingContent\" />" + lineSeparator() + //
				"      <ref name=\"bodyContent\" />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"toContent\">" + lineSeparator() + //
				"    <element name=\"to\">" + lineSeparator() + //
				"      <text />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"fromContent\">" + lineSeparator() + //
				"    <element name=\"from\">" + lineSeparator() + //
				"      <text />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"headingContent\">" + lineSeparator() + //
				"    <element name=\"heading\">" + lineSeparator() + //
				"      <text />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"bodyContent\">" + lineSeparator() + //
				"    <element name=\"body\">" + lineSeparator() + //
				"      <text />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void schemaWithNS() throws IOException {
		String xml = "<note xmlns=\"https://www.w3schools.com\">\r\n" + //
				"	<to>Tove</to>\r\n" + //
				"	<from>Jani</from>\r\n" + //
				"	<heading>Reminder</heading>\r\n" + //
				"	<body>Don't forget me this weekend!</body>\r\n" + //
				"</note>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\" ns=\"https://www.w3schools.com\""
				+ lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">"
				+ lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"noteContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"noteContent\">" + lineSeparator() + //
				"    <element name=\"note\">" + lineSeparator() + //
				"      <ref name=\"toContent\" />" + lineSeparator() + //
				"      <ref name=\"fromContent\" />" + lineSeparator() + //
				"      <ref name=\"headingContent\" />" + lineSeparator() + //
				"      <ref name=\"bodyContent\" />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"toContent\">" + lineSeparator() + //
				"    <element name=\"to\">" + lineSeparator() + //
				"      <text />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"fromContent\">" + lineSeparator() + //
				"    <element name=\"from\">" + lineSeparator() + //
				"      <text />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"headingContent\">" + lineSeparator() + //
				"    <element name=\"heading\">" + lineSeparator() + //
				"      <text />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"bodyContent\">" + lineSeparator() + //
				"    <element name=\"body\">" + lineSeparator() + //
				"      <text />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void schemaWithDecimalAttr() throws IOException {
		String xml = "<note version=\"1.2\" />";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"noteContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"noteContent\">" + lineSeparator() + //
				"    <element name=\"note\">" + lineSeparator() + //
				"      <attribute name=\"version\">" + lineSeparator() + //
				"        <data type=\"decimal\" />" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void schemaWithDecimalAttrAndContent() throws IOException {
		String xml = "<note version=\"1.2\" >ABCD</note>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"noteContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"noteContent\">" + lineSeparator() + //
				"    <element name=\"note\">" + lineSeparator() + //
				"      <text />" + lineSeparator() + //
				"      <attribute name=\"version\">" + lineSeparator() + //
				"        <data type=\"decimal\" />" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void schemaWithDecimalAttrAndMixedContent() throws IOException {
		String xml = "<note version=\"1.2\" >AB<C/>D</note>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"noteContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"noteContent\">" + lineSeparator() + //
				"    <element name=\"note\">" + lineSeparator() + //
				"      <mixed>" + lineSeparator() + //
				"        <ref name=\"CContent\" />" + lineSeparator() + //
				"      </mixed>" + lineSeparator() + //
				"      <attribute name=\"version\">" + lineSeparator() + //
				"        <data type=\"decimal\" />" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"CContent\">" + lineSeparator() + //
				"    <element name=\"C\">" + lineSeparator() + //
				"      <empty />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void schemaWithAttrs() throws IOException {
		String xml = "<note version=\"1.2\" >\r\n" + //
				"	<to attr1=\"abcd\" attr2=\"efgh\">Tove</to>\r\n" + //
				"	<from>Jani</from>\r\n" + //
				"	<heading>Reminder</heading>\r\n" + //
				"	<body>Don't forget me this weekend!</body>\r\n" + //
				"</note>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"noteContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"noteContent\">" + lineSeparator() + //
				"    <element name=\"note\">" + lineSeparator() + //
				"      <ref name=\"toContent\" />" + lineSeparator() + //
				"      <ref name=\"fromContent\" />" + lineSeparator() + //
				"      <ref name=\"headingContent\" />" + lineSeparator() + //
				"      <ref name=\"bodyContent\" />" + lineSeparator() + //
				"      <attribute name=\"version\">" + lineSeparator() + //
				"        <data type=\"decimal\" />" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"toContent\">" + lineSeparator() + //
				"    <element name=\"to\">" + lineSeparator() + //
				"      <text />" + lineSeparator() + //
				"      <attribute name=\"attr1\" />" + lineSeparator() + //
				"      <attribute name=\"attr2\" />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"fromContent\">" + lineSeparator() + //
				"    <element name=\"from\">" + lineSeparator() + //
				"      <text />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"headingContent\">" + lineSeparator() + //
				"    <element name=\"heading\">" + lineSeparator() + //
				"      <text />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"bodyContent\">" + lineSeparator() + //
				"    <element name=\"body\">" + lineSeparator() + //
				"      <text />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void mixedContent() {
		String xml = "<a><b/>text</a>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"aContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"aContent\">" + lineSeparator() + //
				"    <element name=\"a\">" + lineSeparator() + //
				"      <mixed>" + lineSeparator() + //
				"        <ref name=\"bContent\" />" + lineSeparator() + //
				"      </mixed>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"bContent\">" + lineSeparator() + //
				"    <element name=\"b\">" + lineSeparator() + //
				"      <empty />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void threeLevel() {
		String xml = "<a>\r\n" + //
				"	<b>\r\n" + //
				"		<c/>\r\n" + //
				"	</b>\r\n" + //
				"</a>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"aContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"aContent\">" + lineSeparator() + //
				"    <element name=\"a\">" + lineSeparator() + //
				"      <ref name=\"bContent\" />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"bContent\">" + lineSeparator() + //
				"    <element name=\"b\">" + lineSeparator() + //
				"      <ref name=\"cContent\" />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"cContent\">" + lineSeparator() + //
				"    <element name=\"c\">" + lineSeparator() + //
				"      <empty />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void threeLevelAndText() {
		String xml = "<a>\r\n" + //
				"	<b>\r\n" + //
				"		<c />\r\n" + //
				"		<d>X</d>\r\n" + //
				"	</b>\r\n" + //
				"</a>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"aContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"aContent\">" + lineSeparator() + //
				"    <element name=\"a\">" + lineSeparator() + //
				"      <ref name=\"bContent\" />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"bContent\">" + lineSeparator() + //
				"    <element name=\"b\">" + lineSeparator() + //
				"      <ref name=\"cContent\" />" + lineSeparator() + //
				"      <ref name=\"dContent\" />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"cContent\">" + lineSeparator() + //
				"    <element name=\"c\">" + lineSeparator() + //
				"      <empty />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"dContent\">" + lineSeparator() + //
				"    <element name=\"d\">" + lineSeparator() + //
				"      <text />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";

		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void occurrences() {
		String xml = "<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"  xsi:noNamespaceSchemaLocation=\"grocery-invoice.xsd\">\r\n" + //
				"  <item name=\"Rice\" price=\"4.99\" />\r\n" + //
				"  <item name=\"Baked Beans\" price=\"1.99\" />\r\n" + //
				"  <item name=\"Salad\" price=\"5.99\" />\r\n" + //
				"</invoice>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"invoiceContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"invoiceContent\">" + lineSeparator() + //
				"    <element name=\"invoice\">" + lineSeparator() + //
				"      <oneOrMore>" + lineSeparator() + //
				"        <ref name=\"itemContent\" />" + lineSeparator() + //
				"      </oneOrMore>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"itemContent\">" + lineSeparator() + //
				"    <element name=\"item\">" + lineSeparator() + //
				"      <attribute name=\"name\" />" + lineSeparator() + //
				"      <attribute name=\"price\">" + lineSeparator() + //
				"        <data type=\"decimal\" />" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void oneZeroOccurrences() {
		String xml = "<root>\r\n" + //
				"    <a>\r\n" + //
				"        <b />\r\n" + //
				"    </a>\r\n" + //
				"    <a></a>\r\n" + //
				"</root>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"rootContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"rootContent\">" + lineSeparator() + //
				"    <element name=\"root\">" + lineSeparator() + //
				"      <oneOrMore>" + lineSeparator() + //
				"        <ref name=\"aContent\" />" + lineSeparator() + //
				"      </oneOrMore>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"aContent\">" + lineSeparator() + //
				"    <element name=\"a\">" + lineSeparator() + //
				"      <optional>" + lineSeparator() + //
				"        <ref name=\"bContent\" />" + lineSeparator() + //
				"      </optional>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"bContent\">" + lineSeparator() + //
				"    <element name=\"b\">" + lineSeparator() + //
				"      <empty />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void oneZeroOccurrences2() {
		String xml = "<root>\r\n" + //
				"    <a>\r\n" + //
				"        <b />\r\n" + //
				"    </a>\r\n" + //
				"    <a>\r\n" + //
				"        <c />\r\n" + //
				"    </a>\r\n" + //
				"    <a>\r\n" + //
				"        <b />\r\n" + //
				"        <c />\r\n" + //
				"    </a>\r\n" + //
				"    <a></a>\r\n" + //
				"</root>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"rootContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"rootContent\">" + lineSeparator() + //
				"    <element name=\"root\">" + lineSeparator() + //
				"      <oneOrMore>" + lineSeparator() + //
				"        <ref name=\"aContent\" />" + lineSeparator() + //
				"      </oneOrMore>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"aContent\">" + lineSeparator() + //
				"    <element name=\"a\">" + lineSeparator() + //
				"      <optional>" + lineSeparator() + //
				"        <ref name=\"bContent\" />" + lineSeparator() + //
				"      </optional>" + lineSeparator() + //
				"      <optional>" + lineSeparator() + //
				"        <ref name=\"cContent\" />" + lineSeparator() + //
				"      </optional>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"bContent\">" + lineSeparator() + //
				"    <element name=\"b\">" + lineSeparator() + //
				"      <empty />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"cContent\">" + lineSeparator() + //
				"    <element name=\"c\">" + lineSeparator() + //
				"      <empty />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void interleave() {
		String xml = "<root>\r\n" + //
				"	<a>\r\n" + //
				"		<c />\r\n" + //
				"		<b />\r\n" + //
				"	</a>\r\n" + //
				"	<a>\r\n" + //
				"		<b />\r\n" + //
				"		<c />\r\n" + //
				"	</a>\r\n" + //
				"</root>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"rootContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"rootContent\">" + lineSeparator() + //
				"    <element name=\"root\">" + lineSeparator() + //
				"      <oneOrMore>" + lineSeparator() + //
				"        <ref name=\"aContent\" />" + lineSeparator() + //
				"      </oneOrMore>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"aContent\">" + lineSeparator() + //
				"    <element name=\"a\">" + lineSeparator() + //
				"      <interleave>" + lineSeparator() + //
				"        <ref name=\"cContent\" />" + lineSeparator() + //
				"        <ref name=\"bContent\" />" + lineSeparator() + //
				"      </interleave>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"cContent\">" + lineSeparator() + //
				"    <element name=\"c\">" + lineSeparator() + //
				"      <empty />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"bContent\">" + lineSeparator() + //
				"    <element name=\"b\">" + lineSeparator() + //
				"      <empty />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void optionalAttribute() {
		String xml = "<root>\r\n" + //
				"	<item attr1=\"\" attr2=\"\"/>\r\n" + //
				"	<item attr1=\"\" />\r\n" + //
				"</root>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"rootContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"rootContent\">" + lineSeparator() + //
				"    <element name=\"root\">" + lineSeparator() + //
				"      <oneOrMore>" + lineSeparator() + //
				"        <ref name=\"itemContent\" />" + lineSeparator() + //
				"      </oneOrMore>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"itemContent\">" + lineSeparator() + //
				"    <element name=\"item\">" + lineSeparator() + //
				"      <attribute name=\"attr1\" />" + lineSeparator() + //
				"      <optional>" + lineSeparator() + //
				"        <attribute name=\"attr2\" />" + lineSeparator() + //
				"      </optional>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void attrIDsAndFixed() {
		String xml = "<root>\r\n" + //
				"	<item attr1=\"id1\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id2\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id3\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id4\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id5\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id6\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id7\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id8\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id9\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id10\" attr2=\"A\" />\r\n" + //
				"</root>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"rootContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"rootContent\">" + lineSeparator() + //
				"    <element name=\"root\">" + lineSeparator() + //
				"      <oneOrMore>" + lineSeparator() + //
				"        <ref name=\"itemContent\" />" + lineSeparator() + //
				"      </oneOrMore>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"itemContent\">" + lineSeparator() + //
				"    <element name=\"item\">" + lineSeparator() + //
				"      <attribute name=\"attr1\" />" + lineSeparator() + //
				"      <attribute name=\"attr2\">" + lineSeparator() + //
				"        <value>A</value>" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void attrIDsAndEnums() {
		String xml = "<root>\r\n" + //
				"	<item attr1=\"id1\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id2\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id3\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id4\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id5\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id6\" attr2=\"B\" />\r\n" + //
				"	<item attr1=\"id7\" attr2=\"B\" />\r\n" + //
				"	<item attr1=\"id8\" attr2=\"B\" />\r\n" + //
				"	<item attr1=\"id9\" attr2=\"B\" />\r\n" + //
				"	<item attr1=\"id10\" attr2=\"B\" />\r\n" + //
				"</root>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"rootContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"rootContent\">" + lineSeparator() + //
				"    <element name=\"root\">" + lineSeparator() + //
				"      <oneOrMore>" + lineSeparator() + //
				"        <ref name=\"itemContent\" />" + lineSeparator() + //
				"      </oneOrMore>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"itemContent\">" + lineSeparator() + //
				"    <element name=\"item\">" + lineSeparator() + //
				"      <attribute name=\"attr1\" />" + lineSeparator() + //
				"      <attribute name=\"attr2\">" + lineSeparator() + //
				"        <choice>" + lineSeparator() + //
				"          <value>A</value>" + lineSeparator() + //
				"          <value>B</value>" + lineSeparator() + //
				"        </choice>" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void attrTypes() {
		String xml = "<root>\r\n" + //
				"	<item dateTime=\"2001-10-26T21:32:52+02:00\"\r\n" + //
				"		  date=\"2001-10-26\"\r\n" + //
				"         boolean=\"true\"\r\n" + //
				"         integer=\"1\"\r\n" + //
				"         decimal=\"1.2\"  />\r\n" + //
				"</root>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"rootContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"rootContent\">" + lineSeparator() + //
				"    <element name=\"root\">" + lineSeparator() + //
				"      <ref name=\"itemContent\" />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"itemContent\">" + lineSeparator() + //
				"    <element name=\"item\">" + lineSeparator() + //
				"      <attribute name=\"dateTime\">" + lineSeparator() + //
				"        <data type=\"dateTime\" />" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"      <attribute name=\"date\">" + lineSeparator() + //
				"        <data type=\"date\" />" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"      <attribute name=\"boolean\">" + lineSeparator() + //
				"        <data type=\"boolean\" />" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"      <attribute name=\"integer\">" + lineSeparator() + //
				"        <data type=\"integer\" />" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"      <attribute name=\"decimal\">" + lineSeparator() + //
				"        <data type=\"decimal\" />" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void attrTypesWith2Occurs() {
		String xml = "<root>\r\n" + //
				"	<item dateTime=\"2001-10-26T21:32:52+02:00\"\r\n" + //
				"		  date=\"2001-10-26\"\r\n" + //
				"         boolean=\"true\"\r\n" + //
				"         integer=\"1\"\r\n" + //
				"         decimal=\"1.2\"  />\r\n" + //
				"	<item dateTime=\"2001-10-26T21:32:52+02:00\"\r\n" + //
				"		  date=\"2001-10-26\"\r\n" + //
				"         boolean=\"true\"\r\n" + //
				"         integer=\"1\"\r\n" + //
				"         decimal=\"XXXXXXXXXXXXXXXXXXXXXX\"  />\r\n" + //
				"</root>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"rootContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"rootContent\">" + lineSeparator() + //
				"    <element name=\"root\">" + lineSeparator() + //
				"      <oneOrMore>" + lineSeparator() + //
				"        <ref name=\"itemContent\" />" + lineSeparator() + //
				"      </oneOrMore>" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"itemContent\">" + lineSeparator() + //
				"    <element name=\"item\">" + lineSeparator() + //
				"      <attribute name=\"dateTime\">" + lineSeparator() + //
				"        <data type=\"dateTime\" />" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"      <attribute name=\"date\">" + lineSeparator() + //
				"        <data type=\"date\" />" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"      <attribute name=\"boolean\">" + lineSeparator() + //
				"        <data type=\"boolean\" />" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"      <attribute name=\"integer\">" + lineSeparator() + //
				"        <data type=\"integer\" />" + lineSeparator() + //
				"      </attribute>" + lineSeparator() + //
				"      <attribute name=\"decimal\" />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), rng);
	}

	@Test
	public void schemaWithTabSizeZero() throws IOException {
		String xml = "<note>\r\n" + //
				"	<to>Tove</to>\r\n" + //
				"</note>";
		String rng = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"noteContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"noteContent\">" + lineSeparator() + //
				"    <element name=\"note\">" + lineSeparator() + //
				"      <ref name=\"toContent\" />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"  <define name=\"toContent\">" + lineSeparator() + //
				"    <element name=\"to\">" + lineSeparator() + //
				"      <text />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(0);
		assertGrammarGenerator(xml, new RelaxNGGeneratorSettings(), settings, rng);
	}
}
