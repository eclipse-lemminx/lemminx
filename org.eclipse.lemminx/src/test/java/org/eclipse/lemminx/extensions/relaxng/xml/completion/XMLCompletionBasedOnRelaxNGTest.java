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
package org.eclipse.lemminx.extensions.relaxng.xml.completion;

import static org.eclipse.lemminx.XMLAssert.CATALOG_SNIPPETS;
import static org.eclipse.lemminx.XMLAssert.CDATA_SNIPPETS;
import static org.eclipse.lemminx.XMLAssert.COMMENT_SNIPPETS;
import static org.eclipse.lemminx.XMLAssert.NEW_XML_SNIPPETS;
import static org.eclipse.lemminx.XMLAssert.PROCESSING_INSTRUCTION_SNIPPETS;
import static org.eclipse.lemminx.XMLAssert.REGION_SNIPPETS;
import static org.eclipse.lemminx.XMLAssert.c;
import static org.eclipse.lemminx.XMLAssert.te;

import java.util.Arrays;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.contentmodel.BaseFileTempTest;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemCapabilities;
import org.junit.jupiter.api.Test;

/**
 * XML completion tests based on RelaxNG.
 *
 */
public class XMLCompletionBasedOnRelaxNGTest extends BaseFileTempTest {

	@Test
	public void completionOnRoot() throws BadLocationException {
		// completion on <|
		String xml = "<?xml-model href=\"tei_all.rng\" ?>\r\n" + //
				"<|";
		testCompletionFor(xml, //
				null, //
				c("TEI", te(1, 0, 1, 1, "<TEI></TEI>"), "<TEI"), //
				c("teiCorpus", te(1, 0, 1, 1, "<teiCorpus></teiCorpus>"), "<teiCorpus"));
	}

	@Test
	public void completionInDocumentElement() throws BadLocationException {
		// completion on <|
		String xml = "<?xml-model href=\"tei_all.rng\" ?>\r\n" + //
				"<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\r\n" + //
				"  <|\r\n" + //
				"</TEI>";
		testCompletionFor(xml, //
				1 /* teiHeader */ + CDATA_SNIPPETS + COMMENT_SNIPPETS, //
				c("teiHeader", te(2, 2, 2, 3, "<teiHeader></teiHeader>"), "<teiHeader"));
	}

	@Test
	public void completionWithTwoSameRelaxNG() throws BadLocationException {
		// completion on <|
		String xml = "<?xml-model href=\"tei_all.rng\" ?>\r\n" + //
				"<?xml-model href=\"tei_all.rng\" ?>\r\n" + //
				"<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\r\n" + //
				"  <|\r\n" + //
				"</TEI>";
		testCompletionFor(xml, //
				2 /* 2 * teiHeader */ + CDATA_SNIPPETS + COMMENT_SNIPPETS, //
				c("teiHeader", te(3, 2, 3, 3, "<teiHeader></teiHeader>"), "<teiHeader"));

		xml = "<?xml-model href=\"tei_all.rng\" schematypens=\"http://relaxng.org/ns/structure/1.0\" ?>\r\n" + // <--
																												// applicable
				"<?xml-model href=\"tei_all.rng\" schematypens=\"http://purl.oclc.org/dsdl/schematron\" ?>\r\n" + // //
																													// <--
																													// NOT
																													// applicable
				"<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\r\n" + //
				"  <|\r\n" + //
				"</TEI>";
		testCompletionFor(xml, //
				1 /* teiHeader */ + CDATA_SNIPPETS + COMMENT_SNIPPETS, //
				c("teiHeader", te(3, 2, 3, 3, "<teiHeader></teiHeader>"), "<teiHeader"));
	}

	@Test
	public void completionOnRootWithXMLNS() throws BadLocationException {
		// completion on <|
		String xml = "<?xml-model href=\"simple.rng\"?>\r\n" + //
				"|";
		testCompletionFor(xml, //
				1 + //
						REGION_SNIPPETS /* #region */ + //
						NEW_XML_SNIPPETS /* DOCTYPE snippets */ + //
						PROCESSING_INSTRUCTION_SNIPPETS /* Processing Instruction snippets */ + //
						COMMENT_SNIPPETS /* Comment snippets */ + //
						CATALOG_SNIPPETS /* Catalog snippets */ , //
				c("rootelt", te(1, 0, 1, 0,
						"<rootelt xmlns:lmx=\"https://github.com/eclipse/lemminx\" xml:lang=\"\" lmx:type=\"\"></rootelt>"),
						"rootelt"));
	}

