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

/**
 * CharSequence backed by a char array. Provides zero-copy substring operations.
 * Inspired by IntelliJ Platform's CharArrayCharSequence.
 */
public class CharSequenceBackedByChars implements CharSequence {
    
    private final char[] chars;
    private final int start;
    private final int end;
    
    public CharSequenceBackedByChars(char[] chars) {
        this(chars, 0, chars.length);
    }
    
    public CharSequenceBackedByChars(char[] chars, int start, int end) {
        if (start < 0 || end > chars.length || start > end) {
            throw new IndexOutOfBoundsException("start: " + start + ", end: " + end + ", length: " + chars.length);
        }
        this.chars = chars;
        this.start = start;
        this.end = end;
    }
    
    @Override
    public int length() {
        return end - start;
    }
    
    @Override
    public char charAt(int index) {
        if (index < 0 || index >= length()) {
            throw new IndexOutOfBoundsException("index: " + index + ", length: " + length());
        }
        return chars[start + index];
    }
    
    @Override
    public CharSequence subSequence(int start, int end) {
        if (start < 0 || end > length() || start > end) {
            throw new IndexOutOfBoundsException("start: " + start + ", end: " + end + ", length: " + length());
        }
        return new CharSequenceBackedByChars(chars, this.start + start, this.start + end);
    }
    
    @Override
    public String toString() {
        return new String(chars, start, end - start);
    }
    
    /**
     * Get the underlying char array (for efficient operations).
     * WARNING: Do not modify the returned array!
     */
    public char[] getChars() {
        return chars;
    }
    
    public int getStart() {
        return start;
    }
    
    public int getEnd() {
        return end;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        
        // Compare with CharSequence (including String) character by character
        // to avoid creating String copies via toString()
        if (obj instanceof CharSequence) {
            CharSequence other = (CharSequence) obj;
            int len = length();
            if (len != other.length()) return false;
            
            for (int i = 0; i < len; i++) {
                if (charAt(i) != other.charAt(i)) return false;
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        // Use same hashCode as String for consistency
        int h = 0;
        int len = length();
        for (int i = 0; i < len; i++) {
            h = 31 * h + charAt(i);
        }
        return h;
    }
}

// Made with Bob
