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
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.missingelement.required_element_missingCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.missingelement.required_elements_missing_expectedCodeAction;
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
					DOMAttr attr = data.getAttributeNode("type");
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
			case missing_type_attribute: {
				DOMNode node = document.findNodeAt(offset);
				if (node != null && node.isElement()) {
					return XMLPositionUtility.selectStartTagName((DOMElement) node);
				}
			}

		}
		return null;
	}

	
	public static void registerCodeActionParticipants(Map<String, ICodeActionParticipant> codeActions,
			SharedSettings sharedSettings) {
		codeActions.put(incomplete_element_required_element_missing.getCode(), new required_element_missingCodeAction());
		codeActions.put(incomplete_element_required_elements_missing_expected.getCode(), new required_elements_missing_expectedCodeAction());
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
