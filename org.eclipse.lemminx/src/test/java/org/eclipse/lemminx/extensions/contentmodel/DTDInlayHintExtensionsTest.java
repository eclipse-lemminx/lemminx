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
import static org.eclipse.lemminx.XMLAssert.testInlayHintFor;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.XMLInlayHintSettings;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

/**
 * Inlay hint tests for default attribute values from DTD.
 *
 * Uses inlayHintNote.dtd which declares:
 * <ul>
 * <li>note element (#PCDATA) with attrs: status(CDATA,default="draft"),
 * priority(CDATA,default="normal")</li>
 * </ul>
 */
public class DTDInlayHintExtensionsTest extends AbstractCacheBasedTest {

	private static final String XML_FILE_URI = "src/test/resources/test-note.xml";

	@Test
	public void defaultAttributeValues() throws BadLocationException {
		// note: > at (1, 5), defaults: status="draft", priority="normal"
		String xml = "<!DOCTYPE note SYSTEM \"dtd/inlayHintNote.dtd\">\r\n" + //
				"<note>Remember to review the inlay hint PR.</note>";
		testInlayHintFor(xml, XML_FILE_URI, //
				ih(new Position(1, 5), "status=\"draft\""), //
				ih(new Position(1, 5), "priority=\"normal\""));
	}

	@Test
	public void noHintWhenAttributeExplicitlySet() throws BadLocationException {
		// status is explicitly set, only priority is shown as default
		String xml = "<!DOCTYPE note SYSTEM \"dtd/inlayHintNote.dtd\">\r\n" + //
				"<note status=\"published\">Some content.</note>";
		testInlayHintFor(xml, XML_FILE_URI, //
				ih(new Position(1, 24), "priority=\"normal\""));
	}

	@Test
	public void noHintWhenAllAttributesExplicitlySet() throws BadLocationException {
		String xml = "<!DOCTYPE note SYSTEM \"dtd/inlayHintNote.dtd\">\r\n" + //
				"<note status=\"published\" priority=\"high\">Some content.</note>";
		testInlayHintFor(xml, XML_FILE_URI);
	}

	@Test
	public void selfClosedNote() throws BadLocationException {
		// Self-closed <note />: / at (1, 6)
		String xml = "<!DOCTYPE note SYSTEM \"dtd/inlayHintNote.dtd\">\r\n" + //
				"<note />";
		testInlayHintFor(xml, XML_FILE_URI, //
				ih(new Position(1, 6), "status=\"draft\""), //
				ih(new Position(1, 6), "priority=\"normal\""));
	}

	@Test
	public void internalDTDWithDefaultAttributes() throws BadLocationException {
		// Internal DTD with default attributes, self-closed <note />: / at (4, 6)
		String xml = "<!DOCTYPE note [\r\n" + //
				"    <!ELEMENT note (#PCDATA)>\r\n" + //
				"    <!ATTLIST note lang CDATA \"en\">\r\n" + //
				"]>\r\n" + //
				"<note />";
		testInlayHintFor(xml, (String) null, //
				ih(new Position(4, 6), "lang=\"en\""));
	}

	@Test
	public void emptyDefaultValue() throws BadLocationException {
		// Empty default value should still produce a hint
		String xml = "<!DOCTYPE note [\r\n" + //
				"    <!ELEMENT note (#PCDATA)>\r\n" + //
				"    <!ATTLIST note title CDATA \"\">\r\n" + //
				"]>\r\n" + //
				"<note />";
		testInlayHintFor(xml, (String) null, //
				ih(new Position(4, 6), "title=\"\""));
	}

	@Test
	public void noHintForRequiredAttribute() throws BadLocationException {
		String xml = "<!DOCTYPE note [\r\n" + //
				"    <!ELEMENT note (#PCDATA)>\r\n" + //
				"    <!ATTLIST note title CDATA #REQUIRED>\r\n" + //
				"]>\r\n" + //
				"<note />";
		testInlayHintFor(xml, (String) null);
	}

	@Test
	public void noHintForImpliedAttribute() throws BadLocationException {
		String xml = "<!DOCTYPE note [\r\n" + //
				"    <!ELEMENT note (#PCDATA)>\r\n" + //
				"    <!ATTLIST note title CDATA #IMPLIED>\r\n" + //
				"]>\r\n" + //
				"<note />";
		testInlayHintFor(xml, (String) null);
	}

	@Test
	public void noHintWithoutAttlist() throws BadLocationException {
		String xml = "<!DOCTYPE note [\r\n" + //
				"    <!ELEMENT note (#PCDATA)>\r\n" + //
				"]>\r\n" + //
				"<note />";
		testInlayHintFor(xml, (String) null);
	}

	@Test
	public void noHintWhenDisabled() throws BadLocationException {
		String xml = "<!DOCTYPE note SYSTEM \"dtd/inlayHintNote.dtd\">\r\n" + //
				"<note>Some content.</note>";
		XMLInlayHintSettings settings = new XMLInlayHintSettings();
		settings.setEnabled(true);
		settings.setShowDefaultValue(false);
		settings.setShowAttributeType(false);
		settings.setShowElementType(false);
		testInlayHintFor(xml, XML_FILE_URI, settings);
	}

	@Test
	public void noTypeHintsForDTD() throws BadLocationException {
		// DTD has no named types, so no type hints expected even when enabled
		String xml = "<!DOCTYPE note SYSTEM \"dtd/inlayHintNote.dtd\">\r\n" + //
				"<note status=\"published\">Some content.</note>";
		XMLInlayHintSettings settings = new XMLInlayHintSettings();
		settings.setEnabled(true);
		settings.setShowAttributeType(true);
		settings.setShowElementType(true);
		settings.setShowDefaultValue(false);
		testInlayHintFor(xml, XML_FILE_URI, settings);
	}
}
