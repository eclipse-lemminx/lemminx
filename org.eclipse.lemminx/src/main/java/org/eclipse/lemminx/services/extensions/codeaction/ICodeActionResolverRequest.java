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
package org.eclipse.lemminx.services.extensions.codeaction;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.contentmodel.generator.XMLElementGenerator;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Range;

/**
 * The code action resolver request API.
 * 
 * @author Angelo ZERR
 *
 */
public interface ICodeActionResolverRequest extends IBaseCodeActionRequest {

	/**
	 * Returns the unresolved code action.
	 * 
	 * @return the unresolved code action.
	 */
	CodeAction getUnresolved();

	/**
	 * Returns the participant ID {@link ICodeActionResolvesParticipant} which must
	 * resolve the unresolved code action.
	 * 
	 * @return the participant ID {@link ICodeActionResolvesParticipant} which must
	 *         resolve the unresolved code action.
	 */
	String getParticipantId();

	/**
	 * Returns the data property value of the given <code>fieldName</code> and null
	 * otherwise.
	 * 
	 * @param fieldName the field name.
	 * 
	 * @return the data property value of the given <code>fieldName</code> and null
	 *         otherwise.
	 */
	String getDataProperty(String fieldName);

	/**
	 * Returns the XML generator and null otherwise.
	 * 
	 * @param range the range of the Code Action
	 * 
	 * @return the XML generator and null otherwise.
	 */
	XMLElementGenerator getXMLGenerator(Range range) throws BadLocationException;

}
