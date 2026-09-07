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
package org.eclipse.lemminx.extensions.contentmodel.participants.diagnostics;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Multiple SAX {@link ContentHandler}.
 *
 * @author Angelo ZERR
 *
 */
public class MultipleContentHandler implements ContentHandler {

	private ContentHandler[] handlers = new ContentHandler[0];

	public MultipleContentHandler() {
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		ContentHandler[] h = handlers;
		for (int i = 0; i < h.length; i++) {
			h[i].setDocumentLocator(locator);
		}
	}

	@Override
	public void startDocument() throws SAXException {
		ContentHandler[] h = handlers;
		for (int i = 0; i < h.length; i++) {
			h[i].startDocument();
		}
	}

	@Override
	public void endDocument() throws SAXException {
		ContentHandler[] h = handlers;
		for (int i = 0; i < h.length; i++) {
			h[i].endDocument();
		}
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		ContentHandler[] h = handlers;
		for (int i = 0; i < h.length; i++) {
			h[i].startPrefixMapping(prefix, uri);
		}
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		ContentHandler[] h = handlers;
		for (int i = 0; i < h.length; i++) {
			h[i].endPrefixMapping(prefix);
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		ContentHandler[] h = handlers;
		for (int i = 0; i < h.length; i++) {
			h[i].startElement(uri, localName, qName, atts);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		ContentHandler[] h = handlers;
		for (int i = 0; i < h.length; i++) {
			h[i].endElement(uri, localName, qName);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		ContentHandler[] h = handlers;
		for (int i = 0; i < h.length; i++) {
			h[i].characters(ch, start, length);
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		ContentHandler[] h = handlers;
		for (int i = 0; i < h.length; i++) {
			h[i].ignorableWhitespace(ch, start, length);
		}
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		ContentHandler[] h = handlers;
		for (int i = 0; i < h.length; i++) {
			h[i].processingInstruction(target, data);
		}
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		ContentHandler[] h = handlers;
		for (int i = 0; i < h.length; i++) {
			h[i].skippedEntity(name);
		}
	}

	public void addContentHandler(ContentHandler contentHandler) {
		ContentHandler[] old = handlers;
		for (int i = 0; i < old.length; i++) {
			if (old[i] == contentHandler) {
				return;
			}
		}
		ContentHandler[] newHandlers = new ContentHandler[old.length + 1];
		System.arraycopy(old, 0, newHandlers, 0, old.length);
		newHandlers[old.length] = contentHandler;
		handlers = newHandlers;
	}

}
