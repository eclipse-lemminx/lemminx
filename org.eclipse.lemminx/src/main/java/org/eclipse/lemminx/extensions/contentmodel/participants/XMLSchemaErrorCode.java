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
package org.eclipse.lemminx.extensions.contentmodel.participants;

import static org.eclipse.lemminx.utils.StringUtils.getString;

import java.util.HashMap;
import java.util.Map;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.apache.xerces.xni.XMLLocator;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMRange;
import org.eclipse.lemminx.dom.NoNamespaceSchemaLocation;
import org.eclipse.lemminx.dom.SchemaLocation;
import org.eclipse.lemminx.dom.SchemaLocationHint;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.TargetNamespace_1CodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.TargetNamespace_2CodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.RemoveAttributeCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.ReplaceAttributeValueCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.cvc_complex_type_2_1CodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.cvc_complex_type_2_3CodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.cvc_complex_type_2_4_aCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.cvc_complex_type_2_4_bCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.cvc_complex_type_3_1CodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.cvc_complex_type_3_2_2CodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.cvc_complex_type_4CodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.cvc_elt_1_aCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.cvc_enumeration_validCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.cvc_type_3_1_1CodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.missinggrammar.schema_reference_4CodeAction;
import org.eclipse.lemminx.extensions.xsd.utils.XSDUtils;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.diagnostics.IXMLErrorCode;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ResourceOperationKind;

/**
 * XML Schema error code.
 *
 * @see https://wiki.xmldation.com/Support/Validator
 *
 */
public enum XMLSchemaErrorCode implements IXMLErrorCode {

	// ---------- cvc-attribute ----------

	cvc_attribute_3("cvc-attribute.3"), //
	cvc_attribute_4("cvc-attribute.4"), //

	// ---------- cvc-complex-type ----------

	cvc_complex_type_2_1("cvc-complex-type.2.1"), //
	cvc_complex_type_2_2("cvc-complex-type.2.2"), //
	cvc_complex_type_2_3("cvc-complex-type.2.3"), //
	cvc_complex_type_2_4_a("cvc-complex-type.2.4.a"), //
	cvc_complex_type_2_4_b("cvc-complex-type.2.4.b"), //
	cvc_complex_type_2_4_c("cvc-complex-type.2.4.c"), //
	cvc_complex_type_2_4_d("cvc-complex-type.2.4.d"), //
	cvc_complex_type_2_4_e("cvc-complex-type.2.4.e"), //
	cvc_complex_type_2_4_f("cvc-complex-type.2.4.f"), //
	cvc_complex_type_2_4_g("cvc-complex-type.2.4.g"), //
	cvc_complex_type_2_4_h("cvc-complex-type.2.4.h"), //
	cvc_complex_type_2_4_i("cvc-complex-type.2.4.i"), //
	cvc_complex_type_2_4_j("cvc-complex-type.2.4.j"), //
	cvc_complex_type_3_1("cvc-complex-type.3.1"), //
	cvc_complex_type_3_2_1("cvc-complex-type.3.2.1"), //
	cvc_complex_type_3_2_2("cvc-complex-type.3.2.2"), //
	cvc_complex_type_4("cvc-complex-type.4"), //
	cvc_complex_type_5_1("cvc-complex-type.5.1"), //
	cvc_complex_type_5_2("cvc-complex-type.5.2"), //

	// ---------- cvc-datatype-valid ----------

	cvc_datatype_valid_1_2_1("cvc-datatype-valid.1.2.1"), //
	cvc_datatype_valid_1_2_2("cvc-datatype-valid.1.2.2"), //
	cvc_datatype_valid_1_2_3("cvc-datatype-valid.1.2.3"), //

	// ---------- cvc-elt ----------

	cvc_elt_1_a("cvc-elt.1.a"), //
	cvc_elt_1_b("cvc-elt.1.b"), //
	cvc_elt_2("cvc-elt.2"), //
	cvc_elt_3_1("cvc-elt.3.1"), //
	cvc_elt_3_2_1("cvc-elt.3.2.1"), //
	cvc_elt_3_2_2("cvc-elt.3.2.2"), //
	cvc_elt_4_1("cvc-elt.4.1"), //
	cvc_elt_4_2("cvc-elt.4.2"), //
	cvc_elt_4_3("cvc-elt.4.3"), //
	cvc_elt_5_1_1("cvc-elt.5.1.1"), //
	cvc_elt_5_2_2_1("cvc-elt.5.2.2.1"), //
	cvc_elt_5_2_2_2_1("cvc-elt.5.2.2.2.1"), //
	cvc_elt_5_2_2_2_2("cvc-elt.5.2.2.2.2"), //

