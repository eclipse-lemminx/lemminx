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
package org.eclipse.lemminx.extensions.xsd.participants;

import static org.eclipse.lemminx.utils.StringUtils.getString;

import java.util.HashMap;

import java.util.Map;

import org.apache.xerces.xni.XMLLocator;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.RemoveAttributeCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.RemoveDefaultOrFixedCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.s4s_elt_invalid_content_3CodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.src_import_1_2CodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.src_annotationCodeAction;
import org.eclipse.lemminx.extensions.xsd.utils.XSDUtils;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.diagnostics.IXMLErrorCode;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.Range;

/**
 * XSD error code.
 *
 * @see https://wiki.xmldation.com/Support/Validator
 *
 *      All error code types and messages can be found in the Xerces library
 *      https://github.com/apache/xerces2-j/blob/trunk/src/org/apache/xerces/impl/msg/XMLSchemaMessages.properties
 *
 */
public enum XSDErrorCode implements IXMLErrorCode {

	// ---------- Constraint validation ----------

	ag_props_correct_2("ag-props-correct.2"), //
	ag_props_correct_3("ag-props-correct.3"), //
	a_props_correct_2("a-props-correct.2"), //
	a_props_correct_3("a-props-correct.3"), //
	au_props_correct_2("au-props-correct.2"), //
	cos_all_limited_1_2("cos-all-limited.1.2"), //
	cos_all_limited_2("cos-all-limited.2"), //
	cos_applicable_facets("cos-applicable-facets"), //
	cos_ct_extends_1_1("cos-ct-extends.1.1"), //
	cos_ct_extends_1_4_3_2_2_1_a("cos-ct-extends.1.4.3.2.2.1.a"), //
	cos_ct_extends_1_4_3_2_2_1_b("cos-ct-extends.1.4.3.2.2.1.b"), //
	cos_element_consistent("cos-element-consistent"), //
	cos_list_of_atomic("cos-list-of-atomic"), //
	cos_nonambig("cos-nonambig"), //
	cos_particle_restrict_a("cos-particle-restrict.a"), //
	cos_particle_restrict_b("cos-particle-restrict.b"), //
	cos_particle_restrict_2("cos-particle-restrict.2"), //
	cos_st_restricts_1_1("cos-st-restricts.1.1"), //
	cos_st_restricts_2_1("cos-st-restricts.2.1"), //
	cos_st_restricts_2_3_1_1("cos-st-restricts.2.3.1.1"), //
	cos_st_restricts_3_3_1_1("cos-st-restricts.3.3.1.1"), //
	cos_valid_default_2_1("cos-valid-default.2.1"), //
	cos_valid_default_2_2_2("cos-valid-default.2.2.2"), //
	c_props_correct_2("c-props-correct.2"), //
	ct_props_correct_3("ct-props-correct.3"), //
	ct_props_correct_4("ct-props-correct.4"), //
	ct_props_correct_5("ct-props-correct.5"), //

	// ---------- Derivation restriction ----------

	derivation_ok_restriction_1("derivation-ok-restriction.1"), //
	derivation_ok_restriction_2_1_1("derivation-ok-restriction.2.1.1"), //
	derivation_ok_restriction_2_1_2("derivation-ok-restriction.2.1.2"), //
	derivation_ok_restriction_2_1_3_a("derivation-ok-restriction.2.1.3.a"), //
	derivation_ok_restriction_2_1_3_b("derivation-ok-restriction.2.1.3.b"), //
	derivation_ok_restriction_2_2_a("derivation-ok-restriction.2.2.a"), //
	derivation_ok_restriction_2_2_b("derivation-ok-restriction.2.2.b"), //
	derivation_ok_restriction_3("derivation-ok-restriction.3"), //
	derivation_ok_restriction_4_1("derivation-ok-restriction.4.1"), //
	derivation_ok_restriction_4_2("derivation-ok-restriction.4.2"), //
	derivation_ok_restriction_4_3("derivation-ok-restriction.4.3"), //
	derivation_ok_restriction_5_2_2_1("derivation-ok-restriction.5.2.2.1"), //
	derivation_ok_restriction_5_3_2("derivation-ok-restriction.5.3.2"), //
	derivation_ok_restriction_5_4_1_2("derivation-ok-restriction.5.4.1.2"), //
	derivation_ok_restriction_5_4_2("derivation-ok-restriction.5.4.2"), //

