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
package org.eclipse.lemminx.services.extensions.codeaction;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.generator.XMLElementGenerator;
import org.eclipse.lemminx.extensions.contentmodel.model.CMAttributeDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Base code action to insert missing attributes on an element.
 *
 * <p>
 * Handles the common logic: finding the insert position after the last
 * existing attribute (or after the tag name), and generating SnippetTextEdit
 * via {@link XMLElementGenerator#generate(Collection, String)} which handles
 * formatting (split attributes, namespace prefixes, snippet choice/placeholder
 * syntax).
 * </p>
 *
 * <p>
 * Subclasses override
 * {@link #getAttributesToInsert(DOMElement, ICodeActionRequest)} to provide
 * context-specific attributes from the content model.
 * </p>
 *
 * <p>
 * For the simple single-attribute case, use
 * {@link #InsertAttributeCodeAction(String)} — the default implementation of
 * {@link #getAttributesToInsert} looks up the CM attribute declaration
 * automatically.
 * </p>
 */
public class InsertAttributeCodeAction implements ICodeActionParticipant {

	private final String attributeName;

	/**
	 * Creates a code action that inserts a single attribute with the given name.
	 * The attribute declaration is looked up from the content model.
	 *
	 * @param attributeName the attribute name to insert.
	 */
	public InsertAttributeCodeAction(String attributeName) {
		this.attributeName = attributeName;
	}

	/**
	 * Creates a code action for subclasses that override
	 * {@link #getAttributesToInsert(DOMElement, ICodeActionRequest)} to provide
	 * their own attributes.
	 */
	protected InsertAttributeCodeAction() {
		this.attributeName = null;
	}

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		if (codeActionAlreadyExists(codeActions, diagnostic)) {
			return;
		}
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		try {
			int offset = document.offsetAt(diagnosticRange.getStart());
			DOMNode node = document.findNodeAt(offset);
			if (node == null || !node.isElement()) {
				return;
			}
			DOMElement element = (DOMElement) node;

			Collection<CMAttributeDeclaration> attributes = getAttributesToInsert(element, request);
			if (attributes.isEmpty()) {
				return;
			}

			Position insertPosition = getInsertPosition(element, document);
			boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings()
					.isSnippetEditSupported();

			XMLElementGenerator generator = request.getXMLGenerator();
			String insertText = generator.generate(attributes, element.getTagName(), false, element);

			CodeAction insertAction = CodeActionFactory.insert(
					getCodeActionTitle(attributes), insertPosition, insertText,
					document.getTextDocument(), diagnostic, snippetSupport);
			codeActions.add(insertAction);
		} catch (BadLocationException e) {
			// Do nothing
		}
	}

	/**
	 * Returns the attributes to insert on the element.
	 *
	 * <p>
	 * When {@link #attributeName} is set, looks up that single CM attribute.
	 * Otherwise, returns all missing required attributes from the content model.
	 * </p>
	 *
	 * @param element the element where attributes will be inserted.
	 * @param request the code action request.
	 * @return the attributes to insert, or empty if none.
	 */
	protected Collection<CMAttributeDeclaration> getAttributesToInsert(DOMElement element,
			ICodeActionRequest request) {
		if (attributeName != null) {
			if (element.hasAttribute(attributeName)) {
				return Collections.emptyList();
			}
			CMAttributeDeclaration cmAttribute = findCMAttribute(element, attributeName, request);
			if (cmAttribute != null) {
				return Collections.singletonList(cmAttribute);
			}
			return Collections.emptyList();
		}
		return findRequiredAttributes(element, request);
	}

	/**
	 * Returns the title for the code action.
	 *
	 * @param attributes the attributes that will be inserted.
	 * @return the code action title.
	 */
	protected String getCodeActionTitle(Collection<CMAttributeDeclaration> attributes) {
		if (attributes.size() == 1) {
			return "Insert '" + attributes.iterator().next().getLocalName() + "' attribute";
		}
		return "Insert required attributes";
	}

	/**
	 * Returns the position where new attributes should be inserted: after the
	 * last existing attribute's closing quote, or after the tag name if no
	 * attributes exist.
	 */
	protected static Position getInsertPosition(DOMElement element, DOMDocument document)
			throws BadLocationException {
		if (element.hasAttributes()) {
			DOMAttr lastAttr = null;
			for (DOMAttr attr : element.attributes()) {
				lastAttr = attr;
			}
			if (lastAttr != null) {
				return document.positionAt(lastAttr.getEnd());
			}
		}
		String tagName = element.getTagName();
		if (tagName != null) {
			return document.positionAt(element.getStart() + 1 + tagName.length());
		}
		return document.positionAt(element.getStart());
	}

	/**
	 * Finds a CM attribute declaration for the given attribute name on the given
	 * element, using the content model.
	 *
	 * @param element       the element to look up.
	 * @param attributeName the attribute name to find.
	 * @param request       the code action request.
	 * @return the attribute declaration, or {@code null} if not found.
	 */
	protected static CMAttributeDeclaration findCMAttribute(DOMElement element, String attributeName,
			ICodeActionRequest request) {
		ContentModelManager contentModelManager = request.getComponent(ContentModelManager.class);
		for (CMDocument cmDocument : contentModelManager.findCMDocument(element)) {
			CMElementDeclaration cmElement = cmDocument.findCMElement(element);
			if (cmElement != null) {
				CMAttributeDeclaration cmAttribute = cmElement.findCMAttribute(attributeName, null);
				if (cmAttribute != null) {
					return cmAttribute;
				}
			}
		}
		return null;
	}

	/**
	 * Finds the missing required attributes from the content model for the given
	 * element. Shared by XSD, DTD, and RNG code actions.
	 *
	 * @param element the element to find missing required attributes for.
	 * @param request the code action request.
	 * @return the missing required attributes, or empty if none.
	 */
	protected static Collection<CMAttributeDeclaration> findRequiredAttributes(DOMElement element,
			ICodeActionRequest request) {
		ContentModelManager contentModelManager = request.getComponent(ContentModelManager.class);
		for (CMDocument cmDocument : contentModelManager.findCMDocument(element)) {
			CMElementDeclaration elementDeclaration = cmDocument.findCMElement(element);
			if (elementDeclaration != null) {
				List<CMAttributeDeclaration> requiredAttributes = elementDeclaration.getAttributes().stream()
						.filter(CMAttributeDeclaration::isRequired)
						.filter(cmAttr -> !element.hasAttribute(cmAttr.getLocalName()))
						.collect(Collectors.toList());
				if (!requiredAttributes.isEmpty()) {
					return requiredAttributes;
				}
			}
		}
		return Collections.emptyList();
	}

	private static boolean codeActionAlreadyExists(List<CodeAction> codeActions, Diagnostic diagnostic) {
		for (CodeAction codeAction : codeActions) {
			for (Diagnostic d : codeAction.getDiagnostics()) {
				if (d.getCode().equals(diagnostic.getCode())
						&& d.getRange().equals(diagnostic.getRange())) {
					return true;
				}
			}
		}
		return false;
	}
}
