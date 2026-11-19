/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.relaxng.xml.contentmodel;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelProvider;
import org.eclipse.lemminx.extensions.relaxng.contentmodel.CMRelaxNGDocument;
import org.eclipse.lemminx.extensions.relaxng.contentmodel.MySchemaPatternBuilder;
import org.eclipse.lemminx.extensions.relaxng.jing.RelaxNGDescription;
import org.eclipse.lemminx.extensions.relaxng.jing.SchemaProvider;
import org.eclipse.lemminx.extensions.relaxng.jing.toremove.MyPatternSchema;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.eclipse.lemminx.utils.DOMUtils;

import com.thaiopensource.relaxng.pattern.Pattern;
import com.thaiopensource.validate.Schema;

/**
 * RelaxNG content model provider.
 */
public class CMRelaxNGContentModelProvider implements ContentModelProvider {

	private final URIResolverExtensionManager resolverExtensionManager;

	public CMRelaxNGContentModelProvider(URIResolverExtensionManager resolverExtensionManager) {
		this.resolverExtensionManager = resolverExtensionManager;
	}

	@Override
	public boolean adaptFor(DOMDocument document, boolean internal) {
		return false;
	}

	@Override
	public boolean adaptFor(String uri) {
		return DOMUtils.isRelaxNGUri(uri);
	}

	@Override
	public Collection<Identifier> getIdentifiers(DOMDocument xmlDocument, String namespaceURI) {
		Collection<Identifier> identifiers = new ArrayList<>();
		return identifiers;
	}

	@Override
	public CMDocument createCMDocument(String key, boolean resolveExternalEntities) {
		try {
			RelaxNGDescription description = new RelaxNGDescription(key, null);
			Schema schema = SchemaProvider.loadSchema(description, resolverExtensionManager, null,
					new MySchemaPatternBuilder());
			if (schema instanceof MyPatternSchema) {
				Pattern start = ((MyPatternSchema) schema).getStart();
				return new CMRelaxNGDocument(key, start, resolverExtensionManager);
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			// ignore the error.
		}
		return null;
	}

	@Override
	public CMDocument createInternalCMDocument(DOMDocument xmlDocument, boolean resolveExternalEntities) {
		return null;
	}

}
