/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.lemminx.extensions.colors.participants;

import org.eclipse.lemminx.commons.*;
import org.eclipse.lemminx.dom.*;
import org.eclipse.lemminx.extensions.colors.*;
import org.eclipse.lemminx.extensions.colors.settings.*;
import org.eclipse.lemminx.services.extensions.*;
import org.eclipse.lemminx.utils.*;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.*;

import java.util.*;

import static org.eclipse.lemminx.extensions.colors.utils.ColorUtils.*;

/**
 * XML document color particpant based on the {@link XMLColorsSettings}.
 *
 * @author Angelo ZERR
 */
public class XMLDocumentColorParticipant implements IDocumentColorParticipant {

	private final XMLColorsPlugin xmlColorsPlugin;

	public XMLDocumentColorParticipant(XMLColorsPlugin xmlColorsPlugin) {
		this.xmlColorsPlugin = xmlColorsPlugin;
	}

	@Override
	public void doDocumentColor(DOMDocument xmlDocument, List<ColorInformation> colors, CancelChecker cancelChecker) {
		List<XMLColorExpression> expressions = findColorExpression(xmlDocument);
		if (expressions.isEmpty()) {
			return;
		}
		doDocumentColor(xmlDocument, expressions, colors, cancelChecker);
	}

	private void doDocumentColor(DOMNode node, List<XMLColorExpression> expressions, List<ColorInformation> colors, CancelChecker cancelChecker) {
		if (node.isElement()) {
			DOMElement element = (DOMElement) node;
			if (element.hasAttributes()) {
				List<DOMAttr> attributes = element.getAttributeNodes();
				for (DOMAttr attr : attributes) {
					if (isColorNode(attr, expressions)) {
						// The current attribute node matches an XML color expression declared in the
						// "xml/colors"
						// settings
						// ex :
						// - xpath="foo/@color"
						Color color = getColorValue(attr.getValue());
						if (color != null) {
							Range range = XMLPositionUtility.selectAttributeValue(attr, true);
							ColorInformation colorInformation = new ColorInformation(range, color);
							colors.add(colorInformation);
						}
					}
				}
			}
		} else if (node.isText()) {
			if (isColorNode(node, expressions)) {
				// The current text node matches an XML color expression declared in the
				// "xml/colors"
				// settings
				// ex :
				// - xpath="resources/color/text()"
				DOMText text = (DOMText) node;
				Color color = getColorValue(text.getData());
				if (color != null) {
					Range range = XMLPositionUtility.selectText(text);
					ColorInformation colorInformation = new ColorInformation(range, color);
					colors.add(colorInformation);
				}
			}
		}
		List<DOMNode> children = node.getChildren();
		for (DOMNode child : children) {
			cancelChecker.checkCanceled();
			doDocumentColor(child, expressions, colors, cancelChecker);
		}
	}

	@Override
	public void doColorPresentations(DOMDocument xmlDocument, ColorPresentationParams params, List<ColorPresentation> presentations, CancelChecker cancelChecker) {
		Color color = params.getColor();
		Range replace = params.getRange();
		Position replaceStart = replace.getStart();
		Position replaceEnd = replace.getEnd();
		try {
			int startOffset = xmlDocument.offsetAt(replaceStart);
			int endOffset = xmlDocument.offsetAt(replaceEnd);
			String rangeText = xmlDocument.getText().substring(startOffset, endOffset);
			presentations.add(toHex(color, replace, rangeText.startsWith("#")));
			if (rangeText.startsWith("rgb"))
				presentations.add(rangeText.startsWith("rgb") ? 0 : 1, toRGB(color, replace));
		} catch (BadLocationException ignored) {
		}
	}

	/**
	 * Returns true if the given <code>node>code> matches an XML color expression
	 * and false otherwise.
	 *
	 * @param node        the node to match.
	 * @param expressions XML color expressions.
	 * @return true if the given <code>node>code> matches an XML color expression
	 * and false otherwise.
	 */
	private static boolean isColorNode(DOMNode node, List<XMLColorExpression> expressions) {
		if (node.isAttribute()) {
			DOMAttr attr = (DOMAttr) node;
			if (attr.getValue() == null || attr.getValue().isEmpty()) {
				return false;
			}
		} else if (node.isText()) {
			DOMText text = (DOMText) node;
			if (!text.hasData()) {
				return false;
			}
		}
		for (XMLColorExpression expression : expressions) {
			if (expression.match(node)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the list of {@link XMLColorExpression} for the given document and an
	 * empty list otherwise.
	 *
	 * @param xmlDocument the DOM document
	 * @return the list of {@link XMLColorExpression} for the given document and an
	 * empty list otherwise.
	 */
	private List<XMLColorExpression> findColorExpression(DOMDocument xmlDocument) {
		XMLColorsSettings settings = xmlColorsPlugin.getColorsSettings();
		if (settings == null) {
			return Collections.emptyList();
		}

		List<XMLColors> colorsDef = settings.getColors();
		if (colorsDef == null) {
			return Collections.emptyList();
		}
		List<XMLColorExpression> expressions = new ArrayList<>();
		for (XMLColors xmlColors : colorsDef) {
			if (xmlColors.matches(xmlDocument.getDocumentURI())) {
				expressions.addAll(xmlColors.getExpressions());
			}
		}
		return expressions;
	}

}
