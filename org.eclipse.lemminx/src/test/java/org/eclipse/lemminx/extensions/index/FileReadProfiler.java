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
package org.eclipse.lemminx.extensions.index;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Compares peak heap usage between two file-reading strategies:
 *   1. Files.readString()   — allocates the whole file as a single String.
 *   2. BufferedReader + char-by-char — streams without accumulating all chars.
 *
 * Run from the IDE: right-click → Run As → Java Application.
 * Add -Xmx128m as a VM argument to stay close to the LemMinX server heap.
 */
public class FileReadProfiler {

	static final Path FILE = Path.of(
			"C:/Users/AngeloZerr/Downloads/TestXML/TestXML/AUTOSAR_00048.xsd");

	static final int ITERATIONS = 5;

	public static void main(String[] args) throws IOException {
		if (!Files.exists(FILE)) {
			System.err.println("File not found: " + FILE);
			return;
		}

		long fileBytes = Files.size(FILE);
		System.out.printf("File : %s%n", FILE.getFileName());
		System.out.printf("Size : %s%n%n", fmt(fileBytes));

		System.out.printf("%-6s  %-30s  %-12s  %-12s  %-12s%n",
				"Iter", "Strategy", "Before", "Peak", "After");
		System.out.println("-".repeat(80));

		for (int i = 1; i <= ITERATIONS; i++) {
			benchReadString(i);
			benchReaderCharByChar(i);
			if (i < ITERATIONS) System.out.println();
		}
	}

	// ---- Strategy 1: Files.readString ----------------------------------------

	private static void benchReadString(int iter) throws IOException {
		gc();
		long before = usedMemory();
		String text = Files.readString(FILE);
		long peak = usedMemory(); // String still alive
		int len = text.length();  // prevent dead-code elimination
		gc();
		long after = usedMemory();
		System.out.printf("%-6d  %-30s  %-12s  %-12s  %-12s  (len=%d)%n",
				iter, "Files.readString", fmt(before), fmt(peak), fmt(after), len);
	}

	// ---- Strategy 2: BufferedReader char-by-char (no accumulation) -----------

	private static void benchReaderCharByChar(int iter) throws IOException {
		gc();
		long before = usedMemory();
		long peak = before;
		int count = 0;
		try (BufferedReader reader = Files.newBufferedReader(FILE)) {
			int ch;
			while ((ch = reader.read()) != -1) {
				count++;
				if (count % 100_000 == 0) {
					long m = usedMemory();
					if (m > peak) peak = m;
				}
			}
		}
		gc();
		long after = usedMemory();
		System.out.printf("%-6d  %-30s  %-12s  %-12s  %-12s  (chars=%d)%n",
				iter, "BufferedReader char-by-char", fmt(before), fmt(peak), fmt(after), count);
	}

	// ---- Helpers -------------------------------------------------------------

	private static long usedMemory() {
		Runtime rt = Runtime.getRuntime();
		return rt.totalMemory() - rt.freeMemory();
	}

	private static void gc() {
		System.gc();
		System.gc();
		try { Thread.sleep(50); } catch (InterruptedException ignored) { }
	}

	private static String fmt(long bytes) {
		if (Math.abs(bytes) >= 1024L * 1024) {
			return String.format("%.2fMB", bytes / (1024.0 * 1024));
		}
		if (Math.abs(bytes) >= 1024) {
			return String.format("%.1fKB", bytes / 1024.0);
		}
		return bytes + "B";
	}
}