	// ---------- Enumeration ----------

	enumeration_required_notation("enumeration-required-notation"), //
	enumeration_valid_restriction("enumeration-valid-restriction"), //

	// ---------- Element properties ----------

	e_props_correct_2("e-props-correct.2"), //
	e_props_correct_4("e-props-correct.4"), //
	e_props_correct_5("e-props-correct.5"), //
	e_props_correct_6("e-props-correct.6"), //

	// ---------- Facet restriction ----------

	fractionDigits_valid_restriction("fractionDigits-valid-restriction"), //
	fractionDigits_totalDigits("fractionDigits-totalDigits"), //
	length_minLength_maxLength_1_1("length-minLength-maxLength.1.1"), //
	length_minLength_maxLength_1_2_a("length-minLength-maxLength.1.2.a"), //
	length_minLength_maxLength_1_2_b("length-minLength-maxLength.1.2.b"), //
	length_minLength_maxLength_2_1("length-minLength-maxLength.2.1"), //
	length_minLength_maxLength_2_2_a("length-minLength-maxLength.2.2.a"), //
	length_minLength_maxLength_2_2_b("length-minLength-maxLength.2.2.b"), //
	length_valid_restriction("length-valid-restriction"), //
	maxExclusive_valid_restriction_1("maxExclusive-valid-restriction.1"), //
	maxExclusive_valid_restriction_2("maxExclusive-valid-restriction.2"), //
	maxExclusive_valid_restriction_3("maxExclusive-valid-restriction.3"), //
	maxExclusive_valid_restriction_4("maxExclusive-valid-restriction.4"), //
	maxInclusive_maxExclusive("maxInclusive-maxExclusive"), //
	maxInclusive_valid_restriction_1("maxInclusive-valid-restriction.1"), //
	maxInclusive_valid_restriction_2("maxInclusive-valid-restriction.2"), //
	maxInclusive_valid_restriction_3("maxInclusive-valid-restriction.3"), //
	maxInclusive_valid_restriction_4("maxInclusive-valid-restriction.4"), //
	maxLength_valid_restriction("maxLength-valid-restriction"), //
	mg_props_correct_2("mg-props-correct.2"), //
	minExclusive_less_than_equal_to_maxExclusive("minExclusive-less-than-equal-to-maxExclusive"), //
	minExclusive_less_than_maxInclusive("minExclusive-less-than-maxInclusive"), //
	minExclusive_valid_restriction_1("minExclusive-valid-restriction.1"), //
	minExclusive_valid_restriction_2("minExclusive-valid-restriction.2"), //
	minExclusive_valid_restriction_3("minExclusive-valid-restriction.3"), //
	minExclusive_valid_restriction_4("minExclusive-valid-restriction.4"), //
	minInclusive_less_than_equal_to_maxInclusive("minInclusive-less-than-equal-to-maxInclusive"), //
	minInclusive_less_than_maxExclusive("minInclusive-less-than-maxExclusive"), //
	minInclusive_minExclusive("minInclusive-minExclusive"), //
	minInclusive_valid_restriction_1("minInclusive-valid-restriction.1"), //
	minInclusive_valid_restriction_2("minInclusive-valid-restriction.2"), //
	minInclusive_valid_restriction_3("minInclusive-valid-restriction.3"), //
	minInclusive_valid_restriction_4("minInclusive-valid-restriction.4"), //
	minLength_less_than_equal_to_maxLength("minLength-less-than-equal-to-maxLength"), //
	minLength_valid_restriction("minLength-valid-restriction"), //
	no_xmlns("no-xmlns"), //
	no_xsi("no-xsi"), //
	totalDigits_valid_restriction("totalDigits-valid-restriction"), //
	whiteSpace_valid_restriction_1("whiteSpace-valid-restriction.1"), //
	whiteSpace_valid_restriction_2("whiteSpace-valid-restriction.2"), //

