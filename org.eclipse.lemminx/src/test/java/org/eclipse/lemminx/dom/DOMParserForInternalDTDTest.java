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
package org.eclipse.lemminx.dom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.lemminx.dom.DTDEntityDecl;
import org.junit.jupiter.api.Test;

/**
 * DOMParsre test with internal DTD.
 *
 */
public class DOMParserForInternalDTDTest {

	@Test
	public void onlyDocTypeNotClosed() {
		String xml = "<!DOCTYPE ";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(1, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(10, documentType.getEnd());
		assertNull(documentType.getName());
		assertFalse(documentType.isClosed());
		assertEquals(0, documentType.getChildren().size());
	}

	@Test
	public void docTypeWithNameNotClosed() {
		String xml = "<!DOCTYPE foo";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(1, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(13, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertFalse(documentType.isClosed());
		assertEquals(0, documentType.getChildren().size());
	}

	@Test
	public void docTypeWithStartInternalSubsetNotClosed() {
		String xml = "<!DOCTYPE foo [";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(1, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(15, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertFalse(documentType.isClosed());
		assertEquals(0, documentType.getChildren().size());
	}

	@Test
	public void docTypeWithStartInternalSubsetTextNotClosed() {
		String xml = "<!DOCTYPE foo [aaa";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(1, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(18, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertFalse(documentType.isClosed());

		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isText());
		DOMText text = (DOMText) documentType.getChild(0);
		assertEquals("aaa", text.getTextContent());
	}

	@Test
	public void docTypeNotClosedAndElement() {
		String xml = "<!DOCTYPE foo []<foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(16, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(actual.getChild(1).isClosed()); // here close comes from the '>' of <foo />
	}

	@Test
	public void docTypeClosedEmptySubsetContentAndElement() {
		String xml = "<!DOCTYPE foo []><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(17, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <foo/> element
		assertTrue(actual.getChild(1).isElement());

	}

	@Test
	public void docTypeClosedNoSubsetContentAndElement() {
		String xml = "<!DOCTYPE foo ><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(15, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <foo/> element
		assertTrue(actual.getChild(1).isElement());

	}

	@Test
	public void docTypeClosedWithBadContentAndElement() {
		String xml = "<!DOCTYPE foo [aaaa]><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(21, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <foo />element
		assertTrue(actual.getChild(1).isElement());

	}

	@Test
	public void elementDeclNotClosed() {
		String xml = "<!DOCTYPE foo [<!ELEMENT]><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(26, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ELEMENT
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDElementDecl());
		DTDElementDecl elementDecl = (DTDElementDecl) documentType.getChild(0);
		assertEquals(15, elementDecl.getStart());
		assertEquals(23, elementDecl.getEnd());
		assertFalse(elementDecl.isClosed());

		// <foo />element
		assertTrue(actual.getChild(1).isElement());
	}

	@Test
	public void elementDeclWithNameNotClosed() {
		String xml = "<!DOCTYPE foo [<!ELEMENT a]><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(28, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ELEMENT
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDElementDecl());
		DTDElementDecl elementDecl = (DTDElementDecl) documentType.getChild(0);
		assertEquals(15, elementDecl.getStart());
		assertEquals(25, elementDecl.getEnd());
		assertFalse(elementDecl.isClosed());

		// <foo />element
		assertTrue(actual.getChild(1).isElement());
	}

	@Test
	public void elementDeclClosed() {
		String xml = "<!DOCTYPE foo [<!ELEMENT >]><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(28, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ELEMENT
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDElementDecl());
		DTDElementDecl elementDecl = (DTDElementDecl) documentType.getChild(0);
		assertEquals(15, elementDecl.getStart());
		assertEquals(26, elementDecl.getEnd());
		assertTrue(elementDecl.isClosed());

		// <foo />element
		assertTrue(actual.getChild(1).isElement());
	}

	@Test
	public void elementDeclWithNameClosed() {
		String xml = "<!DOCTYPE foo [<!ELEMENT a >]><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(30, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ELEMENT
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDElementDecl());
		DTDElementDecl elementDecl = (DTDElementDecl) documentType.getChild(0);
		assertEquals(15, elementDecl.getStart());
		assertEquals(28, elementDecl.getEnd());
		assertTrue(elementDecl.isClosed());

		// <foo />element
		assertTrue(actual.getChild(1).isElement());
	}

	@Test
	public void elementDeclClosedWithBraces() {
		String xml = "<!DOCTYPE error-page [<!ELEMENT error-page ((error-code | exception-type), location)>]><error-page/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(87, documentType.getEnd());
		assertEquals("error-page", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ELEMENT
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDElementDecl());
		DTDElementDecl elementDecl = (DTDElementDecl) documentType.getChild(0);
		assertEquals(22, elementDecl.getStart());
		assertEquals(85, elementDecl.getEnd());
		assertTrue(elementDecl.isClosed());
		assertEquals("error-page", elementDecl.getName());
		assertEquals("((error-code | exception-type), location)", elementDecl.getContent());

		// <error-page />element
		assertTrue(actual.getChild(1).isElement());
	}
	
	@Test
	public void elementDeclNotClosedWithBraces() {
		String xml = "<!DOCTYPE error-page [<!ELEMENT error-page ((error-code | exception-type), location)]><error-page/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(86, documentType.getEnd());
		assertEquals("error-page", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ELEMENT
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDElementDecl());
		DTDElementDecl elementDecl = (DTDElementDecl) documentType.getChild(0);
		assertEquals(22, elementDecl.getStart());
		assertEquals(83, elementDecl.getEnd());
		assertEquals("error-page", elementDecl.getName());
		assertEquals("((error-code | exception-type), location)", elementDecl.getContent());
		assertFalse(elementDecl.isClosed());

		// <error-page />element
		assertTrue(actual.getChild(1).isElement());
	}
	
	@Test
	public void attListDeclNotClosed() {
		String xml = "<!DOCTYPE foo [<!ATTLIST]><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(26, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ATTLIST
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDAttListDecl());
		DTDAttlistDecl attListDecl = (DTDAttlistDecl) documentType.getChild(0);
		assertEquals(15, attListDecl.getStart());
		assertEquals(23, attListDecl.getEnd());
		assertFalse(attListDecl.isClosed());

		// <foo />element
		assertTrue(actual.getChild(1).isElement());
	}

	@Test
	public void attListDeclClosed() {
		String xml = "<!DOCTYPE foo [<!ATTLIST >]><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(28, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ATTLIST
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDAttListDecl());
		DTDAttlistDecl attListDecl = (DTDAttlistDecl) documentType.getChild(0);
		assertEquals(15, attListDecl.getStart());
		assertEquals(26, attListDecl.getEnd());
		assertTrue(attListDecl.isClosed());

		// <foo />element
		assertTrue(actual.getChild(1).isElement());
	}

