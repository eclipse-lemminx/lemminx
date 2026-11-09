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
import java.util.List;
import java.util.Map;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.apache.xerces.xni.XMLLocator;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMDocumentType;
import org.eclipse.lemminx.dom.DTDAttlistDecl;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMRange;
import org.eclipse.lemminx.dom.DTDDeclParameter;
import org.eclipse.lemminx.dom.DTDElementDecl;
import org.eclipse.lemminx.dom.DTDEntityDecl;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.AttDefaultInvalidCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.AttTypeRequiredInAttDefCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.ContentModelCloseParenCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.ContentSpecRequiredCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.ExternalIdCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.NotationDeclTypeCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.ElementDeclUnterminatedCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.EnumerationUnterminatedCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.MixedContentUnterminatedCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.EntityNotDeclaredCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.ExternalIDorPublicIDRequiredCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.IDDefaultTypeInvalidCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.FixMissingSpaceCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.ClosingQuoteMissingCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.MSG_ATTRIBUTE_NOT_DECLAREDCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.MSG_MARKUP_NOT_RECOGNIZED_IN_DTDCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.MSG_ATTRIBUTE_VALUE_NOT_IN_LISTCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.MSG_CONTENT_INCOMPLETECodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.MSG_CONTENT_INVALIDCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.MSG_ELEMENT_NOT_DECLAREDCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.MSG_FIXED_ATTVALUE_INVALIDCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.NotationNotDeclaredForEntityCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.MSG_REQUIRED_ATTRIBUTE_NOT_SPECIFIEDCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.QuoteRequiredInDeclCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.RemoveDTDDeclCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.missinggrammar.DTDNotFoundCodeAction;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.services.extensions.diagnostics.IXMLErrorCode;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lemminx.utils.XMLPositionUtility.EntityReferenceRange;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ResourceOperationKind;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.w3c.dom.NamedNodeMap;

/**
 * DTD error code.
 *
 * @see https://wiki.xmldation.com/Support/Validator
 *
 */
public enum DTDErrorCode implements IXMLErrorCode {