	// ---------- cvc-enumeration / cvc-pattern ----------

	cvc_enumeration_valid("cvc-enumeration-valid"), //
	cvc_pattern_valid("cvc-pattern-valid"), //

	// ---------- cvc-type ----------

	cvc_type_1("cvc-type.1"), //
	cvc_type_2("cvc-type.2"), //
	cvc_type_3_1_1("cvc-type.3.1.1"), //
	cvc_type_3_1_2("cvc-type.3.1.2"), //
	cvc_type_3_1_3("cvc-type.3.1.3"), //

	// ---------- cvc-facet validation ----------

	cvc_fractionDigits_valid("cvc-fractionDigits-valid"), //
	cvc_length_valid("cvc-length-valid"), //
	cvc_maxlength_valid("cvc-maxLength-valid"), //
	cvc_minlength_valid("cvc-minLength-valid"), //
	cvc_maxExclusive_valid("cvc-maxExclusive-valid"), //
	cvc_maxInclusive_valid("cvc-maxInclusive-valid"), //
	cvc_minExclusive_valid("cvc-minExclusive-valid"), //
	cvc_minInclusive_valid("cvc-minInclusive-valid"), //
	cvc_totalDigits_valid("cvc-totalDigits-valid"), //

	// ---------- cvc-id ----------

	cvc_id_1("cvc-id.1"), //
	cvc_id_2("cvc-id.2"), //
	cvc_id_3("cvc-id.3"), //

	// ---------- Identity constraints ----------

	AbsentKeyValue, //
	DuplicateField, //
	DuplicateKey, //
	DuplicateUnique, //
	FieldMultipleMatch, //
	FixedDiffersFromActual, //
	KeyMatchesNillable, //
	KeyNotEnoughValues, //
	KeyNotFound, //
	KeyRefOutOfScope, //
	KeyRefReferNotFound, //
	UnknownField, //

	// ---------- Schema references ----------

	TargetNamespace_1("TargetNamespace.1"), //
	TargetNamespace_2("TargetNamespace.2"), //
	SchemaLocation("SchemaLocation"), //
	schema_reference_4("schema_reference.4"), //
	src_element_3("src-element.3"), //

	// ---------- Other ----------

	UndeclaredEntity, //
	UndeclaredPrefix;

	private final String code;

	private XMLSchemaErrorCode() {
		this(null);
	}

	private XMLSchemaErrorCode(String code) {
		this.code = code;
	}

	@Override
	public String getCode() {
		if (code == null) {
			return name();
		}
		return code;
	}

	@Override
	public String toString() {
		return getCode();
	}

	private final static Map<String, XMLSchemaErrorCode> codes;

	static {
		codes = new HashMap<>();
		for (XMLSchemaErrorCode errorCode : values()) {
			codes.put(errorCode.getCode(), errorCode);
		}
	}

	public static XMLSchemaErrorCode get(String name) {
		return codes.get(name);
	}

