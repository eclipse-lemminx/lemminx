/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.relaxng.xml.validator;

import org.apache.xerces.impl.XMLErrorReporter;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLParseException;
import org.eclipse.lemminx.extensions.relaxng.RelaxNGConstants;

/**
 * {@link XMLErrorHandler} Xerces implementation for RelaxNG.
 *
 * <p>
 * Jing provides no structured error codes—only English message strings via
 * {@code SAXParseException}. This handler matches those messages by
 * prefix/suffix/contains patterns and maps them to {@link RelaxNGErrorCode}
 * enum values for precise diagnostic ranges and code actions.
 * </p>
 *
 * @author Angelo ZERR
 *
 * @see RelaxNGErrorCode
 */
public class RelaxNGErrorHandler implements XMLErrorHandler {

	private XMLErrorReporter report;

	public RelaxNGErrorHandler(XMLErrorReporter report) {
		this.report = report;
	}

	private static class RelaxNGReportInfo {

		public final RelaxNGErrorCode errorCode;

		public final Object[] arguments;

		public RelaxNGReportInfo(RelaxNGErrorCode errorCode, Object... arguments) {
			this.errorCode = errorCode;
			this.arguments = arguments;
		}
	}

	@Override
	public void warning(String domain, String key, XMLParseException exception) throws XNIException {
		reportError(domain, key, XMLErrorReporter.SEVERITY_WARNING, exception);
	}

	@Override
	public void fatalError(String domain, String key, XMLParseException exception) throws XNIException {
		reportError(domain, key, XMLErrorReporter.SEVERITY_FATAL_ERROR, exception);
	}

	@Override
	public void error(String domain, String key, XMLParseException exception) throws XNIException {
		reportError(domain, key, XMLErrorReporter.SEVERITY_ERROR, exception);
	}

	private void reportError(String domain, String key, short severity, XMLParseException exception) {
		if (domain.isEmpty() && key.isEmpty()) {
			// RelaxNG
			String message = exception.getMessage();
			if (!message.isEmpty()) {
				RelaxNGReportInfo info = getRelaxNGReportInfo(message);
				report.reportError(RelaxNGConstants.RELAX_NG_DOMAIN, info.errorCode.getCode(), info.arguments, severity,
						exception);
				return;
			}
		}
		report.reportError(domain, key, null, severity, exception);
	}

