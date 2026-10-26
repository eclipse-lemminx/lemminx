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
package org.eclipse.lemminx.commons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.lemminx.utils.TextEditUtils;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CreateFile;
import org.eclipse.lsp4j.CreateFileOptions;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.SnippetTextEdit;
import org.eclipse.lsp4j.StringValue;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Factory for simple {@link CodeAction}
 *
 */
public class CodeActionFactory {

	/**
	 * Create a CodeAction to remove the content from the given range.
	 *
	 * @param title
	 * @param range
	 * @param document
	 * @param diagnostic
	 * @return
	 */
	public static CodeAction remove(String title, Range range, TextDocumentItem document, Diagnostic diagnostic) {
		return replace(title, range, "", document, diagnostic);
	}

	/**
	 * Create a CodeAction to insert a new content at the end of the given range.
	 *
	 * @param title
	 * @param range
	 * @param insertText
	 * @param document
	 * @param diagnostic
	 *
	 * @return the CodeAction to insert a new content at the end of the given range.
	 */
	public static CodeAction insert(String title, Position position, String insertText, TextDocumentItem document,
			Diagnostic diagnostic) {
		return insert(title, position, insertText, document, diagnostic, false);
	}

	/**
	 * Creates a {@link CodeAction} that inserts the given text at the given
	 * position. When {@code snippetSupport} is true, the text is treated as a
	 * LSP 3.18 snippet (with tabstops {@code $1}, {@code $0}) and wrapped in a
	 * {@link SnippetTextEdit}; otherwise a plain {@link TextEdit} is used.
	 *
	 * @param title          the code action title.
	 * @param position       the position to insert at.
	 * @param insertText     the text to insert (plain or snippet syntax).
	 * @param document       the text document.
	 * @param diagnostic     the diagnostic this code action fixes.
	 * @param snippetSupport true if the client supports
	 *                       {@code workspace.workspaceEdit.snippetEditSupport}.
	 * @return the code action.
	 */
	public static CodeAction insert(String title, Position position, String insertText, TextDocumentItem document,
			Diagnostic diagnostic, boolean snippetSupport) {
		return replace(title, new Range(position, position), insertText, document, diagnostic, snippetSupport);
	}

	/**
	 * Returns the text edit to insert a new content at the end of the given range.
	 *
	 * @param insertText text to insert.
	 * @param position   the position.
	 * @param document   the text document.
	 *
	 * @return the text edit to insert a new content at the end of the given range.
	 */
	public static TextDocumentEdit insertEdit(String insertText, Position position, TextDocumentItem document) {
		return insertEdit(insertText, position, document, false);
	}

	public static TextDocumentEdit insertEdit(String insertText, Position position, TextDocumentItem document,
			boolean snippetSupport) {
		TextEdit edit = insertEdit(insertText, position);
		return insertEdits(document, Collections.singletonList(edit), snippetSupport);
	}

	public static TextEdit insertEdit(String insertText, Position position) {
		return new TextEdit(new Range(position, position), insertText);
	}

	public static TextDocumentEdit insertEdits(TextDocumentItem document, List<TextEdit> edits) {
		return insertEdits(document, edits, false);
	}

	public static TextDocumentEdit insertEdits(TextDocumentItem document, List<TextEdit> edits,
			boolean snippetSupport) {
		VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier(
				document.getUri(), document.getVersion());
		List<Either<TextEdit, SnippetTextEdit>> eitherEdits;
		if (snippetSupport) {
			eitherEdits = edits.stream()
					.map(te -> {
						SnippetTextEdit se = new SnippetTextEdit(te.getRange(),
								new StringValue("snippet", te.getNewText()));
						return Either.<TextEdit, SnippetTextEdit>forRight(se);
					})
					.collect(Collectors.toList());
		} else {
			eitherEdits = TextEditUtils.toEitherTextEdits(edits);
		}
		return new TextDocumentEdit(versionedTextDocumentIdentifier, eitherEdits);
	}

	public static CodeAction replace(String title, Range range, String replaceText, TextDocumentItem document,
			Diagnostic diagnostic) {
		return replace(title, range, replaceText, document, diagnostic, false);
	}

