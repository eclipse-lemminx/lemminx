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

package org.eclipse.lemminx.extensions.filepath.participants;

import static org.eclipse.lemminx.utils.FilesUtils.getFileName;
import static org.eclipse.lemminx.utils.platform.Platform.isWindows;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMRange;
import org.eclipse.lemminx.dom.DOMText;
import org.eclipse.lemminx.dom.DTDDeclParameter;
import org.eclipse.lemminx.dom.NoNamespaceSchemaLocation;
import org.eclipse.lemminx.dom.SchemaLocation;
import org.eclipse.lemminx.extensions.filepath.FilePathPlugin;
import org.eclipse.lemminx.extensions.filepath.IFilePathExpression;
import org.eclipse.lemminx.extensions.filepath.SimpleFilePathExpression;
import org.eclipse.lemminx.services.extensions.completion.CompletionParticipantAdapter;
import org.eclipse.lemminx.services.extensions.completion.ICompletionRequest;
import org.eclipse.lemminx.services.extensions.completion.ICompletionResponse;
import org.eclipse.lemminx.utils.CompletionSortTextHelper;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.FilesUtils;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Extension to support completion for file, folder path in:
 *
 * <ul>
 * <li>attribute value:
 *
 * <pre>
 * &lt;item path="file:///C:/folder" /&gt;
 * &lt;item path="file:///C:/folder file:///C:/file.txt" /&gt;
 * &lt;item path="/folder" /&gt;
 * </pre>
 *
 * </li>
 * <li>DTD DOCTYPE SYSTEM
 *
 * <pre>
 * &lt;!DOCTYPE parent SYSTEM "file.dtd"&gt;
 * </pre>
 *
 * </li>
 *
 * </ul>
 *
 * <p>
 *
 * </p>
 */
public class FilePathCompletionParticipant extends CompletionParticipantAdapter {

	private static final Logger LOGGER = Logger.getLogger(FilePathCompletionParticipant.class.getName());

	private static final IFilePathExpression DOCTYPE_FILE_PATH_EXPRESSION = new SimpleFilePathExpression() {

		@Override
		protected boolean acceptFile(Path path) {
			return DOMUtils.isDTD(getFileName(path));
		};
	};

	private static final IFilePathExpression NO_NAMESPACE_SCHEMALOCATION_FILE_PATH_EXPRESSION = new SimpleFilePathExpression() {

		@Override
		protected boolean acceptFile(Path path) {
			return DOMUtils.isXSD(getFileName(path));
		};
	};

	private static final IFilePathExpression SCHEMALOCATION_FILE_PATH_EXPRESSION = new SimpleFilePathExpression() {

		@Override
		protected boolean acceptFile(Path path) {
			return DOMUtils.isXSD(getFileName(path));
		};

		public Character getSeparator() {
			return ' ';
		};
	};

	private final FilePathPlugin filePathPlugin;

	public FilePathCompletionParticipant(FilePathPlugin filePathPlugin) {
		this.filePathPlugin = filePathPlugin;
	}

	@Override
	public void onAttributeValue(String value, ICompletionRequest request, ICompletionResponse response,
			CancelChecker cancelChecker) throws Exception {
		DOMDocument document = request.getXMLDocument();
		int completionOffset = request.getOffset();
		DOMNode node = request.getNode();
		DOMAttr attr = node.findAttrAt(request.getOffset());

		// Check if completion is triggered in the value of
		// xsi:noNamespaceSchemaLocation
		NoNamespaceSchemaLocation noNamespaceSchemaLocation = document.getNoNamespaceSchemaLocation();
		if (noNamespaceSchemaLocation != null && attr == noNamespaceSchemaLocation.getAttr()) {
			addFileCompletionItems(attr, document, completionOffset, NO_NAMESPACE_SCHEMALOCATION_FILE_PATH_EXPRESSION,
					response, cancelChecker);
			return;
		}

		// Check if completion is triggered in the value of xsi:schemaLocation
		SchemaLocation schemaLocation = document.getSchemaLocation();
		if (schemaLocation != null && attr == schemaLocation.getAttr()) {
			addFileCompletionItems(attr, document, completionOffset, SCHEMALOCATION_FILE_PATH_EXPRESSION,
					response, cancelChecker);
			return;
		}

		// File path completion on attribute value
		List<IFilePathExpression> expressions = filePathPlugin.findFilePathExpressions(document);
		if (expressions.isEmpty()) {
			return;
		}

		for (IFilePathExpression expression : expressions) {
			if (expression.match(attr)) {
				cancelChecker.checkCanceled();
				addFileCompletionItems(attr, document, completionOffset, expression, response, cancelChecker);
			}
		}
	}

