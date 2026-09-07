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
package org.eclipse.lemminx.dom.green;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.parser.Scanner;
import org.eclipse.lemminx.dom.parser.TokenType;
import org.eclipse.lemminx.dom.parser.XMLScanner;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Builds an immutable {@link GreenDocument} from XML text using the scanner.
 *
 * <p>This mirrors the logic of {@code DOMParser} but produces green nodes
 * (width-based, immutable, no parent pointers) instead of red DOM nodes.</p>
 */
public final class GreenTreeBuilder {

	private GreenTreeBuilder() {
	}

	/**
	 * Parses the given XML text and returns an immutable green tree.
	 *
	 * @param text    the XML text
	 * @param uri     the document URI
	 * @param monitor optional cancel checker
	 * @return the root green document node
	 */
	public static GreenDocument parse(CharSequence text, String uri, CancelChecker monitor) {
		return parseRange(text, uri, 0, text.length(), monitor);
	}

	public static GreenDocument parseRange(CharSequence text, String uri,
			int rangeStart, int rangeEnd, CancelChecker monitor) {
		boolean isDTD = DOMUtils.isDTD(uri);
		Scanner scanner = XMLScanner.createScanner(text, rangeStart, isDTD);

		Deque<NodeBuilder> stack = new ArrayDeque<>();
		List<GreenNode> rootChildren = new ArrayList<>();

		if (isDTD) {
			NodeBuilder dtdRoot = new NodeBuilder(NodeKind.DOCUMENT_TYPE, rangeStart);
			dtdRoot.closed = true;
			stack.push(dtdRoot);
		}

		Map<String, String> tagIntern = new HashMap<>();
		GreenAttrBuilder currentAttr = null;
		int endTagOpenOffset = -1;
		boolean previousTokenWasEndTagOpen = false;
		boolean isInitialDeclaration = true;
		boolean inDTDInternalSubset = false;
		GreenText tempWhitespaceContent = null;
		int tempWhitespaceStart = -1;
		int[] nextRootChildEnd = { rangeStart };

		NodeBuilder lastClosed = null;

		TokenType token = scanner.scan();
		while (token != TokenType.EOS) {
			if (rangeEnd < text.length() && scanner.getTokenOffset() >= rangeEnd && stack.isEmpty()) {
				break;
			}
			if (monitor != null) {
				monitor.checkCanceled();
			}

			if (tempWhitespaceContent != null && token != TokenType.EndTagOpen) {
				tempWhitespaceContent = null;
			}

			if (previousTokenWasEndTagOpen) {
				previousTokenWasEndTagOpen = false;
				boolean linkToEmptyStartTag = false;

				if (token == TokenType.EndTagClose) {
					// </>
					while (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						if (top.kind == NodeKind.ELEMENT && top.tag == null) {
							break;
						}
						if (stack.size() == 1 && !isDTD) {
							break;
						}
						top.nodeEnd = endTagOpenOffset;
						GreenNode built = top.buildGreen();
						stack.pop();
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
					}
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						linkToEmptyStartTag = true;
						top.closed = true;
						top.endTagOpenOffset = endTagOpenOffset;
						top.endTagHasClose = true;
						top.nodeEnd = scanner.getTokenEnd();
					}
				}

				if (token != TokenType.EndTag && !linkToEmptyStartTag) {
					GreenElement fakeEndTag = new GreenElement(
							2, false, null, false,
							GreenElement.NULL_VALUE, 0, false,
							null, null);
					addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, fakeEndTag, endTagOpenOffset);
				}
			}

			if (currentAttr != null
					&& token != TokenType.DelimiterAssign
					&& token != TokenType.AttributeValue
					&& token != TokenType.Whitespace) {
				if (!stack.isEmpty()) {
					NodeBuilder top = stack.peek();
					top.addAttribute(currentAttr.build(top.nodeStart));
				}
				currentAttr = null;
			}

