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
package org.eclipse.lemminx.utils;

import org.eclipse.lsp4j.StringValue;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Utility class for inline completion tests.
 */
public class InlineCompletionTestUtils {

	/**
	 * Helper method to extract string from Either<String, StringValue>
	 * 
	 * @param insertText the insert text as Either
	 * @return the string value or null
	 */
	public static String getInsertTextAsString(Either<String, StringValue> insertText) {
		if (insertText == null) {
			return null;
		}
		if (insertText.isLeft()) {
			return insertText.getLeft();
		} else {
			return insertText.getRight().getValue();
		}
	}
}