	@Test
	public void attListDeclWithNameClosed() {
		String xml = "<!DOCTYPE foo [<!ATTLIST a >]><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(30, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ATTLIST
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDAttListDecl());
		DTDAttlistDecl attListDecl = (DTDAttlistDecl) documentType.getChild(0);
		assertEquals(15, attListDecl.getStart());
		assertEquals(28, attListDecl.getEnd());
		assertTrue(attListDecl.isClosed());

		// <foo />element
		assertTrue(actual.getChild(1).isElement());
	}

	@Test
	public void entityDeclNotClosed() {
		String xml = "<!DOCTYPE foo [<!ENTITY]><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(25, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ENTITY
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDEntityDecl());
		DTDEntityDecl entityDecl = (DTDEntityDecl) documentType.getChild(0);
		assertEquals(15, entityDecl.getStart());
		assertEquals(22, entityDecl.getEnd());
		assertFalse(entityDecl.isClosed());
		assertNull(entityDecl.getNodeName());

		// <foo />element
		assertTrue(actual.getChild(1).isElement());
	}

	@Test
	public void entityDeclWithNameNotClosed() {
		String xml = "<!DOCTYPE foo [<!ENTITY a]><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(27, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ENTITY
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDEntityDecl());
		DTDEntityDecl entityDecl = (DTDEntityDecl) documentType.getChild(0);
		assertEquals(15, entityDecl.getStart());
		assertEquals(24, entityDecl.getEnd());
		assertFalse(entityDecl.isClosed());
		assertEquals("a", entityDecl.getNodeName());

		// <foo />element
		assertTrue(actual.getChild(1).isElement());
	}

	@Test
	public void entityDeclClosed() {
		String xml = "<!DOCTYPE foo [<!ENTITY >]><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(27, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ENTITY
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDEntityDecl());
		DTDEntityDecl entityDecl = (DTDEntityDecl) documentType.getChild(0);
		assertEquals(15, entityDecl.getStart());
		assertEquals(25, entityDecl.getEnd());
		assertTrue(entityDecl.isClosed());
		assertNull(entityDecl.getNodeName());

		// <foo />element
		assertTrue(actual.getChild(1).isElement());
	}

	@Test
	public void entityDeclWithNameClosed() {
		String xml = "<!DOCTYPE foo [<!ENTITY a >]><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(29, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ENTITY
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDEntityDecl());
		DTDEntityDecl entityDecl = (DTDEntityDecl) documentType.getChild(0);
		assertEquals(15, entityDecl.getStart());
		assertEquals(27, entityDecl.getEnd());
		assertTrue(entityDecl.isClosed());
		assertEquals("a", entityDecl.getNodeName());

		// <foo />element
		assertTrue(actual.getChild(1).isElement());
	}

	@Test
	public void entityDeclWithNameClosedAndParameters() {
		String xml = "<!DOCTYPE foo [<!ENTITY % eName PUBLIC \"publicId\" \"systemId\" >]><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(64, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ENTITY
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDEntityDecl());
		DTDEntityDecl entityDecl = (DTDEntityDecl) documentType.getChild(0);
		assertEquals(15, entityDecl.getStart());
		assertEquals(62, entityDecl.getEnd());
		assertTrue(entityDecl.isClosed());
		assertEquals("%", entityDecl.getPercent());
		assertEquals("eName", entityDecl.getNodeName());
		assertEquals("PUBLIC", entityDecl.getKind());
		assertEquals("publicId", entityDecl.getPublicId());
		assertEquals("systemId", entityDecl.getSystemId());

		// <foo />element
		assertTrue(actual.getChild(1).isElement());
	}

	@Test
	public void entityDeclWithNameClosedAndSomeParameters() {
		String xml = "<!DOCTYPE foo [<!ENTITY % eName PUBLIC \"publicId\" >]><foo/>";

		DOMDocument actual = createDOMDocument(xml);
		assertEquals(2, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(53, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ENTITY
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDEntityDecl());
		DTDEntityDecl entityDecl = (DTDEntityDecl) documentType.getChild(0);
		assertEquals(15, entityDecl.getStart());
		assertEquals(51, entityDecl.getEnd());
		assertTrue(entityDecl.isClosed());
		assertEquals("%", entityDecl.getPercent());
		assertEquals("eName", entityDecl.getNodeName());
		assertEquals("PUBLIC", entityDecl.getKind());
		assertEquals("publicId", entityDecl.getPublicId());
		assertEquals(null, entityDecl.getSystemId());

		// <foo />element
		assertTrue(actual.getChild(1).isElement());
	}

	
	@Test
	public void attlistWithMultipleInternalDeclarations() {
		
		String dtd = 
		"<!DOCTYPE foo [<!ATTLIST Institution\n" +
		"    to CDATA #REQUIRED\n" +
		"    from CDATA #REQUIRED>]\n" +
		">";

		DOMDocument actual = createDOMDocument(dtd);
		assertEquals(1, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(88, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ATTLIST
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDAttListDecl());
		DTDAttlistDecl attlistDecl = (DTDAttlistDecl) documentType.getChild(0);
		
		assertEquals(15, attlistDecl.getStart());
		assertEquals(85, attlistDecl.getEnd());
		assertEquals("Institution", attlistDecl.getElementName());
		assertEquals("to", attlistDecl.getAttributeName());
		assertEquals("CDATA", attlistDecl.getAttributeType());
		assertEquals("#REQUIRED", attlistDecl.getAttributeValue());

		assertNotNull(attlistDecl.getInternalChildren());
		assertEquals(1, attlistDecl.getInternalChildren().size());

		DTDAttlistDecl internalDecl = (DTDAttlistDecl) attlistDecl.getInternalChildren().get(0);

		assertEquals("from", internalDecl.getAttributeName());
		assertEquals("CDATA", internalDecl.getAttributeType());
		assertEquals("#REQUIRED", internalDecl.getAttributeValue());

	}

	@Test
	public void attlistWithMultipleInternalDeclarationsMissingInformation() {
		
		String dtd = 
		"<!DOCTYPE foo [<!ATTLIST Institution\n" +
		"    to CDATA \n" +
		"    from CDATA #REQUIRED>]\n" +
		">";

		DOMDocument actual = createDOMDocument(dtd);
		assertEquals(1, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(79, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ATTLIST
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDAttListDecl());
		DTDAttlistDecl attlistDecl = (DTDAttlistDecl) documentType.getChild(0);
		
		assertEquals(15, attlistDecl.getStart());
		assertEquals(76, attlistDecl.getEnd());
		assertEquals("Institution", attlistDecl.getElementName());
		assertEquals("to", attlistDecl.getAttributeName());
		assertEquals("CDATA", attlistDecl.getAttributeType());
		assertEquals(null, attlistDecl.getAttributeValue());

		assertEquals(55, attlistDecl.unrecognized.start);
		assertEquals(75, attlistDecl.unrecognized.end);

	}

	@Test
	public void elementWithContent() {
		
		String dtd = 
		"<!DOCTYPE foo [\n" +
		"    <!ELEMENT elName (aa1,bb2,cc3) \n" +
		"    ]\n" +
		">";

		DOMDocument actual = createDOMDocument(dtd);
		assertEquals(1, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(59, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ELEMENT
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDElementDecl());
		DTDElementDecl elementDecl = (DTDElementDecl) documentType.getChild(0);
		
		assertEquals(20, elementDecl.getStart());
		assertEquals(55, elementDecl.getEnd());
		assertEquals("elName", elementDecl.getName());
		assertEquals("(aa1,bb2,cc3)", elementDecl.getContent());
		assertEquals(null, elementDecl.getCategory());
		assertFalse(elementDecl.isClosed());
	}

	@Test
	public void allDeclsUnclosed() {
		
		String dtd = 
		"<!DOCTYPE foo [ <!ELEMENT   <!ATTLIST elName <!ENTITY garbage   <!NOTATION garbage  ]>";

		DOMDocument actual = createDOMDocument(dtd);
		assertEquals(1, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(86, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ELEMENT
		assertEquals(4, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDElementDecl());
		assertTrue(documentType.getChild(1).isDTDAttListDecl());
		assertTrue(documentType.getChild(2).isDTDEntityDecl());
		assertTrue(documentType.getChild(3).isDTDNotationDecl());

		DTDElementDecl elementDecl = (DTDElementDecl) documentType.getChild(0);
		
		assertEquals(16, elementDecl.getStart());
		assertEquals(28, elementDecl.getEnd());
		assertEquals(null, elementDecl.getName());
		assertEquals(null, elementDecl.getContent());
		assertEquals(null, elementDecl.getCategory());
		assertFalse(elementDecl.isClosed());

		DTDAttlistDecl attlistDecl = (DTDAttlistDecl) documentType.getChild(1);
		
		assertEquals(28, attlistDecl.getStart());
		assertEquals(45, attlistDecl.getEnd());
		assertEquals("elName", attlistDecl.getElementName());
		assertFalse(attlistDecl.isClosed());

		DTDEntityDecl entityDecl = (DTDEntityDecl) documentType.getChild(2);
		
		assertEquals(45, entityDecl.getStart());
		assertEquals(64, entityDecl.getEnd());
		assertEquals("garbage", entityDecl.getNodeName());
		assertFalse(entityDecl.isClosed());

		DTDNotationDecl notationDecl = (DTDNotationDecl) documentType.getChild(3);
		
		assertEquals(64, notationDecl.getStart());
		assertEquals(83, notationDecl.getEnd());
		assertEquals("garbage", notationDecl.getName());
		assertFalse(notationDecl.isClosed());
	}

	@Test
	public void dtdUnrecognizedContent() {
		
		String dtd = 
		"<!DOCTYPE foo [\n" +
		"    <!ELEMENT elName (aa1,bb2,cc3) BAD UNRECOGNIZED CONTENT> \n" +
		"    ]\n" +
		">";

		DOMDocument actual = createDOMDocument(dtd);
		assertEquals(1, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(85, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!ELEMENT
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDElementDecl());
		DTDElementDecl elementDecl = (DTDElementDecl) documentType.getChild(0);
		
		assertEquals(20, elementDecl.getStart());
		assertEquals(76, elementDecl.getEnd());
		assertEquals("elName", elementDecl.getName());
		assertEquals("(aa1,bb2,cc3)", elementDecl.getContent());
		assertEquals(null, elementDecl.getCategory());
		assertEquals(51, elementDecl.unrecognized.start);
		assertEquals(75, elementDecl.unrecognized.end);
		assertTrue(elementDecl.isClosed());
	}

	@Test
	public void dtdNotationComplete() {
		
		String dtd = 
		"<!DOCTYPE foo [\n" +
		"    <!NOTATION Name PUBLIC \"PublicID\" \"SystemID\"> \n" +
		"    ]\n" +
		">";

		DOMDocument actual = createDOMDocument(dtd);
		assertEquals(1, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(74, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!NOTATION
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDNotationDecl());
		DTDNotationDecl elementDecl = (DTDNotationDecl) documentType.getChild(0);
		
		assertEquals(20, elementDecl.getStart());
		assertEquals(65, elementDecl.getEnd());
		assertEquals("Name", elementDecl.getName());
		assertEquals("PUBLIC", elementDecl.getKind());
		assertEquals("PublicID", elementDecl.getPublicId());
		assertEquals("SystemID", elementDecl.getSystemId());
		
		assertTrue(elementDecl.isClosed());
	}

	@Test
	public void dtdNotationSYSTEMUnrecognizedParameter() {
		
		String dtd = 
		"<!DOCTYPE foo [\n" +
		"    <!NOTATION Name SYSTEM \"PublicID\" \"SystemID\"> \n" +
		"    ]\n" +
		">";

		DOMDocument actual = createDOMDocument(dtd);
		assertEquals(1, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(74, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertTrue(documentType.isClosed());

		// <!NOTATION
		assertEquals(1, documentType.getChildren().size());
		assertTrue(documentType.getChild(0).isDTDNotationDecl());
		DTDNotationDecl elementDecl = (DTDNotationDecl) documentType.getChild(0);
		
		assertEquals(20, elementDecl.getStart());
		assertEquals(65, elementDecl.getEnd());
		assertEquals("Name", elementDecl.getName());
		assertEquals("SYSTEM", elementDecl.getKind());
		assertEquals("PublicID", elementDecl.getSystemId());
		assertEquals("\"SystemID\"", elementDecl.getUnrecognized());
		
		assertTrue(elementDecl.isClosed());
	}

	@Test
	public void testDoctypeUnrecognizedContent() {
		//Ensure unrecognized content goes all the way till the end
		String dtd = 
		"<!DOCTYPE foo BAD_VALUE [\n" +
		"    <!NOTATION Name SYSTEM \"PublicID\" \"SystemID\"> \n" +
		"    ]\n" +
		">";

		DOMDocument actual = createDOMDocument(dtd);
		assertEquals(1, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(0, documentType.getStart());
		assertEquals(84, documentType.getEnd());
		assertEquals("foo", documentType.getName());
		assertEquals(14, documentType.unrecognized.start);
		assertEquals(82, documentType.unrecognized.end);
		assertTrue(documentType.isClosed());	
	}

	@Test
	public void testAttlistUncompletedInternalDecl() {
		//Ensure unrecognized content goes all the way till the end
		String dtd = 
		"<!DOCTYPE foo [\n" +
		"    <!ATTLIST payment type CDATA \"check\" BAD \n" +
		"    <!NOTATION Name SYSTEM \"PublicID\" \"SystemID\"> \n" +
		"    ]\n" +
		">";

		DOMDocument actual = createDOMDocument(dtd);
		assertEquals(1, actual.getChildren().size());
		assertTrue(actual.getChild(0).isDoctype());
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(2, documentType.getChildren().size());
		assertTrue(documentType.getChild(0) instanceof DTDAttlistDecl);
		assertTrue(documentType.getChild(1) instanceof DTDNotationDecl);
	}


	@Test
	public void testAttlistWithEnumerationType() {
		String dtd =
		"<!DOCTYPE root [\n" +
		"  <!ATTLIST root status (one|two|three) \"one\">\n" +
		"]>";

		DOMDocument actual = createDOMDocument(dtd);
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		assertEquals(1, documentType.getChildren().size());
		DTDAttlistDecl attlistDecl = (DTDAttlistDecl) documentType.getChild(0);

		assertEquals("root", attlistDecl.getElementName());
		assertEquals("status", attlistDecl.getAttributeName());
		assertEquals("(one|two|three)", attlistDecl.getAttributeType());
		assertEquals("\"one\"", attlistDecl.getAttributeValue());
		assertNull(attlistDecl.getInternalChildren());
	}

	@Test
	public void testAttlistEnumerationTypeWithPrologAndElement() {
		String dtd =
		"<?xml version = \"1.0\"?>\n" +
		"<!DOCTYPE root [\n" +
		"\t<!ELEMENT root (#PCDATA)>\n" +
		"\t<!ATTLIST root status (one|two|three) \"four\">\n" +
		"]>\n" +
		"<root />";

		DOMDocument actual = createDOMDocument(dtd);
		DOMDocumentType documentType = actual.getDoctype();
		assertNotNull(documentType);
		assertEquals(2, documentType.getChildren().size());

		assertTrue(documentType.getChild(1) instanceof DTDAttlistDecl);
		DTDAttlistDecl attlistDecl = (DTDAttlistDecl) documentType.getChild(1);
		assertEquals("root", attlistDecl.getElementName());
		assertEquals("status", attlistDecl.getAttributeName());
		assertEquals("(one|two|three)", attlistDecl.getAttributeType());
		assertEquals("\"four\"", attlistDecl.getAttributeValue());

		assertNotNull(attlistDecl.attributeType);
		CharSequence text = actual.getTextSequence();
		assertEquals('(', text.charAt(attlistDecl.attributeType.getStart()));
		assertEquals(')', text.charAt(attlistDecl.attributeType.getEnd() - 1));
	}

	@Test
	public void testAttlistWithSingleValueEnumType() {
		String dtd =
		"<!DOCTYPE root [\n" +
		"  <!ATTLIST root flag (yes) #REQUIRED>\n" +
		"]>";

		DOMDocument actual = createDOMDocument(dtd);
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		DTDAttlistDecl attlistDecl = (DTDAttlistDecl) documentType.getChild(0);

		assertEquals("root", attlistDecl.getElementName());
		assertEquals("flag", attlistDecl.getAttributeName());
		assertEquals("(yes)", attlistDecl.getAttributeType());
		assertEquals("#REQUIRED", attlistDecl.getAttributeValue());
	}

	@Test
	public void testAttlistWithIDType() {
		String dtd =
		"<!DOCTYPE root [\n" +
		"  <!ATTLIST root id ID #REQUIRED>\n" +
		"]>";

		DOMDocument actual = createDOMDocument(dtd);
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		DTDAttlistDecl attlistDecl = (DTDAttlistDecl) documentType.getChild(0);

		assertEquals("root", attlistDecl.getElementName());
		assertEquals("id", attlistDecl.getAttributeName());
		assertEquals("ID", attlistDecl.getAttributeType());
		assertEquals("#REQUIRED", attlistDecl.getAttributeValue());
	}

	@Test
	public void testAttlistWithNMTOKENSType() {
		String dtd =
		"<!DOCTYPE root [\n" +
		"  <!ATTLIST root vals NMTOKENS #IMPLIED>\n" +
		"]>";

		DOMDocument actual = createDOMDocument(dtd);
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		DTDAttlistDecl attlistDecl = (DTDAttlistDecl) documentType.getChild(0);

		assertEquals("root", attlistDecl.getElementName());
		assertEquals("vals", attlistDecl.getAttributeName());
		assertEquals("NMTOKENS", attlistDecl.getAttributeType());
		assertEquals("#IMPLIED", attlistDecl.getAttributeValue());
	}

	@Test
	public void testAttlistWithFixedValue() {
		String dtd =
		"<!DOCTYPE root [\n" +
		"  <!ATTLIST root ver CDATA #FIXED \"1.0\">\n" +
		"]>";

		DOMDocument actual = createDOMDocument(dtd);
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		DTDAttlistDecl attlistDecl = (DTDAttlistDecl) documentType.getChild(0);

		assertEquals("root", attlistDecl.getElementName());
		assertEquals("ver", attlistDecl.getAttributeName());
		assertEquals("CDATA", attlistDecl.getAttributeType());
		assertEquals("#FIXED \"1.0\"", attlistDecl.getAttributeValue());
	}

	@Test
	public void testAttlistEnumWithMultipleAttributesOnSameDecl() {
		String dtd =
		"<!DOCTYPE root [\n" +
		"  <!ATTLIST root\n" +
		"    status (active|inactive) #REQUIRED\n" +
		"    flag (yes|no) \"yes\">\n" +
		"]>";

		DOMDocument actual = createDOMDocument(dtd);
		DOMDocumentType documentType = (DOMDocumentType) actual.getChild(0);
		DTDAttlistDecl attlistDecl = (DTDAttlistDecl) documentType.getChild(0);

		assertEquals("root", attlistDecl.getElementName());
		assertEquals("status", attlistDecl.getAttributeName());
		assertEquals("(active|inactive)", attlistDecl.getAttributeType());
		assertEquals("#REQUIRED", attlistDecl.getAttributeValue());

		assertNotNull(attlistDecl.getInternalChildren());
		assertEquals(1, attlistDecl.getInternalChildren().size());

		DTDAttlistDecl internalDecl = attlistDecl.getInternalChildren().get(0);
		assertEquals("flag", internalDecl.getAttributeName());
		assertEquals("(yes|no)", internalDecl.getAttributeType());
		assertEquals("\"yes\"", internalDecl.getAttributeValue());
	}

	@Test
	public void testEntityWithNDATA() {
		String xml = "<?xml version = \"1.0\"?>\n" +
				"<!DOCTYPE root [\n" +
				"\t<!ELEMENT root EMPTY>\n" +
				"\t<!ENTITY logo SYSTEM \"logo.gif\" NDATA gif>\n" +
				"]>\n" +
				"<root />";
		DOMDocument doc = createDOMDocument(xml);
		DOMDocumentType docType = doc.getDoctype();
		assertNotNull(docType);

		// Entity should be a child of the DOCTYPE
		boolean foundEntity = false;
		for (DOMNode child : docType.getChildren()) {
			if (child instanceof DTDEntityDecl) {
				DTDEntityDecl entity = (DTDEntityDecl) child;
				if ("logo".equals(entity.getName())) {
					foundEntity = true;
					assertEquals("SYSTEM", entity.getKind());
					assertEquals("logo.gif", entity.getSystemId());
					assertNotNull(entity.unrecognized);
				}
			}
		}
		assertTrue(foundEntity, "Entity 'logo' should be found in DOCTYPE children");
	}

	@Test
	public void testMixedContentElement() {
		// Verify that content is properly set for mixed content models
		String xml = "<?xml version = \"1.0\"?>\n" +
				"<!DOCTYPE root [\n" +
				"\t<!ELEMENT root (#PCDATA|item|item)*>\n" +
				"]>\n" +
				"<root />";
		DOMDocument doc = createDOMDocument(xml);
		DOMDocumentType docType = doc.getDoctype();
		assertNotNull(docType);

		boolean found = false;
		for (DOMNode child : docType.getChildren()) {
			if (child instanceof DTDElementDecl) {
				DTDElementDecl elementDecl = (DTDElementDecl) child;
				if ("root".equals(elementDecl.getName())) {
					found = true;
					assertNotNull(elementDecl.content, "content should not be null for mixed content");
					assertEquals("(#PCDATA|item|item)*", elementDecl.getContent());
				}
			}
		}
		assertTrue(found, "DTDElementDecl 'root' should be found");
	}

	private static DOMDocument createDOMDocument(String xml) {
		return DOMParser.getInstance().parse(xml, "uri", null);
	}
}