	@Test
	public void completionInElementWithXMLNS() throws BadLocationException {
		// completion on <|
		String xml = "<?xml-model href=\"simple.rng\"?>\r\n" + //
				"<rootelt xmlns:lmx=\"https://github.com/eclipse/lemminx\" xml:lang=\"\" lmx:type=\"\">\r\n" + //
				"|\r\n" + //
				"</rootelt>";
		testCompletionFor(xml, //
				2 + //
						REGION_SNIPPETS /* #region */ + //
						COMMENT_SNIPPETS /* Comment snippets */ + //
						CDATA_SNIPPETS /* CDATA snippets */ , //
				c("child", te(2, 0, 2, 0,
						"<child xmlns:vx=\"https://github.com/redhat-developer/vscode-xml\" vx:type=\"\"></child>"),
						"child"));

		xml = "<?xml-model href=\"simple.rng\"?>\r\n" + //
				"<rootelt xmlns:lmx=\"https://github.com/eclipse/lemminx\" xml:lang=\"\" lmx:type=\"\">\r\n" + //
				"<|\r\n" + //
				"</rootelt>";
		testCompletionFor(xml, //
				2 + //
						COMMENT_SNIPPETS /* Comment snippets */ + //
						CDATA_SNIPPETS /* CDATA snippets */ , //
				c("child", te(2, 0, 2, 1,
						"<child xmlns:vx=\"https://github.com/redhat-developer/vscode-xml\" vx:type=\"\"></child>"),
						"<child"));
	}

	@Test
	public void completionInElementWithXMLNSAndDefinedNS() throws BadLocationException {
		// completion on <|
		String xml = "<?xml-model href=\"simple.rng\"?>\r\n" + //
				"<rootelt xmlns:lmx=\"https://github.com/eclipse/lemminx\" xmlns:myvx=\"https://github.com/redhat-developer/vscode-xml\" xml:lang=\"\" lmx:type=\"\">\r\n"
				+ //
				"|\r\n" + //
				"</rootelt>";
		testCompletionFor(xml, //
				3 + //
						REGION_SNIPPETS /* #region */ + //
						COMMENT_SNIPPETS /* Comment snippets */ + //
						CDATA_SNIPPETS /* CDATA snippets */ , //
				c("child", te(2, 0, 2, 0, "<child myvx:type=\"\"></child>"), "child"));

		xml = "<?xml-model href=\"simple.rng\"?>\r\n" + //
				"<rootelt xmlns:lmx=\"https://github.com/eclipse/lemminx\" xmlns:myvx=\"https://github.com/redhat-developer/vscode-xml\" xml:lang=\"\" lmx:type=\"\">\r\n"
				+ //
				"<|\r\n" + //
				"</rootelt>";
		testCompletionFor(xml, //
				3 + //
						COMMENT_SNIPPETS /* Comment snippets */ + //
						CDATA_SNIPPETS /* CDATA snippets */ , //
				c("child", te(2, 0, 2, 1, "<child myvx:type=\"\"></child>"), "<child"));
	}

	@Test
	public void completionOnAttributes() throws BadLocationException {
		// completion on <|
		String xml = "<?xml-model href=\"docbook.rng\"?>\r\n"
				+ "<book xmlns=\"http://docbook.org/ns/docbook\">\r\n"
				+ "	<acknowledgements |></acknowledgements>	\r\n"
				+ "</book>";
		testCompletionFor(xml, //
				30, //
				c("role", te(2, 19, 2, 19, "role=\"\""), "role"), //
				c("xlink:actuate", te(2, 19, 2, 19, "xlink:actuate=\"\""),
						Arrays.asList(te(1, 43, 1, 43, " xmlns:xlink=\"http://www.w3.org/1999/xlink\"")), //
						"xlink:actuate"));
	}

