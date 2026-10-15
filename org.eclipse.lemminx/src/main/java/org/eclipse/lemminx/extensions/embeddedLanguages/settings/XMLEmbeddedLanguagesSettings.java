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
package org.eclipse.lemminx.extensions.embeddedLanguages.settings;

import java.util.List;

import org.eclipse.lemminx.utils.JSONUtility;

/**
 * XML embedded languages settings:
 *
 * <code>
 * "xml.embeddedLanguages": [
 *   {
 *     "pattern": "**&#47;mybatis-mapper.xml",
 *     "expressions": [
 *       {
 *         "xpath": "mapper/select/text()",
 *         "languageId": "sql"
 *       },
 *       {
 *         "xpath": "mapper/insert/text()",
 *         "languageId": "sql"
 *       }
 *     ]
 *   },
 *   {
 *     "pattern": "**&#47;*.xhtml",
 *     "expressions": [
 *       {
 *         "xpath": "//script/text()",
 *         "languageId": "javascript"
 *       },
 *       {
 *         "xpath": "//style/text()",
 *         "languageId": "css"
 *       }
 *     ]
 *   }
 * ]
 * </code>
 */
public class XMLEmbeddedLanguagesSettings {

	private List<XMLEmbeddedLanguages> embeddedLanguages;

	public List<XMLEmbeddedLanguages> getEmbeddedLanguages() {
		return embeddedLanguages;
	}

	public void setEmbeddedLanguages(List<XMLEmbeddedLanguages> embeddedLanguages) {
		this.embeddedLanguages = embeddedLanguages;
	}

	public static XMLEmbeddedLanguagesSettings getXMLEmbeddedLanguagesSettings(
			Object initializationOptionsSettings) {
		return JSONUtility.toModel(initializationOptionsSettings, XMLEmbeddedLanguagesSettings.class);
	}
}