	AttNameRequiredInAttDef, //
	AttTypeRequiredInAttDef, //
	DoctypedeclUnterminated, //
	DuplicateTypeInMixedContent, //
	ElementDeclUnterminated, //
	EmptyList, //
	ENTITIESInvalid, //
	ENTITYInvalid, //
	ENTITYNotUnparsed, //
	ENTITYNotValid, //
	EntityDeclUnterminated, //
	EntityExpansionLimitExceeded, //
	EntityNotDeclared, //
	EnumerationUnterminated, //
	ExternalIDorPublicIDRequired, //
	ExternalIDRequired, //
	IDDefaultTypeInvalid, //
	IDInvalid, //
	IDInvalidWithNamespaces, //
	IDNotUnique, //
	IDREFInvalid, //
	IDREFInvalidWithNamespaces, //
	IDREFSInvalid, //
	IgnoreSectUnterminated, //
	ILL_FORMED_PARAMETER_ENTITY_WHEN_USED_IN_DECL, //
	ImproperDeclarationNesting, //
	ImproperGroupNesting, //
	IncludeSectUnterminated, //
	INVALID_PE_IN_CONDITIONAL, //
	InvalidCharInEntityValue, //
	InvalidCharInExternalSubset, //
	InvalidCharInIgnoreSect, //
	InvalidCharInInternalSubset, //
	InvalidCharInPublicID, //
	InvalidCharInSystemID, //
	InvalidCharInTextDecl, //
	LessthanInAttValue, //
	MixedContentUnterminated, //
	MSG_ATT_DEFAULT_INVALID, //
	MSG_ATTRIBUTE_NOT_DECLARED, //
	MSG_ATTRIBUTE_VALUE_NOT_IN_LIST, //
	MSG_ATTVALUE_CHANGED_DURING_NORMALIZATION_WHEN_STANDALONE, //
	MSG_CLOSE_PAREN_REQUIRED_IN_CHILDREN, //
	MSG_CLOSE_PAREN_REQUIRED_IN_MIXED, //
	MSG_CONTENT_INCOMPLETE, //
	MSG_CONTENT_INVALID, //
	MSG_CONTENT_INVALID_SPECIFIED, //
	MSG_CONTENTSPEC_REQUIRED_IN_ELEMENTDECL, //
	MSG_DEFAULTED_ATTRIBUTE_NOT_SPECIFIED, //
	MSG_DISTINCT_NOTATION_IN_ENUMERATION, //
	MSG_DISTINCT_TOKENS_IN_ENUMERATION, //
	MSG_DUPLICATE_ATTDEF, //
	MSG_DUPLICATE_ATTRIBUTE_DEFINITION, //
	MSG_DUPLICATE_ENTITY_DEFINITION, //
	MSG_ELEMENT_ALREADY_DECLARED, //
	MSG_ELEMENT_NOT_DECLARED, //
	MSG_ELEMENT_TYPE_REQUIRED_IN_ATTLISTDECL, //
	MSG_ELEMENT_TYPE_REQUIRED_IN_ELEMENTDECL, //
	MSG_ELEMENT_TYPE_REQUIRED_IN_MIXED_CONTENT, //
	MSG_ELEMENT_WITH_ID_REQUIRED, //
	MSG_ENTITY_NAME_REQUIRED_IN_ENTITYDECL, //
	MSG_EXTERNAL_ENTITY_NOT_PERMITTED, //
	MSG_FIXED_ATTVALUE_INVALID, //
	MSG_GRAMMAR_NOT_FOUND, //
	MSG_MARKUP_NOT_RECOGNIZED_IN_DTD, //
	MSG_MORE_THAN_ONE_ID_ATTRIBUTE, //
	MSG_MORE_THAN_ONE_NOTATION_ATTRIBUTE, //
	MSG_NAME_REQUIRED_IN_NOTATIONTYPE, //
	MSG_NMTOKEN_REQUIRED_IN_ENUMERATION, //
	MSG_NOTATION_NAME_REQUIRED_FOR_UNPARSED_ENTITYDECL, //
	MSG_NOTATION_NAME_REQUIRED_IN_NOTATIONDECL, //
	MSG_NOTATION_NOT_DECLARED_FOR_NOTATIONTYPE_ATTRIBUTE, //
	MSG_NOTATION_NOT_DECLARED_FOR_UNPARSED_ENTITYDECL, //
	MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN, //
	MSG_OPEN_PAREN_REQUIRED_IN_NOTATIONTYPE, //
	MSG_REFERENCE_TO_EXTERNALLY_DECLARED_ENTITY_WHEN_STANDALONE, //
	MSG_REQUIRED_ATTRIBUTE_NOT_SPECIFIED, //
	MSG_ROOT_ELEMENT_TYPE_REQUIRED, //
	MSG_SPACE_REQUIRED_AFTER_ENTITY_NAME_IN_ENTITYDECL, //
	MSG_SPACE_REQUIRED_AFTER_FIXED_IN_DEFAULTDECL, //
	MSG_SPACE_REQUIRED_AFTER_NOTATION_IN_NOTATIONTYPE, //
	MSG_SPACE_REQUIRED_AFTER_NOTATION_NAME_IN_NOTATIONDECL, //
	MSG_SPACE_REQUIRED_AFTER_PUBIDLITERAL_IN_EXTERNALID, //
	MSG_SPACE_REQUIRED_AFTER_SYSTEMLITERAL_IN_EXTERNALID, //
	MSG_SPACE_REQUIRED_BEFORE_ATTRIBUTE_NAME_IN_ATTDEF, //
	MSG_SPACE_REQUIRED_BEFORE_ATTTYPE_IN_ATTDEF, //
	MSG_SPACE_REQUIRED_BEFORE_CONTENTSPEC_IN_ELEMENTDECL, //
	MSG_SPACE_REQUIRED_BEFORE_DEFAULTDECL_IN_ATTDEF, //
	MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ATTLISTDECL, //
	MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ELEMENTDECL, //
	MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_ENTITYDECL, //
	MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_PEDECL, //
	MSG_SPACE_REQUIRED_BEFORE_NDATA_IN_UNPARSED_ENTITYDECL, //
	MSG_SPACE_REQUIRED_BEFORE_NOTATION_NAME_IN_NOTATIONDECL, //
	MSG_SPACE_REQUIRED_BEFORE_NOTATION_NAME_IN_UNPARSED_ENTITYDECL, //
	MSG_SPACE_REQUIRED_BEFORE_PERCENT_IN_PEDECL, //
	MSG_SPACE_REQUIRED_BEFORE_PUBIDLITERAL_IN_EXTERNALID, //
	MSG_SPACE_REQUIRED_BEFORE_ROOT_ELEMENT_TYPE_IN_DOCTYPEDECL, //
	MSG_SPACE_REQUIRED_BEFORE_SYSTEMLITERAL_IN_EXTERNALID, //
	MSG_URI_FRAGMENT_IN_SYSTEMID, //
	MSG_WHITE_SPACE_IN_ELEMENT_CONTENT_WHEN_STANDALONE, //
	MSG_XML_SPACE_DECLARATION_ILLEGAL, //
	NameRequiredInPEReference, //
	NMTOKENInvalid, //
	NMTOKENSInvalid, //
	NoNotationOnEmptyElement, //
	NotationDeclUnterminated, //
	NotationTypeUnterminated, //
	OpenQuoteExpected, //
	OpenQuoteMissingInDecl, //
	PEReferenceWithinMarkup, //
	PublicIDUnterminated, //
	PubidCharIllegal, //
	QuoteRequiredInPublicID, //
	QuoteRequiredInSystemID, //
	RecursiveGeneralReference, //
	RecursivePEReference, //
	RecursiveReference, //
	ReferenceToExternalEntity, //
	ReferenceToUnparsedEntity, //
	SemicolonRequiredInPEReference, //
	SpaceRequiredAfterPUBLIC, //
	SpaceRequiredAfterSYSTEM, //
	SpaceRequiredBetweenPublicAndSystem, //
	SystemIDUnterminated, //
	UndeclaredElementInContentSpec, //
	UniqueNotationName, //
	DTDNotFound;

