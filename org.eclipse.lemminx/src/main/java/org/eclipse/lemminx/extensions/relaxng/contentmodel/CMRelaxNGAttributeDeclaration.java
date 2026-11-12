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
package org.eclipse.lemminx.extensions.relaxng.contentmodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.contentmodel.model.CMAttributeDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.relaxng.utils.RelaxNGUtils;
import org.eclipse.lemminx.services.extensions.ISharedSettingsRequest;

import com.thaiopensource.relaxng.pattern.AttributePattern;
import com.thaiopensource.relaxng.pattern.ElementPattern;
import com.thaiopensource.relaxng.pattern.NameClass;
import com.thaiopensource.relaxng.pattern.Pattern;
import com.thaiopensource.relaxng.pattern.SimpleNameClass;
import com.thaiopensource.xml.util.Name;

/**
 * RelaxNG content attribute implementation.
 * 
 * <p>
 * NOTE : this class is hosted in 'com.thaiopensource.relaxng.pattern' because
 * {@link Pattern} implementation like {@link ElementPattern} are not public.
 * Once https://github.com/relaxng/jing-trang/issues/271 will be fixed we could
 * move this class in 'org.eclipse.lemminx.extensions.relaxng.contentmodel'
 * package.
 * </p>
 * 
 * @author Angelo ZERR
 *
 */
public class CMRelaxNGAttributeDeclaration implements CMAttributeDeclaration {

	private static final Logger LOGGER = Logger.getLogger(CMRelaxNGAttributeDeclaration.class.getName());

	private static final String RELAXNG_NS = "http://relaxng.org/ns/structure/1.0";

	private static final String TYPE_ATTR = RelaxNGUtils.TYPE_ATTR;

	private static final List<String> BUILTIN_DATATYPE_VALUES = Arrays.asList("string", "token");

	private static List<String> xsdDatatypeValues;

	private final CMRelaxNGElementDeclaration cmElement;
	private final AttributePattern pattern;

	private String prefix;

	private boolean computedPrefix;

	private boolean required;
	private List<String> values;

	// Additional patterns from other <choice> branches that declare the same
	// attribute name. Their enumeration values and documentation are merged with
	// the primary pattern so that completion shows all possible values.
	private List<AttributePattern> mergedPatterns;

	public CMRelaxNGAttributeDeclaration(CMRelaxNGElementDeclaration element, AttributePattern pattern) {
		this.cmElement = element;
		this.pattern = pattern;
		this.computedPrefix = false;
	}

	/**
	 * Merges an additional {@link AttributePattern} from another {@code <choice>}
	 * branch that declares the same attribute name. The merged pattern's
	 * enumeration values and documentation will be included in
	 * {@link #getEnumerationValues()} and
	 * {@link #getAttributeValueDocumentation(String, ISharedSettingsRequest)}.
	 *
	 * @param p the attribute pattern to merge.
	 */
	void addMergedPattern(AttributePattern p) {
		if (mergedPatterns == null) {
			mergedPatterns = new ArrayList<>();
		}
		mergedPatterns.add(p);
	}

	@Override
	public String getPrefix() {
		String namespaceURI = getNamespace();
		if (namespaceURI == null) {
			return null;
		}
		if (computedPrefix) {
			return prefix;
		}
		DOMElement element = ((CMRelaxNGElementDeclaration) getOwnerElementDeclaration()).getDOMElement();
		if (element != null) {
			prefix = element.getPrefix(namespaceURI);
		}
		computedPrefix = true;
		return prefix;
	}

	@Override
	public String getLocalName() {
		return getJingName().getLocalName();
	}

	@Override
	public String getNamespace() {
		return getJingName().getNamespaceUri();
	}

	@Override
	public CMElementDeclaration getOwnerElementDeclaration() {
		return cmElement;
	}

	Name getJingName() {
		NameClass nameClass = pattern.getNameClass();
		if (nameClass instanceof SimpleNameClass) {
			return ((SimpleNameClass) nameClass).getName();
		}
		return null;
	}

	@Override
	public String getDefaultValue() {
		return pattern.getDefaultValue();
	}

	@Override
	public Collection<String> getEnumerationValues() {
		if (values == null) {
			values = new CMRelaxNGAttributeValuesCollector(pattern.getContent()).getValues();
			// Collect enumeration values from merged <choice> branch patterns
			if (mergedPatterns != null) {
				for (AttributePattern mp : mergedPatterns) {
					for (String v : new CMRelaxNGAttributeValuesCollector(mp.getContent()).getValues()) {
						if (!values.contains(v)) {
							values.add(v);
						}
					}
				}
			}
			if (values.isEmpty()) {
				values = getDatatypeValues();
			}
		}
		return values;
	}