	/**
	 * Create the LSP range from the SAX error.
	 *
	 * @param location
	 * @param key
	 * @param arguments
	 * @param document
	 * @return the LSP range from the SAX error.
	 */
	public static Range toLSPRange(XMLLocator location, XMLSchemaErrorCode code, Object[] arguments,
			DOMDocument document) {
		int offset = location.getCharacterOffset() - 1;
		// adjust positions
		switch (code) {

			// ---------- Select start tag name ----------

			case cvc_complex_type_2_2:
			case cvc_complex_type_2_4_a:
			case cvc_complex_type_2_4_b:
			case cvc_complex_type_2_4_c:
			case cvc_complex_type_2_4_d:
			case cvc_complex_type_2_4_e:
			case cvc_complex_type_2_4_f:
			case cvc_complex_type_2_4_g:
			case cvc_complex_type_2_4_h:
			case cvc_complex_type_2_4_i:
			case cvc_complex_type_2_4_j:
			case cvc_complex_type_4:
			case cvc_elt_1_a:
			case cvc_elt_1_b:
			case cvc_elt_2:
			case cvc_elt_5_1_1:
			case cvc_elt_5_2_2_1:
			case cvc_type_1:
			case cvc_type_2:
			case cvc_type_3_1_2:
			case src_element_3:
			case UndeclaredPrefix:
				return XMLPositionUtility.selectStartTagName(offset, document);

			// ---------- Select first non-whitespace text ----------

			case cvc_complex_type_2_3:
				return XMLPositionUtility.selectFirstNonWhitespaceText(offset, document);

			// ---------- Select attribute name ----------

			case cvc_complex_type_3_2_1:
			case cvc_complex_type_3_2_2:
			case cvc_complex_type_5_1:
			case cvc_complex_type_5_2: {
				String attrName = getString(arguments[1]);
				return XMLPositionUtility.selectAttributeNameFromGivenNameAt(attrName, offset, document);
			}

			// ---------- Select attribute value ----------

			case cvc_attribute_3:
			case cvc_attribute_4:
			case cvc_complex_type_3_1: {
				String attrName = getString(arguments[1]);
				return XMLPositionUtility.selectAttributeValueAt(attrName, offset, document);
			}

			case cvc_elt_4_1:
			case cvc_elt_4_2:
			case cvc_elt_4_3: {
				String xsiPrefix = document.getDocumentElement()
						.getPrefix("http://www.w3.org/2001/XMLSchema-instance");
				String attrName = (xsiPrefix != null && !xsiPrefix.isEmpty()) ? xsiPrefix + ":type" : "type";
				return XMLPositionUtility.selectAttributeValueAt(attrName, offset, document);
			}

			// ---------- Select xsi:nil attribute ----------

			case cvc_elt_3_1:
			case cvc_elt_3_2_2: {
				String namespaceAntAttrName = getString(arguments[1]); // http://www.w3.org/2001/XMLSchema-instance,nil
				String attrName = namespaceAntAttrName;
				int index = namespaceAntAttrName.indexOf(",");
				if (index != -1) {
					String namespaceURI = namespaceAntAttrName.substring(0, index);
					String prefix = document.getDocumentElement().getPrefix(namespaceURI);
					attrName = namespaceAntAttrName.substring(index + 1, namespaceAntAttrName.length());
					if (prefix != null && !prefix.isEmpty()) {
						attrName = prefix + ":" + attrName;
					}
				}
				return XMLPositionUtility.selectAttributeFromGivenNameAt(attrName, offset, document);
			}

			// ---------- Select all attributes ----------

			case cvc_type_3_1_1:
				return XMLPositionUtility.selectAllAttributes(offset, document);

			// ---------- Select content ----------

			case cvc_complex_type_2_1:
			case cvc_elt_3_2_1:
				return XMLPositionUtility.selectContent(offset, document);

			// ---------- Select content (value mismatch) ----------

			case cvc_elt_5_2_2_2_1:
			case cvc_elt_5_2_2_2_2:
			case FixedDiffersFromActual: {
				DOMElement element = (DOMElement) document.findNodeAt(offset);
				if (element != null && DOMUtils.containsTextOnly(element)) {
					return XMLPositionUtility.selectTrimmedText(offset, document);
				}
				return XMLPositionUtility.selectContent(offset, document);
			}

			// ---------- Select pattern value ----------

			case cvc_pattern_valid: {
				String value = getString(arguments[0]);
				Range result = XMLPositionUtility.selectAttributeValueByGivenValueAt(value, offset, document);
				if (result != null) {
					return result;
				}
				return XMLPositionUtility.selectTrimmedText(offset, document);
			}

			// ---------- Value validation (attribute value or text) ----------

			case cvc_type_3_1_3:
			case cvc_datatype_valid_1_2_1:
			case cvc_datatype_valid_1_2_2:
			case cvc_datatype_valid_1_2_3:
			case cvc_enumeration_valid:
			case cvc_fractionDigits_valid:
			case cvc_length_valid:
			case cvc_maxlength_valid:
			case cvc_minlength_valid:
			case cvc_maxExclusive_valid:
			case cvc_maxInclusive_valid:
			case cvc_minExclusive_valid:
			case cvc_minInclusive_valid:
			case cvc_totalDigits_valid: {
				// this error can occur for attribute value or text
				// Try for attribute value
				String attrValue = getString(arguments[0]);
				Range range = XMLPositionUtility.selectAttributeValueFromGivenValue(attrValue, offset, document);
				if (range != null) {
					return range;
				} else {
					// Try with text
					DOMElement element = (DOMElement) document.findNodeAt(offset);
					if (element != null && element.isEmpty()) {
						return XMLPositionUtility.selectStartTagName(element);
					} else if (DOMUtils.containsTextOnly(element)) {
						return XMLPositionUtility.selectTrimmedText(offset, document);
					} else {
						return XMLPositionUtility.selectFirstChild(offset, document);
					}
				}
			}

			// ---------- ID/IDREF validation ----------

			case cvc_id_1:
			case cvc_id_2: {
				String idValue = getString(arguments[0]);
				return XMLPositionUtility.selectAttributeValueByGivenValueAt(idValue, offset, document);
			}

			case cvc_id_3:
				return XMLPositionUtility.selectStartTagName(offset, document);

			// ---------- Identity constraints ----------

			case DuplicateKey:
			case DuplicateUnique: {
				String attrValue = getString(arguments[0]);
				Range range = XMLPositionUtility.selectAttributeValueFromGivenValue(attrValue, offset, document);
				if (range != null) {
					return range;
				} else {
					return XMLPositionUtility.selectContent(offset, document);
				}
			}

			case AbsentKeyValue:
			case DuplicateField:
			case FieldMultipleMatch:
			case KeyMatchesNillable:
			case KeyNotEnoughValues:
			case KeyNotFound:
			case KeyRefOutOfScope:
			case KeyRefReferNotFound:
			case UnknownField:
				return XMLPositionUtility.selectStartTagName(offset, document);

			// ---------- Entity reference ----------

			case UndeclaredEntity: {
				XMLPositionUtility.EntityReferenceRange entityRange = XMLPositionUtility
						.selectEntityReference(offset - 1, document);
				if (entityRange != null) {
					return entityRange.getRange();
				}
				return XMLPositionUtility.selectStartTagName(offset, document);
			}

			// ---------- Schema location ----------

			case SchemaLocation: {
				SchemaLocation schemaLocation = document.getSchemaLocation();
				DOMRange locationRange = schemaLocation.getAttr().getNodeAttrValue();
				return locationRange != null ? XMLPositionUtility.createRange(locationRange) : null;
			}

			case schema_reference_4: {
				String grammarURI = arguments.length == 1 ? (String) arguments[0] : null;
				if (DOMUtils.isXSD(document)) {
					DOMAttr schemaLocationAttr = XSDUtils.findSchemaLocationAttrByURI(document, grammarURI);
					if (schemaLocationAttr != null) {
						return XMLPositionUtility.selectAttributeValue(schemaLocationAttr, true);
					}
				} else {
					DOMRange locationRange = XMLModelUtils.getHrefNode(document, grammarURI);
					if (locationRange == null) {
						NoNamespaceSchemaLocation noNamespaceSchemaLocation = document.getNoNamespaceSchemaLocation();
						if (noNamespaceSchemaLocation != null) {
							locationRange = noNamespaceSchemaLocation.getAttr().getNodeAttrValue();
						} else {
							SchemaLocation schemaLocation = document.getSchemaLocation();
							if (schemaLocation != null) {
								String invalidSchemaPath = arguments[0] instanceof String ? (String) arguments[0]
										: null;

								if (invalidSchemaPath != null) {
									for (SchemaLocationHint locHintRange : schemaLocation.getSchemaLocationHints()) {
										String expandedHint = getResolvedLocation(document.getDocumentURI(),
												locHintRange.getHint());
										if (invalidSchemaPath.equals(expandedHint)) {
											return XMLPositionUtility.createRange(locHintRange);
										}
									}
								}
								locationRange = schemaLocation.getAttr().getNodeAttrValue();
							} else {
								DOMElement documentElement = document.getDocumentElement();
								if (documentElement != null) {
									return XMLPositionUtility.selectStartTagName(documentElement);
								}
							}
						}
					}
					return locationRange != null ? XMLPositionUtility.selectValueWithoutQuote(locationRange) : null;
				}
			}

			// ---------- Target namespace ----------

			case TargetNamespace_1:
				return XMLPositionUtility.selectRootAttributeValue(DOMAttr.XMLNS_ATTR, document);
			case TargetNamespace_2:
				return XMLPositionUtility.selectRootStartTag(document);
			default:
		}
		return null;
	}