	@Test
	public void completionOnAttributesWithPrefix() throws BadLocationException {
		// completion on <|
		String xml = "<?xml-model href=\"docbook.rng\"?>\r\n"
				+ "<book xmlns=\"http://docbook.org/ns/docbook\">\r\n"
				+ "	<acknowledgements |></acknowledgements>	\r\n"
				+ "</book>";
		testCompletionFor(xml, //
				30, //
				c("xlink:actuate", te(2, 19, 2, 19, "xlink:actuate=\"\""),
						Arrays.asList(te(1, 43, 1, 43, " xmlns:xlink=\"http://www.w3.org/1999/xlink\"")), //
						"xlink:actuate"));
		
		xml = "<?xml-model href=\"docbook.rng\"?>\r\n"
				+ "<book xmlns=\"http://docbook.org/ns/docbook\" >\r\n"
				+ "	<acknowledgements |></acknowledgements>	\r\n"
				+ "</book>";
		testCompletionFor(xml, //
				30, //
				c("xlink:actuate", te(2, 19, 2, 19, "xlink:actuate=\"\""),
						Arrays.asList(te(1, 43, 1, 43, " xmlns:xlink=\"http://www.w3.org/1999/xlink\"")), //
						"xlink:actuate"));
		
		xml = "<?xml-model href=\"docbook.rng\"?>\r\n"
				+ "<book\r\n"
				+ "	<acknowledgements |></acknowledgements>	\r\n"
				+ "</book>";
		testCompletionFor(xml, //
				30, //
				c("xlink:actuate", te(2, 19, 2, 19, "xlink:actuate=\"\""),
						Arrays.asList(te(1, 5, 1, 5, " xmlns:xlink=\"http://www.w3.org/1999/xlink\"")), //
						"xlink:actuate"));
	}

	@Test
	public void completionOnRootWithChoiceNameClass() throws BadLocationException {
		String xml = "<?xml-model href=\"choiceNameElement.rng\" ?>\r\n" + //
				"<|";
		testCompletionFor(xml, //
				null, //
				c("odoo", te(1, 0, 1, 1, "<odoo></odoo>"), "<odoo"), //
				c("openerp", te(1, 0, 1, 1, "<openerp></openerp>"), "<openerp"), //
				c("data", te(1, 0, 1, 1, "<data></data>"), "<data"));
	}

	@Test
	public void completionInElementWithChoiceNameClass() throws BadLocationException {
		String xml = "<?xml-model href=\"choiceNameElement.rng\" ?>\r\n" + //
				"<odoo>\r\n" + //
				"  <|\r\n" + //
				"</odoo>";
		testCompletionFor(xml, //
				null, //
				c("record", te(2, 2, 2, 3, "<record model=\"\"></record>"), "<record"));
	}

	@Test
	public void completionOnAttributesWithChoiceNameClass() throws BadLocationException {
		String xml = "<?xml-model href=\"choiceNameElement.rng\" ?>\r\n" + //
				"<odoo |></odoo>";
		testCompletionFor(xml, //
				null, //
				c("noupdate", te(1, 6, 1, 6, "noupdate=\"\""), "noupdate"));
	}

	// Tests for https://github.com/redhat-developer/vscode-xml/issues/1107

	@Test
	public void completionOnElementWithChoiceAttributeGroup() throws BadLocationException {
		String xml = "<?xml-model href=\"choiceAttributeGroup.rng\" ?>\r\n" + //
				"<root>\r\n" + //
				"  <|\r\n" + //
				"</root>";
		// Without snippet
		testCompletionFor(xml, //
				null, //
				c("key", te(2, 2, 2, 3, "<key name=\"\" type=\"\"></key>"), "<key"));
		// With snippet
		testCompletionSnippetSupportFor(xml, //
				null, //
				c("key", te(2, 2, 2, 3, "<key name=\"$1\" type=\"${2|dict,arr,str|}\">$3</key>$0"), "<key"));
	}

	@Test
	public void completionOnAttributeNameWithChoiceAttributeGroup() throws BadLocationException {
		String xml = "<?xml-model href=\"choiceAttributeGroup.rng\" ?>\r\n" + //
				"<root>\r\n" + //
				"  <key name=\"foo\" |></key>\r\n" + //
				"</root>";
		// Without snippet
		testCompletionFor(xml, //
				1, //
				c("type", te(2, 18, 2, 18, "type=\"\""), "type"));
		// With snippet
		testCompletionSnippetSupportFor(xml, //
				1, //
				c("type", te(2, 18, 2, 18, "type=\"${1|dict,arr,str|}\"$0"), "type"));
	}

