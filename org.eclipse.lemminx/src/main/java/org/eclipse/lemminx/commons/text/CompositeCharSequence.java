/**
 *  Copyright (c) 2024 Red Hat Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Red Hat Inc. - initial API and implementation (inspired by IntelliJ Platform)
 */
package org.eclipse.lemminx.commons.text;

import java.util.Arrays;

/**
 * Composite CharSequence that concatenates multiple CharSequences without copying.
 * This allows efficient incremental text updates by reusing unchanged segments.
 * 
 * Example: For "Hello World" → "Hello Beautiful World"
 * Instead of copying the entire text, we create:
 * CompositeCharSequence("Hello ", "Beautiful ", "World")
 * 
 * Inspired by IntelliJ Platform's StringUtil.join() and rope data structures.
 */
public class CompositeCharSequence implements ImmutableCharSequence {
    
    private final CharSequence[] segments;
    private final int[] offsets; // Cumulative offsets for each segment
    private final int totalLength;
    private String stringCache;
    
    public CompositeCharSequence(CharSequence... segments) {
        if (segments == null || segments.length == 0) {
            throw new IllegalArgumentException("Segments cannot be null or empty");
        }
        
        this.segments = segments;
        this.offsets = new int[segments.length];
        
        int length = 0;
        for (int i = 0; i < segments.length; i++) {
            offsets[i] = length;
            length += segments[i].length();
        }
        this.totalLength = length;
    }
    
    @Override
    public int length() {
        return totalLength;
    }
    
    @Override
    public char charAt(int index) {
        if (index < 0 || index >= totalLength) {
            throw new IndexOutOfBoundsException("index: " + index + ", length: " + totalLength);
        }
        
        // Binary search to find the segment
        int segmentIndex = findSegmentIndex(index);
        int localIndex = index - offsets[segmentIndex];
        return segments[segmentIndex].charAt(localIndex);
    }
    
    @Override
    public ImmutableCharSequence subSequence(int start, int end) {
        if (start < 0 || end > totalLength || start > end) {
            throw new IndexOutOfBoundsException("start: " + start + ", end: " + end + ", length: " + totalLength);
        }
        
        if (start == 0 && end == totalLength) {
            return this;
        }
        
        // Zero-copy subSequence implementation
        // Find which segments are involved and create a new CompositeCharSequence
        int startSegmentIndex = findSegmentIndex(start);
        int endSegmentIndex = findSegmentIndex(end - 1); // end is exclusive
        
        if (startSegmentIndex == endSegmentIndex) {
            // SubSequence is within a single segment - delegate to that segment
            int localStart = start - offsets[startSegmentIndex];
            int localEnd = end - offsets[startSegmentIndex];
            CharSequence segment = segments[startSegmentIndex];
            
            if (segment instanceof ImmutableCharSequence) {
                return ((ImmutableCharSequence) segment).subSequence(localStart, localEnd);
            }
            // Fallback for non-immutable segments
            return ImmutableCharSequenceImpl.fromString(segment.subSequence(localStart, localEnd).toString());
        }
        
        // SubSequence spans multiple segments - create a new CompositeCharSequence
        int newSegmentCount = endSegmentIndex - startSegmentIndex + 1;
        CharSequence[] newSegments = new CharSequence[newSegmentCount];
        
        for (int i = 0; i < newSegmentCount; i++) {
            int segmentIndex = startSegmentIndex + i;
            CharSequence segment = segments[segmentIndex];
            
            if (i == 0) {
                // First segment: may need to trim from start
                int localStart = start - offsets[segmentIndex];
                if (localStart > 0) {
                    newSegments[i] = segment.subSequence(localStart, segment.length());
                } else {
                    newSegments[i] = segment;
                }
            } else if (i == newSegmentCount - 1) {
                // Last segment: may need to trim from end
                int localEnd = end - offsets[segmentIndex];
                if (localEnd < segment.length()) {
                    newSegments[i] = segment.subSequence(0, localEnd);
                } else {
                    newSegments[i] = segment;
                }
            } else {
                // Middle segments: use as-is
                newSegments[i] = segment;
            }
        }
        
        return new CompositeCharSequence(newSegments);
    }
    
