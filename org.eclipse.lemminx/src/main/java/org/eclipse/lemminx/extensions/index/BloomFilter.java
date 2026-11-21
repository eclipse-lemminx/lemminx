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

import java.util.Collection;

/**
 * A compact, immutable bloom filter for fast set-membership testing.
 *
 * <p>Used in the cross-file index to answer the question "does this XML file
 * possibly reference grammar X?" without reading the file. Each indexed file
 * gets its own BloomFilter containing the grammar names and symbols it
 * references (see {@link XMLReferenceCollector}).
 *
 * <p><b>Use case (UC19):</b> When a user triggers "Find References" on a
 * grammar file like {@code schema.xsd}, the {@link WorkspaceIndex} iterates
 * all BloomFilters and calls {@link #mightContain mightContain("schema.xsd")}
 * to instantly identify candidate files — without opening any of them.
 *
 * <p><b>Guarantees:</b>
 * <ul>
 *   <li>No false negatives: if {@link #mightContain} returns {@code false},
 *       the item was definitely not added</li>
 *   <li>False positives at ~1% rate with default sizing (10 bits/element)</li>
 *   <li>Thread-safe for concurrent {@link #mightContain} queries after construction</li>
 *   <li>Memory: ~12 KB for 10,000 indexed files</li>
 * </ul>
 *
 * <p><b>Hashing:</b> Uses double-hashing with FNV-1a (hash1) and a murmur-ish
 * mix (hash2). Inspired by the IdentifierBloomFilter from java-ls (MIT License,
 * Thomas Mäder / Castle Ridge Software).
 *
 * @see XMLReferenceCollector
 * @see WorkspaceIndex
 * @see FileBloomEntry
 */
public final class BloomFilter {

	private static final double TARGET_FPR = 0.01;
	private static final int MIN_BITS = 64;
	private static final int MAX_HASH_FUNCTIONS = 8;

	private final long[] bits;
	private final int bitCount;
	private final int hashCount;

	private BloomFilter(long[] bits, int bitCount, int hashCount) {
		this.bits = bits;
		this.bitCount = bitCount;
		this.hashCount = hashCount;
	}

	/**
	 * Build a filter from the given names.
	 *
	 * @param names the names to add to the filter.
	 * @return the bloom filter.
	 */
	public static BloomFilter create(Collection<? extends CharSequence> names) {
		int n = names.size();
		if (n == 0) {
			return new BloomFilter(new long[1], MIN_BITS, 1);
		}
		int bitCount = Math.max(MIN_BITS, optimalBitCount(n, TARGET_FPR));
		int hashCount = Math.min(MAX_HASH_FUNCTIONS, optimalHashCount(bitCount, n));
		int wordCount = (bitCount + 63) >>> 6;
		long[] bits = new long[wordCount];
		for (CharSequence name : names) {
			addTo(bits, bitCount, hashCount, name);
		}
		return new BloomFilter(bits, bitCount, hashCount);
	}

	/**
	 * Returns true if the given name might have been added to this filter.
	 * False negatives never occur; false positives are possible at a low rate.
	 *
	 * @param name the name to test.
	 * @return true if the name might be in the filter, false if definitely not.
	 */
	public boolean mightContain(CharSequence name) {
		if (name == null || name.length() == 0) {
			return false;
		}
		int h1 = hash1(name);
		int h2 = hash2(name);
		for (int i = 0; i < hashCount; i++) {
			int combined = h1 + i * h2;
			int index = Math.floorMod(combined, bitCount);
			if (!isSet(bits, index)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the underlying bit array for serialization.
	 *
	 * @return the bit array.
	 */
	public long[] getBits() {
		return bits;
	}

	/**
	 * Returns the number of bits in the filter.
	 *
	 * @return the bit count.
	 */
	public int getBitCount() {
		return bitCount;
	}

	/**
	 * Returns the number of hash functions used.
	 *
	 * @return the hash count.
	 */
	public int getHashCount() {
		return hashCount;
	}

	/**
	 * Reconstruct a filter from serialized data.
	 *
	 * @param bits      the bit array.
	 * @param bitCount  the number of bits.
	 * @param hashCount the number of hash functions.
	 * @return the bloom filter.
	 */
	public static BloomFilter fromBits(long[] bits, int bitCount, int hashCount) {
		return new BloomFilter(bits, bitCount, hashCount);
	}

	private static void addTo(long[] bits, int bitCount, int hashCount, CharSequence name) {
		int h1 = hash1(name);
		int h2 = hash2(name);
		for (int i = 0; i < hashCount; i++) {
			int combined = h1 + i * h2;
			int index = Math.floorMod(combined, bitCount);
			set(bits, index);
		}
	}

	private static boolean isSet(long[] bits, int index) {
		int word = index >>> 6;
		int bit = index & 63;
		return (bits[word] & (1L << bit)) != 0;
	}

	private static void set(long[] bits, int index) {
		int word = index >>> 6;
		int bit = index & 63;
		bits[word] |= (1L << bit);
	}

	private static int optimalBitCount(int n, double fpr) {
		double m = -n * Math.log(fpr) / (Math.log(2) * Math.log(2));
		return (int) Math.ceil(m);
	}

	private static int optimalHashCount(int bitCount, int n) {
		double k = (bitCount / (double) n) * Math.log(2);
		return Math.max(1, (int) Math.round(k));
	}

	/** FNV-1a 32-bit hash. */
	private static int hash1(CharSequence s) {
		int h = 0x811c9dc5;
		for (int i = 0; i < s.length(); i++) {
			h ^= s.charAt(i);
			h *= 0x01000193;
		}
		return h;
	}

	/** Murmur-ish mixing of length + first/last chars. */
	private static int hash2(CharSequence s) {
		int h = s.length();
		if (s.length() > 0) {
			h = 31 * h + s.charAt(0);
			h = 31 * h + s.charAt(s.length() - 1);
		}
		h ^= h >>> 16;
		h *= 0x85ebca6b;
		h ^= h >>> 13;
		h *= 0xc2b2ae35;
		h ^= h >>> 16;
		// Avoid zero stride in double hashing
		return h | 1;
	}
}
