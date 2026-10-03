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
package org.eclipse.lemminx.services.format;

import java.util.List;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMProcessingInstruction;
import org.eclipse.lsp4j.TextEdit;

/**
 * Formats processing instructions ({@code <?target ...?>}).
 *
 * <p>Processing instructions like {@code <?xml version="1.0"?>} are formatted
 * with three operations:</p>
 * <ol>
 * <li><b>Indentation</b> — when nested inside a parent element, the PI is
 * placed on a new line with proper indentation.
 * <pre>&lt;a&gt;&lt;?m2e?&gt;&lt;/a&gt; → &lt;a&gt;\n  &lt;?m2e?&gt;\n&lt;/a&gt;</pre></li>
 *
 * <li><b>Attribute spacing</b> — extra spaces around {@code =} and between
 * attributes are removed.
 * <pre>&lt;?xml version = "1.0"  encoding = "UTF-8"?&gt; → &lt;?xml version="1.0" encoding="UTF-8"?&gt;</pre></li>
 *
 * <li><b>Closing bracket</b> — trailing space before {@code ?>} is removed.
 * <pre>&lt;?xml version="1.0" ?&gt; → &lt;?xml version="1.0"?&gt;</pre></li>
 * </ol>
 *
 * @author Angelo ZERR
 */
public class DOMProcessingInstructionFormatter {

	private final XMLFormatterDocument formatterDocument;

	private final DOMAttributeFormatter attributeFormatter;

	/**
	 * Creates a new processing instruction formatter.
	 *
	 * @param formatterDocument  the parent formatter document (provides settings and edit helpers).
	 * @param attributeFormatter the formatter for PI pseudo-attributes.
	 */
	public DOMProcessingInstructionFormatter(XMLFormatterDocument formatterDocument,
			DOMAttributeFormatter attributeFormatter) {
		this.formatterDocument = formatterDocument;
		this.attributeFormatter = attributeFormatter;
	}

	/**
	 * Formats a processing instruction: indentation, attribute spacing, and
	 * closing bracket cleanup.
	 *
	 * @param processingInstruction the PI node to format.
	 * @param parentConstraints     the parent element's formatting constraints.
	 * @param edits                 the list of text edits to populate.
	 */
	public void formatProcessingInstruction(DOMProcessingInstruction processingInstruction,
			XMLFormattingConstraints parentConstraints, List<TextEdit> edits) {
		int prevOffset = processingInstruction.getStartContent();
		DOMElement parentElement = processingInstruction.getParentElement();

		// Indent PI inside a parent element
		// Use case: <a><?m2e?></a> → <a>\n  <?m2e?>\n</a>
		if (parentElement != null) {
			int indentLevel = parentConstraints.getIndentLevel();
			int parentStartCloseOffset = parentElement.getStartTagCloseOffset() + 1;
			replaceLeftSpacesWithIndentation(indentLevel, parentStartCloseOffset, processingInstruction.getStart(),
					true, edits);
		}
		// Normalize attribute spacing to single spaces
		// Use case: <?xml version = "1.0" encoding = "UTF-8"?>
		//         → <?xml version="1.0" encoding="UTF-8"?>
		if (processingInstruction.hasAttributes()) {
			boolean singleAttribute = processingInstruction.hasSingleAttribute();
			for (DOMAttr attr : processingInstruction.attributes()) {
				attributeFormatter.formatAttribute(attr, prevOffset, singleAttribute, false, false, parentConstraints, edits);
				prevOffset = attr.getEnd();
			}
		}
		// Remove trailing space before ?>
		// Use case: <?xml version="1.0" ?> → <?xml version="1.0"?>
		if (processingInstruction.isClosed()) {
			int endPIOffset = processingInstruction.getEnd() - 2;
			if (prevOffset != endPIOffset) {
				replaceLeftSpacesWith(prevOffset, endPIOffset, "", edits);
			}
		}
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWith}. */
	private void replaceLeftSpacesWith(int leftLimit, int to, String replacement, List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWith(leftLimit, to, replacement, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithIndentation}. */
	private int replaceLeftSpacesWithIndentation(int indentLevel, int from, int to, boolean addLineSeparator,
			List<TextEdit> edits) {
		return formatterDocument.replaceLeftSpacesWithIndentation(indentLevel, from, to, addLineSeparator, edits);
	}
}
