/**
 *  Copyright (c) 2018, 2026 Red Hat, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Red Hat Inc. - initial API and implementation
 */
package org.eclipse.lemminx.settings.capabilities;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionOptions;
import org.eclipse.lsp4j.CodeLensOptions;
import org.eclipse.lsp4j.ColorProviderOptions;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.DocumentLinkOptions;
import org.eclipse.lsp4j.RenameOptions;
import org.eclipse.lsp4j.SemanticTokenModifiers;
import org.eclipse.lsp4j.SemanticTokenTypes;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.TextDocumentSyncKind;

/**
 * Server Capabilities Constants
 */
public class ServerCapabilitiesConstants {

	private ServerCapabilitiesConstants() {
	}

	public static final String TEXT_DOCUMENT_FORMATTING = "textDocument/formatting";
	public static final String TEXT_DOCUMENT_RANGE_FORMATTING = "textDocument/rangeFormatting";
	public static final String TEXT_DOCUMENT_ON_TYPE_FORMATTING = "textDocument/onTypeFormatting";
	public static final String TEXT_DOCUMENT_CODE_LENS = "textDocument/codeLens";
	public static final String TEXT_DOCUMENT_SIGNATURE_HELP = "textDocument/signatureHelp";
	public static final String TEXT_DOCUMENT_RENAME = "textDocument/rename";
	public static final String TEXT_DOCUMENT_COMPLETION = "textDocument/completion";
	public static final String TEXT_DOCUMENT_SYNC = "textDocument/synchronization";
	public static final String TEXT_DOCUMENT_LINK = "textDocument/documentLink";
	public static final String TEXT_DOCUMENT_COLOR = "textDocument/documentColor";
	public static final String TEXT_DOCUMENT_FOLDING_RANGE = "textDocument/foldingRange";
	public static final String TEXT_DOCUMENT_DOCUMENT_SYMBOL = "textDocument/documentSymbol";
	public static final String TEXT_DOCUMENT_CODE_ACTION = "textDocument/codeAction";
	public static final String TEXT_DOCUMENT_DEFINITION = "textDocument/definition";
	public static final String TEXT_DOCUMENT_TYPEDEFINITION = "textDocument/typeDefinition";
	public static final String TEXT_DOCUMENT_HOVER = "textDocument/hover";
	public static final String TEXT_DOCUMENT_REFERENCES = "textDocument/references";
	public static final String TEXT_DOCUMENT_LINKED_EDITING_RANGE = "textDocument/linkedEditingRange";
	public static final String TEXT_DOCUMENT_HIGHLIGHT = "textDocument/documentHighlight";
	public static final String TEXT_DOCUMENT_SELECTION_RANGE = "textDocument/selectionRange";
	public static final String TEXT_DOCUMENT_INLINE_COMPLETION = "textDocument/inlineCompletion";
	public static final String TEXT_DOCUMENT_SEMANTIC_TOKENS = "textDocument/semanticTokens";

	public static final String WORKSPACE_CHANGE_FOLDERS = "workspace/didChangeWorkspaceFolders";
	public static final String WORKSPACE_EXECUTE_COMMAND = "workspace/executeCommand";
	public static final String WORKSPACE_SYMBOL = "workspace/symbol";
	public static final String WORKSPACE_WATCHED_FILES = "workspace/didChangeWatchedFiles";

	public static final String FORMATTING_ID = UUID.randomUUID().toString();
	public static final String COMPLETION_ID = UUID.randomUUID().toString();
	public static final String SYNC_ID = UUID.randomUUID().toString();
	public static final String FOLDING_RANGE_ID = UUID.randomUUID().toString();
	public static final String LINK_ID = UUID.randomUUID().toString();
	public static final String COLOR_ID = UUID.randomUUID().toString();
	public static final String FORMATTING_ON_TYPE_ID = UUID.randomUUID().toString();
	public static final String FORMATTING_RANGE_ID = UUID.randomUUID().toString();
	public static final String CODE_LENS_ID = UUID.randomUUID().toString();
	public static final String SIGNATURE_HELP_ID = UUID.randomUUID().toString();
	public static final String RENAME_ID = UUID.randomUUID().toString();
	public static final String WORKSPACE_EXECUTE_COMMAND_ID = UUID.randomUUID().toString();
	public static final String WORKSPACE_SYMBOL_ID = UUID.randomUUID().toString();
	public static final String DOCUMENT_SYMBOL_ID = UUID.randomUUID().toString();
	public static final String CODE_ACTION_ID = UUID.randomUUID().toString();
	public static final String DEFINITION_ID = UUID.randomUUID().toString();
	public static final String TYPEDEFINITION_ID = UUID.randomUUID().toString();
	public static final String HOVER_ID = UUID.randomUUID().toString();
	public static final String REFERENCES_ID = UUID.randomUUID().toString();
	public static final String DOCUMENT_HIGHLIGHT_ID = UUID.randomUUID().toString();
	public static final String SELECTION_RANGE_ID = UUID.randomUUID().toString();
	public static final String WORKSPACE_CHANGE_FOLDERS_ID = UUID.randomUUID().toString();
	public static final String WORKSPACE_WATCHED_FILES_ID = UUID.randomUUID().toString();
	public static final String LINKED_EDITING_RANGE_ID = UUID.randomUUID().toString();
	public static final String INLINE_COMPLETION_ID = UUID.randomUUID().toString();
	public static final String SEMANTIC_TOKENS_ID = UUID.randomUUID().toString();

