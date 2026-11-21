/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/

package org.eclipse.lemminx.extensions.catalog;

import org.eclipse.lemminx.services.extensions.diagnostics.IXMLErrorCode;

/**
 * XML Catalog error code.
 *
 */
public enum XMLCatalogErrorCode implements IXMLErrorCode {
	catalog_uri("catalog_uri"),
	catalog_not_registered("catalog_not_registered"),
	catalog_entry_unused("catalog_entry_unused");
	
	private final String code;

	private XMLCatalogErrorCode(String code) {
		this.code = code;
	}

	@Override
	public String getCode() {
		if (code == null) {
			return name();
		}
		return code;
	}
}