	private void addFileCompletionItems(DOMAttr attr, DOMDocument xmlDocument, int completionOffset,
			IFilePathExpression expression, ICompletionResponse response, CancelChecker cancelChecker)
			throws Exception {
		DOMRange attrValueRange = attr.getNodeAttrValue();
		addFileCompletionItems(xmlDocument, attrValueRange.getStart() + 1 /* increment to be after the quote */,
				attrValueRange.getEnd() - 1, completionOffset, expression,
				response, cancelChecker);
	}

	@Override
	public void onXMLContent(ICompletionRequest request, ICompletionResponse response, CancelChecker cancelChecker)
			throws Exception {
		// File path completion on text node
		List<IFilePathExpression> expressions = filePathPlugin.findFilePathExpressions(request.getXMLDocument());
		if (expressions.isEmpty()) {
			return;
		}
		DOMText textNode = findTextNode(request.getNode(), request.getOffset());
		if (textNode == null) {
			return;
		}
		DOMDocument xmlDocument = request.getXMLDocument();
		for (IFilePathExpression expression : expressions) {
			if (expression.match(textNode)) {
				DOMRange textRange = textNode;
				addFileCompletionItems(xmlDocument, textRange.getStart(), textRange.getEnd(), request.getOffset(),
						expression, response, cancelChecker);
			}
		}
	}

	private static DOMText findTextNode(DOMNode node, int offset) {
		if (node == null || node.isText()) {
			return (DOMText) node;
		}
		if (node.isElement()) {
			DOMText text = ((DOMElement) node).findTextAt(offset);
			if (text != null) {
				return text;
			}
		}
		return null;
	}

	@Override
	public void onDTDSystemId(String value, ICompletionRequest request, ICompletionResponse response,
			CancelChecker cancelChecker) throws Exception {
		// File path completion on DTD DOCTYPE SYSTEM
		DOMDocument xmlDocument = request.getXMLDocument();
		DTDDeclParameter systemId = xmlDocument.getDoctype().getSystemIdNode();
		addFileCompletionItems(xmlDocument, systemId.getStart() + 1 /* increment to be after the quote */,
				systemId.getEnd() - 1, request.getOffset(), DOCTYPE_FILE_PATH_EXPRESSION, response, cancelChecker);
	}

	private static void addFileCompletionItems(DOMDocument xmlDocument, int startOffset, int endOffset,
			int completionOffset, IFilePathExpression expression, ICompletionResponse response,
			CancelChecker cancelChecker) throws Exception {
		// Get the resolved base dir of the file path declared insidet startOffset and
		// endOffset
		// ex: <a href="|path/to/fil|" />
		// base dir is equals for instance to C://path/to
		Character separator = expression != null ? expression.getSeparator() : null;
		FilePathCompletionResult result = FilePathCompletionResult.create(xmlDocument.getTextSequence(),
				xmlDocument.getDocumentURI(), startOffset, endOffset, completionOffset, separator);
		Path baseDir = result.getBaseDir();
		if (baseDir == null) {
			// The base dir cannot be resolved, stop file completion
			return;
		}
		// Fill completions with files / directories of the resolved base dir.
		Range replaceRange = XMLPositionUtility.createRange(result.getStartOffset(), result.getEndOffset(),
				xmlDocument);
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir)) {
			for (Path entry : stream) {
				cancelChecker.checkCanceled();
				if (expression == null || expression.acceptPath(entry)) {
					createFilePathCompletionItem(entry.toFile(), replaceRange, response);
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error while getting files/directories", e);
		}
	}

	private static void createFilePathCompletionItem(File file, Range replaceRange, ICompletionResponse response) {
		CompletionItem item = new CompletionItem();
		String fileName = FilesUtils.encodePath(file.getName());
		if (isWindows && fileName.isEmpty()) { // Edge case for Windows drive letter
			fileName = file.getPath();
			fileName = fileName.substring(0, fileName.length() - 1);
		}
		String insertText = fileName;
		item.setLabel(insertText);

		CompletionItemKind kind = file.isDirectory() ? CompletionItemKind.Folder : CompletionItemKind.File;
		item.setKind(kind);

		item.setSortText(CompletionSortTextHelper.getSortText(kind));
		item.setFilterText(insertText);
		item.setTextEdit(Either.forLeft(new TextEdit(replaceRange, insertText)));
		response.addCompletionItem(item);
	}
}