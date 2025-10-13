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

import static org.apache.commons.lang3.StringEscapeUtils.unescapeJava;
import static org.apache.commons.lang3.StringEscapeUtils.unescapeXml;

import java.util.logging.Logger;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;

/**
 * Converts HTML content into Markdown equivalent.
 *
 * @author Fred Bricon
 */
public class MarkdownConverter {

	private static final Logger LOGGER = Logger.getLogger(MarkdownConverter.class.getName());

	private static final FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();

	private MarkdownConverter(){
		//no public instanciation
	}

	@SuppressWarnings("deprecation")
	public static String convert(String html) {
		if(!StringUtils.isTagOutsideOfBackticks(html)) {
			return unescapeXml(html); // is not html so it can be returned as is (aside from unescaping)
		}
		return unescapeJava(converter.convert(html));
	}

}