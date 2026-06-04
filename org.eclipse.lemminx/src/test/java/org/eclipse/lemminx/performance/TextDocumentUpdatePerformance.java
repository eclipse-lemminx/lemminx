/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.performance;

import static org.eclipse.lemminx.utils.IOUtils.convertStreamToString;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;

/**
 * This utility class is used to check the performance of
 * {@link TextDocument#update(List)}, updating the large content.xml file
 * 
 * @author Angelo ZERR
 *
 */
public class TextDocumentUpdatePerformance {

	public static void main(String[] args) {
		InputStream in = TextDocumentUpdatePerformance.class.getResourceAsStream("/xml/content.xml");
		String text = convertStreamToString(in);
		TextDocument document = new TextDocument(text, "content.xml");
		document.setIncremental(true);
		// Continuously parses the large content.xml file with the DOM parser.
		while (true) {
			long start = System.currentTimeMillis();
			// Insert a space
			List<TextDocumentContentChangeEvent> changes = new ArrayList<>();
			TextDocumentContentChangeEvent change = new TextDocumentContentChangeEvent(
					new Range(new Position(14, 13), new Position(14, 13)), " ");
			changes.add(change);
			document.update(changes);
			System.err.println("Update 'content.xml' text document in " + (System.currentTimeMillis() - start) + " ms.");
		}

	}

}
