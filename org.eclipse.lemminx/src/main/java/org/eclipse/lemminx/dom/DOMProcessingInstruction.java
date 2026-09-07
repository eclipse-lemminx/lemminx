/**
 *  Copyright (c) 2018 Angelo ZERR.
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
package org.eclipse.lemminx.dom;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;

/**
 * A processing instruction node.
 *
 */
public class DOMProcessingInstruction extends DOMCharacterData implements org.w3c.dom.ProcessingInstruction {

	DOMAttr[] attributeNodes;
	boolean startTagClose;
	String target;
	boolean prolog = false;
	boolean processingInstruction = false;
	int startContent;
	int endContent;
	int endTagOpenOffset = NULL_VALUE;

	public DOMProcessingInstruction(int start, int end) {
		super(start, end);
	}

	@Override
	public boolean hasAttributes() {
		return attributeNodes != null && attributeNodes.length > 0;
	}

	@Override
	public void setAttributeNode(DOMAttr attr) {
		if (attributeNodes == null) {
			attributeNodes = new DOMAttr[] { attr };
		} else {
			attributeNodes = Arrays.copyOf(attributeNodes, attributeNodes.length + 1);
			attributeNodes[attributeNodes.length - 1] = attr;
		}
	}

	@Override
	public List<DOMAttr> getAttributeNodes() {
		return attributeNodes != null ? Arrays.asList(attributeNodes) : null;
	}

	@Override
	public DOMAttr getAttributeAtIndex(int index) {
		if (!hasAttributes()) {
			return null;
		}
		if (index < 0 || index >= attributeNodes.length) {
			return null;
		}
		return attributeNodes[index];
	}

	@Override
	public NamedNodeMap getAttributes() {
		return attributeNodes != null ? new AttrNamedNodeMap(attributeNodes) : null;
	}

	public boolean isProlog() {
		return prolog;
	}

	public boolean isProcessingInstruction() {
		return processingInstruction;
	}

	public int getStartContent() {
		return startContent;
	}

	public int getEndContent() {
		return endContent;
	}

	/**
	 * Returns the end tag start offset and {@link DOMNode#NULL_VALUE} if it doesn't
	 * exist.
	 * 
	 * @return the end tag start offset and {@link DOMNode#NULL_VALUE} if it doesn't
	 *         exist.
	 */
	public int getEndTagStart() {
		return endTagOpenOffset;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getNodeType()
	 */
	@Override
	public short getNodeType() {
		return DOMNode.PROCESSING_INSTRUCTION_NODE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getNodeName()
	 */
	@Override
	public String getNodeName() {
		return getTarget();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.ProcessingInstruction#getTarget()
	 */
	@Override
	public String getTarget() {
		return target;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.ProcessingInstruction#getData()
	 */
	@Override
	public String getData() {
		return super.getData().trim();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.ProcessingInstruction#setData(java.lang.String)
	 */
	@Override
	public void setData(String data) throws DOMException {
		throw new UnsupportedOperationException();
	}

}
