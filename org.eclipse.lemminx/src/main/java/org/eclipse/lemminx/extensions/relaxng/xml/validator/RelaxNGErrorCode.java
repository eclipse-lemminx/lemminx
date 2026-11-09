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

import java.util.HashMap;
import java.util.Map;

import org.apache.xerces.xni.XMLLocator;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMRange;
import org.eclipse.lemminx.dom.DOMText;
import org.eclipse.lemminx.extensions.contentmodel.participants.XMLModelUtils;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.RemoveAttributeCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.invalid_attribute_nameCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.invalid_attribute_valueCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.no_attributes_allowedCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.out_of_context_elementCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.unknown_elementCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.missingelement.required_element_missingCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.required_attribute_missingCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.missingelement.required_elements_missing_expectedCodeAction;
import org.eclipse.lemminx.services.extensions.codeaction.InsertAttributeCodeAction;
import org.eclipse.lemminx.extensions.relaxng.grammar.rng.codeaction.RNGReplaceDatatypeCodeAction;
import org.eclipse.lemminx.extensions.relaxng.grammar.rng.codeaction.any_name_except_contains_any_nameCodeAction;
import org.eclipse.lemminx.extensions.relaxng.grammar.rng.codeaction.expected_defineCodeAction;
import org.eclipse.lemminx.extensions.relaxng.grammar.rng.codeaction.expected_patternCodeAction;
import org.eclipse.lemminx.extensions.relaxng.grammar.rng.codeaction.ns_name_except_contains_any_nameCodeAction;
import org.eclipse.lemminx.extensions.relaxng.utils.RelaxNGUtils;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.diagnostics.IXMLErrorCode;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.Range;

/**
 * RelaxNG error code when XML is validated with *.rng / *.rnc schemas.
 * 
 * @author Angelo ZERR
 * 
 * @see <a href=
 *      "https://github.com/relaxng/jing-trang/blob/master/mod/pattern/src/main/com/thaiopensource/relaxng/pattern/resources/Messages.properties">https://github.com/relaxng/jing-trang/blob/master/mod/pattern/src/main/com/thaiopensource/relaxng/pattern/resources/Messages.properties</a>
 * @see <a href=
 *      "https://github.com/relaxng/jing-trang/blob/master/mod/rng-parse/src/main/com/thaiopensource/relaxng/parse/sax/resources/Messages.properties">https://github.com/relaxng/jing-trang/blob/master/mod/rng-parse/src/main/com/thaiopensource/relaxng/parse/sax/resources/Messages.properties</a>
 *
 */
public enum RelaxNGErrorCode implements IXMLErrorCode {

	// XML Validation based on RNG, RNC errors

	unknown_element, // element {0} not allowed anywhere{1}
	unexpected_element_required_element_missing, // , element {0} not allowed yet; missing required element {1}
	unexpected_element_required_elements_missing, // element {0} not allowed yet; missing required elements {1}
	element_not_allowed_yet, // element {0} not allowed yet{1}
	out_of_context_element, // element {0} not allowed here{1}
	no_attributes_allowed, // found attribute {0}, but no attributes allowed here
	invalid_attribute_name, // attribute {0} not allowed here{1}
	invalid_attribute_value, // value of attribute {0} is invalid{1}
	required_attributes_missing_expected, // element {0} missing one or more required attributes{1}
	required_attribute_missing, // element {0} missing required attribute {1}
	required_attributes_missing, // element {0} missing required attributes {1}
	incomplete_element_required_elements_missing_expected, // element {0} incomplete{1}
	incomplete_element_required_element_missing, // element {0} incomplete; missing required element {1}
	incomplete_element_required_elements_missing, // element {0} incomplete; missing required elements {1}
	text_not_allowed, // text not allowed here{0}
	document_incomplete, // document incompletely matched
	invalid_element_value, // character content of element {0} invalid{1}
	blank_not_allowed, // empty content for element {0} not allowed{1}
	schema_allows_nothing, // schema does not allow anything: it is equivalent to <notAllowed/>

	RelaxNGNotFound, //

