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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Diagnostic tool: indexes files from a workspace (or a specific file in a loop)
 * and prints heap usage before and after each indexFile() call.
 *
 * Run with:  java -Xmx256m WorkspaceIndexProfiler
 *
 * Modes:
 *   LOOP  — indexes a single large file (AUTOSAR) N times; reveals memory leaks.
 *   SCAN  — indexes every XML-family file in a directory once; reveals peak allocation.
 */
public class WorkspaceIndexProfiler {

	// ---- Configuration ----
	static final Path AUTOSAR_XSD = Path.of(
			"C:/Users/AngeloZerr/Downloads/TestXML/TestXML/AUTOSAR_00048.xsd");
	static final Path WORKSPACE_ROOT = Path.of(
			"C:/Users/AngeloZerr/Downloads/TestXML/TestXML");
	// Number of loop iterations for LOOP mode
	static final int LOOP_ITERATIONS = 20;
	// true = loop on AUTOSAR; false = scan the full workspace once
	static final boolean LOOP_MODE = true;
	// Head limits: empty map = no limits (read full files)
	static final Map<String, String> HEAD_LIMITS = Collections.emptyMap();
	// ---- End configuration ----

	public static void main(String[] args) throws IOException {
		WorkspaceIndex index = new WorkspaceIndex();
		index.setHeadLimits(HEAD_LIMITS);

		System.out.printf("Mode: %s%n", LOOP_MODE ? "LOOP x" + LOOP_ITERATIONS : "SCAN");
		System.out.printf("Head limits: %s%n%n", HEAD_LIMITS.isEmpty() ? "none (full file)" : HEAD_LIMITS);

		if (LOOP_MODE) {
			runLoop(index);
		} else {
			runScan(index);
		}
	}

	// ---- LOOP mode: re-index one large file repeatedly ----

	private static void runLoop(WorkspaceIndex index) throws IOException {
		if (!Files.exists(AUTOSAR_XSD)) {
			System.err.println("File not found: " + AUTOSAR_XSD);
			return;
		}
		long fileBytes = Files.size(AUTOSAR_XSD);
		System.out.printf("File: %s  (%s)%n%n", AUTOSAR_XSD.getFileName(), fmt(fileBytes));
		System.out.printf("%-6s  %-12s  %-12s  %-12s%n", "Iter", "Before", "After", "Delta");
		System.out.println("-".repeat(50));

		for (int i = 1; i <= LOOP_ITERATIONS; i++) {
			gc();
			long before = usedMemory();
			try {
				index.indexFile(AUTOSAR_XSD);
			} catch (OutOfMemoryError e) {
				System.out.printf("%-6d  %-12s  OOM%n", i, fmt(before));
				break;
			}
			gc();
			long after = usedMemory();
			System.out.printf("%-6d  %-12s  %-12s  %-12s%n", i, fmt(before), fmt(after), fmtDelta(after - before));
		}
	}

	// ---- SCAN mode: index every file in WORKSPACE_ROOT once ----

	private static void runScan(WorkspaceIndex index) throws IOException {
		List<Path> files = collectXmlFiles(WORKSPACE_ROOT);
		System.out.printf("Found %d XML-family files%n%n", files.size());
		System.out.printf("%-50s  %-10s  %-12s  %-12s  %-12s%n",
				"File", "Size", "Before", "After", "Delta");
		System.out.println("-".repeat(102));

		for (Path file : files) {
			gc();
			long before = usedMemory();
			long fileBytes = Files.size(file);
			try {
				index.indexFile(file);
			} catch (OutOfMemoryError e) {
				System.out.printf("%-50s  %-10s  %-12s  OOM%n",
						shorten(file), fmt(fileBytes), fmt(before));
				break;
			}
			gc();
			long after = usedMemory();
			System.out.printf("%-50s  %-10s  %-12s  %-12s  %-12s%n",
					shorten(file), fmt(fileBytes), fmt(before), fmt(after), fmtDelta(after - before));
		}
	}

	// ---- Helpers ----

	private static List<Path> collectXmlFiles(Path root) throws IOException {
		List<Path> result = new ArrayList<>();
		Files.walkFileTree(root, new java.nio.file.SimpleFileVisitor<Path>() {
			@Override
			public java.nio.file.FileVisitResult visitFile(Path file,
					java.nio.file.attribute.BasicFileAttributes attrs) {
				String name = file.getFileName().toString().toLowerCase();
				if (name.endsWith(".xml") || name.endsWith(".xsd")
						|| name.endsWith(".dtd") || name.endsWith(".rng")
						|| name.endsWith(".rnc") || name.endsWith(".wsdl")
						|| name.endsWith(".xsl") || name.endsWith(".xslt")) {
					result.add(file);
				}
				return java.nio.file.FileVisitResult.CONTINUE;
			}
		});
		// Sort by size descending so the largest files are indexed first
		result.sort((a, b) -> {
			try {
				return Long.compare(Files.size(b), Files.size(a));
			} catch (IOException e) {
				return 0;
			}
		});
		return result;
	}

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
			return String.format("%.1fMB", bytes / (1024.0 * 1024));
		}
		if (Math.abs(bytes) >= 1024) {
			return String.format("%.1fKB", bytes / 1024.0);
		}
		return bytes + "B";
	}

	private static String fmtDelta(long bytes) {
		String s = fmt(bytes);
		return bytes > 0 ? "+" + s : s;
	}

	private static String shorten(Path p) {
		String s = p.toString();
		return s.length() > 50 ? "..." + s.substring(s.length() - 47) : s;
	}
}