	private final String code;

	private DTDErrorCode() {
		this(null);
	}

	private DTDErrorCode(String code) {
		this.code = code;
	}

	@Override
	public String getCode() {
		if (code == null) {
			return name();
		}
		return code;
	}

	private final static Map<String, DTDErrorCode> codes;

	static {
		codes = new HashMap<>();
		for (DTDErrorCode errorCode : values()) {
			codes.put(errorCode.getCode(), errorCode);
		}
	}

	public static DTDErrorCode get(String name) {
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
	public static Range toLSPRange(XMLLocator location, DTDErrorCode code, Object[] arguments, DOMDocument document) {
		int offset = location.getCharacterOffset() - 1;
		// adjust positions
		switch (code) {

		// ---------- XML validation errors (select start tag name) ----------

		case MSG_CONTENT_INCOMPLETE:
		case MSG_REQUIRED_ATTRIBUTE_NOT_SPECIFIED:
		case MSG_ELEMENT_NOT_DECLARED:
		case MSG_CONTENT_INVALID:
		case MSG_CONTENT_INVALID_SPECIFIED:
		case MSG_DEFAULTED_ATTRIBUTE_NOT_SPECIFIED: {
			return XMLPositionUtility.selectStartTagName(offset, document);
		}
		case MSG_WHITE_SPACE_IN_ELEMENT_CONTENT_WHEN_STANDALONE: {
			// When the offset lands on a whitespace-only DOMText node
			// (e.g. standalone document with externally declared element-only model),
			// highlight the enclosing element instead.
			DOMNode node = document.findNodeAt(offset);
			if (node != null && node.isText()) {
				DOMElement parentElement = node.getParentElement();
				if (parentElement != null) {
					return XMLPositionUtility.selectStartTagName(parentElement);
				}
			}
			return XMLPositionUtility.selectStartTagName(offset, document);
		}
		case MSG_ATTRIBUTE_NOT_DECLARED: {
			return XMLPositionUtility.selectAttributeNameFromGivenNameAt(getString(arguments[1]), offset, document);
		}
		case MSG_FIXED_ATTVALUE_INVALID: {
			String attrName = getString(arguments[1]);
			return XMLPositionUtility.selectAttributeValueAt(attrName, offset, document);
		}
		case MSG_ATTRIBUTE_VALUE_NOT_IN_LIST: {
			String attrName = getString(arguments[0]);
			return XMLPositionUtility.selectAttributeValueAt(attrName, offset, document);
		}
		case MSG_ATTVALUE_CHANGED_DURING_NORMALIZATION_WHEN_STANDALONE: {
			String attrName = getString(arguments[0]);
			return XMLPositionUtility.selectAttributeValueAt(attrName, offset, document);
		}

		case MSG_ELEMENT_WITH_ID_REQUIRED: {
			DOMElement element = document.getDocumentElement();
			if (element != null) {
				return XMLPositionUtility.selectStartTagName(element);
			}
		}

		// ---------- ID/IDREF/NMTOKEN/ENTITY attribute value validation ----------

		case IDInvalid:
		case IDNotUnique:
		case IDREFInvalid:
		case NMTOKENInvalid:
		case NMTOKENSInvalid:
		case IDREFSInvalid:
		case IDREFInvalidWithNamespaces:
		case IDInvalidWithNamespaces: {
			String attrValue = getString(arguments[0]);
			return XMLPositionUtility.selectAttributeValueByGivenValueAt(attrValue, offset, document);
		}
		case ENTITIESInvalid:
		case ENTITYInvalid: {
			// arguments[0] = attribute name, arguments[1] = entity name
			String attrName = getString(arguments[0]);
			String entityName = getString(arguments[1]);
			Range range = XMLPositionUtility.selectAttributeValueByGivenValueAt(entityName, offset, document);
			if (range != null) {
				return range;
			}
			// Token within a space-separated list (e.g. "a" in refs="a b")
			return XMLPositionUtility.selectAttributeValueTokenAt(attrName, entityName, offset, document);
		}
		case ENTITYNotUnparsed:
		case ENTITYNotValid: {
			String attrValue = getString(arguments[0]);
			return XMLPositionUtility.selectAttributeValueByGivenValueAt(attrValue, offset, document);
		}
		case EmptyList: {
			if (arguments == null || arguments.length == 0) {
				return null;
			}
			String attrValue = getString(arguments[0]);
			return XMLPositionUtility.selectAttributeValueByGivenValueAt(attrValue, offset, document);
		}

		// ---------- Multiple attribute type declarations ----------

		case MSG_MORE_THAN_ONE_ID_ATTRIBUTE:
		case MSG_MORE_THAN_ONE_NOTATION_ATTRIBUTE: {
			String attrName = getString(arguments[2]);
			return XMLPositionUtility.selectAttlistAttributeAt(attrName, offset, document);
		}

		// ---------- Grammar not found ----------

		case MSG_GRAMMAR_NOT_FOUND: {
			return XMLPositionUtility.selectRootStartTag(document);
		}

		// ---------- DTD markup not recognized ----------

		case MSG_MARKUP_NOT_RECOGNIZED_IN_DTD: {
			return XMLPositionUtility.selectWholeTag(offset + 2, document);
		}

		// ---------- DTD Doc type ----------

		case ExternalIDorPublicIDRequired:
		case ExternalIDRequired: {
			return XMLPositionUtility.selectDTDDeclNameAt(offset, document);
		}

		case PEReferenceWithinMarkup: {
			return XMLPositionUtility.getLastValidDTDDeclParameter(offset, document, true);
		}

		// ---------- Entity reference errors ----------

		case EntityExpansionLimitExceeded:
		case EntityNotDeclared:
		case ReferenceToExternalEntity:
		case ReferenceToUnparsedEntity:
		case RecursiveGeneralReference:
		case RecursiveReference:
		case RecursivePEReference:
		case MSG_EXTERNAL_ENTITY_NOT_PERMITTED:
		case MSG_REFERENCE_TO_EXTERNALLY_DECLARED_ENTITY_WHEN_STANDALONE: {
			EntityReferenceRange range = XMLPositionUtility.selectEntityReference(offset - 1, document);
			return range != null ? range.getRange() : null;
		}

		// ---------- DOCTYPE unterminated ----------

		case DoctypedeclUnterminated: {
			try {
				CharSequence text = document.getTextSequence();
				int pos = offset - 1;
				while (pos >= 0) {
					char c = text.charAt(pos);
					if (c == ']') {
						return new Range(document.positionAt(pos), document.positionAt(pos + 1));
					}
					if (!Character.isWhitespace(c)) {
						break;
					}
					pos--;
				}
				return new Range(document.positionAt(offset), document.positionAt(offset + 1));
			} catch (BadLocationException e) {
			}
			return null;
		}

		// ---------- DTD declaration errors (getLastValidDTDDeclParameterOrUnrecognized) ----------

		// ---------- Select whole last parameter (for space/close code actions) ----------

		case SpaceRequiredAfterSYSTEM:
		case SpaceRequiredAfterPUBLIC:
		case MSG_SPACE_REQUIRED_AFTER_NOTATION_NAME_IN_NOTATIONDECL:
		case MSG_SPACE_REQUIRED_BEFORE_ATTRIBUTE_NAME_IN_ATTDEF:
		case MSG_SPACE_REQUIRED_BEFORE_ATTTYPE_IN_ATTDEF:
		case MSG_SPACE_REQUIRED_BEFORE_DEFAULTDECL_IN_ATTDEF:
		case MSG_SPACE_REQUIRED_AFTER_FIXED_IN_DEFAULTDECL:
		case MSG_SPACE_REQUIRED_BEFORE_PERCENT_IN_PEDECL:
		case MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_PEDECL:
		case MSG_SPACE_REQUIRED_AFTER_ENTITY_NAME_IN_ENTITYDECL:
		case MSG_SPACE_REQUIRED_BEFORE_NDATA_IN_UNPARSED_ENTITYDECL:
		case MSG_SPACE_REQUIRED_BEFORE_NOTATION_NAME_IN_UNPARSED_ENTITYDECL:
		case MSG_SPACE_REQUIRED_BEFORE_PUBIDLITERAL_IN_EXTERNALID:
		case MSG_SPACE_REQUIRED_AFTER_PUBIDLITERAL_IN_EXTERNALID:
		case MSG_SPACE_REQUIRED_BEFORE_SYSTEMLITERAL_IN_EXTERNALID:
		case MSG_SPACE_REQUIRED_AFTER_SYSTEMLITERAL_IN_EXTERNALID:
		case MSG_SPACE_REQUIRED_BEFORE_NOTATION_NAME_IN_NOTATIONDECL:
		case MSG_SPACE_REQUIRED_AFTER_NOTATION_IN_NOTATIONTYPE:
		case MSG_CONTENTSPEC_REQUIRED_IN_ELEMENTDECL:
		case ElementDeclUnterminated:
		case EntityDeclUnterminated:
		case NotationDeclUnterminated: {
			return XMLPositionUtility.getLastValidDTDDeclParameterOrUnrecognized(offset, document, true);
		}

		case QuoteRequiredInPublicID:
		case QuoteRequiredInSystemID:
		case OpenQuoteMissingInDecl:
		case SpaceRequiredBetweenPublicAndSystem:
		case AttTypeRequiredInAttDef:
		case LessthanInAttValue:
		case OpenQuoteExpected:
		case AttNameRequiredInAttDef:
		case MixedContentUnterminated:
		case NotationTypeUnterminated:
		case EnumerationUnterminated:
		case SystemIDUnterminated:
		case PublicIDUnterminated:
		case IncludeSectUnterminated:
		case IgnoreSectUnterminated:
		case PubidCharIllegal:
		case MSG_OPEN_PAREN_REQUIRED_IN_NOTATIONTYPE:
		case MSG_NAME_REQUIRED_IN_NOTATIONTYPE:
		case MSG_NMTOKEN_REQUIRED_IN_ENUMERATION:
		case NameRequiredInPEReference:
		case SemicolonRequiredInPEReference:
		case MSG_NOTATION_NAME_REQUIRED_FOR_UNPARSED_ENTITYDECL:
		case MSG_URI_FRAGMENT_IN_SYSTEMID:
		case InvalidCharInEntityValue:
		case InvalidCharInExternalSubset:
		case InvalidCharInIgnoreSect:
		case InvalidCharInInternalSubset:
		case InvalidCharInPublicID:
		case InvalidCharInSystemID:
		case InvalidCharInTextDecl:
		case MSG_ROOT_ELEMENT_TYPE_REQUIRED: {
			return XMLPositionUtility.getLastValidDTDDeclParameterOrUnrecognized(offset, document);
		}

		// ---------- DTD content model errors ----------

		case MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN: {
			// <!ELEMENT root > or <!ELEMENT root XXXX> → highlights element name "root"
			return XMLPositionUtility.selectDTDDeclNameAt(offset, document);
		}

		case MSG_CLOSE_PAREN_REQUIRED_IN_CHILDREN:
		case MSG_CLOSE_PAREN_REQUIRED_IN_MIXED:
		case MSG_ELEMENT_TYPE_REQUIRED_IN_MIXED_CONTENT: {
			return XMLPositionUtility.getElementDeclMissingContentOrCategory(offset, document);
		}

		// ---------- Undeclared notation in unparsed entity ----------

		case MSG_NOTATION_NOT_DECLARED_FOR_UNPARSED_ENTITYDECL: {
			// arguments[0] = entity name, arguments[1] = notation name
			// Reported at endDTD, offset may not point to the entity.
			if (arguments != null && arguments.length >= 1) {
				String entityName = getString(arguments[0]);
				DOMDocumentType docType = document.getDoctype();
				if (docType != null && entityName != null) {
					for (DOMNode child : docType.getChildren()) {
						if (child instanceof DTDEntityDecl) {
							DTDEntityDecl entity = (DTDEntityDecl) child;
							if (entityName.equals(entity.getName())) {
								DTDDeclParameter nameParam = entity.getNameParameter();
								if (nameParam != null) {
									return XMLPositionUtility.createRange(nameParam.getStart(),
											nameParam.getEnd(), document);
								}
							}
						}
					}
				}
			}
			return XMLPositionUtility.selectDTDDeclTagNameAt(offset, document);
		}

		// ---------- DTD duplicate declaration errors (name range) ----------

		case MSG_ELEMENT_ALREADY_DECLARED:
		case MSG_DUPLICATE_ATTRIBUTE_DEFINITION:
		case MSG_DUPLICATE_ENTITY_DEFINITION: {
			return XMLPositionUtility.selectDTDDeclNameAt(offset, document);
		}

		// ---------- ATTLIST default value invalid ----------

		case MSG_ATT_DEFAULT_INVALID: {
			// <!ATTLIST root status (one|two|three) "four"> → highlights "four"
			return XMLPositionUtility.selectAttlistAttributeValueAt(offset, document);
		}

		// ---------- DTD declaration tag name errors ----------

		case MSG_NOTATION_NAME_REQUIRED_IN_NOTATIONDECL:
		case MSG_ENTITY_NAME_REQUIRED_IN_ENTITYDECL:
		case MSG_ELEMENT_TYPE_REQUIRED_IN_ATTLISTDECL:
		case MSG_ELEMENT_TYPE_REQUIRED_IN_ELEMENTDECL:
		case MSG_DUPLICATE_ATTDEF:
		case UniqueNotationName:
		case UndeclaredElementInContentSpec:
		case IDDefaultTypeInvalid:
		case MSG_XML_SPACE_DECLARATION_ILLEGAL:
		case MSG_NOTATION_NOT_DECLARED_FOR_NOTATIONTYPE_ATTRIBUTE:
		case MSG_DISTINCT_NOTATION_IN_ENUMERATION: {
			return XMLPositionUtility.selectDTDDeclTagNameAt(offset, document);
		}

		case DuplicateTypeInMixedContent: {
			// arguments[0] = element decl name, arguments[1] = duplicate element type name
			// Reported at endDTD, offset may not point to the ELEMENT declaration.
			String elementName = getString(arguments[0]);
			String duplicateName = getString(arguments[1]);
			DOMDocumentType dtDocType = document.getDoctype();
			if (dtDocType != null) {
				for (DOMNode child : dtDocType.getChildren()) {
					if (child instanceof DTDElementDecl) {
						DTDElementDecl elementDecl = (DTDElementDecl) child;
						if (elementName.equals(elementDecl.getName())) {
							Range range = XMLPositionUtility.selectDuplicateMixedContentType(
									duplicateName, elementDecl, document);
							if (range != null) {
								return range;
							}
						}
					}
				}
			}
			return XMLPositionUtility.selectDTDDeclTagNameAt(offset, document);
		}

		case NoNotationOnEmptyElement: {
			// arguments[0] = element name, arguments[1] = attribute name
			// Reported at endDTD, offset may not point to the ATTLIST.
			String attrName = getString(arguments[1]);
			DOMDocumentType docType = document.getDoctype();
			if (docType != null) {
				for (DOMNode child : docType.getChildren()) {
					if (child instanceof DTDAttlistDecl) {
						DTDAttlistDecl attlist = (DTDAttlistDecl) child;
						if (attrName.equals(attlist.getAttributeName()) && attlist.attributeName != null) {
							return XMLPositionUtility.createRange(attlist.attributeName.getStart(),
									attlist.attributeName.getEnd(), document);
						}
						java.util.List<DTDAttlistDecl> internalChildren = attlist.getInternalChildren();
						if (internalChildren != null) {
							for (DTDAttlistDecl inner : internalChildren) {
								if (attrName.equals(inner.getAttributeName())
										&& inner.attributeName != null) {
									return XMLPositionUtility.createRange(inner.attributeName.getStart(),
											inner.attributeName.getEnd(), document);
								}
							}
						}
					}
				}
			}
			return XMLPositionUtility.selectDTDDeclTagNameAt(offset, document);
		}

		case MSG_DISTINCT_TOKENS_IN_ENUMERATION: {
			// <!ATTLIST root status (one|two|one) #IMPLIED> → highlights "|one" (duplicate)
			// arguments[1] = duplicate token name
			String tokenName = getString(arguments[1]);
			Range range = XMLPositionUtility.selectDuplicateEnumTokenAt(tokenName, offset, document);
			if (range != null) {
				return range;
			}
			return XMLPositionUtility.selectDTDDeclTagNameAt(offset, document);
		}

		// ---------- DTD declaration space-required (before content spec) ----------

		case MSG_SPACE_REQUIRED_BEFORE_CONTENTSPEC_IN_ELEMENTDECL: {
			return XMLPositionUtility.selectDTDDeclNameAt(offset, document);
		}

		// ---------- DTD declaration space-required (before tag name) ----------

		case MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ATTLISTDECL:
		case MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ELEMENTDECL:
		case MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_ENTITYDECL:
		case MSG_SPACE_REQUIRED_BEFORE_ROOT_ELEMENT_TYPE_IN_DOCTYPEDECL: {
			return XMLPositionUtility.selectDTDDeclTagNameAt(offset, document);
		}

		// ---------- Parameter entity errors ----------

		case ILL_FORMED_PARAMETER_ENTITY_WHEN_USED_IN_DECL:
		case ImproperDeclarationNesting:
		case ImproperGroupNesting:
		case INVALID_PE_IN_CONDITIONAL: {
			return XMLPositionUtility.getLastValidDTDDeclParameterOrUnrecognized(offset, document);
		}

		// ---------- DTD Not Found ----------

		case DTDNotFound: {

			// Check if DTD location is declared with xml-model/@href
			// ex : <xml-model href="http://www.docbook.org/xml/4.4/docbookx.dtd" [
			String hrefLocation = (String) arguments[1];
			DOMRange locationRange = XMLModelUtils.getHrefNode(document, hrefLocation);
			if (locationRange != null) {
				return XMLPositionUtility.createRange(locationRange);
			}

			try {
				DOMDocumentType docType = document.getDoctype();
				if (docType != null) {

					// Check if DTD location is declared with <!DOCTYPE SYSTEM
					// - <!DOCTYPE chapter PUBLIC "-//OASIS//DTD DocBook XML V4.4//EN"
					// "http://www.docbook.org/xml/4.4/docbookx.dtd" [
					String documentURI = document.getDocumentURI();
					String dtdLocation = getResolvedLocation(documentURI, docType.getSystemIdWithoutQuotes());
					if (hrefLocation.equals(dtdLocation)) {
						return XMLPositionUtility.selectValueWithoutQuote(docType.getSystemIdNode());
					}

					// Check if DTD location is declared with <!ENTITY SYSTEM
					// - <!ENTITY % document SYSTEM "document.ent">
					// - <!ENTITY % eval "<!ENTITY &#x25; exfiltrate SYSTEM
					// 'http://localhost:8080/dtd.xml?%file;'>">
					NamedNodeMap entities = docType.getEntities();
					for (int i = 0; i < entities.getLength(); i++) {
						DTDEntityDecl entity = (DTDEntityDecl) entities.item(i);
						String entityLocation = getResolvedLocation(documentURI, entity.getSystemId());
						if (hrefLocation.equals(entityLocation)) {
							// <!ENTITY % document SYSTEM "document.ent">
							return XMLPositionUtility.selectValueWithoutQuote(entity.getSystemIdNode());
						}
						String entityValue = entity.getValue();
						if (entityValue != null && entityValue.startsWith("<!ENTITY")) {
							int index = entityValue.indexOf(hrefLocation);
							if (index != -1) {
								// <!ENTITY % eval "<!ENTITY &#x25; exfiltrate SYSTEM
								// 'http://localhost:8080/dtd.xml?%file;'>">
								DTDDeclParameter parameter = entity.getValueNode();
								int start = parameter.getStart() + index + 1;
								int end = start + hrefLocation.length();
								return new Range(document.positionAt(start), document.positionAt(end));
							}
						}
					}
				}
			} catch (BadLocationException e) {
			}

			// DTD location cannot be found (ex : the DTD not found comes from an ENTITY
			// which is declared in another included file.
			// the error is displayed on the XML root element
			return XMLPositionUtility.selectRootStartTag(document);
		}
		default:
			try {
				return new Range(new Position(0, 0), document.positionAt(document.getEnd()));
			} catch (BadLocationException e) {
			}
		}
		return null;
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

	public static void registerCodeActionParticipants(Map<String, ICodeActionParticipant> codeActions,
			SharedSettings sharedSettings) {
		codeActions.put(ElementDeclUnterminated.getCode(), new ElementDeclUnterminatedCodeAction());
		codeActions.put(EntityNotDeclared.getCode(), new EntityNotDeclaredCodeAction());
		if (sharedSettings != null
				&& sharedSettings.getWorkspaceSettings().isResourceOperationSupported(ResourceOperationKind.Create)) {
			codeActions.put(DTDNotFound.getCode(), new DTDNotFoundCodeAction());
		}
		codeActions.put(MSG_REQUIRED_ATTRIBUTE_NOT_SPECIFIED.getCode(), new MSG_REQUIRED_ATTRIBUTE_NOT_SPECIFIEDCodeAction());
		codeActions.put(MSG_CONTENT_INCOMPLETE.getCode(), new MSG_CONTENT_INCOMPLETECodeAction());
		codeActions.put(MSG_ATTRIBUTE_NOT_DECLARED.getCode(), new MSG_ATTRIBUTE_NOT_DECLAREDCodeAction());
		codeActions.put(MSG_ELEMENT_NOT_DECLARED.getCode(), new MSG_ELEMENT_NOT_DECLAREDCodeAction());
		codeActions.put(MSG_CONTENT_INVALID.getCode(), new MSG_CONTENT_INVALIDCodeAction());
		codeActions.put(MSG_ATTRIBUTE_VALUE_NOT_IN_LIST.getCode(), new MSG_ATTRIBUTE_VALUE_NOT_IN_LISTCodeAction());
		codeActions.put(MSG_FIXED_ATTVALUE_INVALID.getCode(), new MSG_FIXED_ATTVALUE_INVALIDCodeAction());
		ICodeActionParticipant fixMissingSpace = new FixMissingSpaceCodeAction();
		codeActions.put(MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ATTLISTDECL.getCode(), fixMissingSpace);
		codeActions.put(MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ELEMENTDECL.getCode(), fixMissingSpace);
		codeActions.put(MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_ENTITYDECL.getCode(), fixMissingSpace);
		codeActions.put(MSG_SPACE_REQUIRED_BEFORE_ROOT_ELEMENT_TYPE_IN_DOCTYPEDECL.getCode(), fixMissingSpace);
		codeActions.put(MSG_SPACE_REQUIRED_BEFORE_CONTENTSPEC_IN_ELEMENTDECL.getCode(), fixMissingSpace);
		codeActions.put(MSG_SPACE_REQUIRED_BEFORE_ATTRIBUTE_NAME_IN_ATTDEF.getCode(), fixMissingSpace);
		codeActions.put(MSG_SPACE_REQUIRED_BEFORE_ATTTYPE_IN_ATTDEF.getCode(), fixMissingSpace);
		codeActions.put(MSG_SPACE_REQUIRED_BEFORE_DEFAULTDECL_IN_ATTDEF.getCode(), fixMissingSpace);
		codeActions.put(MSG_SPACE_REQUIRED_BEFORE_PERCENT_IN_PEDECL.getCode(), fixMissingSpace);
		codeActions.put(MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_PEDECL.getCode(), fixMissingSpace);
		codeActions.put(MSG_SPACE_REQUIRED_BEFORE_NOTATION_NAME_IN_NOTATIONDECL.getCode(), fixMissingSpace);
		codeActions.put(MSG_SPACE_REQUIRED_BEFORE_NOTATION_NAME_IN_UNPARSED_ENTITYDECL.getCode(), fixMissingSpace);
		codeActions.put(MSG_SPACE_REQUIRED_BEFORE_NDATA_IN_UNPARSED_ENTITYDECL.getCode(), fixMissingSpace);
		codeActions.put(MSG_SPACE_REQUIRED_BEFORE_PUBIDLITERAL_IN_EXTERNALID.getCode(), fixMissingSpace);
		codeActions.put(MSG_SPACE_REQUIRED_BEFORE_SYSTEMLITERAL_IN_EXTERNALID.getCode(), fixMissingSpace);
		codeActions.put(SpaceRequiredAfterSYSTEM.getCode(), new ExternalIdCodeAction(ExternalIdCodeAction.Kind.SYSTEM));
		codeActions.put(SpaceRequiredAfterPUBLIC.getCode(), new ExternalIdCodeAction(ExternalIdCodeAction.Kind.PUBLIC));
		codeActions.put(MSG_SPACE_REQUIRED_AFTER_NOTATION_NAME_IN_NOTATIONDECL.getCode(), new NotationDeclTypeCodeAction());
		codeActions.put(AttTypeRequiredInAttDef.getCode(), new AttTypeRequiredInAttDefCodeAction());
		codeActions.put(MSG_ATT_DEFAULT_INVALID.getCode(), new AttDefaultInvalidCodeAction());
		ICodeActionParticipant contentSpecRequired = new ContentSpecRequiredCodeAction();
		codeActions.put(MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN.getCode(), contentSpecRequired);
		codeActions.put(MSG_CONTENTSPEC_REQUIRED_IN_ELEMENTDECL.getCode(), contentSpecRequired);
		codeActions.put(MixedContentUnterminated.getCode(), new MixedContentUnterminatedCodeAction());
		codeActions.put(MSG_CLOSE_PAREN_REQUIRED_IN_MIXED.getCode(),
				new ContentModelCloseParenCodeAction(")*", "Close with ')*'"));
		codeActions.put(MSG_CLOSE_PAREN_REQUIRED_IN_CHILDREN.getCode(),
				new ContentModelCloseParenCodeAction(")", "Close with ')'"));
		codeActions.put(EnumerationUnterminated.getCode(), new EnumerationUnterminatedCodeAction());
		ICodeActionParticipant closeDeclAction = new ElementDeclUnterminatedCodeAction();
		codeActions.put(EntityDeclUnterminated.getCode(), closeDeclAction);
		codeActions.put(NotationDeclUnterminated.getCode(), closeDeclAction);
		codeActions.put(DoctypedeclUnterminated.getCode(), closeDeclAction);
		ICodeActionParticipant removeDuplicateAction = new RemoveDTDDeclCodeAction();
		codeActions.put(MSG_ELEMENT_ALREADY_DECLARED.getCode(), removeDuplicateAction);
		codeActions.put(MSG_DUPLICATE_ATTRIBUTE_DEFINITION.getCode(), removeDuplicateAction);
		codeActions.put(MSG_DUPLICATE_ENTITY_DEFINITION.getCode(), removeDuplicateAction);
		// (one|two|one) #IMPLIED → removes "|one" (the duplicate token)
		codeActions.put(MSG_DISTINCT_TOKENS_IN_ENUMERATION.getCode(), new ICodeActionParticipant() {
			@Override
			public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions,
					CancelChecker cancelChecker) {
				codeActions.add(CodeActionFactory.remove("Remove duplicate token",
						request.getDiagnostic().getRange(), request.getDocument().getTextDocument(),
						request.getDiagnostic()));
			}
		});
		codeActions.put(ExternalIDorPublicIDRequired.getCode(), new ExternalIDorPublicIDRequiredCodeAction());
		ICodeActionParticipant quoteRequiredAction = new QuoteRequiredInDeclCodeAction();
		codeActions.put(QuoteRequiredInPublicID.getCode(), quoteRequiredAction);
		codeActions.put(QuoteRequiredInSystemID.getCode(), quoteRequiredAction);
		codeActions.put(OpenQuoteMissingInDecl.getCode(), quoteRequiredAction);
		codeActions.put(IDDefaultTypeInvalid.getCode(), new IDDefaultTypeInvalidCodeAction());
		codeActions.put(NotationTypeUnterminated.getCode(), new EnumerationUnterminatedCodeAction());
		codeActions.put(MSG_MORE_THAN_ONE_ID_ATTRIBUTE.getCode(), removeDuplicateAction);
		codeActions.put(NoNotationOnEmptyElement.getCode(), removeDuplicateAction);
		codeActions.put(MSG_NOTATION_NOT_DECLARED_FOR_UNPARSED_ENTITYDECL.getCode(),
				new NotationNotDeclaredForEntityCodeAction());
		codeActions.put(MSG_MARKUP_NOT_RECOGNIZED_IN_DTD.getCode(),
				new MSG_MARKUP_NOT_RECOGNIZED_IN_DTDCodeAction());
		ICodeActionParticipant closingQuoteAction = new ClosingQuoteMissingCodeAction();
		codeActions.put(PublicIDUnterminated.getCode(), closingQuoteAction);
		codeActions.put(SystemIDUnterminated.getCode(), closingQuoteAction);
		codeActions.put(InvalidCharInPublicID.getCode(), closingQuoteAction);
		codeActions.put(InvalidCharInSystemID.getCode(), closingQuoteAction);
		// (#PCDATA|item|item)* → removes "|item" (the duplicate type, including preceding '|')
		codeActions.put(DuplicateTypeInMixedContent.getCode(), new ICodeActionParticipant() {
			@Override
			public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions,
					CancelChecker cancelChecker) {
				Range diagRange = request.getDiagnostic().getRange();
				boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings()
						.isSnippetEditSupported();
				// Extend range one character to the left to include the preceding '|'
				Position start = diagRange.getStart();
				int col = start.getCharacter() > 0 ? start.getCharacter() - 1 : 0;
				Range removeRange = new Range(new Position(start.getLine(), col), diagRange.getEnd());
				String replaceText = snippetSupport ? "$0" : "";
				codeActions.add(CodeActionFactory.replace("Remove duplicate type",
						removeRange, replaceText, request.getDocument().getTextDocument(),
						request.getDiagnostic(), snippetSupport));
			}
		});
	}
}