	public static void registerCodeActionParticipants(Map<String, ICodeActionParticipant> codeActions,
			SharedSettings sharedSettings) {
		codeActions.put(cvc_complex_type_2_3.getCode(), new cvc_complex_type_2_3CodeAction());
		codeActions.put(cvc_complex_type_2_1.getCode(), new cvc_complex_type_2_1CodeAction());
		ICodeActionParticipant invalidContentCodeAction = new cvc_complex_type_2_4_aCodeAction();
		codeActions.put(cvc_complex_type_2_4_a.getCode(), invalidContentCodeAction);
		codeActions.put(cvc_complex_type_2_4_c.getCode(), invalidContentCodeAction);
		codeActions.put(cvc_complex_type_2_4_d.getCode(), invalidContentCodeAction);
		codeActions.put(cvc_complex_type_2_4_e.getCode(), invalidContentCodeAction);
		codeActions.put(cvc_complex_type_2_4_f.getCode(), invalidContentCodeAction);
		codeActions.put(cvc_complex_type_2_4_g.getCode(), invalidContentCodeAction);
		codeActions.put(cvc_complex_type_2_4_h.getCode(), invalidContentCodeAction);
		ICodeActionParticipant incompleteContentCodeAction = new cvc_complex_type_2_4_bCodeAction();
		codeActions.put(cvc_complex_type_2_4_b.getCode(), incompleteContentCodeAction);
		codeActions.put(cvc_complex_type_2_4_i.getCode(), incompleteContentCodeAction);
		codeActions.put(cvc_complex_type_2_4_j.getCode(), incompleteContentCodeAction);
		codeActions.put(cvc_complex_type_3_1.getCode(), new cvc_complex_type_3_1CodeAction());
		ICodeActionParticipant replaceAttributeNameCodeAction = new cvc_complex_type_3_2_2CodeAction();
		codeActions.put(cvc_complex_type_3_2_1.getCode(), replaceAttributeNameCodeAction);
		codeActions.put(cvc_complex_type_3_2_2.getCode(), replaceAttributeNameCodeAction);
		ICodeActionParticipant removeAttributeCodeAction = new RemoveAttributeCodeAction();
		codeActions.put(cvc_complex_type_5_1.getCode(), removeAttributeCodeAction);
		codeActions.put(cvc_complex_type_5_2.getCode(), removeAttributeCodeAction);
		codeActions.put(cvc_complex_type_4.getCode(), new cvc_complex_type_4CodeAction());
		codeActions.put(cvc_elt_1_a.getCode(), new cvc_elt_1_aCodeAction());
		codeActions.put(cvc_type_3_1_1.getCode(), new cvc_type_3_1_1CodeAction());
		ICodeActionParticipant replaceAttributeValueCodeAction = new ReplaceAttributeValueCodeAction();
		codeActions.put(cvc_attribute_3.getCode(), replaceAttributeValueCodeAction);
		codeActions.put(cvc_attribute_4.getCode(), replaceAttributeValueCodeAction);
		codeActions.put(cvc_enumeration_valid.getCode(), new cvc_enumeration_validCodeAction());
		codeActions.put(TargetNamespace_1.getCode(), new TargetNamespace_1CodeAction());
		codeActions.put(TargetNamespace_2.getCode(), new TargetNamespace_2CodeAction());
		if (sharedSettings != null
				&& sharedSettings.getWorkspaceSettings().isResourceOperationSupported(ResourceOperationKind.Create)) {
			codeActions.put(schema_reference_4.getCode(), new schema_reference_4CodeAction());
		}
	}

	/**
	 * Returns the expanded system location
	 *
	 * @return the expanded system location
	 */
	private static String getResolvedLocation(String documentURI, String location) {
		if (location == null) {
			return null;
		}
		try {
			return XMLEntityManager.expandSystemId(location, documentURI, false);
		} catch (MalformedURIException e) {
			return location;
		}
	}
}