	// ---------- Particle restriction (rcase-*) ----------

	rcase_MapAndSum_1("rcase-MapAndSum.1"), //
	rcase_MapAndSum_2("rcase-MapAndSum.2"), //
	rcase_NameAndTypeOK_1("rcase-NameAndTypeOK.1"), //
	rcase_NameAndTypeOK_2("rcase-NameAndTypeOK.2"), //
	rcase_NameAndTypeOK_3("rcase-NameAndTypeOK.3"), //
	rcase_NameAndTypeOK_4_a("rcase-NameAndTypeOK.4.a"), //
	rcase_NameAndTypeOK_4_b("rcase-NameAndTypeOK.4.b"), //
	rcase_NameAndTypeOK_5("rcase-NameAndTypeOK.5"), //
	rcase_NameAndTypeOK_6("rcase-NameAndTypeOK.6"), //
	rcase_NameAndTypeOK_7("rcase-NameAndTypeOK.7"), //
	rcase_NSCompat_1("rcase-NSCompat.1"), //
	rcase_NSCompat_2("rcase-NSCompat.2"), //
	rcase_NSRecurseCheckCardinality_1("rcase-NSRecurseCheckCardinality.1"), //
	rcase_NSRecurseCheckCardinality_2("rcase-NSRecurseCheckCardinality.2"), //
	rcase_NSSubset_1("rcase-NSSubset.1"), //
	rcase_NSSubset_2("rcase-NSSubset.2"), //
	rcase_NSSubset_3("rcase-NSSubset.3"), //
	rcase_Recurse_1("rcase-Recurse.1"), //
	rcase_Recurse_2("rcase-Recurse.2"), //
	rcase_RecurseLax_1("rcase-RecurseLax.1"), //
	rcase_RecurseLax_2("rcase-RecurseLax.2"), //
	rcase_RecurseUnordered_1("rcase-RecurseUnordered.1"), //
	rcase_RecurseUnordered_2("rcase-RecurseUnordered.2"), //

	// ---------- Simple type properties ----------

	st_props_correct_2("st-props-correct.2"), //
	st_props_correct_3("st-props-correct.3"), //

	// ---------- Schema-for-schemas (s4s) ----------

	s4s_att_invalid_value("s4s-att-invalid-value"), //
	s4s_att_must_appear("s4s-att-must-appear"), //
	s4s_att_not_allowed("s4s-att-not-allowed"), //
	s4s_elt_character("s4s-elt-character"), //
	s4s_elt_invalid("s4s-elt-invalid"), //
	s4s_elt_invalid_content_1("s4s-elt-invalid-content.1"), //
	s4s_elt_invalid_content_2("s4s-elt-invalid-content.2"), //
	s4s_elt_invalid_content_3("s4s-elt-invalid-content.3"), //
	s4s_elt_must_match_1("s4s-elt-must-match.1"), //
	s4s_elt_must_match_2("s4s-elt-must-match.2"), //
	s4s_elt_schema_ns("s4s-elt-schema-ns"), //
	sch_props_correct_2("sch-props-correct.2"), //

	// ---------- Schema reference ----------

	schema_reference_4("schema_reference.4"), //

	// ---------- Source (src) errors ----------

