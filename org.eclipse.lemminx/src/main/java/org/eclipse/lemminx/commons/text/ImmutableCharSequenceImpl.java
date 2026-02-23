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
 * Immutable CharSequence implementation backed by a char array.
 * Provides zero-copy substring operations.
 * 
 * Inspired by IntelliJ Platform's CharArrayCharSequence.
 */
public class ImmutableCharSequenceImpl extends CharSequenceBackedByChars implements ImmutableCharSequence {
    
    private String stringCache;
    
    public ImmutableCharSequenceImpl(char[] chars) {
        super(chars);
    }
    
    public ImmutableCharSequenceImpl(char[] chars, int start, int end) {
        super(chars, start, end);
    }
    
    public ImmutableCharSequenceImpl(String text) {
        this(text.toCharArray());
        this.stringCache = text;
    }
    
    @Override
    public ImmutableCharSequence subSequence(int start, int end) {
        if (start < 0 || end > length() || start > end) {
            throw new IndexOutOfBoundsException("start: " + start + ", end: " + end + ", length: " + length());
        }
        if (start == 0 && end == length()) {
            return this;
        }
        return new ImmutableCharSequenceImpl(getChars(), getStart() + start, getStart() + end);
    }
    
    @Override
    public String toString() {
        if (stringCache == null) {
            stringCache = super.toString();
        }
        return stringCache;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        
        // Compare with CharSequence (including String) character by character
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
     * Create an immutable CharSequence from a String.
     * The String is converted to char[] to allow zero-copy subSequence operations.
     */
    public static ImmutableCharSequence fromString(String text) {
        if (text == null) {
            return null;
        }
        return new ImmutableCharSequenceImpl(text);
    }
}

// Made with Bob