	@Test
	public void completionOnAttributeValueWithChoiceAttributeGroup() throws BadLocationException {
		String xml = "<?xml-model href=\"choiceAttributeGroup.rng\" ?>\r\n" + //
				"<root>\r\n" + //
				"  <key name=\"foo\" type=\"|\" />\r\n" + //
				"</root>";
		testCompletionFor(xml, //
				3, //
				c("dict", te(2, 24, 2, 24, "dict"), "dict"), //
				c("arr", te(2, 24, 2, 24, "arr"), "arr"), //
				c("str", te(2, 24, 2, 24, "str"), "str"));
	}

	@Test
	public void completionOnAttributeNameWithChoiceAttributeGroupDistinct() throws BadLocationException {
		// When Type="Bar" is set, only BarAttr should be offered (not FooAttr)
		String xml = "<?xml-model href=\"choiceAttributeGroupDistinct.rng\" ?>\r\n" + //
				"<Items>\r\n" + //
				"  <Item Type=\"Bar\" |></Item>\r\n" + //
				"</Items>";
		testCompletionFor(xml, //
				1, //
				c("BarAttr", te(2, 19, 2, 19, "BarAttr=\"\""), "BarAttr"));
	}

	@Test
	public void completionOnAttributeNameWithChoiceAttributeGroupDistinctFoo() throws BadLocationException {
		// When Type="Foo" is set, only FooAttr should be offered (not BarAttr)
		String xml = "<?xml-model href=\"choiceAttributeGroupDistinct.rng\" ?>\r\n" + //
				"<Items>\r\n" + //
				"  <Item Type=\"Foo\" |></Item>\r\n" + //
				"</Items>";
		testCompletionFor(xml, //
				1, //
				c("FooAttr", te(2, 19, 2, 19, "FooAttr=\"\""), "FooAttr"));
	}

	@Test
	public void completionOnAttributeNameWithChoiceAttributeGroupDistinctNoContext() throws BadLocationException {
		// When no attributes are set, all attributes should be offered
		String xml = "<?xml-model href=\"choiceAttributeGroupDistinct.rng\" ?>\r\n" + //
				"<Items>\r\n" + //
				"  <Item |></Item>\r\n" + //
				"</Items>";
		testCompletionFor(xml, //
				3, //
				c("Type", te(2, 8, 2, 8, "Type=\"\""), "Type"), //
				c("FooAttr", te(2, 8, 2, 8, "FooAttr=\"\""), "FooAttr"), //
				c("BarAttr", te(2, 8, 2, 8, "BarAttr=\"\""), "BarAttr"));
	}

	// role,xml:id,version,xml:lang,xml:base,remap,xreflabel,revisionflag,dir,arch,audience,condition,conformance,os,revision,security,userlevel,vendor,wordsize,annotations,linkend,xlink:href,xlink:type,xlink:role,xlink:arcrole,xlink:title,xlink:show,xlink:actuate,label,status

	private static void testCompletionFor(String value, Integer expectedCount, CompletionItem... expectedItems)
			throws BadLocationException {
		XMLAssert.testCompletionFor(new XMLLanguageService(), value, null, null, "src/test/resources/relaxng/test.xml",
				expectedCount, true, expectedItems);
	}

	private static void testCompletionSnippetSupportFor(String value, Integer expectedCount,
			CompletionItem... expectedItems) throws BadLocationException {
		CompletionCapabilities completionCapabilities = new CompletionCapabilities();
		CompletionItemCapabilities completionItem = new CompletionItemCapabilities(true);
		completionCapabilities.setCompletionItem(completionItem);

		SharedSettings sharedSettings = new SharedSettings();
		sharedSettings.getCompletionSettings().setCapabilities(completionCapabilities);
		XMLAssert.testCompletionFor(new XMLLanguageService(), value, null, null, "src/test/resources/relaxng/test.xml",
				expectedCount, sharedSettings, expectedItems);
	}

}