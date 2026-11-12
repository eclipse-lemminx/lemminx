/**
 *  Copyright (c) 2018 Angelo ZERR
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
package org.eclipse.lemminx.extensions.xsd.contentmodel;

import static org.eclipse.lemminx.dom.parser.Constants.DOCUMENTATION_CONTENT;
import static org.eclipse.lemminx.utils.StringUtils.isEmpty;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.xerces.impl.dv.ValidatedInfo;
import org.apache.xerces.xs.XSAnnotation;
import org.apache.xerces.xs.XSMultiValueFacet;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.datatypes.ObjectList;
import org.eclipse.lemminx.converter.HtmlTag;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Extract xs:document & xs:appinfo from the xs:annotation.
 *
 */
class XSDAnnotationModel {

	private final List<String> appInfo;

	private final List<String> documentation;

	private XSDAnnotationModel() {
		this.appInfo = new ArrayList<>();
		this.documentation = new ArrayList<>();
	}

	/**
	 * Returns content from appinfo element(s)
	 * 
	 * @return content from appinfo element(s)
	 */
	public List<String> getAppInfo() {
		return appInfo;
	}

	/**
	 * Returns content from documentation elements(s)
	 * 
	 * @return content from documentation elements(s)
	 */
	public List<String> getDocumentation() {
		return documentation;
	}

	/**
	 * Returns documentation content from the provided collection of annotations
	 * 
	 * @param annotations the collection of annotations
	 * @return documentation content from the provided collection of annotations
	 */
	public static List<String> getDocumentation(XSObjectList annotations) {
		return getDocumentation(annotations, null);
	}

