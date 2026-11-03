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
package org.eclipse.lemminx.extensions.embeddedLanguages.participants;

import java.util.List;

import org.eclipse.lemminx.settings.capabilities.ServerCapabilitiesConstants;

/**
 * Maps TextMate grammar scopes to LSP semantic token types and modifiers.
 */
public class TextMateScopeMapper {

	private TextMateScopeMapper() {
	}

	/**
	 * Holds the result of mapping a TextMate scope to an LSP semantic token.
	 */
	public static class TokenMapping {
		public final int tokenType;
		public final int tokenModifiers;

		public TokenMapping(int tokenType, int tokenModifiers) {
			this.tokenType = tokenType;
			this.tokenModifiers = tokenModifiers;
		}
	}

	/**
	 * Returns the LSP semantic token type and modifiers for the given TextMate
	 * scope stack, or {@code null} if no mapping is found.
	 *
	 * @param scopes the TextMate scope stack from outermost to innermost.
	 * @return the token mapping, or {@code null}.
	 */
	public static TokenMapping toTokenMapping(List<String> scopes) {
		int tokenType = -1;
		int tokenModifiers = 0;

		for (int i = scopes.size() - 1; i >= 0; i--) {
			String scope = scopes.get(i);

			// --- Comments ---
			if (scope.startsWith("comment.block.documentation")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_COMMENT,
						ServerCapabilitiesConstants.TOKEN_MODIFIER_DOCUMENTATION);
			}
			if (scope.startsWith("comment.")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_COMMENT, 0);
			}

			// --- Strings (regexp before general string) ---
			if (scope.startsWith("string.regexp")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_REGEXP, 0);
			}
			if (scope.startsWith("string.")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_STRING, 0);
			}

			// --- Constants ---
			if (scope.startsWith("constant.numeric")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_NUMBER, 0);
			}
			if (scope.startsWith("constant.language")) {
				// true, false, null, etc. — readonly built-in values
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_ENUM_MEMBER,
						ServerCapabilitiesConstants.TOKEN_MODIFIER_READONLY
								| ServerCapabilitiesConstants.TOKEN_MODIFIER_DEFAULT_LIBRARY);
			}
			if (scope.startsWith("constant.character") || scope.startsWith("constant.other")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_VARIABLE,
						ServerCapabilitiesConstants.TOKEN_MODIFIER_READONLY);
			}

			// --- Entities (names of things) ---
			if (scope.startsWith("entity.name.function.method") || scope.startsWith("meta.method-call")
					|| scope.startsWith("entity.name.function.accessor")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_METHOD, 0);
			}
			if (scope.startsWith("entity.name.function")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_FUNCTION,
						ServerCapabilitiesConstants.TOKEN_MODIFIER_DECLARATION);
			}
			if (scope.startsWith("entity.name.type.enum")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_ENUM, 0);
			}
			if (scope.startsWith("entity.name.type.interface")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_INTERFACE, 0);
			}
			if (scope.startsWith("entity.name.type") || scope.startsWith("entity.name.class")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_CLASS, 0);
			}
			if (scope.startsWith("entity.name.namespace") || scope.startsWith("entity.name.module")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_NAMESPACE, 0);
			}
			if (scope.startsWith("entity.name.label")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_LABEL, 0);
			}
			if (scope.startsWith("entity.name.tag")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_KEYWORD, 0);
			}

			// --- Annotations / Decorators ---
			if (scope.startsWith("meta.annotation") || scope.startsWith("storage.type.annotation")
					|| scope.startsWith("entity.name.type.annotation")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_DECORATOR, 0);
			}

			// --- Support (built-in / library) ---
			if (scope.startsWith("support.function")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_FUNCTION,
						ServerCapabilitiesConstants.TOKEN_MODIFIER_DEFAULT_LIBRARY);
			}
			if (scope.startsWith("support.class") || scope.startsWith("support.type")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_CLASS,
						ServerCapabilitiesConstants.TOKEN_MODIFIER_DEFAULT_LIBRARY);
			}
			if (scope.startsWith("support.variable") || scope.startsWith("support.constant")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_VARIABLE,
						ServerCapabilitiesConstants.TOKEN_MODIFIER_DEFAULT_LIBRARY);
			}

			// --- Variables ---
			if (scope.startsWith("variable.parameter")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_PARAMETER, 0);
			}
			if (scope.startsWith("variable.other.property") || scope.startsWith("variable.other.object.property")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_PROPERTY, 0);
			}
			if (scope.startsWith("variable.language")) {
				// this, self, super — readonly built-in variable
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_VARIABLE,
						ServerCapabilitiesConstants.TOKEN_MODIFIER_READONLY
								| ServerCapabilitiesConstants.TOKEN_MODIFIER_DEFAULT_LIBRARY);
			}
			if (scope.startsWith("variable.")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_VARIABLE, 0);
			}

			// --- Keywords / Storage / Control ---
			// keyword.operator before keyword.
			if (scope.startsWith("keyword.operator")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_OPERATOR, 0);
			}
			if (scope.startsWith("storage.modifier")) {
				// public, private, static, final, abstract, etc.
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_MODIFIER, 0);
			}
			if (scope.startsWith("storage.type")) {
				// def, class, interface, enum, etc.
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_TYPE, 0);
			}
			if (scope.startsWith("keyword.")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_KEYWORD, 0);
			}

			// --- Punctuation (operators) ---
			if (scope.startsWith("punctuation.definition.tag") || scope.startsWith("punctuation.section")
					|| scope.startsWith("punctuation.separator") || scope.startsWith("punctuation.accessor")) {
				return new TokenMapping(ServerCapabilitiesConstants.TOKEN_TYPE_OPERATOR, 0);
			}
		}

		return tokenType >= 0 ? new TokenMapping(tokenType, tokenModifiers) : null;
	}
}