	public static CodeAction replace(String title, List<TextEdit> replace, TextDocumentItem document,
			Diagnostic diagnostic) {

		CodeAction insertContentAction = new CodeAction(title);
		insertContentAction.setKind(CodeActionKind.QuickFix);
		insertContentAction.setDiagnostics(Arrays.asList(diagnostic));

		VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier(
				document.getUri(), document.getVersion());
		List<Either<TextEdit, SnippetTextEdit>> eitherEdits = TextEditUtils.toEitherTextEdits(replace);
		TextDocumentEdit textDocumentEdit = new TextDocumentEdit(versionedTextDocumentIdentifier, eitherEdits);
		WorkspaceEdit workspaceEdit = new WorkspaceEdit(Collections.singletonList(Either.forLeft(textDocumentEdit)));
		insertContentAction.setEdit(workspaceEdit);
		return insertContentAction;
	}

	/**
	 * Returns the workspace edit of a given replacement text and range.
	 *
	 * @param replaceText the text to replace.
	 * @param range       the range to replace the text over.
	 * @param document    the text document.
	 * @return the workspace edit of a given replacement text and range.
	 */
	public static WorkspaceEdit getReplaceWorkspaceEdit(String replaceText, Range range, TextDocumentItem document) {
		return getReplaceWorkspaceEdit(replaceText, range, document, false);
	}

	/**
	 * Creates a {@link CodeAction} that replaces the given range with the given
	 * text. When {@code snippetSupport} is true, the text is treated as a LSP 3.18
	 * snippet (with tabstops {@code $1}, {@code $0}) and wrapped in a
	 * {@link SnippetTextEdit}; otherwise a plain {@link TextEdit} is used.
	 *
	 * @param title          the code action title.
	 * @param range          the range to replace.
	 * @param replaceText    the replacement text (plain or snippet syntax).
	 * @param document       the text document.
	 * @param diagnostic     the diagnostic this code action fixes.
	 * @param snippetSupport true if the client supports
	 *                       {@code workspace.workspaceEdit.snippetEditSupport}.
	 * @return the code action.
	 */
	public static CodeAction replace(String title, Range range, String replaceText, TextDocumentItem document,
			Diagnostic diagnostic, boolean snippetSupport) {
		CodeAction codeAction = new CodeAction(title);
		codeAction.setKind(CodeActionKind.QuickFix);
		codeAction.setDiagnostics(Arrays.asList(diagnostic));
		codeAction.setEdit(getReplaceWorkspaceEdit(replaceText, range, document, snippetSupport));
		return codeAction;
	}

	/**
	 * Returns the workspace edit that replaces the given range with the given text.
	 * When {@code snippetSupport} is true, the text is wrapped in a
	 * {@link SnippetTextEdit}; otherwise a plain {@link TextEdit} is used.
	 *
	 * @param replaceText    the replacement text (plain or snippet syntax).
	 * @param range          the range to replace.
	 * @param document       the text document.
	 * @param snippetSupport true if the client supports
	 *                       {@code workspace.workspaceEdit.snippetEditSupport}.
	 * @return the workspace edit.
	 */
	public static WorkspaceEdit getReplaceWorkspaceEdit(String replaceText, Range range, TextDocumentItem document,
			boolean snippetSupport) {
		VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier(
				document.getUri(), document.getVersion());
		List<Either<TextEdit, SnippetTextEdit>> edits;
		if (snippetSupport) {
			StringValue snippet = new StringValue("snippet", replaceText);
			SnippetTextEdit snippetEdit = new SnippetTextEdit(range, snippet);
			edits = Collections.singletonList(Either.forRight(snippetEdit));
		} else {
			TextEdit replace = new TextEdit(range, replaceText);
			edits = Collections.singletonList(Either.forLeft(replace));
		}
		TextDocumentEdit textDocumentEdit = new TextDocumentEdit(versionedTextDocumentIdentifier, edits);
		return new WorkspaceEdit(Collections.singletonList(Either.forLeft(textDocumentEdit)));
	}

	public static CodeAction replaceAt(String title, String replaceText, TextDocumentItem document,
			Diagnostic diagnostic, Collection<Range> ranges) {
		return replaceAt(title, replaceText, document, diagnostic, ranges, false);
	}

