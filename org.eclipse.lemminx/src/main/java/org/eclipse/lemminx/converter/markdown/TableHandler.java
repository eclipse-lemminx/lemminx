/*******************************************************************************
 * Copyright (c) 2026 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.lemminx.converter.markdown;

import org.eclipse.lemminx.converter.HtmlAttributes;
import org.eclipse.lemminx.converter.TableBuilder;
import org.eclipse.lemminx.converter.TagHandler;

/**
 * Handles table tags ({@code <table>}, {@code <tr>}, {@code <th>},
 * {@code <td>}) by delegating to {@link TableBuilder} for GFM-style
 * pipe-table rendering.
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>{@code <table><tr><th>A</th><th>B</th></tr><tr><td>1</td><td>2</td></tr></table>}
 *       → {@code "| A | B |\n| --- | --- |\n| 1 | 2 |"}</li>
 *   <li>{@code <td colspan="2">merged</td>} → spans 2 columns with empty filler cells</li>
 *   <li>Nested tables are inlined into the parent cell content</li>
 * </ul>
 */
public class TableHandler {

	private final TableBuilder tableBuilder = new TableBuilder();

	/** {@code <table>} → opens table context; {@code </table>} → emits GFM pipe table. */
	public final TagHandler table = new TagHandler() {
		@Override
		public void open(HtmlAttributes attrs, StringBuilder sb) {
			tableBuilder.openTable(sb);
		}

		@Override
		public void close(StringBuilder sb) {
			tableBuilder.closeTable(sb);
		}
	};

	/** {@code <tr>} → opens a new row; {@code </tr>} → finalizes the row. */
	public final TagHandler row = new TagHandler() {
		@Override
		public void open(HtmlAttributes attrs, StringBuilder sb) {
			tableBuilder.openRow();
		}

		@Override
		public void close(StringBuilder sb) {
			tableBuilder.closeRow();
		}
	};

	/** {@code <th>} → opens a header cell (triggers separator row); {@code </th>} → closes it. */
	public final TagHandler header = new TagHandler() {
		@Override
		public void open(HtmlAttributes attrs, StringBuilder sb) {
			tableBuilder.openCell(true, attrs.getColspan(), attrs.getRowspan());
		}

		@Override
		public void close(StringBuilder sb) {
			tableBuilder.closeCell();
		}
	};

	/** {@code <td>} → opens a data cell (with optional colspan/rowspan); {@code </td>} → closes it. */
	public final TagHandler cell = new TagHandler() {
		@Override
		public void open(HtmlAttributes attrs, StringBuilder sb) {
			tableBuilder.openCell(false, attrs.getColspan(), attrs.getRowspan());
		}

		@Override
		public void close(StringBuilder sb) {
			tableBuilder.closeCell();
		}
	};

	/**
	 * Returns the buffer where content should be written: the cell buffer
	 * when inside a table, or the main buffer otherwise.
	 *
	 * <p>Use case: when processing {@code <td>cell text</td>}, returns the
	 * cell's internal buffer so text goes there instead of the main output.</p>
	 */
	public StringBuilder getTarget(StringBuilder sb) {
		return tableBuilder.getTarget(sb);
	}
}