	src_annotation("src-annotation"), //
	src_attribute_1("src-attribute.1"), //
	src_attribute_2("src-attribute.2"), //
	src_attribute_3_1("src-attribute.3.1"), //
	src_attribute_3_2("src-attribute.3.2"), //
	src_attribute_4("src-attribute.4"), //
	src_attribute_group_2("src-attribute_group.2"), //
	src_attribute_group_3("src-attribute_group.3"), //
	src_ct_1("src-ct.1"), //
	src_ct_2_1("src-ct.2.1"), //
	src_ct_2_2("src-ct.2.2"), //
	src_ct_4("src-ct.4"), //
	src_ct_5("src-ct.5"), //
	src_element_1("src-element.1"), //
	src_element_2_1("src-element.2.1"), //
	src_element_2_2("src-element.2.2"), //
	src_element_3("src-element.3"), //
	src_import_1_1("src-import.1.1"), //
	src_import_1_2("src-import.1.2"), //
	src_import_2("src-import.2"), //
	src_import_3_1("src-import.3.1"), //
	src_import_3_2("src-import.3.2"), //
	src_include_1("src-include.1"), //
	src_include_2_1("src-include.2.1"), //
	src_redefine_2("src-redefine.2"), //
	src_redefine_3_1("src-redefine.3.1"), //
	src_redefine_5_a_a("src-redefine.5.a.a"), //
	src_redefine_5_a_b("src-redefine.5.a.b"), //
	src_redefine_5_a_c("src-redefine.5.a.c"), //
	src_redefine_5_b_a("src-redefine.5.b.a"), //
	src_redefine_5_b_b("src-redefine.5.b.b"), //
	src_redefine_5_b_c("src-redefine.5.b.c"), //
	src_redefine_5_b_d("src-redefine.5.b.d"), //
	src_redefine_6_1_1("src-redefine.6.1.1"), //
	src_redefine_6_1_2("src-redefine.6.1.2"), //
	src_redefine_6_2_1("src-redefine.6.2.1"), //
	src_redefine_6_2_2("src-redefine.6.2.2"), //
	src_redefine_7_1("src-redefine.7.1"), //
	src_redefine_7_2_1("src-redefine.7.2.1"), //
	src_redefine_7_2_2("src-redefine.7.2.2"), //
	src_resolve("src-resolve"), //
	src_resolve_4_1("src-resolve.4.1"), //
	src_resolve_4_2("src-resolve.4.2"), //
	src_simple_type_2_a("src-simple-type.2.a"), //
	src_simple_type_2_b("src-simple-type.2.b"), //
	src_simple_type_3_a("src-simple-type.3.a"), //
	src_simple_type_3_b("src-simple-type.3.b"), //
	src_single_facet_value("src-single-facet-value"), //
	src_union_memberTypes_or_simpleTypes("src-union-memberTypes-or-simpleTypes"), //

	// ---------- Other ----------

	c_fields_xpaths("c-fields-xpaths"), //
	c_general_xpath("c-general-xpath"), //
	c_general_xpath_ns("c-general-xpath-ns"), //
	c_selector_xpath("c-selector-xpath"), //
	EmptyTargetNamespace("EmptyTargetNamespace"), //
	FacetValueFromBase("FacetValueFromBase"), //
	FacetsContradict("FacetsContradict"), //
	FixedFacetValue("FixedFacetValue"), //
	InvalidRegex("InvalidRegex"), //
	p_props_correct_2_1("p-props-correct.2.1"), //
	PublicSystemOnNotation("PublicSystemOnNotation");

	private final String code;

	private XSDErrorCode() {
		this(null);
	}

	private XSDErrorCode(String code) {
		this.code = code;
	}

	@Override
	public String getCode() {
		if (code == null) {
			return name();
		}
		return code;
	}

	private final static Map<String, XSDErrorCode> codes;

	static {
		codes = new HashMap<>();
		for (XSDErrorCode errorCode : values()) {
			codes.put(errorCode.getCode(), errorCode);
		}
	}

	public static XSDErrorCode get(String name) {
		return codes.get(name);
	}

