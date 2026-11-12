/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.xsd.contentmodel;

import java.util.Collections;
import java.util.List;

import org.apache.xerces.xs.XSObjectList;
import org.eclipse.lemminx.converter.plaintext.PlainTextConverter;
import org.eclipse.lemminx.settings.SchemaDocumentationType;
import org.eclipse.lemminx.utils.StringUtils;

/**
 * XSD documentation
 * 
 * Represents documentation coming from an XML schema file
 */
public class XSDDocumentation {

	private static final String APPINFO_ELEMENT = "appinfo";
	private static final String DOCUMENTATION_ELEMENT = "documentation";

	private final String prefix;
	private final SchemaDocumentationType strategy;
	private final List<String> documentation;
	private final List<String> appinfo;

	/**
	 * Creates documentation from XSD annotations using the given strategy.
	 *
	 * @param annotations      the XSD annotation list
	 * @param docStrategy      which annotation elements to include (all, documentation, appinfo, none)
	 * @param convertToPlainText if true, HTML tags are stripped from the content
	 */
	public XSDDocumentation(XSObjectList annotations, SchemaDocumentationType docStrategy, boolean convertToPlainText) {
		this(annotations, null, docStrategy, convertToPlainText);
	}

	/**
	 * Creates documentation from XSD annotations, optionally filtering by
	 * enumeration value.
	 *
	 * <p>Use case: for {@code xs:enumeration value="strict"}, pass value="strict"
	 * to extract only the annotation for that specific enumeration value.</p>
	 *
	 * @param annotations      the XSD annotation list
	 * @param value            the enumeration value to filter for, or null for element/attribute docs
	 * @param docStrategy      which annotation elements to include
	 * @param convertToPlainText if true, HTML tags are stripped via {@link PlainTextConverter}
	 */
	public XSDDocumentation(XSObjectList annotations, String value, SchemaDocumentationType docStrategy, boolean convertToPlainText) {
		List<String> documentation = Collections.emptyList();
		List<String> appinfo = Collections.emptyList();
		switch(docStrategy) {
			case all: {
				documentation = XSDAnnotationModel.getDocumentation(annotations, value);
				appinfo = XSDAnnotationModel.getAppInfo(annotations, value);
				break;
			}
			case documentation: {
				documentation = XSDAnnotationModel.getDocumentation(annotations, value);
				break;
			}
			case appinfo: {
				appinfo = XSDAnnotationModel.getAppInfo(annotations, value);
				break;
			}
			case none:{
				break;
			}
		}

		if (convertToPlainText) {
			// convert content to plain text
			
			// if the content contains html tags, converting to plaintext
			// will remove them
			convertToPlainText(documentation);
			convertToPlainText(appinfo);
		}

		this.documentation = documentation;
		this.appinfo = appinfo;
		this.strategy = docStrategy;
		this.prefix = XSDAnnotationModel.getPrefix(annotations, value);
	}
	/**
	 * Returns formatted documentation that displays
	 * contents of the documentation element (if exists) and the appinfo
	 * element (if exists).
	 * 
	 * The returned documentation will return raw html if
	 * <code>html</code> is true. Otherwise, the returned documentation
	 * will be plaintext.
	 * 
	 * @param html if true, the return value will contain raw html
	 * @return formatted documentation that displays
	 * contents of the documentation element (if exists) and the appinfo
	 * element (if exists)
	 */
	public String getFormattedDocumentation(boolean html) {
		StringBuilder result = new StringBuilder();
		boolean prependTitles = prependTitleCheck();
		result.append(getFormatted(prefix, DOCUMENTATION_ELEMENT, documentation, prependTitles, html));
		result.append(getFormatted(prefix, APPINFO_ELEMENT, appinfo, prependTitles, html));
		return result.toString().trim();
	}

	/**
	 * Returns true if documentation title (ie, xs:documentation, xs:appinfo)
	 * should be preprended to the documentation
	 * 
	 * @return true if documentation title (ie, xs:documentation, xs:appinfo)
	 * should be preprended to the documentation
	 */
	private boolean prependTitleCheck() {
		return this.documentation.size() > 0 && this.appinfo.size() > 0
				&& strategy == SchemaDocumentationType.all;
	}

	/**
	 * Converts each string in the list from HTML to plain text in-place.
	 *
	 * <p>Use case: for completion items (not hover), HTML tags like
	 * {@code <b>bold</b>} are stripped to just {@code "bold"}.</p>
	 */
	private static void convertToPlainText(List<String> list) {
		for (int i = 0; i < list.size(); i++) {
			list.set(i, PlainTextConverter.convert(list.get(i)));
		}
	}

	/**
	 * Formats a list of documentation or appinfo strings, optionally prepending
	 * a section title (e.g. {@code "xs:documentation:"}).
	 *
	 * <p>Use case: when both documentation and appinfo exist, each section
	 * is prefixed with its title to distinguish them in the hover tooltip.</p>
	 */
	private static String getFormatted(String prefix, String elementName, List<String> content,
			boolean prependTitles, boolean html) {
		StringBuilder result = new StringBuilder();
		if (prependTitles) {
			result.append(applyPrefix(prefix, elementName, html));
		}
		for (String doc: content) {
			if (!StringUtils.isBlank(doc)) {
				result.append(doc);
				result.append(System.lineSeparator());
				result.append(System.lineSeparator());
			}
		}
		return result.toString();
	}

	/**
	 * Builds the section title line. In HTML mode, the title is bolded.
	 *
	 * <p>Use case: {@code applyPrefix("xs", "documentation", true)}
	 * → {@code "**xs:documentation**:\n\n"}</p>
	 */
	private static String applyPrefix(String prefix, String documentation, boolean html) {
		StringBuilder result = new StringBuilder();
		if (html) {
			result.append("**");
		}
		if (!StringUtils.isEmpty(prefix)) {
			result.append(prefix).append(':');
		}
		result.append(documentation);
		if (html) {
			result.append("**");
		}
		result.append(":");
		result.append(System.lineSeparator());
		result.append(System.lineSeparator());
		return result.toString();
	}
}