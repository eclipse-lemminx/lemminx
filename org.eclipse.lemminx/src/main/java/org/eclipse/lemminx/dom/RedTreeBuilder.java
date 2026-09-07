/*******************************************************************************
 * Copyright (c) 2026 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.lemminx.dom;

import java.util.Arrays;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.green.GreenAttr;
import org.eclipse.lemminx.dom.green.GreenCDATA;
import org.eclipse.lemminx.dom.green.GreenComment;
import org.eclipse.lemminx.dom.green.GreenDTDAttlistDecl;
import org.eclipse.lemminx.dom.green.GreenDTDDeclNode;
import org.eclipse.lemminx.dom.green.GreenDTDElementDecl;
import org.eclipse.lemminx.dom.green.GreenDTDEntityDecl;
import org.eclipse.lemminx.dom.green.GreenDTDNotationDecl;
import org.eclipse.lemminx.dom.green.GreenDocument;
import org.eclipse.lemminx.dom.green.GreenDocumentType;
import org.eclipse.lemminx.dom.green.GreenElement;
import org.eclipse.lemminx.dom.green.GreenNode;
import org.eclipse.lemminx.dom.green.GreenProcessingInstruction;
import org.eclipse.lemminx.dom.green.GreenText;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.w3c.dom.Node;

/**
 * Builds a red tree ({@link DOMDocument}) from an immutable green tree
 * ({@link GreenDocument}).
 *
 * <p>The red tree provides absolute offsets, parent pointers, and the full
 * {@link org.w3c.dom.Node} API that existing LemMinX features expect.
 * The green tree is preserved for structural sharing during incremental
 * reparsing.</p>
 */
public final class RedTreeBuilder {

	private final boolean ignoreWhitespaceContent;
	private final boolean lazy;

	private RedTreeBuilder(boolean ignoreWhitespaceContent, boolean lazy) {
		this.ignoreWhitespaceContent = ignoreWhitespaceContent;
		this.lazy = lazy;
	}

	public static DOMDocument build(GreenDocument greenDoc, TextDocument textDocument,
			URIResolverExtensionManager resolverExtensionManager) {
		return new RedTreeBuilder(true, false).doBuild(greenDoc, textDocument, resolverExtensionManager);
	}

	public static DOMDocument build(GreenDocument greenDoc, TextDocument textDocument,
			URIResolverExtensionManager resolverExtensionManager, boolean ignoreWhitespaceContent) {
		return new RedTreeBuilder(ignoreWhitespaceContent, false).doBuild(greenDoc, textDocument, resolverExtensionManager);
	}

	public static DOMDocument buildLazy(GreenDocument greenDoc, TextDocument textDocument,
			URIResolverExtensionManager resolverExtensionManager) {
		return new RedTreeBuilder(true, true).doBuild(greenDoc, textDocument, resolverExtensionManager);
	}

	public static DOMDocument buildLazy(GreenDocument greenDoc, TextDocument textDocument,
			URIResolverExtensionManager resolverExtensionManager, boolean ignoreWhitespaceContent) {
		return new RedTreeBuilder(ignoreWhitespaceContent, true).doBuild(greenDoc, textDocument, resolverExtensionManager);
	}

	static void expandLazy(DOMNode node, GreenNode green, int absStart) {
		new RedTreeBuilder(true, true).addChildren(node, green, absStart);
	}

	private DOMDocument doBuild(GreenDocument greenDoc, TextDocument textDocument,
			URIResolverExtensionManager resolverExtensionManager) {
		DOMDocument domDoc = new DOMDocument(textDocument, resolverExtensionManager);
		addChildren(domDoc, greenDoc, 0);
		domDoc.compactChildren();
		return domDoc;
	}

	private void addChildrenOrDefer(DOMNode node, GreenNode green, int absStart) {
		if (lazy && green.childCount() > 0) {
			node.setLazy(green, absStart);
		} else {
			addChildren(node, green, absStart);
		}
	}