	// RNG / RNC errors
	missing_start_element, // missing \"start\" element
	reference_to_undefined, // reference to undefined pattern \"{0}\"
	duplicate_define, // multiple definitions of \"{0}\" without \"combine\" attribute
	duplicate_start, // multiple definitions of start without \"combine\" attribute"
	unrecognized_datatype, // datatype \"{1}\" from library \"{0}\" not recognized
	// RNG error, see
	// https://github.com/relaxng/jing-trang/blob/master/mod/rng-parse/src/main/com/thaiopensource/relaxng/parse/sax/resources/Messages.properties
	expected_pattern, // found \"{0}\" element but expected a pattern
	illegal_attribute_ignored, // illegal attribute \"{0}\" ignored
	illegal_name_attribute, // illegal \"name\" attribute
	invalid_ncname, // \"{0}\" is not a valid local name
	missing_children, // missing children
	missing_name_attribute, // missing \"name\" attribute
	missing_name_class, // expected child element specifying name class
	missing_type_attribute, // missing \"type\" attribute

	// RNG SAX parse errors
	// @see com/thaiopensource/relaxng/parse/sax/resources/Messages.properties
	any_name_except_contains_any_name, // \"except\" in \"anyName\" contains \"anyName\"
	attribute_multi_pattern, // \"attribute\" pattern must contain at most one pattern
	combine_attribute_bad_value, // bad value \"{0}\" for \"combine\" attribute
	expected_define, // found \"{0}\" element but expected \"define\" or \"start\" or \"include\" or \"div\" element
	expected_empty, // found \"{0}\" element but expected empty content
	expected_except, // found \"{0}\" element but expected \"except\"
	expected_grammar, // found \"{0}\" element but expected \"grammar\" element
	expected_name, // found \"{0}\" element but expected a name
	expected_name_class, // found \"{0}\" element but expected a name-class
	expected_param_except, // only \"param\" and \"except\" child elements are allowed
	fragment_identifier_datatype_library, // URI in value of \"datatypeLibrary\" attribute must not have a fragment identifier
	href_fragment_id, // fragment identifier ignored in value of \"href\" attribute
	illegal_characters_ignored, // illegal characters ignored
	invalid_uri, // \"{0}\" is not a valid URI reference according to RFC 2396
	missing_href_attribute, // missing \"href\" attribute
	multiple_except, // at most one \"except\" child element is allowed
	name_contains_foreign_element, // \"name\" element cannot contain foreign elements
	ns_name_except_contains_any_name, // \"except\" in \"nsName\" contains \"anyName\"
	ns_name_except_contains_ns_name, // \"except\" in \"nsName\" contains \"nsName\"
	param_after_except, // \"param\" is not allowed after \"except\"
	param_contains_foreign_element, // \"param\" element cannot contain foreign elements
	qualified_attribute, // attribute name \"{0}\" incorrectly qualified with RELAX NG namespace URI
	relative_datatype_library, // URI in value of \"datatypeLibrary\" attribute must not be relative
	root_bad_namespace_uri, // namespace URI of document element must be \"{0}\"
	start_multi_pattern, // \"start\" pattern must contain at most one pattern
	too_many_children, // too many child elements: only one child element is allowed
	undefined_prefix, // undefined prefix \"{0}\"
	value_contains_foreign_element, // \"value\" element cannot contain foreign elements
	wrong_uri_version, // namespace URI has wrong version

	// Pattern-level schema errors
	// @see com/thaiopensource/relaxng/pattern/resources/Messages.properties
	recursive_reference, // bad recursive reference to pattern \"{0}\"
	recursive_include, // recursive inclusion of URL \"{0}\"
	conflict_combine_define, // conflicting values of \"combine\" attribute for definition of \"{0}\"
	conflict_combine_start, // conflicting values of \"combine\" attribute for definition of start
	missing_start_replacement, // \"start\" in \"include\" does not override anything
	missing_define_replacement, // definition of \"{0}\" in \"include\" does not override anything
	interleave_string, // interleave of \"string\" or \"data\" element
	group_string, // group of \"string\" or \"data\" element
	one_or_more_string, // repeat of \"string\" or \"data\" element
	unrecognized_datatype_library, // datatype library \"{0}\" not recognized
	unrecognized_builtin_datatype, // no such builtin datatype \"{0}\"
	invalid_value, // \"{0}\" is not a valid value of the datatype
	parent_ref_outside_grammar, // reference to non-existent parent grammar
	ref_outside_grammar, // reference to non-existent grammar
	expected_one_name_class, // found \"{0}\" element but expected no further content
	builtin_param, // builtin datatypes do not have any parameters
	datatype_requires_parameter, // datatype cannot be used without parameters