	/**
	 * Create the LSP range from the SAX error.
	 *
	 * @param characterOffset
	 * @param key
	 * @param arguments
	 * @param document
	 * @return the LSP range from the SAX error.
	 */
	public static Range toLSPRange(XMLLocator location, XSDErrorCode code, Object[] arguments, DOMDocument document) {
		int offset = location.getCharacterOffset() - 1;
		// adjust positions
		switch (code) {

		// ---------- Special error range handling ----------

		case cos_all_limited_2: {
			String nameValue = getString(arguments[1]);
			DOMNode parent = document.findNodeAt(offset);
			DOMNode child = parent.findChildWithAttributeValue("name", nameValue);

			if (child == null) {
				return XMLPositionUtility.selectStartTagName(offset, document);
			}

			offset = child.getStart() + 1;
			return XMLPositionUtility.selectAttributeValueAt("maxOccurs", offset, document);
		}
		case ct_props_correct_3: {
			String attrValue = getString(arguments[0]);
			if (attrValue.charAt(0) == ':') {
				attrValue = attrValue.substring(1);
			}
			return XMLPositionUtility.selectAttributeValueFromGivenValue(attrValue, offset, document);
		}
		case p_props_correct_2_1:
			return XMLPositionUtility.selectAttributeFromGivenNameAt("minOccurs", offset, document);

		// ---------- Select attribute name ----------

		case s4s_att_not_allowed: {
			String attrName = getString(arguments[1]);
			return XMLPositionUtility.selectAttributeNameFromGivenNameAt(attrName, offset, document);
		}

		// ---------- Select attribute value ----------

		case s4s_att_invalid_value: {
			String attrName = getString(arguments[1]);
			return XMLPositionUtility.selectAttributeValueAt(attrName, offset, document);
		}
		case au_props_correct_2:
			return XMLPositionUtility.selectAttributeValueAt(XSDUtils.FIXED_ATTR, offset, document);
		case src_ct_1:
		case src_ct_2_1:
		case src_ct_2_2:
			return XMLPositionUtility.selectAttributeValueAt("base", offset, document);
		case EmptyTargetNamespace:
			return XMLPositionUtility.selectAttributeValueAt(XSDUtils.TARGET_NAMESPACE_ATTR, offset, document);
		case schema_reference_4:
			return XMLPositionUtility.selectAttributeValueAt(XSDUtils.SCHEMA_LOCATION_ATTR, offset, true, document);
		case src_import_1_1:
			return XMLPositionUtility.selectAttributeValueAt(XSDUtils.NAMESPACE_ATTR, offset, document);
		case src_import_2:
		case src_include_1:
		case src_include_2_1:
		case src_redefine_2:
		case src_redefine_3_1:
			return XMLPositionUtility.selectAttributeValueAt(XSDUtils.SCHEMA_LOCATION_ATTR, offset, document);

		// ---------- Select attribute value by given value ----------

		case sch_props_correct_2: {
			String argument = getString(arguments[0]);
			String attrName = argument.substring(argument.indexOf(",") + 1);
			return XMLPositionUtility.selectAttributeValueFromGivenValue(attrName, offset, document);
		}
		case src_resolve_4_1:
		case src_resolve_4_2: {
			String attrValue = getString(arguments[2]);
			return XMLPositionUtility.selectAttributeValueByGivenValueAt(attrValue, offset, document);
		}
		case src_resolve: {
			String attrValue = getString(arguments[0]);
			return XMLPositionUtility.selectAttributeValueByGivenValueAt(attrValue, offset, document);
		}

		// ---------- Select content ----------

		case s4s_elt_character:
			return XMLPositionUtility.selectContent(offset, document);

		// ---------- Select import namespace/schema location ----------

		case src_import_3_1: {
			DOMNode elementHighlighted = document.findNodeAt(offset);
			if (elementHighlighted.getNodeName().equals(XSDUtils.XS_SCHEMA_TAG)) {
				return XMLPositionUtility.selectChildNodeAttributeValueFromGivenNameAt(XSDUtils.XS_IMPORT_TAG,
						XSDUtils.NAMESPACE_ATTR, offset, document);
			} else {
				return XMLPositionUtility.selectAttributeValueAt(XSDUtils.NAMESPACE_ATTR, offset, document);
			}
		}
		case src_import_3_2:
			return XMLPositionUtility.selectChildNodeAttributeValueFromGivenNameAt(XSDUtils.XS_IMPORT_TAG,
					XSDUtils.SCHEMA_LOCATION_ATTR, offset, document);

		// ---------- All other errors: select start tag name ----------

		case a_props_correct_3: {
			Range defaultRange = XMLPositionUtility.selectAttributeNameFromGivenNameAt(XSDUtils.DEFAULT_ATTR, offset,
					document);
			if (defaultRange != null) {
				return defaultRange;
			}
			return XMLPositionUtility.selectAttributeNameFromGivenNameAt(XSDUtils.FIXED_ATTR, offset, document);
		}

		case ct_props_correct_5: {
			String attrDeclName = getString(arguments[1]);
			DOMNode node = document.findNodeAt(offset);
			if (node != null) {
				DOMNode parent = node;
				if (node.isElement() && XSDUtils.ATTRIBUTE_TAG.equals(((DOMElement) node).getLocalName())) {
					parent = node.getParentNode();
				}
				if (parent != null && parent.hasChildNodes()) {
					for (DOMNode child : parent.children()) {
						if (child.isElement()) {
							DOMElement childElement = (DOMElement) child;
							if (XSDUtils.ATTRIBUTE_TAG.equals(childElement.getLocalName())
									&& attrDeclName.equals(childElement.getAttribute(XSDUtils.NAME_ATTR))) {
								DOMAttr typeAttr = childElement.getAttributeNode(XSDUtils.TYPE_ATTR);
								if (typeAttr != null) {
									return XMLPositionUtility.selectAttributeValue(typeAttr, true);
								}
								return XMLPositionUtility.selectStartTagName(childElement);
							}
						}
					}
				}
			}
			return XMLPositionUtility.selectStartTagName(offset, document);
		}

		case ag_props_correct_2: {
			String dupAttrName = getString(arguments[1]);
			DOMNode agNode = document.findNodeAt(offset);
			if (agNode != null) {
				DOMNode agParent = agNode;
				if (agNode.isElement() && XSDUtils.ATTRIBUTE_TAG.equals(((DOMElement) agNode).getLocalName())) {
					agParent = agNode.getParentNode();
				}
				if (agParent != null && agParent.hasChildNodes()) {
					DOMAttr lastMatch = null;
					for (DOMNode child : agParent.children()) {
						if (child.isElement()) {
							DOMElement childElement = (DOMElement) child;
							if (XSDUtils.ATTRIBUTE_TAG.equals(childElement.getLocalName())
									&& dupAttrName.equals(childElement.getAttribute(XSDUtils.NAME_ATTR))) {
								DOMAttr nameAttr = childElement.getAttributeNode(XSDUtils.NAME_ATTR);
								if (nameAttr != null) {
									lastMatch = nameAttr;
								}
							}
						}
					}
					if (lastMatch != null) {
						return XMLPositionUtility.selectAttributeValue(lastMatch, true);
					}
				}
			}
			return XMLPositionUtility.selectStartTagName(offset, document);
		}

		case ag_props_correct_3: {
			String agAttrDeclName = getString(arguments[1]);
			DOMNode agNode3 = document.findNodeAt(offset);
			if (agNode3 != null) {
				DOMNode agParent3 = agNode3;
				if (agNode3.isElement() && XSDUtils.ATTRIBUTE_TAG.equals(((DOMElement) agNode3).getLocalName())) {
					agParent3 = agNode3.getParentNode();
				}
				if (agParent3 != null && agParent3.hasChildNodes()) {
					for (DOMNode child : agParent3.children()) {
						if (child.isElement()) {
							DOMElement childElement = (DOMElement) child;
							if (XSDUtils.ATTRIBUTE_TAG.equals(childElement.getLocalName())
									&& agAttrDeclName.equals(childElement.getAttribute(XSDUtils.NAME_ATTR))) {
								DOMAttr typeAttr = childElement.getAttributeNode(XSDUtils.TYPE_ATTR);
								if (typeAttr != null) {
									return XMLPositionUtility.selectAttributeValue(typeAttr, true);
								}
								return XMLPositionUtility.selectStartTagName(childElement);
							}
						}
					}
				}
			}
			return XMLPositionUtility.selectStartTagName(offset, document);
		}

		case cos_applicable_facets: {
			String facetName = getString(arguments[0]);
			DOMNode restrictionNode = document.findNodeAt(offset);
			if (restrictionNode != null && restrictionNode.hasChildNodes()) {
				for (DOMNode child : restrictionNode.children()) {
					if (child.isElement() && facetName.equals(child.getLocalName())) {
						return XMLPositionUtility.selectStartTagName(child);
					}
				}
			}
			return XMLPositionUtility.selectStartTagName(offset, document);
		}

		case a_props_correct_2:
		case cos_all_limited_1_2:
		case cos_ct_extends_1_1:
		case cos_ct_extends_1_4_3_2_2_1_a:
		case cos_ct_extends_1_4_3_2_2_1_b:
		case cos_element_consistent:
		case cos_list_of_atomic:
		case cos_nonambig:
		case cos_particle_restrict_a:
		case cos_particle_restrict_b:
		case cos_particle_restrict_2:
		case cos_st_restricts_1_1:
		case cos_st_restricts_2_1:
		case cos_st_restricts_2_3_1_1:
		case cos_st_restricts_3_3_1_1:
		case cos_valid_default_2_1:
		case cos_valid_default_2_2_2:
		case c_props_correct_2:
		case ct_props_correct_4:
		case derivation_ok_restriction_1:
		case derivation_ok_restriction_2_1_1:
		case derivation_ok_restriction_2_1_2:
		case derivation_ok_restriction_2_1_3_a:
		case derivation_ok_restriction_2_1_3_b:
		case derivation_ok_restriction_2_2_a:
		case derivation_ok_restriction_2_2_b:
		case derivation_ok_restriction_3:
		case derivation_ok_restriction_4_1:
		case derivation_ok_restriction_4_2:
		case derivation_ok_restriction_4_3:
		case derivation_ok_restriction_5_2_2_1:
		case derivation_ok_restriction_5_3_2:
		case derivation_ok_restriction_5_4_1_2:
		case derivation_ok_restriction_5_4_2:
		case enumeration_required_notation:
		case enumeration_valid_restriction:
		case e_props_correct_2:
		case e_props_correct_4:
		case e_props_correct_5:
		case e_props_correct_6:
		case fractionDigits_valid_restriction:
		case fractionDigits_totalDigits:
		case length_minLength_maxLength_1_1:
		case length_minLength_maxLength_1_2_a:
		case length_minLength_maxLength_1_2_b:
		case length_minLength_maxLength_2_1:
		case length_minLength_maxLength_2_2_a:
		case length_minLength_maxLength_2_2_b:
		case length_valid_restriction:
		case maxExclusive_valid_restriction_1:
		case maxExclusive_valid_restriction_2:
		case maxExclusive_valid_restriction_3:
		case maxExclusive_valid_restriction_4:
		case maxInclusive_maxExclusive:
		case maxInclusive_valid_restriction_1:
		case maxInclusive_valid_restriction_2:
		case maxInclusive_valid_restriction_3:
		case maxInclusive_valid_restriction_4:
		case maxLength_valid_restriction:
		case mg_props_correct_2:
		case minExclusive_less_than_equal_to_maxExclusive:
		case minExclusive_less_than_maxInclusive:
		case minExclusive_valid_restriction_1:
		case minExclusive_valid_restriction_2:
		case minExclusive_valid_restriction_3:
		case minExclusive_valid_restriction_4:
		case minInclusive_less_than_equal_to_maxInclusive:
		case minInclusive_less_than_maxExclusive:
		case minInclusive_minExclusive:
		case minInclusive_valid_restriction_1:
		case minInclusive_valid_restriction_2:
		case minInclusive_valid_restriction_3:
		case minInclusive_valid_restriction_4:
		case minLength_less_than_equal_to_maxLength:
		case minLength_valid_restriction:
		case no_xmlns:
		case no_xsi:
		case totalDigits_valid_restriction:
		case whiteSpace_valid_restriction_1:
		case whiteSpace_valid_restriction_2:
		case rcase_MapAndSum_1:
		case rcase_MapAndSum_2:
		case rcase_NameAndTypeOK_1:
		case rcase_NameAndTypeOK_2:
		case rcase_NameAndTypeOK_3:
		case rcase_NameAndTypeOK_4_a:
		case rcase_NameAndTypeOK_4_b:
		case rcase_NameAndTypeOK_5:
		case rcase_NameAndTypeOK_6:
		case rcase_NameAndTypeOK_7:
		case rcase_NSCompat_1:
		case rcase_NSCompat_2:
		case rcase_NSRecurseCheckCardinality_1:
		case rcase_NSRecurseCheckCardinality_2:
		case rcase_NSSubset_1:
		case rcase_NSSubset_2:
		case rcase_NSSubset_3:
		case rcase_Recurse_1:
		case rcase_Recurse_2:
		case rcase_RecurseLax_1:
		case rcase_RecurseLax_2:
		case rcase_RecurseUnordered_1:
		case rcase_RecurseUnordered_2:
		case st_props_correct_2:
		case st_props_correct_3:
		case s4s_elt_invalid:
		case s4s_elt_invalid_content_1:
		case s4s_elt_invalid_content_2:
		case s4s_elt_invalid_content_3:
		case s4s_elt_must_match_1:
		case s4s_elt_must_match_2:
		case s4s_elt_schema_ns:
		case s4s_att_must_appear:
		case src_annotation:
		case src_attribute_1:
		case src_attribute_2:
		case src_attribute_3_1:
		case src_attribute_3_2:
		case src_attribute_4:
		case src_attribute_group_2:
		case src_attribute_group_3:
		case src_ct_4:
		case src_ct_5:
		case src_element_1:
		case src_element_2_1:
		case src_element_2_2:
		case src_element_3:
		case src_import_1_2:
		case src_redefine_5_a_a:
		case src_redefine_5_a_b:
		case src_redefine_5_a_c:
		case src_redefine_5_b_a:
		case src_redefine_5_b_b:
		case src_redefine_5_b_c:
		case src_redefine_5_b_d:
		case src_redefine_6_1_1:
		case src_redefine_6_1_2:
		case src_redefine_6_2_1:
		case src_redefine_6_2_2:
		case src_redefine_7_1:
		case src_redefine_7_2_1:
		case src_redefine_7_2_2:
		case src_simple_type_2_a:
		case src_simple_type_2_b:
		case src_simple_type_3_a:
		case src_simple_type_3_b:
		case src_single_facet_value:
		case src_union_memberTypes_or_simpleTypes:
		case c_fields_xpaths:
		case c_general_xpath:
		case c_general_xpath_ns:
		case c_selector_xpath:
		case FacetValueFromBase:
		case FacetsContradict:
		case FixedFacetValue:
		case InvalidRegex:
		case PublicSystemOnNotation:
			return XMLPositionUtility.selectStartTagName(offset, document);
		}

		return null;
	}