			switch (token) {
				case StartTagOpen: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						if (!top.closed) {
							top.nodeEnd = scanner.getTokenOffset();
						}
						if (isDTD) {
							if (top.closed && top.kind != NodeKind.DOCUMENT_TYPE) {
								GreenNode built = top.buildGreen();
								stack.pop();
								addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
							} else if (isDTDDeclKind(top.kind)) {
								GreenNode built = top.buildGreen();
								stack.pop();
								addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
							}
						} else if (top.closed || top.kind == NodeKind.DOCUMENT_TYPE) {
							if (top.kind == NodeKind.DOCUMENT_TYPE) {
								inDTDInternalSubset = false;
							}
							GreenNode built = top.buildGreen();
							stack.pop();
							addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
						}
					}
					NodeBuilder element = new NodeBuilder(NodeKind.ELEMENT, scanner.getTokenOffset());
					element.startTagOpenOffset = scanner.getTokenOffset();
					stack.push(element);
					break;
				}

				case StartTag: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.tag = tagIntern.computeIfAbsent(scanner.getTokenText(), k -> k);
						top.nodeEnd = scanner.getTokenEnd();
					}
					break;
				}

				case StartTagClose: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.nodeEnd = scanner.getTokenEnd();
						if (top.kind == NodeKind.ELEMENT) {
							top.startTagCloseOffset = scanner.getTokenOffset();
						} else if (top.kind == NodeKind.PROCESSING_INSTRUCTION) {
							top.startTagClose = true;
						}
					}
					break;
				}

				case EndTagOpen: {
					if (tempWhitespaceContent != null) {
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, tempWhitespaceContent, tempWhitespaceStart);
						tempWhitespaceContent = null;
					}
					endTagOpenOffset = scanner.getTokenOffset();
					if (!stack.isEmpty() && !stack.peek().closed) {
						stack.peek().nodeEnd = scanner.getTokenOffset();
					}
					previousTokenWasEndTagOpen = true;
					break;
				}

				case EndTag: {
					String closeTag = tagIntern.computeIfAbsent(scanner.getTokenText(), k -> k);
					if (hasMatchingElement(stack, closeTag)) {
						while (!stack.isEmpty()) {
							NodeBuilder top = stack.peek();
							if (top.kind == NodeKind.ELEMENT && closeTag.equals(top.tag)) {
								break;
							}
							if (stack.size() == 1 && !isDTD) {
								break;
							}
							top.nodeEnd = endTagOpenOffset;
							GreenNode built = top.buildGreen();
							stack.pop();
							addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
						}
					}
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						if (top.kind == NodeKind.ELEMENT && closeTag.equals(top.tag)) {
							top.closed = true;
							top.endTagOpenOffset = endTagOpenOffset;
							top.nodeEnd = scanner.getTokenEnd();
						} else {
							NodeBuilder orphan = new NodeBuilder(NodeKind.ELEMENT,
									scanner.getTokenOffset() - 2);
							orphan.endTagOpenOffset = endTagOpenOffset;
							orphan.tag = closeTag;
							orphan.nodeEnd = scanner.getTokenEnd();
							stack.push(orphan);
						}
					} else {
						NodeBuilder orphan = new NodeBuilder(NodeKind.ELEMENT,
								scanner.getTokenOffset() - 2);
						orphan.endTagOpenOffset = endTagOpenOffset;
						orphan.tag = closeTag;
						orphan.nodeEnd = scanner.getTokenEnd();
						stack.push(orphan);
					}
					break;
				}

				case StartTagSelfClose: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.closed = true;
						top.selfClosed = true;
						top.nodeEnd = scanner.getTokenEnd();
						lastClosed = top;
						GreenNode built = top.buildGreen();
						stack.pop();
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
					}
					break;
				}

				case EndTagClose: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.nodeEnd = scanner.getTokenEnd();
						top.endTagHasClose = true;
						if (top.kind == NodeKind.DOCUMENT_TYPE) {
							top.closed = true;
						}
						lastClosed = top;
						GreenNode built = top.buildGreen();
						stack.pop();
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
					}
					break;
				}

				case AttributeName: {
					currentAttr = new GreenAttrBuilder();
					currentAttr.setName(scanner.getTokenOffset(), scanner.getTokenEnd());
					if (!stack.isEmpty()) {
						stack.peek().nodeEnd = scanner.getTokenEnd();
					}
					break;
				}

				case DelimiterAssign: {
					if (currentAttr != null) {
						currentAttr.setDelimiter(scanner.getTokenOffset());
					}
					break;
				}

				case AttributeValue: {
					if (currentAttr != null && !stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						currentAttr.setValue(scanner.getTokenOffset(), scanner.getTokenEnd());
						top.addAttribute(currentAttr.build(top.nodeStart));
					}
					currentAttr = null;
					if (!stack.isEmpty()) {
						stack.peek().nodeEnd = scanner.getTokenEnd();
					}
					break;
				}

				case CDATATagOpen: {
					NodeBuilder cdata = new NodeBuilder(NodeKind.CDATA, scanner.getTokenOffset());
					stack.push(cdata);
					break;
				}

				case CDATAContent: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.startContentOffset = scanner.getTokenOffset();
						top.endContentOffset = scanner.getTokenEnd();
						top.nodeEnd = scanner.getTokenEnd();
					}
					break;
				}

				case CDATATagClose: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.nodeEnd = scanner.getTokenEnd();
						top.closed = true;
						GreenNode built = top.buildGreen();
						stack.pop();
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
					}
					break;
				}

				case StartPrologOrPI: {
					NodeBuilder pi = new NodeBuilder(NodeKind.PROCESSING_INSTRUCTION,
							scanner.getTokenOffset());
					stack.push(pi);
					break;
				}

				case PIName: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.target = scanner.getTokenText();
						top.processingInstruction = true;
					}
					break;
				}

				case PrologName: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.target = scanner.getTokenText();
						top.prolog = true;
					}
					break;
				}

				case PIContent: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.startContentOffset = scanner.getTokenOffset();
						top.endContentOffset = scanner.getTokenEnd();
					}
					break;
				}

				case PIEnd:
				case PrologEnd: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.nodeEnd = scanner.getTokenEnd();
						top.closed = true;
						GreenNode built = top.buildGreen();
						stack.pop();
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
					}
					break;
				}

				case StartCommentTag: {
					if (isDTD || inDTDInternalSubset) {
						while (!stack.isEmpty() && stack.peek().kind != NodeKind.DOCUMENT_TYPE) {
							NodeBuilder top = stack.peek();
							GreenNode built = top.buildGreen();
							stack.pop();
							addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
						}
					} else if (!stack.isEmpty() && stack.peek().closed) {
						NodeBuilder top = stack.peek();
						GreenNode built = top.buildGreen();
						stack.pop();
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
					}
					NodeBuilder comment = new NodeBuilder(NodeKind.COMMENT, scanner.getTokenOffset());
					if (lastClosed != null && lastClosed.nodeEnd <= scanner.getTokenOffset()) {
						comment.commentSameLineEndTag = !containsNewline(text,
								lastClosed.nodeEnd, scanner.getTokenOffset());
					}
					stack.push(comment);
					break;
				}

				case Comment: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.startContentOffset = scanner.getTokenOffset();
						top.endContentOffset = scanner.getTokenEnd();
					}
					break;
				}

				case EndCommentTag: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.nodeEnd = scanner.getTokenEnd();
						top.closed = true;
						GreenNode built = top.buildGreen();
						stack.pop();
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
					}
					break;
				}

				case Content: {
					boolean currIsDeclNode = !stack.isEmpty()
							&& isDTDDeclKind(stack.peek().kind);
					if (currIsDeclNode) {
						NodeBuilder top = stack.peek();
						top.nodeEnd = scanner.getTokenOffset() - 1;
						GreenNode built = top.buildGreen();
						stack.pop();
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
					}
					int start = scanner.getTokenOffset();
					int end = scanner.getTokenEnd();
					boolean isBlank = scanner.isTokenTextBlank();

					if (isBlank && currIsDeclNode) {
						break;
					}

					GreenText textNode = isBlank ? GreenText.whitespace(end - start) : new GreenText(end - start, false);
					addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, textNode, start);
					break;
				}

				// DTD tokens
				case DTDStartDoctypeTag: {
					while (!stack.isEmpty() && stack.peek().kind != NodeKind.DOCUMENT_TYPE) {
						NodeBuilder top = stack.peek();
						top.nodeEnd = scanner.getTokenOffset();
						GreenNode built = top.buildGreen();
						stack.pop();
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
					}
					NodeBuilder doctype = new NodeBuilder(NodeKind.DOCUMENT_TYPE,
							scanner.getTokenOffset());
					stack.push(doctype);
					break;
				}

				case DTDDoctypeName: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdNameParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDDocTypeKindPUBLIC:
				case DTDDocTypeKindSYSTEM: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdKindParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDDoctypePublicId: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdPublicIdParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDDoctypeSystemId: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdSystemIdParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDStartInternalSubset: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.addParam(scanner.getTokenOffset(), scanner.getTokenOffset() + 1);
						top.dtdInternalSubsetParam = top.lastParam();
						inDTDInternalSubset = true;
					}
					break;
				}

				case DTDEndInternalSubset: {
					while (!stack.isEmpty() && stack.peek().kind != NodeKind.DOCUMENT_TYPE) {
						NodeBuilder top = stack.peek();
						top.nodeEnd = scanner.getTokenOffset() - 1;
						GreenNode built = top.buildGreen();
						stack.pop();
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
					}
					inDTDInternalSubset = false;
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						if (top.dtdInternalSubsetParam != null) {
							top.dtdInternalSubsetParam = new int[] {
									top.dtdInternalSubsetParam[0], scanner.getTokenEnd() };
						}
					}
					break;
				}

				case DTDStartElement: {
					while (!stack.isEmpty() && stack.peek().kind != NodeKind.DOCUMENT_TYPE) {
						NodeBuilder top = stack.peek();
						top.nodeEnd = scanner.getTokenOffset();
						GreenNode built = top.buildGreen();
						stack.pop();
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
					}
					NodeBuilder decl = new NodeBuilder(NodeKind.DTD_ELEMENT_DECL,
							scanner.getTokenOffset());
					decl.addDeclType(scanner.getTokenOffset() + 2, scanner.getTokenOffset() + 9);
					stack.push(decl);
					break;
				}

				case DTDElementDeclName: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdNameParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDElementCategory: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdCategoryParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDStartElementContent: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdContentParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDElementContent:
				case DTDEndElementContent: {
					if (!stack.isEmpty()) {
						stack.peek().updateLastParamEnd(scanner.getTokenEnd());
					}
					break;
				}

				case DTDStartAttlist: {
					while (!stack.isEmpty() && stack.peek().kind != NodeKind.DOCUMENT_TYPE) {
						NodeBuilder top = stack.peek();
						top.nodeEnd = scanner.getTokenOffset();
						GreenNode built = top.buildGreen();
						stack.pop();
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
					}
					NodeBuilder decl = new NodeBuilder(NodeKind.DTD_ATTLIST_DECL,
							scanner.getTokenOffset());
					decl.addDeclType(scanner.getTokenOffset() + 2, scanner.getTokenOffset() + 9);
					isInitialDeclaration = true;
					stack.push(decl);
					break;
				}

				case DTDAttlistElementName: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdNameParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDAttlistAttributeName: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						if (top.kind == NodeKind.DTD_ATTLIST_DECL && top.dtdAttributeNameParam != null) {
							int internalStart = lastParamEnd(top);
							if (stack.size() >= 2) {
								NodeBuilder below = peekSecond(stack);
								if (below != null && below.kind == NodeKind.DTD_ATTLIST_DECL) {
									top.nodeEnd = internalStart;
									GreenNode built = top.buildGreen();
									stack.pop();
									addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
								}
							}
							NodeBuilder internalAttlist = new NodeBuilder(NodeKind.DTD_ATTLIST_DECL,
									internalStart);
							stack.push(internalAttlist);
							top = internalAttlist;
						}
						top.addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						top.dtdAttributeNameParam = top.lastParam();
					}
					break;
				}

				case DTDAttlistAttributeType: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						top.dtdAttributeTypeParam = top.lastParam();
					}
					break;
				}

				case DTDAttlistAttributeValue: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						top.dtdAttributeValueParam = top.lastParam();
					}
					break;
				}

				case DTDStartEntity: {
					while (!stack.isEmpty() && stack.peek().kind != NodeKind.DOCUMENT_TYPE) {
						NodeBuilder top = stack.peek();
						top.nodeEnd = scanner.getTokenOffset();
						GreenNode built = top.buildGreen();
						stack.pop();
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
					}
					NodeBuilder decl = new NodeBuilder(NodeKind.DTD_ENTITY_DECL,
							scanner.getTokenOffset());
					decl.addDeclType(scanner.getTokenOffset() + 2, scanner.getTokenOffset() + 8);
					stack.push(decl);
					break;
				}

				case DTDEntityPercent: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdPercentParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDEntityName: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdNameParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDEntityValue: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdValueParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDEntityKindPUBLIC:
				case DTDEntityKindSYSTEM: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdKindParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDEntityPublicId: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdPublicIdParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDEntitySystemId: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdSystemIdParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDStartNotation: {
					while (!stack.isEmpty() && stack.peek().kind != NodeKind.DOCUMENT_TYPE) {
						NodeBuilder top = stack.peek();
						top.nodeEnd = scanner.getTokenOffset();
						GreenNode built = top.buildGreen();
						stack.pop();
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
					}
					NodeBuilder decl = new NodeBuilder(NodeKind.DTD_NOTATION_DECL,
							scanner.getTokenOffset());
					decl.addDeclType(scanner.getTokenOffset() + 2, scanner.getTokenOffset() + 10);
					isInitialDeclaration = true;
					stack.push(decl);
					break;
				}

				case DTDNotationName: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdNameParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDNotationKindPUBLIC:
				case DTDNotationKindSYSTEM: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdKindParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDNotationPublicId: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdPublicIdParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDNotationSystemId: {
					if (!stack.isEmpty()) {
						stack.peek().addParam(scanner.getTokenOffset(), scanner.getTokenEnd());
						stack.peek().dtdSystemIdParam = stack.peek().lastParam();
					}
					break;
				}

				case DTDEndTag: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						if (top.kind == NodeKind.DTD_ELEMENT_DECL
								|| top.kind == NodeKind.DTD_ATTLIST_DECL
								|| top.kind == NodeKind.DTD_ENTITY_DECL
								|| top.kind == NodeKind.DTD_NOTATION_DECL) {
							while (!stack.isEmpty() && stack.peek().kind != NodeKind.DOCUMENT_TYPE) {
								NodeBuilder nb = stack.peek();
								nb.nodeEnd = scanner.getTokenEnd();
								nb.closed = true;
								GreenNode built = nb.buildGreen();
								stack.pop();
								addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, nb.nodeStart);
							}
						}
					}
					break;
				}

				case DTDEndDoctypeTag: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.nodeEnd = scanner.getTokenEnd();
						top.closed = true;
						GreenNode built = top.buildGreen();
						stack.pop();
						addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
					}
					break;
				}

				case DTDUnrecognizedParameters: {
					if (!stack.isEmpty()) {
						NodeBuilder top = stack.peek();
						top.addParam(scanner.getTokenOffset(),
								((XMLScanner) scanner).getLastNonWhitespaceOffset());
						top.dtdUnrecognizedParam = top.lastParam();
					}
					break;
				}

				default:
					if (!stack.isEmpty() && stack.peek().closed
							&& stack.peek().kind != NodeKind.ELEMENT) {
						stack.peek().nodeEnd = scanner.getTokenEnd();
					}
					break;
			}

			token = scanner.scan();
		}

		if (previousTokenWasEndTagOpen) {
			GreenElement fakeEndTag = new GreenElement(
					2, false, null, false,
					GreenElement.NULL_VALUE, 0, false,
					null, null);
			addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, fakeEndTag, endTagOpenOffset);
		}

		// Flush pending attribute before closing remaining nodes
		if (currentAttr != null && !stack.isEmpty()) {
			NodeBuilder top = stack.peek();
			top.addAttribute(currentAttr.build(top.nodeStart));
			currentAttr = null;
		}

		// Flush remaining open nodes
		while (!stack.isEmpty()) {
			NodeBuilder top = stack.peek();
			top.nodeEnd = rangeEnd;
			GreenNode built = top.buildGreen();
			stack.pop();
			addChildToCurrentOrRoot(stack, rootChildren, nextRootChildEnd, built, top.nodeStart);
		}

		int docWidth = rangeEnd - rangeStart;
		return new GreenDocument(docWidth,
				rootChildren.toArray(GreenNode.EMPTY_CHILDREN));
	}

	private static int lastParamEnd(NodeBuilder nb) {
		if (nb.dtdParams != null && nb.paramCount > 0) {
			return nb.dtdParams[nb.paramCount - 1][1];
		}
		return nb.nodeEnd;
	}

	private static NodeBuilder peekSecond(Deque<NodeBuilder> stack) {
		java.util.Iterator<NodeBuilder> it = stack.iterator();
		if (it.hasNext()) {
			it.next();
		}
		return it.hasNext() ? it.next() : null;
	}

	private static boolean isDTDDeclKind(NodeKind kind) {
		return kind == NodeKind.DTD_ENTITY_DECL
				|| kind == NodeKind.DTD_ELEMENT_DECL
				|| kind == NodeKind.DTD_ATTLIST_DECL
				|| kind == NodeKind.DTD_NOTATION_DECL
				|| kind == NodeKind.DTD_DECL;
	}

	private static boolean hasMatchingElement(Deque<NodeBuilder> stack, String tag) {
		for (NodeBuilder nb : stack) {
			if (nb.kind == NodeKind.ELEMENT && tag.equals(nb.tag)) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsNewline(CharSequence text, int from, int to) {
		for (int i = from; i < to; i++) {
			char c = text.charAt(i);
			if (c == '\n' || c == '\r') {
				return true;
			}
		}
		return false;
	}

	private static void addChildToCurrentOrRoot(Deque<NodeBuilder> stack,
			List<GreenNode> rootChildren, int[] nextRootChildEnd,
			GreenNode child, int childAbsStart) {
		if (!stack.isEmpty()) {
			stack.peek().addChild(child, childAbsStart);
		} else {
			if (childAbsStart > nextRootChildEnd[0]) {
				rootChildren.add(GreenText.whitespace(childAbsStart - nextRootChildEnd[0]));
			}
			rootChildren.add(child);
			nextRootChildEnd[0] = childAbsStart + child.width();
		}
	}

	enum NodeKind {
		ELEMENT, COMMENT, TEXT, CDATA, PROCESSING_INSTRUCTION,
		DOCUMENT_TYPE, DTD_DECL,
		DTD_ELEMENT_DECL, DTD_ATTLIST_DECL, DTD_ENTITY_DECL, DTD_NOTATION_DECL
	}

	static final class NodeBuilder {
		final NodeKind kind;
		final int nodeStart;
		int nodeEnd;
		boolean closed;

		// Element fields
		String tag;
		boolean selfClosed;
		int startTagOpenOffset = GreenElement.NULL_VALUE;
		int startTagCloseOffset = GreenElement.NULL_VALUE;
		int endTagOpenOffset = GreenElement.NULL_VALUE;
		boolean endTagHasClose;
		GreenAttr[] attributes;
		int attrCount;

		// PI fields
		boolean startTagClose;
		String target;
		boolean prolog;
		boolean processingInstruction;

		// Content offsets (comment, CDATA, PI)
		int startContentOffset = GreenElement.NULL_VALUE;
		int endContentOffset = GreenElement.NULL_VALUE;
		boolean commentSameLineEndTag;

		// DTD fields
		int[] dtdDeclType;
		int[] dtdNameParam;
		int[] dtdKindParam;
		int[] dtdPublicIdParam;
		int[] dtdSystemIdParam;
		int[] dtdInternalSubsetParam;
		int[] dtdCategoryParam;
		int[] dtdContentParam;
		int[] dtdAttributeNameParam;
		int[] dtdAttributeTypeParam;
		int[] dtdAttributeValueParam;
		int[] dtdPercentParam;
		int[] dtdValueParam;
		int[] dtdUnrecognizedParam;
		int[][] dtdParams;
		int paramCount;

		// Children
		GreenNode[] children;
		int childCount;
		int firstChildAbsStart = GreenElement.NULL_VALUE;
		int nextChildAbsStart = GreenElement.NULL_VALUE;

		NodeBuilder(NodeKind kind, int nodeStart) {
			this.kind = kind;
			this.nodeStart = nodeStart;
			this.nodeEnd = nodeStart;
		}

		void addChild(GreenNode child, int childAbsStart) {
			if (children == null) {
				children = new GreenNode[4];
				childCount = 0;
				firstChildAbsStart = childAbsStart;
				nextChildAbsStart = childAbsStart;
			}
			if (childAbsStart > nextChildAbsStart) {
				appendChild(GreenText.whitespace(childAbsStart - nextChildAbsStart));
			}
			appendChild(child);
			nextChildAbsStart = childAbsStart + child.width();
		}

		private void appendChild(GreenNode child) {
			if (childCount == children.length) {
				GreenNode[] grown = new GreenNode[children.length * 2];
				System.arraycopy(children, 0, grown, 0, childCount);
				children = grown;
			}
			children[childCount++] = child;
		}

		void addAttribute(GreenAttr attr) {
			if (attributes == null) {
				attributes = new GreenAttr[4];
				attrCount = 0;
			}
			if (attrCount == attributes.length) {
				GreenAttr[] grown = new GreenAttr[attributes.length * 2];
				System.arraycopy(attributes, 0, grown, 0, attrCount);
				attributes = grown;
			}
			attributes[attrCount++] = attr;
		}

		void addParam(int start, int end) {
			if (dtdParams == null) {
				dtdParams = new int[4][];
				paramCount = 0;
			}
			if (paramCount == dtdParams.length) {
				int[][] grown = new int[dtdParams.length * 2][];
				System.arraycopy(dtdParams, 0, grown, 0, paramCount);
				dtdParams = grown;
			}
			dtdParams[paramCount++] = new int[] { start, end };
		}

		void addDeclType(int start, int end) {
			dtdDeclType = new int[] { start, end };
		}

		int[] lastParam() {
			return dtdParams[paramCount - 1];
		}

		void updateLastParamEnd(int end) {
			if (dtdParams != null && paramCount > 0) {
				dtdParams[paramCount - 1][1] = end;
			}
		}

		GreenNode buildGreen() {
			int width = nodeEnd - nodeStart;
			GreenNode[] kids = children != null
					? (childCount == children.length ? children : Arrays.copyOf(children, childCount))
					: null;

			switch (kind) {
				case ELEMENT:
					return buildElement(width, kids);
				case COMMENT:
					return buildComment(width);
				case CDATA:
					return buildCDATA(width);
				case PROCESSING_INSTRUCTION:
					return buildPI(width);
				case DOCUMENT_TYPE:
					return buildDocumentType(width, kids);
				case DTD_ELEMENT_DECL:
					return buildDTDElementDecl(width, kids);
				case DTD_ATTLIST_DECL:
					return buildDTDAttlistDecl(width, kids);
				case DTD_ENTITY_DECL:
					return buildDTDEntityDecl(width, kids);
				case DTD_NOTATION_DECL:
					return buildDTDNotationDecl(width, kids);
				case DTD_DECL:
					return buildDTDDeclNode(width, kids);
				default:
					return new GreenText(width, false);
			}
		}

		private GreenElement buildElement(int width, GreenNode[] kids) {
			GreenAttr[] attrs = attributes != null
					? (attrCount == attributes.length ? attributes : Arrays.copyOf(attributes, attrCount))
					: null;
			return new GreenElement(width, closed, tag, selfClosed,
					rel(startTagCloseOffset), rel(endTagOpenOffset),
					endTagHasClose, attrs, kids);
		}

		private GreenComment buildComment(int width) {
			return new GreenComment(width, closed, commentSameLineEndTag,
					rel(startContentOffset), rel(endContentOffset));
		}

		private GreenCDATA buildCDATA(int width) {
			return new GreenCDATA(width, closed,
					rel(startContentOffset), rel(endContentOffset));
		}

		private GreenProcessingInstruction buildPI(int width) {
			GreenAttr[] attrs = attributes != null
					? (attrCount == attributes.length ? attributes : Arrays.copyOf(attributes, attrCount))
					: null;
			return new GreenProcessingInstruction(width, closed, startTagClose,
					target, prolog, processingInstruction,
					rel(startContentOffset), rel(endContentOffset),
					rel(endTagOpenOffset), attrs);
		}

		private GreenDocumentType buildDocumentType(int width, GreenNode[] kids) {
			kids = padChildrenForDocType(kids);
			return new GreenDocumentType(width, closed,
					toParam(dtdUnrecognizedParam),
					toParam(dtdDeclType),
					toParam(dtdNameParam),
					buildDTDParams(),
					kids,
					toParam(dtdKindParam),
					toParam(dtdPublicIdParam),
					toParam(dtdSystemIdParam),
					toParam(dtdInternalSubsetParam));
		}

		private GreenNode[] padChildrenForDocType(GreenNode[] kids) {
			if (dtdInternalSubsetParam == null || kids == null || kids.length == 0
					|| firstChildAbsStart == GreenElement.NULL_VALUE) {
				return kids;
			}
			int contentStart = dtdInternalSubsetParam[0] + 1;
			int leadingGap = firstChildAbsStart - contentStart;
			if (leadingGap <= 0) {
				return kids;
			}
			GreenNode[] padded = new GreenNode[kids.length + 1];
			padded[0] = GreenText.whitespace(leadingGap);
			System.arraycopy(kids, 0, padded, 1, kids.length);
			return padded;
		}

		private GreenDTDElementDecl buildDTDElementDecl(int width, GreenNode[] kids) {
			return new GreenDTDElementDecl(width, closed,
					toParam(dtdUnrecognizedParam),
					toParam(dtdDeclType),
					toParam(dtdNameParam),
					buildDTDParams(),
					kids,
					toParam(dtdCategoryParam),
					toParam(dtdContentParam));
		}

		private GreenDTDAttlistDecl buildDTDAttlistDecl(int width, GreenNode[] kids) {
			return new GreenDTDAttlistDecl(width, closed,
					toParam(dtdUnrecognizedParam),
					toParam(dtdDeclType),
					toParam(dtdNameParam),
					buildDTDParams(),
					kids,
					toParam(dtdAttributeNameParam),
					toParam(dtdAttributeTypeParam),
					toParam(dtdAttributeValueParam),
					null);
		}

		private GreenDTDEntityDecl buildDTDEntityDecl(int width, GreenNode[] kids) {
			return new GreenDTDEntityDecl(width, closed,
					toParam(dtdUnrecognizedParam),
					toParam(dtdDeclType),
					toParam(dtdNameParam),
					buildDTDParams(),
					kids,
					toParam(dtdPercentParam),
					toParam(dtdValueParam),
					toParam(dtdKindParam),
					toParam(dtdPublicIdParam),
					toParam(dtdSystemIdParam));
		}

		private GreenDTDNotationDecl buildDTDNotationDecl(int width, GreenNode[] kids) {
			return new GreenDTDNotationDecl(width, closed,
					toParam(dtdUnrecognizedParam),
					toParam(dtdDeclType),
					toParam(dtdNameParam),
					buildDTDParams(),
					kids,
					toParam(dtdKindParam),
					toParam(dtdPublicIdParam),
					toParam(dtdSystemIdParam));
		}

		private GreenDTDDeclNode buildDTDDeclNode(int width, GreenNode[] kids) {
			return new GreenDTDDeclNode(width, closed,
					toParam(dtdUnrecognizedParam),
					toParam(dtdDeclType),
					toParam(dtdNameParam),
					buildDTDParams(),
					kids);
		}

		private int rel(int absOffset) {
			return absOffset != GreenElement.NULL_VALUE ? absOffset - nodeStart : GreenElement.NULL_VALUE;
		}

		private GreenDTDParam toParam(int[] offsets) {
			if (offsets == null) {
				return null;
			}
			return new GreenDTDParam(offsets[0] - nodeStart, offsets[1] - nodeStart);
		}

		private GreenDTDParam[] buildDTDParams() {
			if (dtdParams == null || paramCount == 0) {
				return null;
			}
			GreenDTDParam[] result = new GreenDTDParam[paramCount];
			for (int i = 0; i < paramCount; i++) {
				int[] p = dtdParams[i];
				result[i] = new GreenDTDParam(p[0] - nodeStart, p[1] - nodeStart);
			}
			return result;
		}
	}
}
