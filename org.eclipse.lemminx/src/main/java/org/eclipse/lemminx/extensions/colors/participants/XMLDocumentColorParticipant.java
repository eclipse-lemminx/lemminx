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

import static org.eclipse.lemminx.extensions.colors.utils.ColorUtils.getColorValue;
import static org.eclipse.lemminx.extensions.colors.utils.ColorUtils.toHexa;
import static org.eclipse.lemminx.extensions.colors.utils.ColorUtils.toRGB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMText;
import org.eclipse.lemminx.extensions.colors.XMLColorsPlugin;
import org.eclipse.lemminx.extensions.colors.settings.XMLColorExpression;
import org.eclipse.lemminx.extensions.colors.settings.XMLColors;
import org.eclipse.lemminx.extensions.colors.settings.XMLColorsSettings;
import org.eclipse.lemminx.extensions.colors.utils.ColorUtils;
import org.eclipse.lemminx.services.extensions.IDocumentColorParticipant;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.Color;
import org.eclipse.lsp4j.ColorInformation;
import org.eclipse.lsp4j.ColorPresentation;
import org.eclipse.lsp4j.ColorPresentationParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * XML document color particpant based on the {@link XMLColorsSettings}.
 *
 * @author Angelo ZERR
 *
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

	private void doDocumentColor(DOMNode node, List<XMLColorExpression> expressions, List<ColorInformation> colors,
			CancelChecker cancelChecker) {
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
	public void doColorPresentations(DOMDocument xmlDocument, ColorPresentationParams params,
			List<ColorPresentation> presentations, CancelChecker cancelChecker) {
		Color color = params.getColor();
		Range replace = params.getRange();
		try {
			DOMAttr attr = xmlDocument.findAttrAt(xmlDocument.offsetAt(params.getRange().getStart()));
			String value = attr.getValue();
			if (value.startsWith("rgb")) {
				presentations.add(toRGB(color, replace));
			} else if (value.startsWith("#") || ColorUtils.isHexNumber(value)) {
				presentations.add(toHexa(color, replace, value.codePointAt(0) == "#".codePointAt(0)));
			} else {
				presentations.add(toRGB(color, replace));
				presentations.add(toHexa(color, replace, true));
			}
			return;
		} catch (BadLocationException e) {
			// we can't identify the existing presentation, fall through to add both
		}
		// RGB color presentation
		presentations.add(toRGB(color, replace));
		// Hexa color presentation
		presentations.add(toHexa(color, replace, true));
	}

	/**
	 * Returns true if the given <code>node>code> matches an XML color expression
	 * and false otherwise.
	 *
	 * @param node        the node to match.
	 * @param expressions XML color expressions.
	 *
	 * @return true if the given <code>node>code> matches an XML color expression
	 *         and false otherwise.
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
	 *
	 * @return the list of {@link XMLColorExpression} for the given document and an
	 *         empty list otherwise.
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
