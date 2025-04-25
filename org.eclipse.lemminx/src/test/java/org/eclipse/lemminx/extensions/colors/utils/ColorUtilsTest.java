/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     David Schneider - Optional `#` prefix for hex colors
 ******************************************************************************/

package org.eclipse.lemminx.extensions.colors.utils;

import org.eclipse.lsp4j.*;
import org.junit.jupiter.api.*;

import java.util.function.*;

import static org.junit.jupiter.api.Assertions.*;
class ColorUtilsTest {
	@Test
	void parseText() {
		assertRgb("000000", 0, 0, 0);
		assertRgb("000", 0, 0, 0);
		assertRgb("ff0000", 0xff, 0, 0);
		assertRgb("f00", 0xff, 0, 0);
		assertRgb("0b0", 0, 0xbb, 0);
		assertRgb("004", 0, 0, 0x44);
		assertRgba("18ce", 0x11, 0x88, 0xcc, 0xee);
		assertRgba("f198dba2", 0xf1, 0x98, 0xdb, 0xa2);
		assertRgba("c001040f", 0xc0, 0x01, 0x04, 0x0f);
		assertRgba("dbb88010", 0xdb, 0xb8, 0x80, 0x10);
		assertRgba("0000f1ff", 0x00, 0x00, 0xf1, 0xff);

	}

	private static void assertRgb(String text, int r, int g, int b) {
		assertRgba(text, r, g, b, 255);
	}

	private static void assertRgba(String text, int r, int g, int b, int a) {
		assertRgbaVariant(text, r, g, b, a);
		assertRgbaVariant("#" + text, r, g, b, a);
	}

	private static void assertRgbaVariant(String text, int r, int g, int b, int a) {
		Color color = ColorUtils.colorFromHex(text);
		assertNotNull(color, "Color is null for text: " + text);
		assertion(r / 255., color::getRed, "Red");
		assertion(g / 255., color::getGreen, "Green");
		assertion(b / 255., color::getBlue, "Blue");
		assertion(a / 255., color::getAlpha, "Alpha");
	}

	private static void assertion(double expected, Supplier<Double> channel, String channelName) {
		double actual = channel.get();
		assertEquals(expected, actual, channelName + " channel: " + actual + " != expected: " + expected);
	}
}
