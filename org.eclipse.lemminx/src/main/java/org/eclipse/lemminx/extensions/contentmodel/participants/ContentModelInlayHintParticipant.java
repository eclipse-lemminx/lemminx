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
package org.eclipse.lemminx.extensions.contentmodel.participants;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.model.CMAttributeDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.services.extensions.inlayhint.IInlayHintParticipant;
import org.eclipse.lemminx.services.extensions.inlayhint.IInlayHintRequest;
import org.eclipse.lemminx.settings.XMLInlayHintSettings;
import org.eclipse.lemminx.uriresolver.CacheResourceDownloadingException;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Inlay hint participant that shows grammar-defined information for XML
 * elements and attributes:
 *
 * <ul>
 * <li><b>Default attribute values</b>: shows default/fixed attribute values
 * from the bound grammar (XSD, DTD) when an element does not explicitly
 * specify them.</li>
 * <li><b>Attribute types</b>: shows the type of each attribute (e.g.,
 * {@code : xs:integer}) after the attribute name.</li>
 * <li><b>Element types</b>: shows the type of each element (e.g.,
 * {@code : inputType}) after the element tag name.</li>
 * </ul>
 *
 * <p>
 * For example, given the following XSD:
 * </p>
 *
 * <pre>
 * &lt;xs:element name="person"&gt;
 *   &lt;xs:complexType&gt;
 *     &lt;xs:sequence&gt;
 *       &lt;xs:element name="age" type="xs:integer" /&gt;
 *     &lt;/xs:sequence&gt;
 *     &lt;xs:attribute name="id" type="xs:int" /&gt;
 *     &lt;xs:attribute name="lang" type="xs:string" default="en" /&gt;
 *   &lt;/xs:complexType&gt;
 * &lt;/xs:element&gt;
 * </pre>
 *
 * <p>
 * When the XML document contains:
 * </p>
 *
 * <pre>
 * &lt;person id="42"&gt;
 *   &lt;age&gt;30&lt;/age&gt;
 * &lt;/person&gt;
 * </pre>
 *
 * <p>
 * The following inlay hints are shown:
 * </p>
 * <ul>
 * <li>{@code id} attribute: {@code : xs:int} (attribute type)</li>
 * <li>{@code lang="en"} (default value)</li>
 * <li>{@code age} element: {@code : xs:integer} (element type)</li>
 * </ul>
 *
 * <p>
 * Types equal to {@code xs:string} are filtered out as they are too common
 * to be informative.
 * </p>
 */
public class ContentModelInlayHintParticipant implements IInlayHintParticipant {

	private static final Logger LOGGER = Logger.getLogger(ContentModelInlayHintParticipant.class.getName());

	private static final String XS_STRING = "string";

	private final ContentModelManager contentModelManager;

	public ContentModelInlayHintParticipant(ContentModelManager contentModelManager) {
		this.contentModelManager = contentModelManager;
	}

	@Override
	public void doInlayHint(IInlayHintRequest request, List<InlayHint> inlayHints, CancelChecker cancelChecker) {
		XMLInlayHintSettings settings = request.getInlayHintSettings();
		DOMDocument document = request.getDocument();
		Range range = request.getRange();
		int rangeStart = -1;
		int rangeEnd = -1;
		if (range != null) {
			try {
				rangeStart = document.offsetAt(range.getStart());
				rangeEnd = document.offsetAt(range.getEnd());
			} catch (BadLocationException e) {
				rangeStart = -1;
				rangeEnd = -1;
			}
		}
		findInlayHintsInNode(document, document, settings, rangeStart, rangeEnd, inlayHints, cancelChecker);
	}

