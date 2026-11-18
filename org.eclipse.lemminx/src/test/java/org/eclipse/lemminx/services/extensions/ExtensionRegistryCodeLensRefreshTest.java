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
package org.eclipse.lemminx.services.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link XMLExtensionsRegistry#refreshCodeLenses()} and
 * {@link XMLExtensionsRegistry#refreshCodeLenses(Predicate)}.
 */
public class ExtensionRegistryCodeLensRefreshTest {

	private XMLExtensionsRegistry registry;
	private List<Predicate<String>> capturedFilters;

	@BeforeEach
	public void setUp() {
		registry = new XMLExtensionsRegistry();
		capturedFilters = new ArrayList<>();
		registry.setCodeLensRefresher(pred -> capturedFilters.add(pred));
	}

	@Test
	public void testRefreshCodeLensesNoFilter_callsRefresherWithUniversalPredicate() {
		registry.refreshCodeLenses();

		assertEquals(1, capturedFilters.size(), "Refresher must be called exactly once");
		Predicate<String> filter = capturedFilters.get(0);
		// No-filter version must pass any URI
		assertTrue(filter.test("schema.xsd"));
		assertTrue(filter.test("order.xml"));
		assertTrue(filter.test("catalog.xml"));
	}

	@Test
	public void testRefreshCodeLensesWithFilter_callsRefresherWithGivenPredicate() {
		Predicate<String> grammarOnly = uri -> uri.endsWith(".xsd") || uri.endsWith(".dtd");
		registry.refreshCodeLenses(grammarOnly);

		assertEquals(1, capturedFilters.size(), "Refresher must be called exactly once");
		Predicate<String> filter = capturedFilters.get(0);
		assertTrue(filter.test("schema.xsd"), ".xsd must pass the grammar filter");
		assertTrue(filter.test("types.dtd"), ".dtd must pass the grammar filter");
		assertFalse(filter.test("order.xml"), ".xml must not pass the grammar filter");
	}

	@Test
	public void testRefreshCodeLensesNoRefresherSet_doesNotThrow() {
		registry.setCodeLensRefresher(null);
		// Must not throw even when no refresher is registered
		registry.refreshCodeLenses();
		registry.refreshCodeLenses(uri -> true);
	}

	@Test
	public void testRefreshCodeLensesWithFilter_notCalledWhenNoRefresherSet() {
		registry.setCodeLensRefresher(null);
		registry.refreshCodeLenses(uri -> uri.endsWith(".xsd"));
		// No assertion needed: just verifying no NPE is thrown
	}

	@Test
	public void testRefreshCodeLensesMultipleCalls_eachCallInvokesRefresher() {
		registry.refreshCodeLenses();
		registry.refreshCodeLenses(uri -> uri.endsWith(".xsd"));
		registry.refreshCodeLenses();

		assertEquals(3, capturedFilters.size(),
				"Each refreshCodeLenses call must invoke the refresher independently");
	}
}
