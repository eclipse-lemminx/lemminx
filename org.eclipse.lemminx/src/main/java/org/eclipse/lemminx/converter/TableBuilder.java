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
package org.eclipse.lemminx.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Builds a GFM-style Markdown pipe table from streamed open/close cell and
 * row events. Accumulates rows and cells in memory, then emits the full table
 * when {@link #closeTable} is called.
 *
 * <p>Use case: converts</p>
 * <pre>{@code
 * <table>
 *   <tr><th>Name</th><th>Value</th></tr>
 *   <tr><td>A</td><td>1</td></tr>
 * </table>
 * }</pre>
 * <p>into:</p>
 * <pre>
 * | Name | Value |
 * | --- | --- |
 * | A | 1 |
 * </pre>
 *
 * <p>Supports colspan (empty filler cells), nested tables (inlined into parent
 * cell), and tables without explicit {@code <tr>}.</p>
 */
public class TableBuilder {

	private static final String LINE_SEP = System.lineSeparator();

	private List<List<String>> rows;
	private List<String> currentRow;
	private StringBuilder currentCell;
	private boolean hasHeader;
	private boolean currentRowIsHeader;
	private int currentCellColspan;
	private int currentCellRowspan;
	private int currentColIndex;
	private Map<Integer, Integer> rowspanRemaining;
	private int depth;

	/**
	 * Opens a new table context. At depth 1 (outermost table), initializes row
	 * accumulation and ensures the table starts on a new line.
	 *
	 * <p>Use case: {@code <table>} → depth becomes 1, rows/cells start accumulating.
	 * Nested {@code <table>} → depth becomes 2, content inlined into parent cell.</p>
	 *
	 * @param sb the main output buffer (used for line-break insertion at depth 1)
	 */
	public void openTable(StringBuilder sb) {
		depth++;
		if (depth == 1) {
			HtmlConverter.trimTrailingSpaces(sb);
			if (sb.length() > 0 && !HtmlConverter.endsWithNewline(sb)) {
				sb.append(LINE_SEP);
			}
			rows = new ArrayList<>();
			hasHeader = false;
			currentRow = null;
			currentCell = null;
			currentRowIsHeader = false;
			currentCellColspan = 1;
			currentCellRowspan = 1;
			currentColIndex = 0;
			rowspanRemaining = new HashMap<>();
		}
	}

	/**
	 * Opens a new table row. Flushes any pending cell, adds the previous row
	 * to the row list, and starts a fresh cell list.
	 *
	 * <p>Use case: {@code <tr>} → previous row saved, new empty row started.</p>
	 */
	public void openRow() {
		if (depth == 1) {
			flushCell();
			currentCell = null;
			if (currentRow != null && rows != null) {
				rows.add(currentRow);
			}
			currentRow = new ArrayList<>();
			currentRowIsHeader = false;
			currentColIndex = 0;
			skipOccupiedColumns();
		}
	}

	/**
	 * Opens a new table cell. Flushes the previous cell (if any), starts a
	 * fresh content buffer, and records whether this is a header cell.
	 *
	 * <p>Use cases:</p>
	 * <ul>
	 *   <li>{@code <th>Name</th>} → isHeader=true, marks row as header row</li>
	 *   <li>{@code <td colspan="3">merged</td>} → colspan=3, adds 2 filler cells on flush</li>
	 * </ul>
	 *
	 * @param isHeader true for {@code <th>} cells, false for {@code <td>}
	 * @param colspan  number of columns this cell spans (1 = normal)
	 * @param rowspan  number of rows this cell spans (1 = normal)
	 */
	public void openCell(boolean isHeader, int colspan, int rowspan) {
		if (depth == 1) {
			if (currentRow == null) {
				currentRow = new ArrayList<>();
			}
			flushCell();
			currentCell = new StringBuilder();
			currentCellColspan = colspan;
			currentCellRowspan = rowspan;
			if (isHeader) {
				currentRowIsHeader = true;
				hasHeader = true;
			}
			skipOccupiedColumns();
		}
	}

	/**
	 * Closes the current table cell. At depth 1, flushes cell content into the
	 * row. At depth &gt; 1 (nested table), appends a space separator so nested
	 * cell contents are space-delimited when inlined.
	 *
	 * <p>Use case: {@code </td>} → cell content flushed to row at outer level,
	 * or space appended for nested table inlining.</p>
	 */
	public void closeCell() {
		if (depth == 1 && currentRow != null) {
			flushCell();
			currentCell = null;
		} else if (depth > 1 && currentCell != null) {
			currentCell.append(" ");
		}
	}

	/**
	 * Closes the current table row. Flushes any pending cell and adds the
	 * completed row to the row list.
	 *
	 * <p>Use case: {@code </tr>} → row with its cells added to the table.</p>
	 */
	public void closeRow() {
		if (depth == 1 && rows != null && currentRow != null) {
			flushCell();
			currentCell = null;
			rows.add(currentRow);
			if (currentRowIsHeader) {
				hasHeader = true;
			}
			currentRow = null;
			Iterator<Map.Entry<Integer, Integer>> it = rowspanRemaining.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, Integer> e = it.next();
				int remaining = e.getValue() - 1;
				if (remaining <= 0) {
					it.remove();
				} else {
					e.setValue(remaining);
				}
			}
		}
	}

	/**
	 * Closes the table context. At depth 1, flushes remaining cells/rows and
	 * emits the complete GFM pipe table to the output buffer. Decrements depth.
	 *
	 * <p>Use case: {@code </table>} → all accumulated rows rendered as
	 * {@code "| A | B |\n| --- | --- |\n| 1 | 2 |"}</p>
	 *
	 * @param sb the main output buffer where the table is appended
	 */
	public void closeTable(StringBuilder sb) {
		if (depth == 1 && rows != null) {
			flushCell();
			currentCell = null;
			if (currentRow != null) {
				rows.add(currentRow);
			}
			currentRow = null;
			emitTable(sb);
			rows = null;
		}
		depth--;
	}

	/**
	 * Returns {@code true} if currently inside a table (any depth).
	 *
	 * @return {@code true} if table depth &gt; 0
	 */
	public boolean isActive() {
		return depth > 0;
	}

	/**
	 * Returns {@code true} if inside the outermost table (depth == 1).
	 * Used to distinguish outer table rows (accumulated) from nested table
	 * content (inlined into parent cell).
	 *
	 * @return {@code true} if at outer table depth
	 */
	public boolean isOuterTable() {
		return depth == 1;
	}

	/**
	 * Returns the cell StringBuilder when inside a table, or null otherwise.
	 * Callers use this to route content into the current cell.
	 */
	public StringBuilder getCellTarget() {
		return currentCell;
	}

	/**
	 * Returns the target StringBuilder: the cell if inside a table cell, or the
	 * main output otherwise.
	 */
	public StringBuilder getTarget(StringBuilder sb) {
		return currentCell != null ? currentCell : sb;
	}

	/**
	 * Flushes the current cell content into the current row. Normalizes
	 * newlines to spaces (pipe tables are single-line), escapes pipe
	 * characters, and adds empty filler cells for colspan.
	 *
	 * <p>Ex: cell with "hello\nworld" and colspan=2
	 * → adds ["hello world", ""] to the row.</p>
	 */
	private void flushCell() {
		if (currentRow == null || currentCell == null) {
			return;
		}
		String cellText = currentCell.toString().trim();
		cellText = cellText.replace("\r\n", " ").replace("\n", " ").replace("\r", " ");
		cellText = cellText.replace("|", "\\|");
		currentRow.add(cellText);
		if (currentCellRowspan > 1) {
			for (int c = 0; c < currentCellColspan; c++) {
				rowspanRemaining.put(currentColIndex + c, currentCellRowspan);
			}
		}
		for (int i = 1; i < currentCellColspan; i++) {
			currentRow.add("");
		}
		currentColIndex += currentCellColspan;
		currentCellColspan = 1;
		currentCellRowspan = 1;
		skipOccupiedColumns();
	}

	private void skipOccupiedColumns() {
		if (currentRow == null || rowspanRemaining == null) {
			return;
		}
		while (rowspanRemaining.containsKey(currentColIndex)) {
			currentRow.add("");
			currentColIndex++;
		}
	}

	private void emitTable(StringBuilder sb) {
		if (rows.isEmpty()) {
			return;
		}
		if (sb.length() > 0 && !HtmlConverter.endsWithNewline(sb)) {
			sb.append(LINE_SEP);
		}
		int maxCols = 0;
		for (List<String> row : rows) {
			maxCols = Math.max(maxCols, row.size());
		}
		if (maxCols == 0) {
			return;
		}
		int startRow = 0;
		if (hasHeader) {
			emitTableRow(rows.get(0), maxCols, sb);
			sb.append(LINE_SEP);
			emitTableSeparator(maxCols, sb);
			startRow = 1;
		} else {
			emitEmptyHeader(maxCols, sb);
			sb.append(LINE_SEP);
			emitTableSeparator(maxCols, sb);
		}
		for (int i = startRow; i < rows.size(); i++) {
			sb.append(LINE_SEP);
			emitTableRow(rows.get(i), maxCols, sb);
		}
		sb.append(LINE_SEP);
	}

	private static void emitTableRow(List<String> row, int maxCols, StringBuilder sb) {
		sb.append("|");
		for (int i = 0; i < maxCols; i++) {
			sb.append(" ");
			sb.append(i < row.size() ? row.get(i) : "");
			sb.append(" |");
		}
	}

	private static void emitTableSeparator(int cols, StringBuilder sb) {
		sb.append("|");
		for (int i = 0; i < cols; i++) {
			sb.append(" --- |");
		}
	}

	private static void emitEmptyHeader(int cols, StringBuilder sb) {
		sb.append("|");
		for (int i = 0; i < cols; i++) {
			sb.append("  |");
		}
	}
}