	/**
	 * Returns documentation content from the provided collection of annotations
	 * 
	 * @param annotations the collection of attribute value annotations
	 * @param value       the attribute value to find documentation content for
	 * @return documentation content from the provided collection of annotations
	 */
	public static List<String> getDocumentation(XSObjectList annotations, String value) {
		if (annotations == null) {
			return Collections.emptyList();
		}
		List<String> result = new ArrayList<>();
		for (Object object : annotations) {
			XSAnnotation annotation = getXSAnnotation((XSObject) object, value);
			if (annotation != null) {
				XSDAnnotationModel annotationModel = XSDAnnotationModel.load(annotation);
				if (annotationModel != null) {
					List<String> documentation = annotationModel.getDocumentation();
					if (documentation.size() > 0) {
						result.addAll(documentation);
					} else {
						String annotationString = annotation.getAnnotationString();
						if (!isEmpty(annotationString)) {
							String docFromPattern = getDocumentation(annotationString);
							if (!isEmpty(docFromPattern)) {
								result.add(docFromPattern);
							}
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Returns appinfo content from the provided collection of annotations
	 * 
	 * @param annotations the collection of annotations
	 * @return appinfo content from the provided collection of annotations
	 */
	public static List<String> getAppInfo(XSObjectList annotations) {
		return getAppInfo(annotations, null);
	}

	/**
	 * Returns appinfo content from the provided collection of annotations
	 * 
	 * @param annotations the collection of attribute value annotations
	 * @param value       the attribute value to find appinfo content for
	 * @return appinfo content from the provided collection of annotations
	 */
	public static List<String> getAppInfo(XSObjectList annotations, String value) {
		if (annotations == null) {
			return Collections.emptyList();
		}
		List<String> appinfo = new ArrayList<>();
		for (Object object : annotations) {
			XSAnnotation annotation = getXSAnnotation((XSObject) object, value);
			if (annotation != null) {
				XSDAnnotationModel annotationModel = XSDAnnotationModel.load(annotation);
				if (annotationModel != null) {
					appinfo.addAll(annotationModel.getAppInfo());
				}
			}
		}
		return appinfo;
	}

	/**
	 * Returns the prefix (ie. xs) from the provided collection of annotations
	 * 
	 * @param annotations the collection of annotations
	 * @return the prefix (ie. xs) from the provided collection of annotations
	 */
	public static String getPrefix(XSObjectList annotations) {
		return getPrefix(annotations, null);
	}

	/**
	 * Returns the prefix (ie. xs) from the provided collection of annotations
	 * 
	 * Prerequisite: <code>value</code> should be provided if
	 * <code>annotations</code> is a collection of attribute value annotations
	 * 
	 * @param annotations the collection of annotations
	 * @param value       the attribute value
	 * @return the prefix (ie. xs) from the provided collection of annotations
	 */
	public static String getPrefix(XSObjectList annotations, String value) {
		if (annotations == null) {
			return "";
		}
		for (Object object : annotations) {
			XSAnnotation annotation = getXSAnnotation((XSObject) object, value);
			if (annotation != null) {
				String content = annotation.getAnnotationString();
				int index = content.indexOf(":");
				if (index != -1) {
					return content.substring(1, index);
				}
			}
		}
		return "";
	}

	public static XSDAnnotationModel load(XSAnnotation annotation) {
		try {
			SAXParserFactory factory = DOMUtils.newSAXParserFactory();
			SAXParser saxParser = factory.newSAXParser();
			XSAnnotationHandler handler = new XSAnnotationHandler();
			saxParser.parse(new InputSource(new StringReader(annotation.getAnnotationString())), handler);
			return handler.getModel();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Returns the <code>XSAnnotation</code> instance for the provided
	 * <code>annotation</code>
	 * 
	 * If <code>value</code> is provided, the <code>XSAnnotation</code> for an
	 * attribute value will be searched for
	 * 
	 * If not provided, the <code>XSAnnotation</code> for an attribute or element
	 * will be searched for
	 * 
	 * @param annotation the annotation object
	 * @param value      the attribute value
	 * @return the <code>XSAnnotation</code> instance for the provided
	 *         <code>annotation</code>
	 */
	private static XSAnnotation getXSAnnotation(XSObject annotation, String value) {
		if (annotation instanceof XSMultiValueFacet && value != null) {
			XSMultiValueFacet multiValueFacet = (XSMultiValueFacet) annotation;
			ObjectList enumerationValues = multiValueFacet.getEnumerationValues();
			XSObjectList annotationValues = multiValueFacet.getAnnotations();
			for (int i = 0; i < enumerationValues.getLength(); i++) {
				Object enumValue = enumerationValues.get(i);

				// Assuming always ValidatedInfo
				String enumString = ((ValidatedInfo) enumValue).stringValue();

				if (value.equals(enumString)) {
					return (XSAnnotation) annotationValues.get(i);
				}
			}
		} else if (annotation instanceof XSAnnotation) {
			return (XSAnnotation) annotation;
		}
		return null;
	}

	private static class XSAnnotationHandler extends DefaultHandler {

		private static final String APPINFO_ELEMENT = "appinfo";
		private static final String DOCUMENTATION_ELEMENT = "documentation";

		private StringBuilder current;
		private final XSDAnnotationModel model;

		public XSAnnotationHandler() {
			model = new XSDAnnotationModel();
		}

		public XSDAnnotationModel getModel() {
			return model;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			if (qName.endsWith(DOCUMENTATION_ELEMENT) || qName.endsWith(APPINFO_ELEMENT)) {
				current = new StringBuilder();
			} else if (current != null) {
				current.append('<').append(qName);
				for (int i = 0; i < attributes.getLength(); i++) {
					current.append(' ').append(attributes.getQName(i))
							.append("=\"")
							.append(attributes.getValue(i).replace("\"", "&quot;"))
							.append('"');
				}
				current.append('>');
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			if (current != null) {
				if (qName.endsWith(APPINFO_ELEMENT)) {
					addIfNonEmptyString(model.appInfo, stripIndent(current.toString()));
					current = null;
				} else if (qName.endsWith(DOCUMENTATION_ELEMENT)) {
					addIfNonEmptyString(model.documentation, stripIndent(current.toString()));
					current = null;
				} else {
					HtmlTag tag = HtmlTag.of(qName.toLowerCase());
					if (tag == null || tag.getCategory() != HtmlTag.Category.VOID) {
						current.append("</").append(qName).append('>');
					}
				}
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (current != null) {
				current.append(ch, start, length);
			}
			super.characters(ch, start, length);
		}

		private static void addIfNonEmptyString(List<String> list, String str) {
			if (!StringUtils.isEmpty(str)) {
				list.add(str);
			}
		}

		/**
		 * Normalizes XSD documentation text: removes common indentation, then
		 * either preserves line structure (if HTML block tags are present) or
		 * joins consecutive prose lines with spaces.
		 *
		 * <p>Use cases:</p>
		 * <ul>
		 *   <li>Indented plain prose → indentation stripped, lines joined:
		 *       {@code "    line1\n    line2"} → {@code "line1 line2"}</li>
		 *   <li>HTML content ({@code <p>}, {@code <table>}) → indentation stripped,
		 *       line structure preserved for the HTML converter</li>
		 * </ul>
		 */
		private static String stripIndent(String text) {
			String stripped = stripCommonIndent(text);
			if (containsHtmlBlockTag(stripped)) {
				return stripped;
			}
			return joinProseLines(stripped);
		}

		/**
		 * Returns true if the text contains any HTML block-level tag (e.g.
		 * {@code <p>}, {@code <table>}, {@code <ul>}). When block tags are
		 * present, line structure must be preserved for the HTML converter
		 * rather than joining lines as prose.
		 *
		 * <p>Use cases:</p>
		 * <ul>
		 *   <li>{@code "<p>text</p>"} → true (p is a block element)</li>
		 *   <li>{@code "<b>bold</b>"} → false (b is inline, not block)</li>
		 *   <li>{@code "plain text"} → false (no tags at all)</li>
		 * </ul>
		 */
		private static boolean containsHtmlBlockTag(String text) {
			int len = text.length();
			for (int i = 0; i < len; i++) {
				if (text.charAt(i) == '<') {
					int nameStart = i + 1;
					if (nameStart < len && text.charAt(nameStart) == '/') {
						nameStart++;
					}
					int nameEnd = nameStart;
					while (nameEnd < len && Character.isLetterOrDigit(text.charAt(nameEnd))) {
						nameEnd++;
					}
					if (nameEnd > nameStart && nameEnd < len) {
						char after = text.charAt(nameEnd);
						if (after == '>' || after == ' ' || after == '/' || after == '\t'
								|| after == '\n' || after == '\r') {
							if (HtmlTag.isBlockElement(text, nameStart, nameEnd)) {
								return true;
							}
						}
					}
				}
			}
			return false;
		}

		/**
		 * Joins consecutive plain prose lines with spaces while preserving
		 * structural lines (code blocks, list items, headings, indented lines).
		 *
		 * <p>XSD documentation often wraps prose at arbitrary line widths.
		 * This method re-joins those lines so they render as a single paragraph
		 * in hover tooltips, while keeping Markdown structures intact.</p>
		 *
		 * <p>Use cases:</p>
		 * <ul>
		 *   <li>Prose wrapping: {@code "This is a\nlong sentence."} \u2192 {@code "This is a long sentence."}</li>
		 *   <li>Fenced code preserved: {@code "```\ncode\n```"} \u2192 unchanged</li>
		 *   <li>List items preserved: {@code "* item1\n* item2"} \u2192 unchanged</li>
		 *   <li>Indented lines converted to NBSP: {@code "  indented"} \u2192 {@code "\u00A0\u00A0indented"}</li>
		 * </ul>
		 */
		private static String joinProseLines(String text) {
			int len = text.length();
			StringBuilder result = new StringBuilder(len);
			int lineStart = 0;
			boolean prevWasProse = false;
			boolean inCodeBlock = false;

			for (int i = 0; i <= len; i++) {
				if (i == len || text.charAt(i) == '\n') {
					// Strip trailing \r for CRLF line endings
					int lineEnd = (i > lineStart && i > 0 && text.charAt(i - 1) == '\r') ? i - 1 : i;

					// Trim trailing whitespace from the line
					int trimmedEnd = lineEnd;
					while (trimmedEnd > lineStart
							&& (text.charAt(trimmedEnd - 1) == ' ' || text.charAt(trimmedEnd - 1) == '\t')) {
						trimmedEnd--;
					}

					// Toggle code block state on fence lines (``` or ~~~)
					boolean isFence = isFencedCodeDelimiter(text, lineStart, trimmedEnd);
					if (isFence) {
						inCodeBlock = !inCodeBlock;
					}

					if (inCodeBlock || isFence) {
						// Inside a fenced code block: preserve lines verbatim
						if (result.length() > 0) {
							result.append('\n');
						}
						result.append(text, lineStart, trimmedEnd);
						prevWasProse = false;
					} else if (lineStart >= trimmedEnd) {
						// Empty line: preserve as paragraph separator
						if (result.length() > 0) {
							result.append('\n');
						}
						prevWasProse = false;
					} else {
						char first = text.charAt(lineStart);
						boolean isIndented = first == ' ' || first == '\t';
						boolean isProse = !isIndented && !isMarkdownMarker(text, lineStart, trimmedEnd);

						if (isProse && prevWasProse) {
							// Two consecutive prose lines: join with space instead of newline
							result.append(' ');
						} else if (result.length() > 0) {
							result.append('\n');
						}
						if (isIndented) {
							// Indented line: check if it's an indented Markdown marker (e.g. nested list)
							int contentStart = lineStart;
							while (contentStart < trimmedEnd
									&& (text.charAt(contentStart) == ' ' || text.charAt(contentStart) == '\t')) {
								contentStart++;
							}
							if (contentStart < trimmedEnd
									&& isMarkdownMarker(text, contentStart, trimmedEnd)) {
								// Indented Markdown marker: keep original indentation
								result.append(text, lineStart, trimmedEnd);
							} else {
								// Indented content: replace spaces with NBSP so hover preserves indentation
								for (int j = lineStart; j < contentStart; j++) {
									result.append('\u00A0');
								}
								result.append(text, contentStart, trimmedEnd);
							}
						} else {
							result.append(text, lineStart, trimmedEnd);
						}
						prevWasProse = isProse;
					}
					lineStart = i + 1;
				}
			}
			return result.toString();
		}

		/**
		 * Returns true if the line is a Markdown fenced code delimiter ({@code ```} or {@code ~~~}).
		 *
		 * <p>Use case: {@code "```xml"} → true; {@code "``not enough"} → false.</p>
		 */
		private static boolean isFencedCodeDelimiter(String text, int start, int end) {
			if (end - start < 3) {
				return false;
			}
			char c = text.charAt(start);
			return (c == '`' || c == '~')
					&& text.charAt(start + 1) == c && text.charAt(start + 2) == c;
		}

		/**
		 * Returns true if the line starts with a Markdown structural marker
		 * (list bullet, heading, pipe table, blockquote, ordered list number,
		 * or fenced code delimiter).
		 *
		 * <p>Use cases:</p>
		 * <ul>
		 *   <li>{@code "* item"} → true (unordered list)</li>
		 *   <li>{@code "# heading"} → true</li>
		 *   <li>{@code "| cell |"} → true (table row)</li>
		 *   <li>{@code "1. first"} → true (ordered list)</li>
		 *   <li>{@code "plain text"} → false</li>
		 * </ul>
		 */
		private static boolean isMarkdownMarker(String text, int start, int end) {
			char c = text.charAt(start);
			if ((c == '*' || c == '-' || c == '+') && start + 1 < end && text.charAt(start + 1) == ' ') {
				return true;
			}
			if (c == '|' || c == '#' || c == '>') {
				return true;
			}
			if ((c == '`' || c == '~') && start + 2 < end
					&& text.charAt(start + 1) == c && text.charAt(start + 2) == c) {
				return true;
			}
			if (c >= '0' && c <= '9') {
				int j = start + 1;
				while (j < end && text.charAt(j) >= '0' && text.charAt(j) <= '9') {
					j++;
				}
				if (j < end && text.charAt(j) == '.' && j + 1 < end && text.charAt(j + 1) == ' ') {
					return true;
				}
			}
			return false;
		}

		/**
		 * Removes the common leading whitespace from all non-blank lines.
		 * XSD documentation is typically indented to match the XML structure;
		 * this removes that structural indentation while preserving relative
		 * indentation between lines.
		 *
		 * <p>Use case: {@code "    line1\n      line2\n    line3"} with
		 * minIndent=4 → {@code "line1\n  line2\nline3"}</p>
		 */
		private static String stripCommonIndent(String text) {
			int len = text.length();
			int minIndent = Integer.MAX_VALUE;
			int lineStart = 0;
			for (int i = 0; i <= len; i++) {
				if (i == len || text.charAt(i) == '\n') {
					int lineEnd = (i > lineStart && text.charAt(i - 1) == '\r') ? i - 1 : i;
					int indent = 0;
					for (int j = lineStart; j < lineEnd; j++) {
						char c = text.charAt(j);
						if (c == ' ' || c == '\t') {
							indent++;
						} else {
							break;
						}
					}
					if (lineStart + indent < lineEnd && indent < minIndent) {
						minIndent = indent;
					}
					lineStart = i + 1;
				}
			}
			if (minIndent == Integer.MAX_VALUE || minIndent == 0) {
				return stripSurroundingBlankLines(text);
			}
			StringBuilder result = new StringBuilder();
			lineStart = 0;
			for (int i = 0; i <= len; i++) {
				if (i == len || text.charAt(i) == '\n') {
					int lineEnd = (i > lineStart && text.charAt(i - 1) == '\r') ? i - 1 : i;
					if (result.length() > 0) {
						result.append('\n');
					}
					if (lineStart + minIndent <= lineEnd) {
						result.append(text, lineStart + minIndent, lineEnd);
					}
					lineStart = i + 1;
				}
			}
			return stripSurroundingBlankLines(result.toString());
		}

		/**
		 * Removes leading and trailing blank lines (lines containing only
		 * whitespace). Interior blank lines are preserved. Does not trim
		 * indentation from content lines.
		 *
		 * <p>Use case: {@code "\n\n  content\n\n"} → {@code "  content"}</p>
		 */
		private static String stripSurroundingBlankLines(String text) {
			int len = text.length();
			// Skip leading blank lines (lines containing only spaces/tabs)
			int start = 0;
			for (int i = 0; i < len; i++) {
				char c = text.charAt(i);
				if (c == '\n') {
					start = i + 1;
				} else if (c == '\r') {
					start = i + 1;
				} else if (c != ' ' && c != '\t') {
					break;
				}
			}
			// Skip trailing blank lines
			int end = len;
			for (int i = len - 1; i >= start; i--) {
				char c = text.charAt(i);
				if (c == '\n' || c == '\r') {
					end = i;
				} else if (c != ' ' && c != '\t') {
					break;
				}
			}
			if (start >= end) {
				return "";
			}
			return text.substring(start, end);
		}

	}

	public static String getDocumentation(String xml) {
		Matcher m = DOCUMENTATION_CONTENT.matcher(xml);
		if (m.find()) {
			return m.group(1);
		}
		return null;
	}

}
