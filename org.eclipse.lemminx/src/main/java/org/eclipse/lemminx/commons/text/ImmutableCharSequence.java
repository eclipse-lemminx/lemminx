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
 * Marker interface for immutable CharSequences.
 * Guarantees that the content will never change, allowing safe sharing and zero-copy operations.
 * 
 * Inspired by IntelliJ Platform's immutability guarantees.
 */
public interface ImmutableCharSequence extends CharSequence {
    
    /**
     * Returns an immutable subsequence. The returned sequence is also immutable.
     */
    @Override
    ImmutableCharSequence subSequence(int start, int end);
}

// Made with Bob
