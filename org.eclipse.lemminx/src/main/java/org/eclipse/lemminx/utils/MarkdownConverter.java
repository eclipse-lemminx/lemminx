/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.lemminx.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.fbricon.mdream4j.HtmlToMarkdown;
import io.github.fbricon.mdream4j.Options;

/**
 * Converts HTML content into Markdown equivalent.
 *
 * @author Fred Bricon
 */
public class MarkdownConverter {

	private MarkdownConverter(){
		//no public instanciation
	}

	public static String convert(String html) {
		String res = HtmlToMarkdown.convert(html, Options.builder().wrapWidth(0).build()) //
				// remark handled local files weirdly; this replicates that
				.replaceAll("\\(file:///", "(file:/");
		
		// link with no whitespace in them don't render italics/bold through `_`; remark exploited this but mdream doesn't rely on it
		Pattern p = Pattern.compile("\\[[^\\]\\s]+\\]");
		Matcher m = p.matcher(res);
		return m.replaceAll((match) -> {
			return match.group().replaceAll("\\\\_", "_");
		});
	}

}