	private static RelaxNGReportInfo getRelaxNGReportInfo(String message) {
		// Elements
		if (message.startsWith("element")) {
			if (message.contains(" not allowed anywhere")) {
				// unknown_element=element {0} not allowed anywhere{1}
				return new RelaxNGReportInfo(RelaxNGErrorCode.unknown_element);
			}
			if (message.contains("not allowed yet; missing required element ")) {
				// unexpected_element_required_element_missing=element {0} not allowed yet;
				// missing required element {1}
				return new RelaxNGReportInfo(RelaxNGErrorCode.unexpected_element_required_element_missing);
			}
			if (message.contains("incomplete; missing required elements")) {
				// incomplete_element_required_elements_missing=element {0} incomplete; missing
				// required elements {1}
				return new RelaxNGReportInfo(RelaxNGErrorCode.incomplete_element_required_elements_missing);
			}
			if (message.contains("incomplete; missing required element")) {
				// incomplete_element_required_element_missing=element {0} incomplete; missing
				// required element {1}
				return new RelaxNGReportInfo(RelaxNGErrorCode.incomplete_element_required_element_missing);
			}
			if (message.contains("incomplete")) {
				// incomplete_element_required_elements_missing_expected=element {0}
				// incomplete{1}
				return new RelaxNGReportInfo(RelaxNGErrorCode.incomplete_element_required_elements_missing_expected);
			}
			if (message.contains("not allowed here")) {
				// out_of_context_element=element {0} not allowed here{1}
				return new RelaxNGReportInfo(RelaxNGErrorCode.out_of_context_element);
			}
			if (message.contains("not allowed yet")) {
				// element {0} not allowed yet{1}
				return new RelaxNGReportInfo(RelaxNGErrorCode.element_not_allowed_yet);
			}

			// Required attributes

			if (message.contains("missing one or more required attributes")) {
				// required_attributes_missing_expected=element {0} missing one or more required
				// attributes{1}
				return new RelaxNGReportInfo(RelaxNGErrorCode.required_attributes_missing_expected);
			}
			if (message.contains("missing required attributes ")) {
				// required_attributes_missing=element {0} missing required attributes {1}
				return new RelaxNGReportInfo(RelaxNGErrorCode.required_attributes_missing);
			}
			if (message.contains("missing required attribute ")) {
				// required_attribute_missing=element {0} missing required attribute {1}
				return new RelaxNGReportInfo(RelaxNGErrorCode.required_attribute_missing);
			}

		}

		// Attributes
		if (message.startsWith("found attribute")) {
			// no_attributes_allowed=found attribute {0}, but no attributes allowed here
			String attrName = extractArg("found attribute", message);
			return new RelaxNGReportInfo(RelaxNGErrorCode.no_attributes_allowed, attrName);
		}
		if (message.startsWith("attribute")) {
			if (message.contains(" not allowed here")) {
				// invalid_attribute_name=attribute {0} not allowed here{1}
				// ex : attribute "bad1" not allowed here...
				String attrName = extractArg("attribute", message);
				return new RelaxNGReportInfo(RelaxNGErrorCode.invalid_attribute_name, attrName);
			}
		}
		if (message.startsWith("value of attribute ")) {
			// invalid_attribute_value=value of attribute {0} is invalid{1}
			// ex : value of attribute "bad1" not allowed here...
			String attrName = extractArg("value of attribute ", message);
			return new RelaxNGReportInfo(RelaxNGErrorCode.invalid_attribute_value, attrName);
		}

		// RNG / RNC errors

		if (message.startsWith("multiple definitions of start")) {
			// duplicate_start=multiple definitions of start without \"combine\" attribute
			return new RelaxNGReportInfo(RelaxNGErrorCode.duplicate_start);
		}
		if (message.startsWith("multiple definitions of")) {
			// duplicate_define=multiple definitions of \"{0}\" without \"combine\"
			// attribute
			String defineName = extractArg("multiple definitions of ", message);
			return new RelaxNGReportInfo(RelaxNGErrorCode.duplicate_define, defineName);
		}
		if (message.contains("from library") && message.endsWith("not recognized")) {
			// unrecognized_datatype=datatype \"{1}\" from library \"{0}\" not recognized
			return new RelaxNGReportInfo(RelaxNGErrorCode.unrecognized_datatype);
		}

		// RNG errors
		if (message.equals("missing \"start\" element")) {
			// missing_start_element=missing \"start\" element
			return new RelaxNGReportInfo(RelaxNGErrorCode.missing_start_element);
		}
		if (message.startsWith("reference to undefined pattern")) {
			// reference_to_undefined=reference to undefined pattern \"{0}\"
			String patternName = extractArg("reference to undefined pattern ", message);
			return new RelaxNGReportInfo(RelaxNGErrorCode.reference_to_undefined, patternName);
		}

		if (message.startsWith("found ") && message.contains("element but expected")) {
			String tagName = extractArg("found ", message);
			if (message.contains("expected \"define\" or \"start\" or \"include\" or \"div\" element")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.expected_define, tagName);
			}
			if (message.contains("expected empty content")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.expected_empty, tagName);
			}
			if (message.contains("expected \"except\"")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.expected_except, tagName);
			}
			if (message.contains("expected \"grammar\" element")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.expected_grammar, tagName);
			}
			if (message.contains("expected a name-class")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.expected_name_class, tagName);
			}
			if (message.contains("expected a name")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.expected_name, tagName);
			}
			if (message.contains("expected no further content")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.expected_one_name_class, tagName);
			}
			if (message.contains("expected a pattern")) {
				// expected_pattern=found \"{0}\" element but expected a pattern
				return new RelaxNGReportInfo(RelaxNGErrorCode.expected_pattern, tagName);
			}
		}
		if (message.startsWith("illegal attribute ")) {
			// illegal_attribute_ignored=illegal attribute \"{0}\" ignored
			String attrName = extractArg("illegal attribute ", message);
			return new RelaxNGReportInfo(RelaxNGErrorCode.illegal_attribute_ignored, attrName);
		}
		if (message.equals("illegal \"name\" attribute")) {
			// illegal_name_attribute=illegal \"name\" attribute
			return new RelaxNGReportInfo(RelaxNGErrorCode.illegal_name_attribute);
		}
		if (message.endsWith("is not a valid local name")) {
			// invalid_ncname=\"{0}\" is not a valid local name
			return new RelaxNGReportInfo(RelaxNGErrorCode.invalid_ncname);
		}
		if (message.endsWith("missing children")) {
			// missing_children=missing children
			return new RelaxNGReportInfo(RelaxNGErrorCode.missing_children);
		}
		if (message.equals("missing \"name\" attribute")) {
			// missing_name_attribute=missing \"name\" attribute
			return new RelaxNGReportInfo(RelaxNGErrorCode.missing_name_attribute);
		}
		if (message.endsWith("expected child element specifying name class")) {
			// missing_name_class=expected child element specifying name class
			return new RelaxNGReportInfo(RelaxNGErrorCode.missing_name_class);
		}
		if (message.equals("missing \"type\" attribute")) {
			// missing_type_attribute=missing \"type\" attribute
			return new RelaxNGReportInfo(RelaxNGErrorCode.missing_type_attribute);
		}
		if (message.equals("missing \"href\" attribute")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.missing_href_attribute);
		}

		// RNG SAX parse errors - exact and prefix matches
		if (message.equals("\"attribute\" pattern must contain at most one pattern")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.attribute_multi_pattern);
		}
		if (message.equals("\"start\" pattern must contain at most one pattern")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.start_multi_pattern);
		}
		if (message.equals("only \"param\" and \"except\" child elements are allowed")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.expected_param_except);
		}
		if (message.startsWith("at most one \"except\" child element")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.multiple_except);
		}
		if (message.startsWith("too many child elements")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.too_many_children);
		}
		if (message.equals("illegal characters ignored")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.illegal_characters_ignored);
		}
		if (message.equals("\"name\" element cannot contain foreign elements")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.name_contains_foreign_element);
		}
		if (message.equals("\"param\" is not allowed after \"except\"")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.param_after_except);
		}
		if (message.equals("\"param\" element cannot contain foreign elements")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.param_contains_foreign_element);
		}
		if (message.equals("\"value\" element cannot contain foreign elements")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.value_contains_foreign_element);
		}
		if (message.startsWith("bad value") && message.contains("for \"combine\" attribute")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.combine_attribute_bad_value);
		}
		if (message.startsWith("undefined prefix")) {
			String prefix = extractArg("undefined prefix ", message);
			return new RelaxNGReportInfo(RelaxNGErrorCode.undefined_prefix, prefix);
		}
		if (message.startsWith("attribute name") && message.contains("incorrectly qualified")) {
			String attrName = extractArg("attribute name ", message);
			return new RelaxNGReportInfo(RelaxNGErrorCode.qualified_attribute, attrName);
		}
		if (message.startsWith("namespace URI of document element must be")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.root_bad_namespace_uri);
		}
		if (message.startsWith("namespace URI has wrong version")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.wrong_uri_version);
		}
		if (message.startsWith("URI in value of \"datatypeLibrary\" attribute must not have a fragment")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.fragment_identifier_datatype_library);
		}
		if (message.startsWith("fragment identifier ignored in value of \"href\"")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.href_fragment_id);
		}
		if (message.startsWith("URI in value of \"datatypeLibrary\" attribute must not be relative")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.relative_datatype_library);
		}
		if (message.endsWith("is not a valid URI reference according to RFC 2396")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.invalid_uri);
		}

		// "except" in ... contains ...
		if (message.startsWith("\"except\" in")) {
			if (message.contains("\"anyName\" contains \"anyName\"")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.any_name_except_contains_any_name);
			}
			if (message.contains("\"nsName\" contains \"anyName\"")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.ns_name_except_contains_any_name);
			}
			if (message.contains("\"nsName\" contains \"nsName\"")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.ns_name_except_contains_ns_name);
			}
		}

		// Pattern-level schema errors
		if (message.startsWith("bad recursive reference to pattern")) {
			String patternName = extractArg("bad recursive reference to pattern ", message);
			return new RelaxNGReportInfo(RelaxNGErrorCode.recursive_reference, patternName);
		}
		if (message.startsWith("recursive inclusion of URL")) {
			String url = extractArg("recursive inclusion of URL ", message);
			return new RelaxNGReportInfo(RelaxNGErrorCode.recursive_include, url);
		}
		if (message.startsWith("conflicting values of \"combine\" attribute for definition of start")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.conflict_combine_start);
		}
		if (message.startsWith("conflicting values of \"combine\" attribute for definition of")) {
			String defineName = extractArg("for definition of ", message);
			return new RelaxNGReportInfo(RelaxNGErrorCode.conflict_combine_define, defineName);
		}
		if (message.equals("\"start\" in \"include\" does not override anything")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.missing_start_replacement);
		}
		if (message.startsWith("definition of") && message.contains("in \"include\" does not override anything")) {
			String defineName = extractArg("definition of ", message);
			return new RelaxNGReportInfo(RelaxNGErrorCode.missing_define_replacement, defineName);
		}
		if (message.startsWith("interleave of \"string\" or \"data\"")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.interleave_string);
		}
		if (message.startsWith("group of \"string\" or \"data\"")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.group_string);
		}
		if (message.startsWith("repeat of \"string\" or \"data\"")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.one_or_more_string);
		}
		if (message.startsWith("datatype library") && message.endsWith("not recognized")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.unrecognized_datatype_library);
		}
		if (message.startsWith("no such builtin datatype")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.unrecognized_builtin_datatype);
		}
		if (message.endsWith("is not a valid value of the datatype")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.invalid_value);
		}
		if (message.equals("reference to non-existent parent grammar")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.parent_ref_outside_grammar);
		}
		if (message.equals("reference to non-existent grammar")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.ref_outside_grammar);
		}
		if (message.contains("builtin datatypes do not have any parameters")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.builtin_param);
		}
		if (message.startsWith("datatype cannot be used without parameters")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.datatype_requires_parameter);
		}
		if (message.startsWith("duplicate attribute")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.duplicate_attribute);
		}
		if (message.startsWith("overlapping element names")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.interleave_element_overlap);
		}
		if (message.equals("both operands of \"interleave\" contain \"text\"")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.interleave_text_overlap);
		}
		if (message.startsWith("attribute using \"nsName\" or \"anyName\" must be in \"oneOrMore\"")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.open_name_class_not_repeated);
		}
		if (message.equals("attribute must not be named \"xmlns\"")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.xmlns_attribute);
		}
		if (message.startsWith("attribute must not have namespace URI")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.xmlns_uri_attribute);
		}

		// Section 7.1 restriction violations
		if (message.startsWith("an attribute pattern must not contain")) {
			if (message.contains("contain an attribute")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.attribute_contains_attribute);
			}
			if (message.contains("contain an element")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.attribute_contains_element);
			}
		}
		if (message.startsWith("a data pattern must not exclude")) {
			if (message.contains("an attribute")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.data_except_contains_attribute);
			}
			if (message.contains("an element")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.data_except_contains_element);
			}
			if (message.contains("an empty")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.data_except_contains_empty);
			}
			if (message.contains("a group")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.data_except_contains_group);
			}
			if (message.contains("an interleaved")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.data_except_contains_interleave);
			}
			if (message.contains("a list")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.data_except_contains_list);
			}
			if (message.contains("a repetition")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.data_except_contains_one_or_more);
			}
			if (message.contains("a text")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.data_except_contains_text);
			}
		}
		if (message.startsWith("a list pattern must not contain")) {
			if (message.contains("contain an attribute")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.list_contains_attribute);
			}
			if (message.contains("contain an element")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.list_contains_element);
			}
			if (message.contains("contain a list")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.list_contains_list);
			}
			if (message.contains("contain a text")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.list_contains_text);
			}
			if (message.contains("contain an interleave")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.list_contains_interleave);
			}
		}
		if (message.startsWith("a group of attributes must not be repeatable")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.one_or_more_contains_group_contains_attribute);
		}
		if (message.startsWith("an interleaved group of attributes must not be repeatable")) {
			return new RelaxNGReportInfo(RelaxNGErrorCode.one_or_more_contains_interleave_contains_attribute);
		}
		if (message.startsWith("found element matching the prohibited path start//")) {
			if (message.contains("start//attribute")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.start_contains_attribute);
			}
			if (message.contains("start//data")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.start_contains_data);
			}
			if (message.contains("start//empty")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.start_contains_empty);
			}
			if (message.contains("start//group")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.start_contains_group);
			}
			if (message.contains("start//interleave")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.start_contains_interleave);
			}
			if (message.contains("start//list")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.start_contains_list);
			}
			if (message.contains("start//oneOrMore")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.start_contains_one_or_more);
			}
			if (message.contains("start//text")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.start_contains_text);
			}
			if (message.contains("start//value")) {
				return new RelaxNGReportInfo(RelaxNGErrorCode.start_contains_value);
			}
		}

		return new RelaxNGReportInfo(RelaxNGErrorCode.to_implement);
	}

	private static String extractArg(String from, String message) {
		int fromIndex = message.indexOf(from) + from.length();
		StringBuilder arg = null;
		for (int i = fromIndex; i < message.length(); i++) {
			char c = message.charAt(i);
			if (c == '"') {
				if (arg == null) {
					arg = new StringBuilder();
				} else {
					break;
				}
			} else {
				if (arg != null) {
					arg.append(c);
				}
			}
		}
		return arg != null ? arg.toString() : null;
	}
}