    @Override
    public String toString() {
        if (stringCache == null) {
            StringBuilder sb = new StringBuilder(totalLength);
            for (CharSequence segment : segments) {
                sb.append(segment);
            }
            stringCache = sb.toString();
        }
        return stringCache;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        
        // Compare with CharSequence (including String) character by character
        // to avoid creating String copies via toString()
        if (obj instanceof CharSequence) {
            CharSequence other = (CharSequence) obj;
            if (length() != other.length()) return false;
            
            for (int i = 0; i < length(); i++) {
                if (charAt(i) != other.charAt(i)) return false;
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        // Use cached String's hashCode if available, otherwise compute it
        if (stringCache != null) {
            return stringCache.hashCode();
        }
        
        // Compute hashCode same way as String does
        int h = 0;
        int len = length();
        for (int i = 0; i < len; i++) {
            h = 31 * h + charAt(i);
        }
        return h;
    }
    
    /**
     * Find the segment index for a given character position using binary search.
     */
    private int findSegmentIndex(int position) {
        int low = 0;
        int high = segments.length - 1;
        
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (offsets[mid] <= position) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        
        return low;
    }
    
    /**
     * Create a composite CharSequence by replacing a range in the original sequence.
     * This is the key method for incremental updates without copying.
     * 
     * @param original The original CharSequence
     * @param start Start offset of the range to replace
     * @param end End offset of the range to replace
     * @param replacement The replacement text
     * @return A new CompositeCharSequence with the replacement applied
     */
    public static ImmutableCharSequence replaceRange(CharSequence original, int start, int end, CharSequence replacement) {
        if (start < 0 || end > original.length() || start > end) {
            throw new IndexOutOfBoundsException("start: " + start + ", end: " + end + ", length: " + original.length());
        }
        
        // If replacing the entire text, just return the replacement
        if (start == 0 && end == original.length()) {
            if (replacement instanceof ImmutableCharSequence) {
                return (ImmutableCharSequence) replacement;
            }
            return ImmutableCharSequenceImpl.fromString(replacement.toString());
        }
        
        // CRITICAL: Convert original to ImmutableCharSequence if it's a String
        // This ensures subSequence() operations are zero-copy
        ImmutableCharSequence immutableOriginal;
        if (original instanceof ImmutableCharSequence) {
            immutableOriginal = (ImmutableCharSequence) original;
        } else {
            // Convert String to ImmutableCharSequence (one-time copy)
            immutableOriginal = ImmutableCharSequenceImpl.fromString(original.toString());
        }
        
        // Build segments: before + replacement + after (now zero-copy)
        CharSequence before = start > 0 ? immutableOriginal.subSequence(0, start) : null;
        CharSequence after = end < immutableOriginal.length() ? immutableOriginal.subSequence(end, immutableOriginal.length()) : null;
        
        // Count non-null segments
        int segmentCount = (before != null ? 1 : 0) + (replacement.length() > 0 ? 1 : 0) + (after != null ? 1 : 0);
        
        if (segmentCount == 0) {
            return ImmutableCharSequenceImpl.fromString("");
        }
        
        if (segmentCount == 1) {
            CharSequence single = before != null ? before : (replacement.length() > 0 ? replacement : after);
            if (single instanceof ImmutableCharSequence) {
                return (ImmutableCharSequence) single;
            }
            return ImmutableCharSequenceImpl.fromString(single.toString());
        }
        
        // Build composite
        CharSequence[] segments = new CharSequence[segmentCount];
        int index = 0;
        if (before != null) segments[index++] = before;
        if (replacement.length() > 0) segments[index++] = replacement;
        if (after != null) segments[index++] = after;
        
        return new CompositeCharSequence(segments);
    }
}

// Made with Bob