	@Override
	public Collection<String> getEnumerationValues(DOMElement element) {
		Collection<String> allValues = getEnumerationValues();
		if (element == null) {
			return allValues;
		}
		if (!TYPE_ATTR.equals(getLocalName()) || !RELAXNG_NS.equals(cmElement.getNamespace())) {
			return allValues;
		}
		String elementName = cmElement.getLocalName();
		if (!RelaxNGUtils.DATA_TAG.equals(elementName) && !RelaxNGUtils.VALUE_TAG.equals(elementName)) {
			return allValues;
		}
		String datatypeLibrary = RelaxNGUtils.getEffectiveDatatypeLibrary(element);
		if (datatypeLibrary != null && !datatypeLibrary.isEmpty()) {
			return allValues;
		}
		return BUILTIN_DATATYPE_VALUES;
	}

	private List<String> getDatatypeValues() {
		if (TYPE_ATTR.equals(getLocalName()) && RELAXNG_NS.equals(cmElement.getNamespace())) {
			String elementName = cmElement.getLocalName();
			if (RelaxNGUtils.DATA_TAG.equals(elementName) || RelaxNGUtils.VALUE_TAG.equals(elementName)) {
				List<String> result = new ArrayList<>(BUILTIN_DATATYPE_VALUES);
				result.addAll(getXSDDatatypeValues());
				return result;
			}
		}
		return new ArrayList<>();
	}

	/**
	 * Returns the builtin RelaxNG datatype values ({@code "string"},
	 * {@code "token"}) available without a {@code datatypeLibrary} attribute.
	 *
	 * @return an unmodifiable list of builtin type names.
	 */
	public static List<String> getBuiltinDatatypeValues() {
		return BUILTIN_DATATYPE_VALUES;
	}

	/**
	 * Returns the XSD datatype values retrieved via reflection from Jing's
	 * {@code DatatypeLibraryImpl.typeMap}, excluding the builtin types
	 * ({@code "string"}, {@code "token"}) which are already covered by
	 * {@link #getBuiltinDatatypeValues()}. The result is sorted alphabetically
	 * and cached after the first call.
	 *
	 * @return a sorted list of XSD type names (e.g. {@code "boolean"},
	 *         {@code "integer"}, {@code "date"}, ...).
	 */
	public static synchronized List<String> getXSDDatatypeValues() {
		if (xsdDatatypeValues != null) {
			return xsdDatatypeValues;
		}
		try {
			com.thaiopensource.datatype.xsd.DatatypeLibraryImpl xsdLib = new com.thaiopensource.datatype.xsd.DatatypeLibraryImpl(
					new com.thaiopensource.datatype.xsd.regex.java.RegexEngineImpl());
			Map<String, ?> typeMap = org.eclipse.lemminx.extensions.xerces.ReflectionUtils.getFieldValue(xsdLib,
					"typeMap");
			List<String> types = new ArrayList<>(typeMap.keySet());
			types.removeAll(BUILTIN_DATATYPE_VALUES);
			Collections.sort(types);
			xsdDatatypeValues = types;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to retrieve XSD datatype values via reflection", e);
			xsdDatatypeValues = Collections.emptyList();
		}
		return xsdDatatypeValues;
	}

	@Override
	public String getAttributeNameDocumentation(ISharedSettingsRequest request) {
		return cmElement.getCMDocument().getDocumentation(pattern.getLocator());
	}

	@Override
	public String getAttributeValueDocumentation(String value, ISharedSettingsRequest request) {
		if (!getEnumerationValues().isEmpty()) {
			String documentation = cmElement.getCMDocument().getDocumentation(pattern.getLocator(), value);
			if (documentation != null) {
				return documentation;
			}
			// Check merged <choice> branch patterns for value-specific documentation
			if (mergedPatterns != null) {
				for (AttributePattern mp : mergedPatterns) {
					documentation = cmElement.getCMDocument().getDocumentation(mp.getLocator(), value);
					if (documentation != null) {
						return documentation;
					}
				}
			}
		}
		// There was no specific documentation for the value, so use the general
		// attribute documentation
		return getAttributeNameDocumentation(request);
	}

	@Override
	public boolean isRequired() {
		return required;
	}

	void setRequired(boolean required) {
		this.required = required;
	}

	public AttributePattern getPattern() {
		return pattern;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("<attribute");
		result.append(" name=\"");
		result.append(getJingName().getLocalName());
		result.append("\"");
		result.append(" namespaceUri=\"");
		result.append(getJingName().getNamespaceUri());
		result.append("\"");
		result.append(" />");
		return result.toString();
	}
}