	// Section 7.1 restriction violations
	attribute_contains_attribute, // an attribute pattern must not contain an attribute pattern
	attribute_contains_element, // an attribute pattern must not contain an element pattern
	data_except_contains_attribute, // a data pattern must not exclude an attribute pattern
	data_except_contains_element, // a data pattern must not exclude an element pattern
	data_except_contains_empty, // a data pattern must not exclude an empty pattern
	data_except_contains_group, // a data pattern must not exclude a group
	data_except_contains_interleave, // a data pattern must not exclude an interleaved group
	data_except_contains_list, // a data pattern must not exclude a list pattern
	data_except_contains_one_or_more, // a data pattern must not exclude a repetition
	data_except_contains_text, // a data pattern must not exclude a text pattern
	list_contains_attribute, // a list pattern must not contain an attribute pattern
	list_contains_element, // a list pattern must not contain an element pattern
	list_contains_list, // a list pattern must not contain a list pattern
	list_contains_text, // a list pattern must not contain a text pattern
	list_contains_interleave, // a list pattern must not contain an interleave pattern
	one_or_more_contains_group_contains_attribute, // a group of attributes must not be repeatable
	one_or_more_contains_interleave_contains_attribute, // an interleaved group of attributes must not be repeatable
	start_contains_attribute, // start//attribute prohibited
	start_contains_data, // start//data prohibited
	start_contains_empty, // start//empty prohibited
	start_contains_group, // start//group prohibited
	start_contains_interleave, // start//interleave prohibited
	start_contains_list, // start//list prohibited
	start_contains_one_or_more, // start//oneOrMore prohibited
	start_contains_text, // start//text prohibited
	start_contains_value, // start//value prohibited
	duplicate_attribute, // duplicate attribute
	interleave_element_overlap, // overlapping element names in operands of \"interleave\"
	interleave_text_overlap, // both operands of \"interleave\" contain \"text\"
	open_name_class_not_repeated, // attribute using \"nsName\" or \"anyName\" must be in \"oneOrMore\"
	xmlns_uri_attribute, // attribute must not have namespace URI xmlns
	xmlns_attribute, // attribute must not be named \"xmlns\"

	to_implement;

	private final String code;

	private RelaxNGErrorCode() {
		this(null);
	}

	private RelaxNGErrorCode(String code) {
		this.code = code;
	}

	@Override
	public String getCode() {
		if (code == null) {
			return name();
		}
		return code;
	}

	private final static Map<String, RelaxNGErrorCode> codes;

	static {
		codes = new HashMap<>();
		for (RelaxNGErrorCode errorCode : values()) {
			codes.put(errorCode.getCode(), errorCode);
		}
	}

	public static RelaxNGErrorCode get(String name) {
		return codes.get(name);
	}