	private void addChildren(DOMNode parent, GreenNode greenParent, int parentAbsStart) {
		int cc = greenParent.childCount();
		if (cc == 0) {
			parent.compactChildren();
			return;
		}

		int childrenStartRel = greenParent.childrenStartRel();
		int childAbsStart = parentAbsStart + childrenStartRel;
		boolean skipWhitespace = ignoreWhitespaceContent
				&& (hasNonWhitespaceChild(greenParent, cc) || greenParent instanceof GreenDocumentType);

		DOMNode[] redChildren = new DOMNode[cc];
		int idx = 0;

		for (int i = 0; i < cc; i++) {
			GreenNode greenChild = greenParent.child(i);
			if (skipWhitespace && isWhitespaceText(greenChild)) {
				childAbsStart += greenChild.width();
				continue;
			}
			DOMNode redChild = createRedNode(greenChild, childAbsStart);
			if (redChild != null) {
				if (parent instanceof DTDAttlistDecl && redChild instanceof DTDAttlistDecl) {
					((DTDAttlistDecl) parent).addAdditionalAttDecl((DTDAttlistDecl) redChild);
					redChild.parent = parent;
				} else {
					redChild.parent = parent;
					redChild.cachedIndexInParent = idx;
					redChildren[idx++] = redChild;
				}
			}
			childAbsStart += greenChild.width();
		}

		if (idx > 0) {
			parent.setChildrenArray((idx == redChildren.length) ? redChildren : Arrays.copyOf(redChildren, idx));
		}
		parent.compactChildren();
	}

