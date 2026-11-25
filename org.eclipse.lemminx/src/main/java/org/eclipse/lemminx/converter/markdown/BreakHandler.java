/*******************************************************************************
 * Copyright (c) 2026 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.lemminx.converter.markdown;

import org.eclipse.lemminx.converter.HtmlAttributes;
import org.eclipse.lemminx.converter.HtmlConverter;
import org.eclipse.lemminx.converter.TagHandler;

/**
 * Handles the {@code <br>} tag by emitting a line separator.
 *
 * <p>Use case: {@code "line1<br>line2"} → {@code "line1\nline2"}</p>
 */
public class BreakHandler implements TagHandler {

	public static final BreakHandler INSTANCE = new BreakHandler();

	private BreakHandler() {
	}

	@Override
	public void open(HtmlAttributes attrs, StringBuilder sb) {
		sb.append(HtmlConverter.LINE_SEP);
	}
}
