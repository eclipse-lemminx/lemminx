/**
 *  Copyright (c) 2026 IBM Corp. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  IBM Corp. - initial API and implementation
 */
package org.eclipse.lemminx.uriresolver;

public class CacheResourceFileNotFoundException extends CacheResourceException {

	public CacheResourceFileNotFoundException(String resourceURI, String message) {
		super(resourceURI, message);
	}

}