	public static void registerCodeActionParticipants(Map<String, ICodeActionParticipant> codeActions) {
		ICodeActionParticipant removeElementCodeAction = new s4s_elt_invalid_content_3CodeAction();
		codeActions.put(s4s_elt_invalid_content_1.getCode(), removeElementCodeAction);
		codeActions.put(s4s_elt_invalid_content_2.getCode(), removeElementCodeAction);
		codeActions.put(s4s_elt_invalid_content_3.getCode(), removeElementCodeAction);
		codeActions.put(src_single_facet_value.getCode(), removeElementCodeAction);
		codeActions.put(ag_props_correct_2.getCode(), removeElementCodeAction);
		codeActions.put(cos_applicable_facets.getCode(), removeElementCodeAction);
		ICodeActionParticipant removeAttributeCodeAction = new RemoveAttributeCodeAction();
		codeActions.put(s4s_att_not_allowed.getCode(), removeAttributeCodeAction);
		codeActions.put(EmptyTargetNamespace.getCode(), removeAttributeCodeAction);
		codeActions.put(p_props_correct_2_1.getCode(), removeAttributeCodeAction);
		codeActions.put(a_props_correct_3.getCode(), removeAttributeCodeAction);
		codeActions.put(ct_props_correct_5.getCode(), removeAttributeCodeAction);
		codeActions.put(ag_props_correct_3.getCode(), removeAttributeCodeAction);
		ICodeActionParticipant removeDefaultOrFixedCodeAction = new RemoveDefaultOrFixedCodeAction();
		codeActions.put(src_element_1.getCode(), removeDefaultOrFixedCodeAction);
		codeActions.put(src_attribute_1.getCode(), removeDefaultOrFixedCodeAction);
		codeActions.put(src_import_1_2.getCode(), new src_import_1_2CodeAction());
		codeActions.put(src_annotation.getCode(), new src_annotationCodeAction());
	}
}
