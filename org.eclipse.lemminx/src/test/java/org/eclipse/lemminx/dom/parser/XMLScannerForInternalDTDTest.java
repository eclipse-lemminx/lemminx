/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.dom.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * XML scanner test to parse internal DTD (declared inside XML with <!DOCTYPE)
 *
 */
public class XMLScannerForInternalDTDTest {

	public Scanner scanner;

	@Test
	public void testDocumentTypeInternalOnly() {
		String xml = "<!DOCTYPE note [\n" + //
				"  <!ENTITY nbsp \"&#xA0;\"> \n" + //
				"  <!ENTITY writer \"Writer: Donald Duck.\">\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartEntity);
		assertOffsetAndToken(27, TokenType.Whitespace);
		assertOffsetAndToken(28, TokenType.DTDEntityName);
		assertOffsetAndToken(32, TokenType.Whitespace);
		assertOffsetAndToken(33, TokenType.DTDEntityValue);
		assertOffsetAndToken(41, TokenType.DTDEndTag);
		assertOffsetAndToken(42, TokenType.Content);
		assertOffsetAndToken(46, TokenType.DTDStartEntity);
		assertOffsetAndToken(54, TokenType.Whitespace);
		assertOffsetAndToken(55, TokenType.DTDEntityName);
		assertOffsetAndToken(61, TokenType.Whitespace);
		assertOffsetAndToken(62, TokenType.DTDEntityValue);
		assertOffsetAndToken(84, TokenType.DTDEndTag);
		assertOffsetAndToken(85, TokenType.Content);
		assertOffsetAndToken(86, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(87, TokenType.DTDEndDoctypeTag);

	}

	@Test
	public void testDocumentTypePublicAndInternal() {

		String xml = "<!DOCTYPE html PUBLIC\n" + //
				"  \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" + //
				"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"\n" + //
				"  [\n" + //
				"    <!ENTITY nbsp \"&#xA0;\"> \n" + //
				"  ]\n" + //
				">";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDDocTypeKindPUBLIC);
		assertOffsetAndToken(21, TokenType.Whitespace);
		assertOffsetAndToken(24, TokenType.DTDDoctypePublicId);
		assertOffsetAndToken(64, TokenType.Whitespace);
		assertOffsetAndToken(67, TokenType.DTDDoctypeSystemId);
		assertOffsetAndToken(124, TokenType.Whitespace);
		assertOffsetAndToken(127, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(128, TokenType.Content);
		assertOffsetAndToken(133, TokenType.DTDStartEntity);
		assertOffsetAndToken(141, TokenType.Whitespace);
		assertOffsetAndToken(142, TokenType.DTDEntityName);
		assertOffsetAndToken(146, TokenType.Whitespace);
		assertOffsetAndToken(147, TokenType.DTDEntityValue);
		assertOffsetAndToken(155, TokenType.DTDEndTag);
		assertOffsetAndToken(156, TokenType.Content);
		assertOffsetAndToken(160, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(161, TokenType.Whitespace);
		assertOffsetAndToken(162, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(163, TokenType.EOS);
	}

	@Test
	public void testDocumentTypeSystemAndInternal() {

		String xml = "<!DOCTYPE html SYSTEM\n" + //
				"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"\n" + //
				"  [\n" + //
				"    <!ELEMENT test (a)> \n" + //
				"  ]\n" + //
				">";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDDocTypeKindSYSTEM);
		assertOffsetAndToken(21, TokenType.Whitespace);
		assertOffsetAndToken(24, TokenType.DTDDoctypeSystemId);
		assertOffsetAndToken(81, TokenType.Whitespace);
		assertOffsetAndToken(84, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(85, TokenType.Content);
		assertOffsetAndToken(90, TokenType.DTDStartElement);
		assertOffsetAndToken(99, TokenType.Whitespace);
		assertOffsetAndToken(100, TokenType.DTDElementDeclName);
		assertOffsetAndToken(104, TokenType.Whitespace);
		assertOffsetAndToken(105, TokenType.DTDStartElementContent);
		assertOffsetAndToken(106, TokenType.DTDElementContent);
		assertOffsetAndToken(107, TokenType.DTDEndElementContent);
		assertOffsetAndToken(108, TokenType.DTDEndTag);
		assertOffsetAndToken(109, TokenType.Content);
		assertOffsetAndToken(113, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(114, TokenType.Whitespace);
		assertOffsetAndToken(115, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(116, TokenType.EOS);
	}

	@Test
	public void testDocumentTypeSystemAndInternalAttlist() {

		String xml = "<!DOCTYPE html SYSTEM\n" + //
				"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"\n" + //
				"  [\n" + //
				"    <!ATTLIST payment type CDATA \"cheque\"> \n" + //
				"  ]\n" + //
				">";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDDocTypeKindSYSTEM);
		assertOffsetAndToken(21, TokenType.Whitespace);
		assertOffsetAndToken(24, TokenType.DTDDoctypeSystemId);
		assertOffsetAndToken(81, TokenType.Whitespace);
		assertOffsetAndToken(84, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(85, TokenType.Content);
		assertOffsetAndToken(90, TokenType.DTDStartAttlist);
		assertOffsetAndToken(99, TokenType.Whitespace);
		assertOffsetAndToken(100, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(107, TokenType.Whitespace);
		assertOffsetAndToken(108, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(112, TokenType.Whitespace);
		assertOffsetAndToken(113, TokenType.DTDAttlistAttributeType);
		assertOffsetAndToken(118, TokenType.Whitespace);
		assertOffsetAndToken(119, TokenType.DTDAttlistAttributeValue);
		assertOffsetAndToken(127, TokenType.DTDEndTag);
		assertOffsetAndToken(128, TokenType.Content);
		assertOffsetAndToken(132, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(133, TokenType.Whitespace);
		assertOffsetAndToken(134, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(135, TokenType.EOS);
	}

	@Test
	public void testDocumentTypeSystemAndInternalAttlist2() {

		String xml = "<!DOCTYPE html SYSTEM\n" + //
				"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"\n" + //
				"  [\n" + //
				"    <!ATTLIST payment type (first|second) \"first\"> \n" + //
				"  ]\n" + //
				">";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDDocTypeKindSYSTEM);
		assertOffsetAndToken(21, TokenType.Whitespace);
		assertOffsetAndToken(24, TokenType.DTDDoctypeSystemId);
		assertOffsetAndToken(81, TokenType.Whitespace);
		assertOffsetAndToken(84, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(85, TokenType.Content);
		assertOffsetAndToken(90, TokenType.DTDStartAttlist);
		assertOffsetAndToken(99, TokenType.Whitespace);
		assertOffsetAndToken(100, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(107, TokenType.Whitespace);
		assertOffsetAndToken(108, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(112, TokenType.Whitespace);
		assertOffsetAndToken(113, TokenType.DTDAttlistAttributeType);
		assertOffsetAndToken(127, TokenType.Whitespace);
		assertOffsetAndToken(128, TokenType.DTDAttlistAttributeValue);
		assertOffsetAndToken(135, TokenType.DTDEndTag);
		assertOffsetAndToken(136, TokenType.Content);
		assertOffsetAndToken(140, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(141, TokenType.Whitespace);
		assertOffsetAndToken(142, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(143, TokenType.EOS);
	}

	@Test
	public void testAttlistWithIDType() {
		// Verify keyword type scanning for ID, IDREF, IDREFS
		String xml = "<!DOCTYPE root [\n" + //
				"  <!ATTLIST root id ID #REQUIRED>\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(29, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(33, TokenType.Whitespace);
		assertOffsetAndToken(34, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(36, TokenType.Whitespace);
		assertOffsetAndToken(37, TokenType.DTDAttlistAttributeType, "ID");
		assertOffsetAndToken(39, TokenType.Whitespace);
		assertOffsetAndToken(40, TokenType.DTDAttlistAttributeValue, "#REQUIRED");
		assertOffsetAndToken(49, TokenType.DTDEndTag);
		assertOffsetAndToken(50, TokenType.Content);
		assertOffsetAndToken(51, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(52, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(53, TokenType.EOS);
	}

	@Test
	public void testAttlistWithThreeValueEnumeration() {
		// <!ATTLIST root status (one|two|three) "four"> → (one|two|three) is a single token
		String xml = "<!DOCTYPE root [\n" + //
				"  <!ATTLIST root status (one|two|three) \"four\">\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(29, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(33, TokenType.Whitespace);
		assertOffsetAndToken(34, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(40, TokenType.Whitespace);
		assertOffsetAndToken(41, TokenType.DTDAttlistAttributeType, "(one|two|three)");
		assertOffsetAndToken(56, TokenType.Whitespace);
		assertOffsetAndToken(57, TokenType.DTDAttlistAttributeValue, "\"four\"");
		assertOffsetAndToken(63, TokenType.DTDEndTag);
		assertOffsetAndToken(64, TokenType.Content);
		assertOffsetAndToken(65, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(66, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(67, TokenType.EOS);
	}

	@Test
	public void testDocumentTypeSystemAndEmptyInternalDTD() {

		String xml = "<!DOCTYPE html SYSTEM\n" + //
				"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"\n" + //
				"  [\n" + //
				"  ]\n" + //
				">";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDDocTypeKindSYSTEM);
		assertOffsetAndToken(21, TokenType.Whitespace);
		assertOffsetAndToken(24, TokenType.DTDDoctypeSystemId);
		assertOffsetAndToken(81, TokenType.Whitespace);
		assertOffsetAndToken(84, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(85, TokenType.Content);
		assertOffsetAndToken(88, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(89, TokenType.Whitespace);
		assertOffsetAndToken(90, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(91, TokenType.EOS);
	}

	@Test
	public void testDocumentTypeSystem() {
		String xml = "<!DOCTYPE html SYSTEM\n" + //
				"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"\n" + //
				">";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDDocTypeKindSYSTEM);
		assertOffsetAndToken(21, TokenType.Whitespace);
		assertOffsetAndToken(24, TokenType.DTDDoctypeSystemId);
		assertOffsetAndToken(81, TokenType.Whitespace);
		assertOffsetAndToken(82, TokenType.DTDEndDoctypeTag);
	}

	@Test
	public void oneElementDeclNotClosed () {
		String xml = "<!DOCTYPE foo [\r\n" + 
				"<!ELEMENT a]>   \r\n" + 
				"<foo />";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(13, TokenType.Whitespace);
		assertOffsetAndToken(14, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(15, TokenType.Content);
		assertOffsetAndToken(17, TokenType.DTDStartElement);
		assertOffsetAndToken(26, TokenType.Whitespace);
		assertOffsetAndToken(27, TokenType.DTDElementDeclName);
		assertOffsetAndToken(28, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(29, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(30, TokenType.Content);
		assertOffsetAndToken(35, TokenType.StartTagOpen);
		assertOffsetAndToken(36, TokenType.StartTag);
		assertOffsetAndToken(39, TokenType.Whitespace);
		assertOffsetAndToken(40, TokenType.StartTagSelfClose);
		assertOffsetAndToken(42, TokenType.EOS);
	}

	@Test
	public void startWithElementDeclNotClosed () {
		String xml = "<!DOCTYPE foo [\r\n" + // 
				"<!ELEMENT a\r\n" + // here element is not closed
				"<!ELEMENT b]>   \r\n" + // here element is not closed
				"<foo />";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(13, TokenType.Whitespace);
		assertOffsetAndToken(14, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(15, TokenType.Content);
		assertOffsetAndToken(17, TokenType.DTDStartElement);
		assertOffsetAndToken(26, TokenType.Whitespace);
		assertOffsetAndToken(27, TokenType.DTDElementDeclName);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(30, TokenType.DTDStartElement);
		assertOffsetAndToken(39, TokenType.Whitespace);
		assertOffsetAndToken(40, TokenType.DTDElementDeclName);
		assertOffsetAndToken(41, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(42, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(43, TokenType.Content);
		assertOffsetAndToken(48, TokenType.StartTagOpen);
		assertOffsetAndToken(49, TokenType.StartTag);
		assertOffsetAndToken(52, TokenType.Whitespace);
		assertOffsetAndToken(53, TokenType.StartTagSelfClose);
		assertOffsetAndToken(55, TokenType.EOS);
	}

	@Test
	public void startWithNotationDeclNotClosed () {
		String xml = 
				"<!DOCTYPE foo [\r\n" + // 
				"  <!NOTATION a\r\n" + // here element is not closed
				"]\r\n" +
				"<foo />";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(13, TokenType.Whitespace);
		assertOffsetAndToken(14, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(15, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartNotation);
		assertOffsetAndToken(29, TokenType.Whitespace);
		assertOffsetAndToken(30, TokenType.DTDNotationName);
		assertOffsetAndToken(31, TokenType.Whitespace);
		assertOffsetAndToken(33, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(34, TokenType.Whitespace);
		assertOffsetAndToken(36, TokenType.StartTagOpen);
		assertOffsetAndToken(37, TokenType.StartTag);
		assertOffsetAndToken(40, TokenType.Whitespace);
		assertOffsetAndToken(41, TokenType.StartTagSelfClose);
		assertOffsetAndToken(43, TokenType.EOS);
	}

	@Test
	public void startWithNotationDeclNotClosedWithContent () {
		String xml = 
				"<!DOCTYPE foo [\r\n" + // 
				"  <!NOTATION a\r\n" + // here element is not closed
				"] CONTENT\r\n" +
				"<foo />";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(13, TokenType.Whitespace);
		assertOffsetAndToken(14, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(15, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartNotation);
		assertOffsetAndToken(29, TokenType.Whitespace);
		assertOffsetAndToken(30, TokenType.DTDNotationName);
		assertOffsetAndToken(31, TokenType.Whitespace);
		assertOffsetAndToken(33, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(34, TokenType.Whitespace);
		assertOffsetAndToken(35, TokenType.DTDUnrecognizedParameters);
		assertOffsetAndToken(44, TokenType.StartTagOpen);
		assertOffsetAndToken(45, TokenType.StartTag);
		assertOffsetAndToken(48, TokenType.Whitespace);
		assertOffsetAndToken(49, TokenType.StartTagSelfClose);
		assertOffsetAndToken(51, TokenType.EOS);
	}

	@Test
	public void startWithEntityDeclNotClosedWithContent () {
		String xml = 
				"<!DOCTYPE foo [\r\n" + // 
				"  <!ENTITY a\r\n" + // here element is not closed
				"] CONTENT\r\n" +
				"<foo />";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(13, TokenType.Whitespace);
		assertOffsetAndToken(14, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(15, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartEntity);
		assertOffsetAndToken(27, TokenType.Whitespace);
		assertOffsetAndToken(28, TokenType.DTDEntityName);
		assertOffsetAndToken(29, TokenType.Whitespace);
		assertOffsetAndToken(31, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(32, TokenType.Whitespace);
		assertOffsetAndToken(33, TokenType.DTDUnrecognizedParameters);
		assertOffsetAndToken(42, TokenType.StartTagOpen);
		assertOffsetAndToken(43, TokenType.StartTag);
		assertOffsetAndToken(46, TokenType.Whitespace);
		assertOffsetAndToken(47, TokenType.StartTagSelfClose);
		assertOffsetAndToken(49, TokenType.EOS);
	}

	@Test
	public void startWithAttlistDeclNotClosed() {
		String xml = 
				"<!DOCTYPE foo [\r\n" + // 
				"  <!ATTLIST \r\n" + // here element is not closed
				"]\r\n" +
				"<foo />";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(13, TokenType.Whitespace);
		assertOffsetAndToken(14, TokenType.DTDStartInternalSubset);

		assertOffsetAndToken(15, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);

		assertOffsetAndToken(31, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(32, TokenType.Whitespace);

		assertOffsetAndToken(34, TokenType.StartTagOpen);
		assertOffsetAndToken(35, TokenType.StartTag);
		assertOffsetAndToken(38, TokenType.Whitespace);
		assertOffsetAndToken(39, TokenType.StartTagSelfClose);
		assertOffsetAndToken(41, TokenType.EOS);
	}

	@Test
	public void startWithAttlistDeclNotClosedAndMissingEndInternalSubset() {
		String xml = 
				"<!DOCTYPE foo [\r\n" + // 
				"  <!ATTLIST \r\n" + // here element is not closed
				"\r\n" +
				"<foo />";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(13, TokenType.Whitespace);
		assertOffsetAndToken(14, TokenType.DTDStartInternalSubset);

		assertOffsetAndToken(15, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);

		assertOffsetAndToken(33, TokenType.Content);
		assertOffsetAndToken(40, TokenType.EOS);
	}

	@Test
	public void testAttlistWithCDATAType() {
		String xml = "<!DOCTYPE root [\n" + //
				"  <!ATTLIST root name CDATA #IMPLIED>\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(29, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(33, TokenType.Whitespace);
		assertOffsetAndToken(34, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(38, TokenType.Whitespace);
		assertOffsetAndToken(39, TokenType.DTDAttlistAttributeType, "CDATA");
		assertOffsetAndToken(44, TokenType.Whitespace);
		assertOffsetAndToken(45, TokenType.DTDAttlistAttributeValue, "#IMPLIED");
		assertOffsetAndToken(53, TokenType.DTDEndTag);
		assertOffsetAndToken(54, TokenType.Content);
		assertOffsetAndToken(55, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(56, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(57, TokenType.EOS);
	}

	@Test
	public void testAttlistWithIDREFType() {
		String xml = "<!DOCTYPE root [\n" + //
				"  <!ATTLIST root ref IDREF #IMPLIED>\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(29, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(33, TokenType.Whitespace);
		assertOffsetAndToken(34, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(37, TokenType.Whitespace);
		assertOffsetAndToken(38, TokenType.DTDAttlistAttributeType, "IDREF");
		assertOffsetAndToken(43, TokenType.Whitespace);
		assertOffsetAndToken(44, TokenType.DTDAttlistAttributeValue, "#IMPLIED");
		assertOffsetAndToken(52, TokenType.DTDEndTag);
		assertOffsetAndToken(53, TokenType.Content);
		assertOffsetAndToken(54, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(55, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(56, TokenType.EOS);
	}

	@Test
	public void testAttlistWithIDREFSType() {
		String xml = "<!DOCTYPE root [\n" + //
				"  <!ATTLIST root refs IDREFS #IMPLIED>\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(29, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(33, TokenType.Whitespace);
		assertOffsetAndToken(34, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(38, TokenType.Whitespace);
		assertOffsetAndToken(39, TokenType.DTDAttlistAttributeType, "IDREFS");
		assertOffsetAndToken(45, TokenType.Whitespace);
		assertOffsetAndToken(46, TokenType.DTDAttlistAttributeValue, "#IMPLIED");
		assertOffsetAndToken(54, TokenType.DTDEndTag);
		assertOffsetAndToken(55, TokenType.Content);
		assertOffsetAndToken(56, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(57, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(58, TokenType.EOS);
	}

	@Test
	public void testAttlistWithNMTOKENType() {
		String xml = "<!DOCTYPE root [\n" + //
				"  <!ATTLIST root tok NMTOKEN #IMPLIED>\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(29, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(33, TokenType.Whitespace);
		assertOffsetAndToken(34, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(37, TokenType.Whitespace);
		assertOffsetAndToken(38, TokenType.DTDAttlistAttributeType, "NMTOKEN");
		assertOffsetAndToken(45, TokenType.Whitespace);
		assertOffsetAndToken(46, TokenType.DTDAttlistAttributeValue, "#IMPLIED");
		assertOffsetAndToken(54, TokenType.DTDEndTag);
		assertOffsetAndToken(55, TokenType.Content);
		assertOffsetAndToken(56, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(57, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(58, TokenType.EOS);
	}

	@Test
	public void testAttlistWithNMTOKENSType() {
		String xml = "<!DOCTYPE root [\n" + //
				"  <!ATTLIST root toks NMTOKENS #IMPLIED>\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(29, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(33, TokenType.Whitespace);
		assertOffsetAndToken(34, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(38, TokenType.Whitespace);
		assertOffsetAndToken(39, TokenType.DTDAttlistAttributeType, "NMTOKENS");
		assertOffsetAndToken(47, TokenType.Whitespace);
		assertOffsetAndToken(48, TokenType.DTDAttlistAttributeValue, "#IMPLIED");
		assertOffsetAndToken(56, TokenType.DTDEndTag);
		assertOffsetAndToken(57, TokenType.Content);
		assertOffsetAndToken(58, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(59, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(60, TokenType.EOS);
	}

	@Test
	public void testAttlistWithENTITYType() {
		String xml = "<!DOCTYPE root [\n" + //
				"  <!ATTLIST root pic ENTITY #IMPLIED>\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(29, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(33, TokenType.Whitespace);
		assertOffsetAndToken(34, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(37, TokenType.Whitespace);
		assertOffsetAndToken(38, TokenType.DTDAttlistAttributeType, "ENTITY");
		assertOffsetAndToken(44, TokenType.Whitespace);
		assertOffsetAndToken(45, TokenType.DTDAttlistAttributeValue, "#IMPLIED");
		assertOffsetAndToken(53, TokenType.DTDEndTag);
		assertOffsetAndToken(54, TokenType.Content);
		assertOffsetAndToken(55, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(56, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(57, TokenType.EOS);
	}

	@Test
	public void testAttlistWithENTITIESType() {
		String xml = "<!DOCTYPE root [\n" + //
				"  <!ATTLIST root pics ENTITIES #IMPLIED>\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(29, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(33, TokenType.Whitespace);
		assertOffsetAndToken(34, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(38, TokenType.Whitespace);
		assertOffsetAndToken(39, TokenType.DTDAttlistAttributeType, "ENTITIES");
		assertOffsetAndToken(47, TokenType.Whitespace);
		assertOffsetAndToken(48, TokenType.DTDAttlistAttributeValue, "#IMPLIED");
		assertOffsetAndToken(56, TokenType.DTDEndTag);
		assertOffsetAndToken(57, TokenType.Content);
		assertOffsetAndToken(58, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(59, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(60, TokenType.EOS);
	}

	@Test
	public void testAttlistWithNOTATIONType() {
		String xml = "<!DOCTYPE root [\n" + //
				"  <!ATTLIST root fmt NOTATION #IMPLIED>\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(29, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(33, TokenType.Whitespace);
		assertOffsetAndToken(34, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(37, TokenType.Whitespace);
		assertOffsetAndToken(38, TokenType.DTDAttlistAttributeType, "NOTATION");
		assertOffsetAndToken(46, TokenType.Whitespace);
		assertOffsetAndToken(47, TokenType.DTDAttlistAttributeValue, "#IMPLIED");
		assertOffsetAndToken(55, TokenType.DTDEndTag);
		assertOffsetAndToken(56, TokenType.Content);
		assertOffsetAndToken(57, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(58, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(59, TokenType.EOS);
	}

	@Test
	public void testAttlistWithSingleValueEnumeration() {
		String xml = "<!DOCTYPE root [\n" + //
				"  <!ATTLIST root status (active) #REQUIRED>\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(29, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(33, TokenType.Whitespace);
		assertOffsetAndToken(34, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(40, TokenType.Whitespace);
		assertOffsetAndToken(41, TokenType.DTDAttlistAttributeType, "(active)");
		assertOffsetAndToken(49, TokenType.Whitespace);
		assertOffsetAndToken(50, TokenType.DTDAttlistAttributeValue, "#REQUIRED");
		assertOffsetAndToken(59, TokenType.DTDEndTag);
		assertOffsetAndToken(60, TokenType.Content);
		assertOffsetAndToken(61, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(62, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(63, TokenType.EOS);
	}

	@Test
	public void testAttlistWithTwoValueEnumeration() {
		String xml = "<!DOCTYPE root [\n" + //
				"  <!ATTLIST root flag (yes|no) \"yes\">\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(29, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(33, TokenType.Whitespace);
		assertOffsetAndToken(34, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(38, TokenType.Whitespace);
		assertOffsetAndToken(39, TokenType.DTDAttlistAttributeType, "(yes|no)");
		assertOffsetAndToken(47, TokenType.Whitespace);
		assertOffsetAndToken(48, TokenType.DTDAttlistAttributeValue, "\"yes\"");
		assertOffsetAndToken(53, TokenType.DTDEndTag);
		assertOffsetAndToken(54, TokenType.Content);
		assertOffsetAndToken(55, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(56, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(57, TokenType.EOS);
	}

	@Test
	public void testAttlistWithFixedValue() {
		String xml = "<!DOCTYPE root [\n" + //
				"  <!ATTLIST root ver CDATA #FIXED \"1.0\">\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(29, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(33, TokenType.Whitespace);
		assertOffsetAndToken(34, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(37, TokenType.Whitespace);
		assertOffsetAndToken(38, TokenType.DTDAttlistAttributeType, "CDATA");
		assertOffsetAndToken(43, TokenType.Whitespace);
		assertOffsetAndToken(44, TokenType.DTDAttlistAttributeValue, "#FIXED \"1.0\"");
		assertOffsetAndToken(56, TokenType.DTDEndTag);
		assertOffsetAndToken(57, TokenType.Content);
		assertOffsetAndToken(58, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(59, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(60, TokenType.EOS);
	}

	@Test
	public void testAttlistKeywordNotConfusedWithPrefix() {
		// "IDENTITY" starts with "ID" but is not a keyword — must be unrecognized
		String xml = "<!DOCTYPE root [\n" + //
				"  <!ATTLIST root attr IDENTITY #IMPLIED>\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(29, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(33, TokenType.Whitespace);
		assertOffsetAndToken(34, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(38, TokenType.Whitespace);
		// "IDENTITY" should NOT match "ID" keyword — must go to unrecognized
		assertOffsetAndToken(39, TokenType.DTDUnrecognizedParameters);
		assertOffsetAndToken(56, TokenType.DTDEndTag);
		assertOffsetAndToken(57, TokenType.Content);
		assertOffsetAndToken(58, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(59, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(60, TokenType.EOS);
	}

	@Test
	public void testAttlistEnumerationUnclosed() {
		// (one|two → missing closing paren
		String xml = "<!DOCTYPE root [\n" + //
				"  <!ATTLIST root status (one|two>\n" + //
				"]>";
		scanner = XMLScanner.createScanner(xml);
		assertOffsetAndToken(0, TokenType.DTDStartDoctypeTag);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.DTDDoctypeName);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.DTDStartInternalSubset);
		assertOffsetAndToken(16, TokenType.Content);
		assertOffsetAndToken(19, TokenType.DTDStartAttlist);
		assertOffsetAndToken(28, TokenType.Whitespace);
		assertOffsetAndToken(29, TokenType.DTDAttlistElementName);
		assertOffsetAndToken(33, TokenType.Whitespace);
		assertOffsetAndToken(34, TokenType.DTDAttlistAttributeName);
		assertOffsetAndToken(40, TokenType.Whitespace);
		// Unclosed enumeration: no ')' before '>' → must go to unrecognized
		assertOffsetAndToken(41, TokenType.DTDUnrecognizedParameters);
		assertOffsetAndToken(49, TokenType.DTDEndTag);
		assertOffsetAndToken(50, TokenType.Content);
		assertOffsetAndToken(51, TokenType.DTDEndInternalSubset);
		assertOffsetAndToken(52, TokenType.DTDEndDoctypeTag);
		assertOffsetAndToken(53, TokenType.EOS);
	}

	public void assertOffsetAndToken(int tokenOffset, TokenType tokenType) {
		TokenType token = scanner.scan();
		// System.err.println("assertOffsetAndToken(" + scanner.getTokenOffset() +  ", TokenType." + scanner.getTokenType() + ");");
		assertEquals(tokenOffset, scanner.getTokenOffset());
		assertEquals(tokenType, token);
	}

	public void assertOffsetAndToken(int tokenOffset, TokenType tokenType, String tokenText) {
		TokenType token = scanner.scan();
		assertEquals(tokenOffset, scanner.getTokenOffset());
		assertEquals(tokenType, token);
		assertEquals(tokenText, scanner.getTokenText());
	}
}
	
