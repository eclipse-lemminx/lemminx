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
package org.eclipse.lemminx.extensions.contentmodel.commands;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.lemminx.commons.SnippetsBuilder;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.services.IXMLDocumentProvider;
import org.eclipse.lemminx.services.extensions.commands.AbstractDOMDocumentCommandHandler;
import org.eclipse.lemminx.services.extensions.commands.ArgumentsUtils;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * XML Command "xml.refactor.surround.with" to support surround:
 *
 * <ul>
 * <li>Surround with Tags (Wrap)</li>
 * <li>Surround with Comments</li>
 * <li>Surround with CDATA</li>
 * <li>Ignore Formatting</li>
 * </ul>
 *
 */
public class SurroundWithCommand extends AbstractDOMDocumentCommandHandler {

	public static final String COMMAND_ID = "xml.refactor.surround.with";

	public static class SurroundWithResponse {

		private TextEdit start;

		private TextEdit end;

		public SurroundWithResponse() {
		}

		public SurroundWithResponse(TextEdit start, TextEdit end) {
			this.start = start;
			this.end = end;
		}

		public TextEdit getStart() {
			return start;
		}

		public TextEdit getEnd() {
			return end;
		}
	}

	public static enum SurroundWithKind {
		tags, //
		comments, //
		cdata, //
		ignoreFormatting;

		public static SurroundWithKind get(String kind) {
			return valueOf(kind);
		}
	}

	private final ContentModelManager contentModelManager;

	public SurroundWithCommand(IXMLDocumentProvider documentProvider, ContentModelManager contentModelManager) {
		super(documentProvider);
		this.contentModelManager = contentModelManager;
	}

	@Override
	protected Object executeCommand(DOMDocument document, ExecuteCommandParams params, SharedSettings sharedSettings,
			CancelChecker cancelChecker) throws Exception {
		// Get parameters
		Range selection = ArgumentsUtils.getArgAt(params, 1, Range.class);
		SurroundWithKind kind = SurroundWithKind.get(ArgumentsUtils.getArgAt(params, 2, String.class));
		boolean snippetsSupported = ArgumentsUtils.getArgAt(params, 3, Boolean.class);

		// Surround process
		boolean emptySelection = selection.getStart().equals(selection.getEnd());
		StringBuilder startText = null;
		StringBuilder endText = null;
		Position startPos = selection.getStart();
		Position endPos = selection.getEnd();
		String prefix = null;

		int offset = document.offsetAt(selection.getStart());
		DOMNode node = document.findNodeAt(offset);

		// Adjust selection if need
		boolean adjusted = false;
		if (emptySelection && node.isElement()) {
			DOMElement element = (DOMElement) node;
			if (element.isInStartTag(offset) || element.isInEndTag(offset)) {
				startPos = document.positionAt(element.getStart());
				endPos = document
						.positionAt(element.isEndTagClosed() ? element.getEndTagCloseOffset() + 1 : element.getEnd());
				adjusted = true;
			}
		}

		switch (kind) {
			case cdata:
				startText = new StringBuilder("<![CDATA[");
				if (snippetsSupported && emptySelection) {
					SnippetsBuilder.tabstops(1, startText);
				}
				endText = new StringBuilder("]]>");
				if (snippetsSupported) {
					SnippetsBuilder.tabstops(0, endText);
				}
				break;
			case comments:
				startText = new StringBuilder("<!--");
				if (snippetsSupported && emptySelection) {
					SnippetsBuilder.tabstops(1, startText);
				}
				endText = new StringBuilder("-->");
				if (snippetsSupported) {
					SnippetsBuilder.tabstops(0, endText);
				}
				break;
			case ignoreFormatting:
				startText = new StringBuilder("<!-- @formatter:off -->");
				if (snippetsSupported && emptySelection) {
					SnippetsBuilder.tabstops(1, startText);
				}
				endText = new StringBuilder("<!-- @formatter:on -->");
				if (snippetsSupported) {
					SnippetsBuilder.tabstops(0, endText);
				}
				break;
			default:
				List<String> tags = getTags(node, prefix, offset, adjusted);
				String tag = tags.isEmpty() ? "" : tags.get(0);

				// Start tag
				startText = new StringBuilder("<");
				if (snippetsSupported) {
					if (!tags.isEmpty()) {
						SnippetsBuilder.choice(1, tags, startText);
					} else {
						SnippetsBuilder.placeholders(1, tag, startText);
					}
				} else {
					startText.append(tag);
				}
				startText.append(">");
				if (!adjusted && emptySelection && snippetsSupported) {
					SnippetsBuilder.tabstops(2, startText);
				}

				// End tag
				endText = new StringBuilder("</");
				if (snippetsSupported) {
					SnippetsBuilder.placeholders(1, tag, endText);
				} else {
					endText.append(tag);
				}
				endText.append(">");
				if (snippetsSupported) {
					SnippetsBuilder.tabstops(0, endText);
				}
		}
		TextEdit start = new TextEdit(new Range(startPos, startPos), startText.toString());
		TextEdit end = new TextEdit(new Range(endPos, endPos), endText.toString());
		return new SurroundWithResponse(start, end);
	}

	private List<String> getTags(DOMNode node, String prefix, int offset, boolean adjusted) {
		DOMElement parentElement = node.isElement() && !adjusted ? (DOMElement) node : node.getParentElement();
		Collection<CMDocument> cmDocuments = parentElement != null ? contentModelManager.findCMDocument(parentElement)
				: contentModelManager.findCMDocument(node.getOwnerDocument(), null);
		if (parentElement == null) {
			return cmDocuments.stream() //
					.flatMap(cmDocument -> cmDocument.getElements().stream()) //
					.map(decl -> decl.getName(prefix)) //
					.distinct() //
					.sorted() //
					.collect(Collectors.toList());
		}
		for (CMDocument cmDocument : cmDocuments) {
			CMElementDeclaration elementDeclaration = cmDocument.findCMElement(parentElement);
			if (elementDeclaration != null) {
				return elementDeclaration.getPossibleElements(parentElement, offset)
						.stream()
						.map(decl -> decl.getName(prefix))
						.sorted()
						.collect(Collectors.toList());
			}
		}
		return Collections.emptyList();
	}

}