	public static final CompletionOptions DEFAULT_COMPLETION_OPTIONS = new CompletionOptions(true,
			Arrays.asList(".", ":", "<", "\"", "=", "/", "\\", "?", "\'", "&", "#", "@"));
	public static final TextDocumentSyncKind DEFAULT_SYNC_OPTION = TextDocumentSyncKind.Full;
	public static final DocumentLinkOptions DEFAULT_LINK_OPTIONS = new DocumentLinkOptions(true);
	public static final RenameOptions DEFAULT_RENAME_OPTIONS = new RenameOptions(true);
	public static final ColorProviderOptions DEFAULT_COLOR_OPTIONS = new ColorProviderOptions();
	public static final CodeLensOptions DEFAULT_CODELENS_OPTIONS = new CodeLensOptions();
	public static final CodeActionOptions DEFAULT_CODEACTION_OPTIONS = createDefaultCodeActionOptions();

	private static CodeActionOptions createDefaultCodeActionOptions() {
		CodeActionOptions options = new CodeActionOptions();
		options.setResolveProvider(Boolean.TRUE);
		options.setCodeActionKinds(Arrays.asList(
			CodeActionKind.QuickFix,
			CodeActionKind.Source
		));
		return options;
	}

	public static final List<String> SEMANTIC_TOKEN_TYPES = Arrays.asList(
			SemanticTokenTypes.Property,      // 0: for attribute names
			SemanticTokenTypes.Variable,      // 1: for entity names / variables
			SemanticTokenTypes.Type,          // 2: for element/notation names
			SemanticTokenTypes.Comment,       // 3: for comments
			SemanticTokenTypes.Keyword,       // 4: for keywords
			SemanticTokenTypes.String,        // 5: for strings
			SemanticTokenTypes.Number,        // 6: for numbers
			SemanticTokenTypes.Function,      // 7: for function names
			SemanticTokenTypes.Operator,      // 8: for operators
			SemanticTokenTypes.Namespace,     // 9: for namespaces
			SemanticTokenTypes.Class,         // 10: for class names
			SemanticTokenTypes.Parameter,     // 11: for parameters
			SemanticTokenTypes.Regexp,        // 12: for regexps
			SemanticTokenTypes.Method,        // 13: for method calls
			SemanticTokenTypes.Modifier,      // 14: for storage modifiers (public, static...)
			SemanticTokenTypes.Decorator,     // 15: for annotations/decorators
			SemanticTokenTypes.Enum,          // 16: for enum types
			SemanticTokenTypes.EnumMember,    // 17: for enum values
			SemanticTokenTypes.Interface,     // 18: for interfaces
			SemanticTokenTypes.Macro,         // 19: for macros/preprocessor
			SemanticTokenTypes.Label          // 20: for labels
	);

	public static final List<String> SEMANTIC_TOKEN_MODIFIERS = Arrays.asList(
			SemanticTokenModifiers.Readonly,       // 0: for required attributes
			SemanticTokenModifiers.Declaration,     // 1: for declarations
			SemanticTokenModifiers.Definition,      // 2: for definitions
			SemanticTokenModifiers.Static,          // 3: for static members
			SemanticTokenModifiers.Deprecated,      // 4: for deprecated elements
			SemanticTokenModifiers.Documentation,   // 5: for doc comments
			SemanticTokenModifiers.DefaultLibrary   // 6: for built-in/support functions
	);

	public static final int TOKEN_TYPE_PROPERTY = 0;
	public static final int TOKEN_TYPE_VARIABLE = 1;
	public static final int TOKEN_TYPE_TYPE = 2;
	public static final int TOKEN_TYPE_COMMENT = 3;
	public static final int TOKEN_TYPE_KEYWORD = 4;
	public static final int TOKEN_TYPE_STRING = 5;
	public static final int TOKEN_TYPE_NUMBER = 6;
	public static final int TOKEN_TYPE_FUNCTION = 7;
	public static final int TOKEN_TYPE_OPERATOR = 8;
	public static final int TOKEN_TYPE_NAMESPACE = 9;
	public static final int TOKEN_TYPE_CLASS = 10;
	public static final int TOKEN_TYPE_PARAMETER = 11;
	public static final int TOKEN_TYPE_REGEXP = 12;
	public static final int TOKEN_TYPE_METHOD = 13;
	public static final int TOKEN_TYPE_MODIFIER = 14;
	public static final int TOKEN_TYPE_DECORATOR = 15;
	public static final int TOKEN_TYPE_ENUM = 16;
	public static final int TOKEN_TYPE_ENUM_MEMBER = 17;
	public static final int TOKEN_TYPE_INTERFACE = 18;
	public static final int TOKEN_TYPE_MACRO = 19;
	public static final int TOKEN_TYPE_LABEL = 20;
	public static final int TOKEN_MODIFIER_READONLY = 1 << 0;
	public static final int TOKEN_MODIFIER_DECLARATION = 1 << 1;
	public static final int TOKEN_MODIFIER_DEFINITION = 1 << 2;
	public static final int TOKEN_MODIFIER_STATIC = 1 << 3;
	public static final int TOKEN_MODIFIER_DEPRECATED = 1 << 4;
	public static final int TOKEN_MODIFIER_DOCUMENTATION = 1 << 5;
	public static final int TOKEN_MODIFIER_DEFAULT_LIBRARY = 1 << 6;

	public static final SemanticTokensWithRegistrationOptions DEFAULT_SEMANTIC_TOKENS_OPTIONS = createDefaultSemanticTokensOptions();

	private static SemanticTokensWithRegistrationOptions createDefaultSemanticTokensOptions() {
		SemanticTokensLegend legend = new SemanticTokensLegend(SEMANTIC_TOKEN_TYPES, SEMANTIC_TOKEN_MODIFIERS);
		SemanticTokensWithRegistrationOptions options = new SemanticTokensWithRegistrationOptions(legend);
		options.setFull(true);
		return options;
	}
}