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
package org.eclipse.lemminx.extensions.contentmodel;

import static org.eclipse.lemminx.XMLAssert.ih;
import static org.eclipse.lemminx.XMLAssert.ihType;
import static org.eclipse.lemminx.XMLAssert.testInlayHintFor;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.XMLInlayHintSettings;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

/**
 * Inlay hint tests for XSD grammar features: default attribute values,
 * attribute types, and element types.
 *
 * Uses form.xsd which declares:
 * <ul>
 * <li>form element (anonymous type) with attrs: lang(xs:string,default="en"),
 * method(xs:string,default="GET"), maxRetries(xs:int),
 * debug(xs:boolean,default="false")</li>
 * <li>input element (type=inputType) with attrs: name(xs:string),
 * type(xs:string,default="text"), maxLength(xs:integer),
 * required(xs:boolean,default="false")</li>
 * </ul>
 */
public class XMLSchemaInlayHintExtensionsTest extends AbstractCacheBasedTest {

	private static final String XML_FILE_URI = "src/test/resources/test-form.xml";

	// ------------------- Default attribute value tests

	@Test
	public void defaultAttributeValues() throws BadLocationException {
		// form: > at (3, 18), defaults: lang="en", method="GET", debug="false"
		// input username: / at (4, 27), defaults: type="text", required="false"
		// input email: / at (5, 56), defaults: type="text"
		String xml = "<form\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"xsd/form.xsd\"\r\n" + //
				"    maxRetries=\"3\">\r\n" + //
				"    <input name=\"username\" />\r\n" + //
				"    <input name=\"email\" maxLength=\"255\" required=\"true\" />\r\n" + //
				"</form>";
		testInlayHintFor(xml, XML_FILE_URI, //
				ih(new Position(3, 18), "lang=\"en\""), //
				ih(new Position(3, 18), "method=\"GET\""), //
				ih(new Position(3, 18), "debug=\"false\""), //
				ih(new Position(4, 27), "type=\"text\""), //
				ih(new Position(4, 27), "required=\"false\""), //
				ih(new Position(5, 56), "type=\"text\""));
	}

	@Test
	public void noHintWhenAttributeExplicitlySet() throws BadLocationException {
		// lang is explicitly set, so only method and debug are shown as defaults
		String xml = "<form\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"xsd/form.xsd\"\r\n" + //
				"    lang=\"fr\">\r\n" + //
				"    <input name=\"email\" required=\"true\" />\r\n" + //
				"</form>";
		testInlayHintFor(xml, XML_FILE_URI, //
				ih(new Position(3, 13), "method=\"GET\""), //
				ih(new Position(3, 13), "debug=\"false\""), //
				ih(new Position(4, 40), "type=\"text\""));
	}

	@Test
	public void noHintWhenAllAttributesExplicitlySet() throws BadLocationException {
		String xml = "<form\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"xsd/form.xsd\"\r\n" + //
				"    lang=\"fr\" method=\"POST\" debug=\"true\">\r\n" + //
				"    <input name=\"email\" type=\"password\" maxLength=\"100\" required=\"true\" />\r\n" + //
				"</form>";
		testInlayHintFor(xml, XML_FILE_URI);
	}

	@Test
	public void selfClosedFormElement() throws BadLocationException {
		// Self-closed <form />: / at (2, 49)
		String xml = "<form\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"xsd/form.xsd\" />";
		testInlayHintFor(xml, XML_FILE_URI, //
				ih(new Position(2, 49), "lang=\"en\""), //
				ih(new Position(2, 49), "method=\"GET\""), //
				ih(new Position(2, 49), "debug=\"false\""));
	}

	@Test
	public void noGrammar() throws BadLocationException {
		String xml = "<form>\r\n" + //
				"    <input />\r\n" + //
				"</form>";
		testInlayHintFor(xml);
	}

	// ------------------- Attribute type hint tests