	private static boolean hasNonWhitespaceChild(GreenNode parent, int cc) {
		for (int i = 0; i < cc; i++) {
			if (!isWhitespaceText(parent.child(i))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isWhitespaceText(GreenNode node) {
		return node instanceof GreenText && ((GreenText) node).whitespace();
	}

	private DOMNode createRedNode(GreenNode green, int absStart) {
		int absEnd = absStart + green.width();

		switch (green.nodeType()) {
			case Node.ELEMENT_NODE:
				return createElement((GreenElement) green, absStart, absEnd);
			case Node.TEXT_NODE:
				return createText((GreenText) green, absStart, absEnd);
			case Node.COMMENT_NODE:
				return createComment((GreenComment) green, absStart, absEnd);
			case Node.CDATA_SECTION_NODE:
				return createCDATA((GreenCDATA) green, absStart, absEnd);
			case Node.PROCESSING_INSTRUCTION_NODE:
				return createPI((GreenProcessingInstruction) green, absStart, absEnd);
			case Node.DOCUMENT_TYPE_NODE:
				return createDocumentType((GreenDocumentType) green, absStart, absEnd);
			case DOMNode.DTD_ELEMENT_DECL_NODE:
				return createDTDElementDecl((GreenDTDElementDecl) green, absStart, absEnd);
			case DOMNode.DTD_ATT_LIST_NODE:
				return createDTDAttlistDecl((GreenDTDAttlistDecl) green, absStart, absEnd);
			case Node.ENTITY_NODE:
				return createDTDEntityDecl((GreenDTDEntityDecl) green, absStart, absEnd);
			case DOMNode.DTD_NOTATION_DECL:
				return createDTDNotationDecl((GreenDTDNotationDecl) green, absStart, absEnd);
			case DOMNode.DTD_DECL_NODE:
				return createDTDDeclNode((GreenDTDDeclNode) green, absStart, absEnd);
			default:
				return null;
		}
	}

	private DOMElement createElement(GreenElement green, int absStart, int absEnd) {
		DOMElement elem = new DOMElement(absStart, absEnd);
		elem.greenElement = green;
		elem.setSelfClosed(green.selfClosed());
		elem.setClosed(green.closed());

		GreenAttr[] greenAttrs = green.attributes();
		if (greenAttrs.length > 0) {
			DOMAttr[] attrs = new DOMAttr[greenAttrs.length];
			for (int i = 0; i < greenAttrs.length; i++) {
				attrs[i] = createAttr(greenAttrs[i], absStart, elem);
			}
			elem.attributeNodes = attrs;
		}

		addChildrenOrDefer(elem, green, absStart);
		return elem;
	}

	private static DOMAttr createAttr(GreenAttr ga, int elemAbsStart, DOMNode owner) {
		int nameStart = elemAbsStart + ga.nameStartRel();
		int nameEnd = elemAbsStart + ga.nameEndRel();
		DOMAttr attr = new DOMAttr(null, nameStart, nameEnd, owner);
		if (ga.delimiterRel() != GreenAttr.NULL_VALUE) {
			attr.setDelimiter(elemAbsStart + ga.delimiterRel());
		}
		if (ga.valueStartRel() != GreenAttr.NULL_VALUE) {
			attr.setValue(null, elemAbsStart + ga.valueStartRel(), elemAbsStart + ga.valueEndRel());
		}
		return attr;
	}

	private static DOMText createText(GreenText green, int absStart, int absEnd) {
		DOMText text = new DOMText(absStart, absEnd);
		text.setWhitespace(green.whitespace());
		text.setClosed(true);
		return text;
	}

	private static DOMComment createComment(GreenComment green, int absStart, int absEnd) {
		DOMComment comment = new DOMComment(absStart, absEnd);
		comment.startContent = abs(green.startContentRel(), absStart);
		comment.endContent = abs(green.endContentRel(), absStart);
		comment.commentSameLineEndTag = green.commentSameLineEndTag();
		comment.setClosed(green.closed());
		return comment;
	}

	private static DOMCDATASection createCDATA(GreenCDATA green, int absStart, int absEnd) {
		DOMCDATASection cdata = new DOMCDATASection(absStart, absEnd);
		cdata.startContent = abs(green.startContentRel(), absStart);
		cdata.endContent = abs(green.endContentRel(), absStart);
		cdata.setClosed(green.closed());
		return cdata;
	}

	private static DOMProcessingInstruction createPI(GreenProcessingInstruction green,
			int absStart, int absEnd) {
		DOMProcessingInstruction pi = new DOMProcessingInstruction(absStart, absEnd);
		pi.target = green.target();
		pi.prolog = green.prolog();
		pi.processingInstruction = green.processingInstruction();
		pi.startTagClose = green.startTagClose();
		if (green.startContentRel() != GreenElement.NULL_VALUE) {
			pi.startContent = absStart + green.startContentRel();
		}
		if (green.endContentRel() != GreenElement.NULL_VALUE) {
			pi.endContent = absStart + green.endContentRel();
		}
		pi.endTagOpenOffset = abs(green.endTagOpenRel(), absStart);
		pi.setClosed(green.closed());

		GreenAttr[] greenAttrs = green.attributes();
		if (greenAttrs.length > 0) {
			DOMAttr[] attrs = new DOMAttr[greenAttrs.length];
			for (int i = 0; i < greenAttrs.length; i++) {
				attrs[i] = createAttr(greenAttrs[i], absStart, pi);
			}
			pi.attributeNodes = attrs;
		}
		return pi;
	}

	private DOMDocumentType createDocumentType(GreenDocumentType green,
			int absStart, int absEnd) {
		DOMDocumentType dt = new DOMDocumentType(absStart, absEnd);
		dt.setClosed(green.closed());
		setDTDDeclFields(dt, green, absStart);

		if (green.kind() != null) {
			dt.setKind(absStart + green.kind().startRel(),
					absStart + green.kind().endRel());
		}
		if (green.publicId() != null) {
			dt.setPublicId(absStart + green.publicId().startRel(),
					absStart + green.publicId().endRel());
		}
		if (green.systemId() != null) {
			dt.setSystemId(absStart + green.systemId().startRel(),
					absStart + green.systemId().endRel());
		}
		if (green.internalSubset() != null) {
			dt.setStartInternalSubset(absStart + green.internalSubset().startRel());
			dt.setEndInternalSubset(absStart + green.internalSubset().endRel());
		}

		setDTDUnrecognized(dt, green, absStart);
		addChildrenOrDefer(dt, green, absStart);
		dt.end = absEnd;
		return dt;
	}

	private DTDElementDecl createDTDElementDecl(GreenDTDElementDecl green,
			int absStart, int absEnd) {
		DTDElementDecl decl = new DTDElementDecl(absStart, absEnd);
		decl.setClosed(green.closed());
		setDTDDeclFields(decl, green, absStart);
		if (green.category() != null) {
			decl.setCategory(absStart + green.category().startRel(),
					absStart + green.category().endRel());
		}
		if (green.content() != null) {
			decl.setContent(absStart + green.content().startRel(),
					absStart + green.content().endRel());
		}
		setDTDUnrecognized(decl, green, absStart);
		addChildrenOrDefer(decl, green, absStart);
		decl.end = absEnd;
		return decl;
	}

	private DTDAttlistDecl createDTDAttlistDecl(GreenDTDAttlistDecl green,
			int absStart, int absEnd) {
		DTDAttlistDecl decl = new DTDAttlistDecl(absStart, absEnd);
		decl.setClosed(green.closed());
		setDTDDeclFields(decl, green, absStart);
		if (green.attributeName() != null) {
			decl.setAttributeName(absStart + green.attributeName().startRel(),
					absStart + green.attributeName().endRel());
		}
		if (green.attributeType() != null) {
			decl.setAttributeType(absStart + green.attributeType().startRel(),
					absStart + green.attributeType().endRel());
		}
		if (green.attributeValue() != null) {
			decl.setAttributeValue(absStart + green.attributeValue().startRel(),
					absStart + green.attributeValue().endRel());
		}
		setDTDUnrecognized(decl, green, absStart);
		addChildrenOrDefer(decl, green, absStart);
		decl.end = absEnd;
		return decl;
	}

	private DTDEntityDecl createDTDEntityDecl(GreenDTDEntityDecl green,
			int absStart, int absEnd) {
		DTDEntityDecl decl = new DTDEntityDecl(absStart, absEnd);
		decl.setClosed(green.closed());
		if (green.percent() != null) {
			decl.setPercent(absStart + green.percent().startRel(),
					absStart + green.percent().endRel());
		}
		setDTDDeclFields(decl, green, absStart);
		if (green.value() != null) {
			decl.setValue(absStart + green.value().startRel(),
					absStart + green.value().endRel());
		}
		if (green.kind() != null) {
			decl.setKind(absStart + green.kind().startRel(),
					absStart + green.kind().endRel());
		}
		if (green.publicId() != null) {
			decl.setPublicId(absStart + green.publicId().startRel(),
					absStart + green.publicId().endRel());
		}
		if (green.systemId() != null) {
			decl.setSystemId(absStart + green.systemId().startRel(),
					absStart + green.systemId().endRel());
		}
		setDTDUnrecognized(decl, green, absStart);
		addChildrenOrDefer(decl, green, absStart);
		decl.end = absEnd;
		return decl;
	}

	private DTDNotationDecl createDTDNotationDecl(GreenDTDNotationDecl green,
			int absStart, int absEnd) {
		DTDNotationDecl decl = new DTDNotationDecl(absStart, absEnd);
		decl.setClosed(green.closed());
		setDTDDeclFields(decl, green, absStart);
		if (green.kind() != null) {
			decl.setKind(absStart + green.kind().startRel(),
					absStart + green.kind().endRel());
		}
		if (green.publicId() != null) {
			decl.setPublicId(absStart + green.publicId().startRel(),
					absStart + green.publicId().endRel());
		}
		if (green.systemId() != null) {
			decl.setSystemId(absStart + green.systemId().startRel(),
					absStart + green.systemId().endRel());
		}
		setDTDUnrecognized(decl, green, absStart);
		addChildrenOrDefer(decl, green, absStart);
		decl.end = absEnd;
		return decl;
	}

	private DTDDeclNode createDTDDeclNode(GreenDTDDeclNode green,
			int absStart, int absEnd) {
		DTDDeclNode decl = new DTDDeclNode(absStart, absEnd);
		decl.setClosed(green.closed());
		setDTDDeclFields(decl, green, absStart);
		setDTDUnrecognized(decl, green, absStart);
		addChildrenOrDefer(decl, green, absStart);
		decl.end = absEnd;
		return decl;
	}

	private static void setDTDDeclFields(DTDDeclNode decl, GreenDTDDeclNode green,
			int absStart) {
		if (green.declType() != null) {
			decl.setDeclType(absStart + green.declType().startRel(),
					absStart + green.declType().endRel());
		}
		if (green.name() != null) {
			decl.setName(absStart + green.name().startRel(),
					absStart + green.name().endRel());
		}
	}

	private static void setDTDUnrecognized(DTDDeclNode decl, GreenDTDDeclNode green,
			int absStart) {
		if (green.unrecognized() != null) {
			decl.setUnrecognized(absStart + green.unrecognized().startRel(),
					absStart + green.unrecognized().endRel());
		}
	}

	private static int abs(int relOffset, int absStart) {
		return relOffset != GreenElement.NULL_VALUE ? absStart + relOffset : DOMNode.NULL_VALUE;
	}
}
