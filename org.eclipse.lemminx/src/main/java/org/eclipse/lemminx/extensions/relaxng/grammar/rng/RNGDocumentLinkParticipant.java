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
package org.eclipse.lemminx.extensions.relaxng.grammar.rng;

import static org.eclipse.lemminx.utils.XMLPositionUtility.createDocumentLink;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMRange;
import org.eclipse.lemminx.extensions.relaxng.utils.RelaxNGUtils;
import org.eclipse.lemminx.services.extensions.IDocumentLinkParticipant;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lsp4j.DocumentLink;

/**
 * 
 * Implements document links in .rng files for
 * <ul>
 * <li>include/@href</li>
 * <li>externalRef/@href</li>
 * </ul>
 * 
 */
public class RNGDocumentLinkParticipant implements IDocumentLinkParticipant {

	private static final Logger LOGGER = Logger.getLogger(RNGDocumentLinkParticipant.class.getName());

	@Override
	public void findDocumentLinks(DOMDocument document, List<DocumentLink> links) {
		if (!DOMUtils.isRelaxNGXMLSyntax(document)) {
			return;
		}
		findDocumentLinks(document, document, links);
	}

	public void findDocumentLinks(DOMNode parent, DOMDocument document, List<DocumentLink> links) {
		for (DOMNode child : parent.children()) {
			if (child.isElement()) {
				DOMElement rngElement = (DOMElement) child;
				if (RelaxNGUtils.isInclude(rngElement) || RelaxNGUtils.isExternalRef(rngElement)) {
					DOMAttr hrefAttr = RelaxNGUtils.getHref(rngElement);
					if (hrefAttr != null && !StringUtils.isEmpty(hrefAttr.getValue())) {
						String location = getResolvedLocation(document.getDocumentURI(), hrefAttr.getValue());
						DOMRange hrefRange = hrefAttr.getNodeAttrValue();
						try {
							links.add(createDocumentLink(hrefRange, location, true));
						} catch (BadLocationException e) {
							LOGGER.log(Level.SEVERE, "Creation of document link failed", e);
						}
					}
				}
				findDocumentLinks(rngElement, document, links);
			}
		}
	}

	private static String getResolvedLocation(String documentURI, String location) {
		if (location == null) {
			return null;
		}
		try {
			return XMLEntityManager.expandSystemId(location, documentURI, false);
		} catch (MalformedURIException e) {
			return location;
		}
	}

}