	private void findInlayHintsInNode(DOMNode node, DOMDocument document, XMLInlayHintSettings settings,
			int rangeStart, int rangeEnd, List<InlayHint> inlayHints, CancelChecker cancelChecker) {
		cancelChecker.checkCanceled();
		if (rangeStart != -1) {
			if (node.getEnd() < rangeStart) {
				return;
			}
			if (node.getStart() > rangeEnd) {
				return;
			}
		}
		if (node.isElement()) {
			DOMElement element = (DOMElement) node;
			try {
				collectHints(element, document, settings, inlayHints);
			} catch (CacheResourceDownloadingException e) {
				// Grammar is being downloaded, skip
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error while computing inlay hints for element '"
						+ element.getTagName() + "'.", e);
			}
		}
		List<DOMNode> children = node.getChildren();
		if (children != null) {
			for (DOMNode child : children) {
				findInlayHintsInNode(child, document, settings, rangeStart, rangeEnd, inlayHints, cancelChecker);
			}
		}
	}

	private void collectHints(DOMElement element, DOMDocument document, XMLInlayHintSettings settings,
			List<InlayHint> inlayHints) {
		if (!element.hasStartTag()
				|| element.getOffsetBeforeCloseOfStartTag() == DOMNode.NULL_VALUE) {
			return;
		}
		Collection<CMDocument> cmDocuments = contentModelManager.findCMDocument(element);
		if (cmDocuments.isEmpty()) {
			return;
		}
		for (CMDocument cmDocument : cmDocuments) {
			CMElementDeclaration cmElement = cmDocument.findCMElement(element);
			if (cmElement != null) {
				if (settings.isShowAttributeType()) {
					collectAttributeTypeHints(element, cmElement, document, inlayHints);
				}
				if (settings.isShowDefaultValue()) {
					collectDefaultAttributeHints(element, cmElement, document, inlayHints);
				}
				if (settings.isShowElementType()) {
					collectElementTypeHint(element, cmElement, document, inlayHints);
				}
				return;
			}
		}
	}

	private void collectAttributeTypeHints(DOMElement element, CMElementDeclaration cmElement,
			DOMDocument document, List<InlayHint> inlayHints) {
		List<DOMAttr> attrs = element.getAttributeNodes();
		if (attrs == null) {
			return;
		}
		for (DOMAttr attr : attrs) {
			CMAttributeDeclaration cmAttr = cmElement.findCMAttribute(attr);
			if (cmAttr == null) {
				continue;
			}
			String typeName = cmAttr.getTypeName();
			if (typeName == null || isStringType(typeName)) {
				continue;
			}
			int offset = attr.getNodeAttrName().getEnd();
			Position position;
			try {
				position = document.positionAt(offset);
			} catch (BadLocationException e) {
				continue;
			}
			String label = ": " + typeName;
			InlayHint hint = new InlayHint(position, Either.forLeft(label));
			hint.setKind(InlayHintKind.Type);
			inlayHints.add(hint);
		}
	}

	private void collectElementTypeHint(DOMElement element, CMElementDeclaration cmElement,
			DOMDocument document, List<InlayHint> inlayHints) {
		String typeName = cmElement.getTypeName();
		if (typeName == null || isStringType(typeName)) {
			return;
		}
		int startTagOffset = element.getStartTagOpenOffset();
		if (startTagOffset == DOMNode.NULL_VALUE) {
			return;
		}
		int insertOffset = startTagOffset + 1 + element.getTagName().length();
		Position position;
		try {
			position = document.positionAt(insertOffset);
		} catch (BadLocationException e) {
			return;
		}
		String label = ": " + typeName;
		InlayHint hint = new InlayHint(position, Either.forLeft(label));
		hint.setKind(InlayHintKind.Type);
		hint.setPaddingLeft(true);
		inlayHints.add(hint);
	}

	private void collectDefaultAttributeHints(DOMElement element, CMElementDeclaration cmElement,
			DOMDocument document, List<InlayHint> inlayHints) {
		Collection<CMAttributeDeclaration> attributes = cmElement.getAttributes();
		if (attributes == null || attributes.isEmpty()) {
			return;
		}
		int insertOffset = element.getOffsetBeforeCloseOfStartTag();
		if (insertOffset == DOMNode.NULL_VALUE) {
			return;
		}
		for (CMAttributeDeclaration cmAttr : attributes) {
			String defaultValue = cmAttr.getDefaultValue();
			if (defaultValue == null) {
				continue;
			}
			String attrName = getAttributeName(cmAttr, element);
			if (element.hasAttribute(attrName)) {
				continue;
			}
			Position position;
			try {
				position = document.positionAt(insertOffset);
			} catch (BadLocationException e) {
				continue;
			}
			String label = attrName + "=\"" + defaultValue + "\"";
			InlayHint hint = new InlayHint(position, Either.forLeft(label));
			hint.setKind(InlayHintKind.Parameter);
			hint.setPaddingLeft(true);
			inlayHints.add(hint);
		}
	}

	private static String getAttributeName(CMAttributeDeclaration cmAttr, DOMElement element) {
		String namespace = cmAttr.getNamespace();
		if (namespace != null && !namespace.isEmpty()) {
			String prefix = element.getPrefix(namespace);
			return cmAttr.getName(prefix);
		}
		return cmAttr.getLocalName();
	}

	private static boolean isStringType(String typeName) {
		return XS_STRING.equals(typeName) || typeName.endsWith(":" + XS_STRING);
	}
}
