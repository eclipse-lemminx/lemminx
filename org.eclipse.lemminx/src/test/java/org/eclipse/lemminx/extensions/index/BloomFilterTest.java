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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BloomFilter}.
 */
public class BloomFilterTest {

	// --- Basic membership ---

	@Test
	public void testAddedElementIsFound() {
		BloomFilter filter = BloomFilter.create(List.of("schema.xsd"));
		assertTrue(filter.mightContain("schema.xsd"));
	}

	@Test
	public void testMultipleElementsFound() {
		BloomFilter filter = BloomFilter.create(List.of("schema.xsd", "types.xsd", "common.dtd"));
		assertTrue(filter.mightContain("schema.xsd"));
		assertTrue(filter.mightContain("types.xsd"));
		assertTrue(filter.mightContain("common.dtd"));
	}

	@Test
	public void testNonAddedElementNotFound() {
		BloomFilter filter = BloomFilter.create(List.of("schema.xsd"));
		assertFalse(filter.mightContain("other.xsd"));
	}

	@Test
	public void testEmptyFilter() {
		BloomFilter filter = BloomFilter.create(Collections.emptyList());
		assertFalse(filter.mightContain("anything"));
	}

	@Test
	public void testNullAndEmptyName() {
		BloomFilter filter = BloomFilter.create(List.of("test"));
		assertFalse(filter.mightContain(null));
		assertFalse(filter.mightContain(""));
	}

	// --- No false negatives ---

	@Test
	public void testNoFalseNegativesSmallSet() {
		List<String> names = List.of("PersonType", "AddressType", "OrderType", "ItemType", "CustomerType");
		BloomFilter filter = BloomFilter.create(names);
		for (String name : names) {
			assertTrue(filter.mightContain(name), "Expected to find: " + name);
		}
	}