	public static CodeAction replaceAt(String title, String replaceText, TextDocumentItem document,
			Diagnostic diagnostic, Collection<Range> ranges, boolean snippetSupport) {
		CodeAction insertContentAction = new CodeAction(title);
		insertContentAction.setKind(CodeActionKind.QuickFix);
		insertContentAction.setDiagnostics(Arrays.asList(diagnostic));

		VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier(
				document.getUri(), document.getVersion());
		List<Either<TextEdit, SnippetTextEdit>> eitherEdits = new ArrayList<>();
		for (Range range : ranges) {
			if (snippetSupport) {
				StringValue snippet = new StringValue("snippet", replaceText);
				eitherEdits.add(Either.forRight(new SnippetTextEdit(range, snippet)));
			} else {
				eitherEdits.add(Either.forLeft(new TextEdit(range, replaceText)));
			}
		}
		TextDocumentEdit textDocumentEdit = new TextDocumentEdit(versionedTextDocumentIdentifier, eitherEdits);
		WorkspaceEdit workspaceEdit = new WorkspaceEdit(Collections.singletonList(Either.forLeft(textDocumentEdit)));

		insertContentAction.setEdit(workspaceEdit);
		return insertContentAction;
	}

	/**
	 * Escapes special snippet characters ({@code \}, {@code $}, {@code }}) so
	 * that the given text is treated as literal content inside a snippet. Call
	 * this on any grammar-provided value before concatenating it with snippet
	 * tabstops like {@code $0}.
	 *
	 * @param text the plain text to escape.
	 * @return the escaped text safe for use in a snippet.
	 */
	public static String escapeSnippetText(String text) {
		StringBuilder sb = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (ch == '\\' || ch == '$' || ch == '}') {
				sb.append('\\');
			}
			sb.append(ch);
		}
		return sb.toString();
	}

	/**
	 * Makes a CodeAction to create a file and add content to the file.
	 *
	 * @param title       The displayed name of the CodeAction
	 * @param fileURI     The file to create
	 * @param fileContent The text to put into the newly created document.
	 * @param diagnostic  The diagnostic that this CodeAction will fix
	 */
	public static CodeAction createFile(String title, String fileURI, String fileContent, Diagnostic diagnostic) {
		WorkspaceEdit createAndAddContentEdit = createFileEdit(fileURI, fileContent);
		CodeAction codeAction = new CodeAction(title);
		codeAction.setEdit(createAndAddContentEdit);
		codeAction.setDiagnostics(Collections.singletonList(diagnostic));
		codeAction.setKind(CodeActionKind.QuickFix);
		return codeAction;
	}

	/**
	 * Returns the file edit to create the file with the given <code>fileURI</code>
	 * and the given <code>fileContent</code>.
	 * 
	 * @param fileURI     the file URI to create.
	 * @param fileContent the file content.
	 * @return the file edit to create the file with the given <code>fileURI</code>
	 *         and the given <code>fileContent</code>.
	 */
	public static WorkspaceEdit createFileEdit(String fileURI, String fileContent) {
		List<Either<TextDocumentEdit, ResourceOperation>> actionsToTake = new ArrayList<>(2);

		// 1. create an empty file
		actionsToTake.add(Either.forRight(new CreateFile(fileURI, new CreateFileOptions(false, true))));

		// 2. update the created file with the given content
		VersionedTextDocumentIdentifier identifier = new VersionedTextDocumentIdentifier(fileURI, 0);
		TextEdit te = new TextEdit(new Range(new Position(0, 0), new Position(0, 0)), fileContent);
		List<Either<TextEdit, SnippetTextEdit>> eitherEdits = Collections.singletonList(
				Either.forLeft(te));
		actionsToTake.add(Either.forLeft(new TextDocumentEdit(identifier, eitherEdits)));

		WorkspaceEdit createAndAddContentEdit = new WorkspaceEdit(actionsToTake);
		return createAndAddContentEdit;
	}

	/**
	 * Makes a CodeAction to call a command from the available server commands.
	 *
	 * @param title         The displayed name of the CodeAction
	 * @param commandId     The id of the given command to add as CodeAction
	 * @param commandParams The document URI of the document the command is called
	 *                      on
	 * @param diagnostic    The diagnostic that this CodeAction will fix
	 */
	public static CodeAction createCommand(String title, String commandId, List<Object> commandParams,
			Diagnostic diagnostic) {
		CodeAction codeAction = new CodeAction(title);
		Command command = new Command(title, commandId, commandParams);
		codeAction.setCommand(command);
		codeAction.setDiagnostics(Collections.singletonList(diagnostic));
		codeAction.setKind(CodeActionKind.QuickFix);
		return codeAction;
	}
}