	public static Range toLSPRange(XMLLocator location, RelaxNGErrorCode rngCode, Object[] arguments,
			DOMDocument document) {
		int offset = location.getCharacterOffset() - 1;
		// adjust positions
		switch (rngCode) {

			// XML Validation based on RNG, RNC errors
			case unknown_element:
			case out_of_context_element:
			case element_not_allowed_yet:
			case incomplete_element_required_elements_missing_expected:
			case unexpected_element_required_element_missing:
			case incomplete_element_required_element_missing:
			case required_attributes_missing_expected:
			case required_attributes_missing:
			case required_attribute_missing: {
				return XMLPositionUtility.selectStartTagName(offset, document);
			}
			case no_attributes_allowed:
			case invalid_attribute_name: {
				String attrName = (String) arguments[0];
				return XMLPositionUtility.selectAttributeNameFromGivenNameAt(attrName, offset, document);
			}
			case invalid_attribute_value: {
				String attrName = (String) arguments[0];
				return XMLPositionUtility.selectAttributeValueAt(attrName, offset, document);
			}
			case RelaxNGNotFound: {
				// Check if RelaxNG location is declared with xml-model/@href
				// ex : <xml-model href=""addressBook.rnc" [
				String hrefLocation = (String) arguments[1];
				DOMRange locationRange = XMLModelUtils.getHrefNode(document, hrefLocation);
				if (locationRange != null) {
					return XMLPositionUtility.createRange(locationRange);
				}
			}

			// RNG + RNC errors
			case missing_start_element: {
				DOMNode node = document.findNodeAt(offset);
				if (node != null) {
					if (node.isElement() && RelaxNGUtils.GRAMMAR_TAG.equals(((DOMElement) node).getLocalName())) {
						return XMLPositionUtility.selectStartTagName((DOMElement) node);
					}
				}
			}
			case reference_to_undefined: {
				String refName = (String) arguments[0];
				// Find the first ref which have the same attribute name than refName
				DOMAttr refAttrName = findRefByName(document, refName);
				if (refAttrName != null) {
					return XMLPositionUtility.selectAttributeValue(refAttrName);
				}
			}
			case duplicate_define: {
				String attrValue = (String) arguments[0];
				return XMLPositionUtility.selectAttributeValueFromGivenValue(attrValue, offset, document);
			}

			case duplicate_start: {

			}
			case unrecognized_datatype: {
				// <element name="email">
				// <data type="stringXXX" />
				// </element>
				DOMNode node = document.findNodeAt(offset);
				if (node != null && node.isElement()) {
					DOMElement data = (DOMElement) node;
					DOMAttr attr = data.getAttributeNode(RelaxNGUtils.TYPE_ATTR);
					if (attr != null) {
						return XMLPositionUtility.selectAttributeValue(attr);
					}
				}
			}

			// RNG errors

			case expected_pattern: {
				return XMLPositionUtility.selectStartTagName(offset, document);
			}
			case illegal_name_attribute: {
				return XMLPositionUtility.selectAttributeNameFromGivenNameAt(RelaxNGUtils.NAME_ATTR, offset, document);
			}
			case illegal_attribute_ignored: {
				String attrName = (String) arguments[0];
				return XMLPositionUtility.selectAttributeNameFromGivenNameAt(attrName, offset, document);
			}
			case invalid_ncname: {
				DOMNode node = document.findNodeAt(offset);
				if (node != null && node.isElement()
						&& RelaxNGUtils.NAME_ATTR.equals(((DOMElement) node).getLocalName())) {
					// <element><name>::::</name></element>
					DOMElement name = (DOMElement) node;
					boolean hasText = DOMUtils.containsTextOnly(name);
					return hasText ? XMLPositionUtility.selectText((DOMText) name.getFirstChild())
							: XMLPositionUtility.selectStartTagName((DOMElement) node);
				}
				// <element name="::::"></element>
				return XMLPositionUtility.selectAttributeValueAt(RelaxNGUtils.NAME_ATTR, offset, false, document);
			}
			case missing_children:
			case missing_name_attribute:
			case missing_name_class:
			case missing_type_attribute:
			case missing_href_attribute: {
				DOMNode node = document.findNodeAt(offset);
				if (node != null && node.isElement()) {
					return XMLPositionUtility.selectStartTagName((DOMElement) node);
				}
				break;
			}

			// RNG SAX parse errors - select start tag name at offset
			case expected_define:
			case expected_empty:
			case expected_except:
			case expected_grammar:
			case expected_name:
			case expected_name_class:
			case expected_param_except:
			case expected_one_name_class:
			case attribute_multi_pattern:
			case start_multi_pattern:
			case too_many_children:
			case multiple_except:
			case illegal_characters_ignored:
			case name_contains_foreign_element:
			case param_after_except:
			case param_contains_foreign_element:
			case value_contains_foreign_element:
			case any_name_except_contains_any_name:
			case ns_name_except_contains_any_name:
			case ns_name_except_contains_ns_name: {
				return XMLPositionUtility.selectStartTagName(offset, document);
			}

			// Pattern-level errors reported after full parsing; Jing's locator
			// points to the document start, so fall back to the root element.
			case interleave_string:
			case group_string:
			case one_or_more_string:
			case duplicate_attribute:
			case interleave_element_overlap:
			case interleave_text_overlap:
			case open_name_class_not_repeated:
			case parent_ref_outside_grammar:
			case ref_outside_grammar:
			case builtin_param:
			case datatype_requires_parameter:
			case missing_start_replacement:
			case missing_define_replacement:
			case xmlns_attribute:
			case xmlns_uri_attribute:
			case conflict_combine_start:
			// Section 7.1 restriction violations
			case attribute_contains_attribute:
			case attribute_contains_element:
			case data_except_contains_attribute:
			case data_except_contains_element:
			case data_except_contains_empty:
			case data_except_contains_group:
			case data_except_contains_interleave:
			case data_except_contains_list:
			case data_except_contains_one_or_more:
			case data_except_contains_text:
			case list_contains_attribute:
			case list_contains_element:
			case list_contains_list:
			case list_contains_text:
			case list_contains_interleave:
			case one_or_more_contains_group_contains_attribute:
			case one_or_more_contains_interleave_contains_attribute:
			case start_contains_attribute:
			case start_contains_data:
			case start_contains_empty:
			case start_contains_group:
			case start_contains_interleave:
			case start_contains_list:
			case start_contains_one_or_more:
			case start_contains_text:
			case start_contains_value: {
				Range range = XMLPositionUtility.selectStartTagName(offset, document);
				if (range != null && !range.getStart().equals(range.getEnd())) {
					return range;
				}
				DOMElement root = document.getDocumentElement();
				if (root != null) {
					return XMLPositionUtility.selectStartTagName(root);
				}
				break;
			}

			// Attribute value errors
			case combine_attribute_bad_value:
			case conflict_combine_define: {
				return XMLPositionUtility.selectAttributeValueAt("combine", offset, document);
			}
			case fragment_identifier_datatype_library:
			case relative_datatype_library:
			case unrecognized_datatype_library: {
				Range attrRange = XMLPositionUtility.selectAttributeValueAt(
						RelaxNGUtils.DATATYPE_LIBRARY_ATTR, offset, document);
				if (attrRange != null) {
					return attrRange;
				}
				DOMNode dtlNode = document.findNodeAt(offset);
				if (dtlNode != null) {
					DOMElement dtlElement = dtlNode.isElement() ? (DOMElement) dtlNode
							: (DOMElement) dtlNode.getParentElement();
					if (dtlElement != null) {
						DOMNode current = dtlElement.getParentNode();
						while (current != null && current.isElement()) {
							DOMAttr dtlAttr = ((DOMElement) current)
									.getAttributeNode(RelaxNGUtils.DATATYPE_LIBRARY_ATTR);
							if (dtlAttr != null) {
								return XMLPositionUtility.selectAttributeValue(dtlAttr);
							}
							current = current.getParentNode();
						}
					}
				}
				return null;
			}
			case href_fragment_id:
			case recursive_include: {
				return XMLPositionUtility.selectAttributeValueAt("href", offset, document);
			}
			case invalid_uri: {
				String invalidUri = arguments.length > 0 ? (String) arguments[0] : null;
				DOMNode uriNode = document.findNodeAt(offset);
				if (uriNode != null && uriNode.isElement()) {
					DOMElement el = (DOMElement) uriNode;
					if (invalidUri != null) {
						DOMAttr hrefAttr = el.getAttributeNode(RelaxNGUtils.HREF_ATTR);
						if (hrefAttr != null && invalidUri.equals(hrefAttr.getValue())) {
							return XMLPositionUtility.selectAttributeValue(hrefAttr);
						}
						DOMAttr dAttr = el.getAttributeNode(RelaxNGUtils.DATATYPE_LIBRARY_ATTR);
						if (dAttr != null && invalidUri.equals(dAttr.getValue())) {
							return XMLPositionUtility.selectAttributeValue(dAttr);
						}
					}
				}
				return XMLPositionUtility.selectStartTagName(offset, document);
			}
			case unrecognized_builtin_datatype: {
				DOMNode btNode = document.findNodeAt(offset);
				if (btNode != null && btNode.isElement()) {
					DOMAttr typeAttr = ((DOMElement) btNode).getAttributeNode(RelaxNGUtils.TYPE_ATTR);
					if (typeAttr != null) {
						return XMLPositionUtility.selectAttributeValue(typeAttr);
					}
				}
				return XMLPositionUtility.selectStartTagName(offset, document);
			}
			case invalid_value: {
				DOMNode valNode = document.findNodeAt(offset);
				if (valNode != null && valNode.isElement()) {
					DOMElement valEl = (DOMElement) valNode;
					boolean hasText = DOMUtils.containsTextOnly(valEl);
					if (hasText) {
						return XMLPositionUtility.selectText((DOMText) valEl.getFirstChild());
					}
				}
				return XMLPositionUtility.selectStartTagName(offset, document);
			}
			case root_bad_namespace_uri:
			case wrong_uri_version: {
				DOMNode nsNode = document.findNodeAt(offset);
				if (nsNode != null && nsNode.isElement()) {
					DOMAttr nsAttr = ((DOMElement) nsNode).getAttributeNode("xmlns");
					if (nsAttr != null) {
						return XMLPositionUtility.selectAttributeValue(nsAttr);
					}
				}
				return XMLPositionUtility.selectStartTagName(offset, document);
			}
			case qualified_attribute: {
				String qualAttrName = (String) arguments[0];
				DOMNode qualNode = document.findNodeAt(offset);
				if (qualNode != null && qualNode.isElement()) {
					DOMElement qualEl = (DOMElement) qualNode;
					for (DOMAttr attr : qualEl.attributes()) {
						if (qualAttrName.equals(attr.getLocalName()) && attr.getPrefix() != null) {
							return XMLPositionUtility.selectAttributeName(attr);
						}
					}
				}
				return XMLPositionUtility.selectAttributeNameFromGivenNameAt(qualAttrName, offset, document);
			}
			case undefined_prefix: {
				return XMLPositionUtility.selectStartTagName(offset, document);
			}
			case recursive_reference: {
				String recRefName = (String) arguments[0];
				DOMNode recNode = document.findNodeAt(offset);
				if (recNode != null && recNode.isElement()) {
					DOMAttr nameAttr = ((DOMElement) recNode).getAttributeNode(RelaxNGUtils.NAME_ATTR);
					if (nameAttr != null) {
						return XMLPositionUtility.selectAttributeValue(nameAttr);
					}
				}
				DOMElement root = document.getDocumentElement();
				if (root != null) {
					return XMLPositionUtility.selectStartTagName(root);
				}
				break;
			}

		}
		return null;
	}

	
	public static void registerCodeActionParticipants(Map<String, ICodeActionParticipant> codeActions,
			SharedSettings sharedSettings) {
		codeActions.put(incomplete_element_required_element_missing.getCode(), new required_element_missingCodeAction());
		codeActions.put(incomplete_element_required_elements_missing_expected.getCode(), new required_elements_missing_expectedCodeAction());
		required_attribute_missingCodeAction requiredAttrCodeAction = new required_attribute_missingCodeAction();
		codeActions.put(required_attribute_missing.getCode(), requiredAttrCodeAction);
		codeActions.put(required_attributes_missing.getCode(), requiredAttrCodeAction);
		codeActions.put(required_attributes_missing_expected.getCode(), requiredAttrCodeAction);
		codeActions.put(invalid_attribute_name.getCode(), new invalid_attribute_nameCodeAction());
		codeActions.put(no_attributes_allowed.getCode(), new no_attributes_allowedCodeAction());
		codeActions.put(invalid_attribute_value.getCode(), new invalid_attribute_valueCodeAction());
		codeActions.put(unknown_element.getCode(), new unknown_elementCodeAction());
		codeActions.put(out_of_context_element.getCode(), new out_of_context_elementCodeAction());
		// RNG grammar code actions
		RemoveAttributeCodeAction removeAttrAction = new RemoveAttributeCodeAction();
		codeActions.put(illegal_attribute_ignored.getCode(), removeAttrAction);
		codeActions.put(illegal_name_attribute.getCode(), removeAttrAction);
		codeActions.put(qualified_attribute.getCode(), removeAttrAction);
		codeActions.put(missing_href_attribute.getCode(), new InsertAttributeCodeAction(RelaxNGUtils.HREF_ATTR));
		codeActions.put(missing_name_attribute.getCode(), new InsertAttributeCodeAction(RelaxNGUtils.NAME_ATTR));
		codeActions.put(missing_type_attribute.getCode(), new InsertAttributeCodeAction(RelaxNGUtils.TYPE_ATTR));
		codeActions.put(expected_pattern.getCode(), new expected_patternCodeAction());
		codeActions.put(expected_define.getCode(), new expected_defineCodeAction());
		codeActions.put(any_name_except_contains_any_name.getCode(), new any_name_except_contains_any_nameCodeAction());
		ns_name_except_contains_any_nameCodeAction nsNameExceptAction = new ns_name_except_contains_any_nameCodeAction();
		codeActions.put(ns_name_except_contains_any_name.getCode(), nsNameExceptAction);
		codeActions.put(ns_name_except_contains_ns_name.getCode(), nsNameExceptAction);
		RNGReplaceDatatypeCodeAction replaceDatatypeAction = new RNGReplaceDatatypeCodeAction();
		codeActions.put(unrecognized_datatype.getCode(), replaceDatatypeAction);
		codeActions.put(unrecognized_builtin_datatype.getCode(), replaceDatatypeAction);
	}

	private static DOMAttr findRefByName(DOMNode parent, String refName) {
		for (DOMNode child : parent.children()) {
			if (child.isElement() && RelaxNGUtils.isRef((DOMElement) child)) {
				DOMElement ref = (DOMElement) child;
				DOMAttr attr = ref.getAttributeNode(RelaxNGUtils.NAME_ATTR);
				if (attr != null && refName.equals(attr.getValue())) {
					return attr;
				}
			}
			DOMAttr attr = findRefByName(child, refName);
			if (attr != null) {
				return attr;
			}
		}
		return null;
	}
}