	@Test
	public void testNoFalseNegativesLargeSet() {
		List<String> names = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			names.add("symbol_" + i);
		}
		BloomFilter filter = BloomFilter.create(names);
		for (String name : names) {
			assertTrue(filter.mightContain(name), "Expected to find: " + name);
		}
	}

	// --- False positive rate ---

	@Test
	public void testFalsePositiveRateUnder5Percent() {
		List<String> added = new ArrayList<>();
		for (int i = 0; i < 500; i++) {
			added.add("added_" + i);
		}
		BloomFilter filter = BloomFilter.create(added);

		int falsePositives = 0;
		int probes = 10000;
		Set<String> addedSet = new HashSet<>(added);
		for (int i = 0; i < probes; i++) {
			String candidate = "probe_" + i;
			if (!addedSet.contains(candidate) && filter.mightContain(candidate)) {
				falsePositives++;
			}
		}

		double fpr = (double) falsePositives / probes;
		assertTrue(fpr < 0.05,
				"False positive rate " + fpr + " exceeds 5%");
	}

	// --- XML-specific names ---

	@Test
	public void testXsdTypeNames() {
		BloomFilter filter = BloomFilter.create(List.of(
				"xs:complexType", "xs:simpleType", "xs:element", "xs:attribute",
				"xs:sequence", "xs:choice", "xs:all", "xs:group"));
		assertTrue(filter.mightContain("xs:complexType"));
		assertTrue(filter.mightContain("xs:element"));
		assertFalse(filter.mightContain("xs:annotation")); // not added
	}

	@Test
	public void testGrammarFilePaths() {
		BloomFilter filter = BloomFilter.create(List.of(
				"schemas/common/types.xsd",
				"../shared/address.xsd",
				"http://example.com/schema.xsd"));
		assertTrue(filter.mightContain("schemas/common/types.xsd"));
		assertTrue(filter.mightContain("../shared/address.xsd"));
		assertTrue(filter.mightContain("http://example.com/schema.xsd"));
	}

	@Test
	public void testNamespaceUris() {
		BloomFilter filter = BloomFilter.create(List.of(
				"http://www.w3.org/2001/XMLSchema",
				"http://www.w3.org/2001/XMLSchema-instance",
				"urn:oasis:names:tc:xliff:document:2.0"));
		assertTrue(filter.mightContain("http://www.w3.org/2001/XMLSchema"));
		assertTrue(filter.mightContain("urn:oasis:names:tc:xliff:document:2.0"));
	}

	// --- Case sensitivity ---

	@Test
	public void testCaseSensitive() {
		BloomFilter filter = BloomFilter.create(List.of("PersonType"));
		assertTrue(filter.mightContain("PersonType"));
		assertFalse(filter.mightContain("persontype"));
		assertFalse(filter.mightContain("PERSONTYPE"));
	}

	// --- Single character names ---

	@Test
	public void testSingleCharacterNames() {
		BloomFilter filter = BloomFilter.create(List.of("a", "b", "c"));
		assertTrue(filter.mightContain("a"));
		assertTrue(filter.mightContain("b"));
		assertTrue(filter.mightContain("c"));
		assertFalse(filter.mightContain("d"));
	}

	// --- Very long names ---

	@Test
	public void testVeryLongNames() {
		String longName = "a".repeat(10000);
		BloomFilter filter = BloomFilter.create(List.of(longName));
		assertTrue(filter.mightContain(longName));
		assertFalse(filter.mightContain("a".repeat(9999)));
	}

	// --- Duplicate names ---

	@Test
	public void testDuplicateNames() {
		BloomFilter filter = BloomFilter.create(List.of("dup", "dup", "dup"));
		assertTrue(filter.mightContain("dup"));
	}

	// --- Serialization round-trip ---

	@Test
	public void testSerializationRoundTrip() {
		List<String> names = List.of("schema.xsd", "types.xsd", "PersonType", "AddressType");
		BloomFilter original = BloomFilter.create(names);

		long[] bits = original.getBits();
		int bitCount = original.getBitCount();
		int hashCount = original.getHashCount();

		BloomFilter restored = BloomFilter.fromBits(
				Arrays.copyOf(bits, bits.length), bitCount, hashCount);

		for (String name : names) {
			assertTrue(restored.mightContain(name), "Restored filter should contain: " + name);
		}
	}

	@Test
	public void testSerializationPreservesNegatives() {
		BloomFilter original = BloomFilter.create(List.of("only-this"));
		BloomFilter restored = BloomFilter.fromBits(
				Arrays.copyOf(original.getBits(), original.getBits().length),
				original.getBitCount(),
				original.getHashCount());

		assertTrue(restored.mightContain("only-this"));
		assertFalse(restored.mightContain("not-this"));
	}

	// --- Sizing ---

	@Test
	public void testFilterBitCountGrowsWithSize() {
		BloomFilter small = BloomFilter.create(List.of("a", "b"));
		BloomFilter large = BloomFilter.create(generateNames(500));
		assertTrue(large.getBitCount() > small.getBitCount());
	}

	@Test
	public void testMinimumBitCount() {
		BloomFilter filter = BloomFilter.create(List.of("single"));
		assertTrue(filter.getBitCount() >= 64);
	}

	// --- Special characters ---

	@Test
	public void testSpecialCharacters() {
		BloomFilter filter = BloomFilter.create(List.of(
				"file:///C:/schemas/types.xsd",
				"name with spaces.xsd",
				"schéma.xsd",
				"名前.xsd"));
		assertTrue(filter.mightContain("file:///C:/schemas/types.xsd"));
		assertTrue(filter.mightContain("name with spaces.xsd"));
		assertTrue(filter.mightContain("schéma.xsd"));
		assertTrue(filter.mightContain("名前.xsd"));
	}

	// --- CharSequence support ---

	@Test
	public void testCharSequenceInput() {
		BloomFilter filter = BloomFilter.create(List.of("test"));
		StringBuilder sb = new StringBuilder("test");
		assertTrue(filter.mightContain(sb));
	}

	// --- Thread safety (basic smoke test) ---

	@Test
	public void testConcurrentReads() throws InterruptedException {
		List<String> names = generateNames(100);
		BloomFilter filter = BloomFilter.create(names);

		List<Thread> threads = new ArrayList<>();
		List<AssertionError> errors = Collections.synchronizedList(new ArrayList<>());

		for (int t = 0; t < 8; t++) {
			Thread thread = new Thread(() -> {
				try {
					for (String name : names) {
						assertTrue(filter.mightContain(name));
					}
				} catch (AssertionError e) {
					errors.add(e);
				}
			});
			threads.add(thread);
			thread.start();
		}

		for (Thread thread : threads) {
			thread.join();
		}
		assertTrue(errors.isEmpty(), "Concurrent reads produced errors");
	}

	// --- Memory efficiency ---

	@Test
	public void testMemoryEfficiency() {
		BloomFilter filter = BloomFilter.create(generateNames(10000));
		long[] bits = filter.getBits();
		long memoryBytes = (long) bits.length * 8;
		assertTrue(memoryBytes < 20_000,
				"Filter for 10K names should use less than 20KB, used: " + memoryBytes);
	}

	// --- Helpers ---

	private static List<String> generateNames(int count) {
		List<String> names = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			names.add("symbol_" + i);
		}
		return names;
	}
}