	@Test
	public void attributeTypeHints() throws BadLocationException {
		// maxRetries: name end at (3, 14), type xs:int
		// maxLength: name end at (5, 33), type xs:integer
		// required: name end at (5, 48), type xs:boolean
		// lang, method, name, type are xs:string → filtered
		String xml = "<form\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"xsd/form.xsd\"\r\n" + //
				"    maxRetries=\"3\">\r\n" + //
				"    <input name=\"username\" />\r\n" + //
				"    <input name=\"email\" maxLength=\"255\" required=\"true\" />\r\n" + //
				"</form>";
		XMLInlayHintSettings settings = new XMLInlayHintSettings();
		settings.setEnabled(true);
		settings.setShowAttributeType(true);
		settings.setShowDefaultValue(false);
		settings.setShowElementType(false);
		testInlayHintFor(xml, XML_FILE_URI, settings, //
				ihType(new Position(3, 14), ": xs:int"), //
				ihType(new Position(5, 33), ": xs:integer"), //
				ihType(new Position(5, 48), ": xs:boolean"));
	}

	@Test
	public void attributeTypeHintsFilterString() throws BadLocationException {
		// lang is xs:string → no type hint
		String xml = "<form\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"xsd/form.xsd\"\r\n" + //
				"    lang=\"en\">\r\n" + //
				"    <input name=\"email\" />\r\n" + //
				"</form>";
		XMLInlayHintSettings settings = new XMLInlayHintSettings();
		settings.setEnabled(true);
		settings.setShowAttributeType(true);
		settings.setShowDefaultValue(false);
		settings.setShowElementType(false);
		testInlayHintFor(xml, XML_FILE_URI, settings);
	}

	// ------------------- Element type hint tests

	@Test
	public void elementTypeHints() throws BadLocationException {
		// input has named type "inputType", form has anonymous type → no hint
		// input username: after tag name at (4, 10)
		// input email: after tag name at (5, 10)
		String xml = "<form\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"xsd/form.xsd\"\r\n" + //
				"    maxRetries=\"3\">\r\n" + //
				"    <input name=\"username\" />\r\n" + //
				"    <input name=\"email\" maxLength=\"255\" required=\"true\" />\r\n" + //
				"</form>";
		XMLInlayHintSettings settings = new XMLInlayHintSettings();
		settings.setEnabled(true);
		settings.setShowElementType(true);
		settings.setShowDefaultValue(false);
		settings.setShowAttributeType(false);
		testInlayHintFor(xml, XML_FILE_URI, settings, //
				ihType(new Position(4, 10), ": inputType"), //
				ihType(new Position(5, 10), ": inputType"));
	}

	@Test
	public void elementTypeHintNotShownForAnonymousType() throws BadLocationException {
		// form has anonymous complex type → no element type hint
		String xml = "<form\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"xsd/form.xsd\" />";
		XMLInlayHintSettings settings = new XMLInlayHintSettings();
		settings.setEnabled(true);
		settings.setShowElementType(true);
		settings.setShowDefaultValue(false);
		settings.setShowAttributeType(false);
		testInlayHintFor(xml, XML_FILE_URI, settings);
	}

	// ------------------- All settings combined

	@Test
	public void allHintsEnabled() throws BadLocationException {
		String xml = "<form\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"xsd/form.xsd\"\r\n" + //
				"    maxRetries=\"3\">\r\n" + //
				"    <input name=\"username\" />\r\n" + //
				"    <input name=\"email\" maxLength=\"255\" required=\"true\" />\r\n" + //
				"</form>";
		XMLInlayHintSettings settings = new XMLInlayHintSettings();
		settings.setEnabled(true);
		testInlayHintFor(xml, XML_FILE_URI, settings, //
				// form: attribute type for maxRetries
				ihType(new Position(3, 14), ": xs:int"), //
				// form: default values
				ih(new Position(3, 18), "lang=\"en\""), //
				ih(new Position(3, 18), "method=\"GET\""), //
				ih(new Position(3, 18), "debug=\"false\""), //
				// input username: default values + element type
				ih(new Position(4, 27), "type=\"text\""), //
				ih(new Position(4, 27), "required=\"false\""), //
				ihType(new Position(4, 10), ": inputType"), //
				// input email: attribute types
				ihType(new Position(5, 33), ": xs:integer"), //
				ihType(new Position(5, 48), ": xs:boolean"), //
				// input email: default value + element type
				ih(new Position(5, 56), "type=\"text\""), //
				ihType(new Position(5, 10), ": inputType"));
	}

}
