package org.eclipse.lemminx.performance;

import static org.eclipse.lemminx.utils.IOUtils.convertStreamToString;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Collections;

import org.eclipse.lemminx.commons.ModelTextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.dom.green.GreenDocument;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;

public class MemoryBenchmark {

	public static void main(String[] args) throws Exception {
		String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		System.out.println("PID: " + pid);
		System.out.println("Waiting 5s before start (attach profiler now)...");
		Thread.sleep(5000);

		InputStream in = MemoryBenchmark.class.getResourceAsStream("/xml/content.xml");
		String text = convertStreamToString(in);
		System.out.println("File size: " + text.length() + " chars");

		ModelTextDocument<DOMDocument> doc = new ModelTextDocument<>(text, "file:///content.xml",
				(document, cancelChecker) -> {
					if (document instanceof ModelTextDocument) {
						@SuppressWarnings("unchecked")
						ModelTextDocument<DOMDocument> mtd = (ModelTextDocument<DOMDocument>) document;
						Object prevData = mtd.getPreviousIncrementalData();
						ModelTextDocument.EditInfo editInfo = mtd.getPendingEdit();
						if (prevData instanceof GreenDocument && editInfo != null) {
							return DOMParser.getInstance().parseIncremental(document,
									(GreenDocument) prevData,
									editInfo.getStartOffset(),
									editInfo.getDeleteLength(),
									editInfo.getInsertLength(),
									null, true, cancelChecker);
						}
					}
					return DOMParser.getInstance().parse(document, null, true, cancelChecker);
				},
				DOMDocument::getGreenDocument);
		doc.setIncremental(true);

		// Phase 1: initial parse
		System.out.println("\n=== Phase 1: Initial parse ===");
		long start = System.currentTimeMillis();
		DOMDocument model = doc.getModel();
		long parseTime = System.currentTimeMillis() - start;
		System.out.println("Parse time: " + parseTime + " ms");
		System.out.println("Elements: " + countElements(model));
		System.gc();
		Thread.sleep(1000);
		System.out.println(">> SNAPSHOT 1: after initial parse. Waiting 60s...");
		Thread.sleep(60000);

		// Phase 2: single edit + reparse
		System.out.println("\n=== Phase 2: Single edit (insert 'X' at pos 100) ===");
		int version = 1;
		doc.setVersion(version);
		doc.update(Collections.singletonList(
				new TextDocumentContentChangeEvent(
						new Range(new Position(0, 100), new Position(0, 100)), "X")));
		start = System.currentTimeMillis();
		model = doc.getModel();
		parseTime = System.currentTimeMillis() - start;
		System.out.println("Incremental parse time: " + parseTime + " ms");
		System.gc();
		Thread.sleep(1000);
		System.out.println(">> SNAPSHOT 2: after 1 incremental edit. Press Enter to continue...");
		Thread.sleep(15000);

		// Phase 3: rapid edits (simulate typing)
		System.out.println("\n=== Phase 3: 10 rapid edits (simulate fast typing) ===");
		for (int i = 0; i < 10; i++) {
			version++;
			doc.setVersion(version);
			doc.update(Collections.singletonList(
					new TextDocumentContentChangeEvent(
							new Range(new Position(0, 100 + i), new Position(0, 100 + i)), "Y")));
		}
		start = System.currentTimeMillis();
		model = doc.getModel();
		parseTime = System.currentTimeMillis() - start;
		System.out.println("Parse time after 10 merged edits: " + parseTime + " ms");
		System.gc();
		Thread.sleep(1000);
		System.out.println(">> SNAPSHOT 3: after 10 rapid edits. Press Enter to continue...");
		Thread.sleep(15000);

		// Phase 4: many edit cycles
		System.out.println("\n=== Phase 4: 50 edit-parse cycles ===");
		long totalParseTime = 0;
		for (int i = 0; i < 50; i++) {
			version++;
			doc.setVersion(version);
			doc.update(Collections.singletonList(
					new TextDocumentContentChangeEvent(
							new Range(new Position(0, 100), new Position(0, 100)), "Z")));
			start = System.currentTimeMillis();
			model = doc.getModel();
			totalParseTime += System.currentTimeMillis() - start;
		}
		System.out.println("Avg incremental parse time: " + (totalParseTime / 50) + " ms");
		System.gc();
		Thread.sleep(1000);
		System.out.println(">> SNAPSHOT 4: after 50 edit-parse cycles. Press Enter to continue...");
		Thread.sleep(15000);

		// Phase 5: steady state
		System.out.println("\n=== Phase 5: Steady state (walk the tree) ===");
		start = System.currentTimeMillis();
		int nodeCount = walkTree(model);
		System.out.println("Walked " + nodeCount + " nodes in " + (System.currentTimeMillis() - start) + " ms");
		System.gc();
		Thread.sleep(1000);
		System.out.println(">> SNAPSHOT 5: after tree walk. Waiting 120s...");
		Thread.sleep(120000);

		System.out.println("Done.");
	}

	private static int countElements(DOMDocument doc) {
		int count = 0;
		for (int i = 0; i < doc.getChildren().size(); i++) {
			count += countNodes(doc.getChildren().get(i));
		}
		return count;
	}

	private static int countNodes(org.eclipse.lemminx.dom.DOMNode node) {
		int count = 1;
		if (node.hasChildNodes()) {
			for (int i = 0; i < node.getChildren().size(); i++) {
				count += countNodes(node.getChildren().get(i));
			}
		}
		return count;
	}

	private static int walkTree(DOMDocument doc) {
		int count = 0;
		for (int i = 0; i < doc.getChildren().size(); i++) {
			count += walkNode(doc.getChildren().get(i));
		}
		return count;
	}

	private static int walkNode(org.eclipse.lemminx.dom.DOMNode node) {
		int count = 1;
		node.getStart();
		node.getEnd();
		if (node.isElement()) {
			org.eclipse.lemminx.dom.DOMElement elem = (org.eclipse.lemminx.dom.DOMElement) node;
			elem.getTagName();
			elem.getStartTagCloseOffset();
			elem.getEndTagOpenOffset();
			if (elem.hasAttributes()) {
				elem.getAttributeNodes();
			}
		}
		if (node.hasChildNodes()) {
			for (int i = 0; i < node.getChildren().size(); i++) {
				count += walkNode(node.getChildren().get(i));
			}
		}
		return count;
	}
}
