/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.lemminx.extensions.colors.utils;

import org.eclipse.lsp4j.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Color utilities.
 * <p>
 * Some piece of code comes the vscode CSS language server written in TypeScript
 * which has been translated to Java.
 *
 * @see <a href=
 * "https://github.com/microsoft/vscode-css-languageservice/blob/main/src/languageFacts/colors.ts">https://github.com/microsoft/vscode-css-languageservice/blob/main/src/languageFacts/colors.ts</a>
 */
public class ColorUtils {

	private static final int Digit0 = 48;
	private static final int Digit9 = 57;
	private static final int A = 65;
	private static final int a = 97;
	private static final int f = 102;

	private static final Map<String, String> colors = new HashMap<>();

	static {
		colors.put("aliceblue", "#f0f8ff");
		colors.put("antiquewhite", "#faebd7");
		colors.put("aqua", "#00ffff");
		colors.put("aquamarine", "#7fffd4");
		colors.put("azure", "#f0ffff");
		colors.put("beige", "#f5f5dc");
		colors.put("bisque", "#ffe4c4");
		colors.put("black", "#000000");
		colors.put("blanchedalmond", "#ffebcd");
		colors.put("blue", "#0000ff");
		colors.put("blueviolet", "#8a2be2");
		colors.put("brown", "#a52a2a");
		colors.put("burlywood", "#deb887");
		colors.put("cadetblue", "#5f9ea0");
		colors.put("chartreuse", "#7fff00");
		colors.put("chocolate", "#d2691e");
		colors.put("coral", "#ff7f50");
		colors.put("cornflowerblue", "#6495ed");
		colors.put("cornsilk", "#fff8dc");
		colors.put("crimson", "#dc143c");
		colors.put("cyan", "#00ffff");
		colors.put("darkblue", "#00008b");
		colors.put("darkcyan", "#008b8b");
		colors.put("darkgoldenrod", "#b8860b");
		colors.put("darkgray", "#a9a9a9");
		colors.put("darkgrey", "#a9a9a9");
		colors.put("darkgreen", "#006400");
		colors.put("darkkhaki", "#bdb76b");
		colors.put("darkmagenta", "#8b008b");
		colors.put("darkolivegreen", "#556b2f");
		colors.put("darkorange", "#ff8c00");
		colors.put("darkorchid", "#9932cc");
		colors.put("darkred", "#8b0000");
		colors.put("darksalmon", "#e9967a");
		colors.put("darkseagreen", "#8fbc8f");
		colors.put("darkslateblue", "#483d8b");
		colors.put("darkslategray", "#2f4f4f");
		colors.put("darkslategrey", "#2f4f4f");
		colors.put("darkturquoise", "#00ced1");
		colors.put("darkviolet", "#9400d3");
		colors.put("deeppink", "#ff1493");
		colors.put("deepskyblue", "#00bfff");
		colors.put("dimgray", "#696969");
		colors.put("dimgrey", "#696969");
		colors.put("dodgerblue", "#1e90ff");
		colors.put("firebrick", "#b22222");
		colors.put("floralwhite", "#fffaf0");
		colors.put("forestgreen", "#228b22");
		colors.put("fuchsia", "#ff00ff");
		colors.put("gainsboro", "#dcdcdc");
		colors.put("ghostwhite", "#f8f8ff");
		colors.put("gold", "#ffd700");
		colors.put("goldenrod", "#daa520");
		colors.put("gray", "#808080");
		colors.put("grey", "#808080");
		colors.put("green", "#008000");
		colors.put("greenyellow", "#adff2f");
		colors.put("honeydew", "#f0fff0");
		colors.put("hotpink", "#ff69b4");
		colors.put("indianred", "#cd5c5c");
		colors.put("indigo", "#4b0082");
		colors.put("ivory", "#fffff0");
		colors.put("khaki", "#f0e68c");
		colors.put("lavender", "#e6e6fa");
		colors.put("lavenderblush", "#fff0f5");
		colors.put("lawngreen", "#7cfc00");
		colors.put("lemonchiffon", "#fffacd");
		colors.put("lightblue", "#add8e6");
		colors.put("lightcoral", "#f08080");
		colors.put("lightcyan", "#e0ffff");
		colors.put("lightgoldenrodyellow", "#fafad2");
		colors.put("lightgray", "#d3d3d3");
		colors.put("lightgrey", "#d3d3d3");
		colors.put("lightgreen", "#90ee90");
		colors.put("lightpink", "#ffb6c1");
		colors.put("lightsalmon", "#ffa07a");
		colors.put("lightseagreen", "#20b2aa");
		colors.put("lightskyblue", "#87cefa");
		colors.put("lightslategray", "#778899");
		colors.put("lightslategrey", "#778899");
		colors.put("lightsteelblue", "#b0c4de");
		colors.put("lightyellow", "#ffffe0");
		colors.put("lime", "#00ff00");
		colors.put("limegreen", "#32cd32");
		colors.put("linen", "#faf0e6");
		colors.put("magenta", "#ff00ff");
		colors.put("maroon", "#800000");
		colors.put("mediumaquamarine", "#66cdaa");
		colors.put("mediumblue", "#0000cd");
		colors.put("mediumorchid", "#ba55d3");
		colors.put("mediumpurple", "#9370d8");
		colors.put("mediumseagreen", "#3cb371");
		colors.put("mediumslateblue", "#7b68ee");
		colors.put("mediumspringgreen", "#00fa9a");
		colors.put("mediumturquoise", "#48d1cc");
		colors.put("mediumvioletred", "#c71585");
		colors.put("midnightblue", "#191970");
		colors.put("mintcream", "#f5fffa");
		colors.put("mistyrose", "#ffe4e1");
		colors.put("moccasin", "#ffe4b5");
		colors.put("navajowhite", "#ffdead");
		colors.put("navy", "#000080");
		colors.put("oldlace", "#fdf5e6");
		colors.put("olive", "#808000");
		colors.put("olivedrab", "#6b8e23");
		colors.put("orange", "#ffa500");
		colors.put("orangered", "#ff4500");
		colors.put("orchid", "#da70d6");
		colors.put("palegoldenrod", "#eee8aa");
		colors.put("palegreen", "#98fb98");
		colors.put("paleturquoise", "#afeeee");
		colors.put("palevioletred", "#d87093");
		colors.put("papayawhip", "#ffefd5");
		colors.put("peachpuff", "#ffdab9");
		colors.put("peru", "#cd853f");
		colors.put("pink", "#ffc0cb");
		colors.put("plum", "#dda0dd");
		colors.put("powderblue", "#b0e0e6");
		colors.put("purple", "#800080");
		colors.put("red", "#ff0000");
		colors.put("rebeccapurple", "#663399");
		colors.put("rosybrown", "#bc8f8f");
		colors.put("royalblue", "#4169e1");
		colors.put("saddlebrown", "#8b4513");
		colors.put("salmon", "#fa8072");
		colors.put("sandybrown", "#f4a460");
		colors.put("seagreen", "#2e8b57");
		colors.put("seashell", "#fff5ee");
		colors.put("sienna", "#a0522d");
		colors.put("silver", "#c0c0c0");
		colors.put("skyblue", "#87ceeb");
		colors.put("slateblue", "#6a5acd");
		colors.put("slategray", "#708090");
		colors.put("slategrey", "#708090");
		colors.put("snow", "#fffafa");
		colors.put("springgreen", "#00ff7f");
		colors.put("steelblue", "#4682b4");
		colors.put("tan", "#d2b48c");
		colors.put("teal", "#008080");
		colors.put("thistle", "#d8bfd8");
		colors.put("tomato", "#ff6347");
		colors.put("turquoise", "#40e0d0");
		colors.put("violet", "#ee82ee");
		colors.put("wheat", "#f5deb3");
		colors.put("white", "#ffffff");
		colors.put("whitesmoke", "#f5f5f5");
		colors.put("yellow", "#ffff00");
		colors.put("yellowgreen", "#9acd32");
	}

	/**
	 * Returns the {@link Color} instance value from the given <code>text</code> and null
	 * otherwise.
	 *
	 * @param text the color text.
	 *
	 * @return the {@link Color} instance value from the given <code>text</code> and null otherwise.
	 */
	public static Color getColorValue(String text) {
		String candidateColor = colors.get(text);
		if (candidateColor != null) {
			return colorFromHex(candidateColor.toLowerCase());
		}
		Color color = colorFromHex(text);
		if (color != null) {
			return color;
		}
		int startComma = text.indexOf('(');
		if (startComma == -1) {
			return null;
		}
		int endComma = text.indexOf(')');
		if (endComma == -1 || endComma < startComma) {
			return null;
		}
		String name = text.substring(0, startComma);
		if (name.isEmpty()) {
			return null;
		}
		String[] colorValues = text.substring(startComma + 1, endComma).split(",");
		if (colorValues.length < 3 || colorValues.length > 4) {
			return null;
		}

		try {
			double alpha = colorValues.length == 4 ? getNumericValue(colorValues[3], 1) : 1;
			if (name.equals("rgb") || name.equals("rgba")) {
				double red = getNumericValue(colorValues[0], 255.0);
				double green = getNumericValue(colorValues[1], 255.0);
				double blue = getNumericValue(colorValues[2], 255.0);
				return new Color(red, green, blue, alpha);
			}
			if (text.startsWith("rgb(") || text.startsWith("rgba(")) {

			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	private static double getNumericValue(String value, double factor) {
		double result = Double.parseDouble(value);
		return result / factor;
	}

	/**
	 * Converts a Color object to RGB integer components (0-255)
	 *
	 * @param color the color to convert
	 *
	 * @return array containing [red, green, blue, alpha] as integers
	 */
	private static int[] getChannelValues(Color color) {
		final DoubleToIntFunction transform = c -> (int) Math.round(255 * c);
		return DoubleStream.of(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
				.mapToInt(transform)
				.toArray();
	}

	public static String getRGB(Color color) {
		var channels = getChannelValues(color);
		return "rgb(" + Arrays.stream(channels, 0, channels[3] < 0xff ? 4 : 3)
				.mapToObj(String::valueOf)
				.collect(Collectors.joining(",")) + ")";
	}

	public static String getHex(Color color, boolean includeHash) {
		var channels = getChannelValues(color);
		var label = new StringBuilder();
		if (includeHash) label.append('#');
		Arrays.stream(channels, 0, channels[3] < 0xff ? 4 : 3)
				.mapToObj(ColorUtils::toTwoDigitHex)
				.forEach(label::append);
		return label.toString();
	}

	private static String toTwoDigitHex(double n) {
		String r = Integer.toHexString((int) n);
		return r.length() != 2 ? '0' + r : r;
	}

	public static int hexDigit(int charCode) {
		if (charCode < Digit0) {
			return 0;
		}
		if (charCode <= Digit9) {
			return charCode - Digit0;
		}
		if (charCode < a) {
			charCode += (a - A);
		}
		if (charCode >= a && charCode <= f) {
			return charCode - a + 10;
		}
		return 0;
	}

	static Color colorFromHex(String text) {
		var trimHash = text.startsWith("#") ? text.substring(1) : text;
		if (!Set.of(3, 4, 6, 8).contains(trimHash.length())) {
			return null;
		}
		return parseText(trimHash);
	}

	private static Color parseText(String text) {
		final var length = text.length();
		final var channels = new double[]{0, 0, 0, 1};
		final var codePoints = text.codePoints().map(ColorUtils::hexDigit).toArray();
		final var duplicateSlot = length <= 4;
		for (int i = 0, channel = 0; i < length; i++, channel++) {
			channels[channel] = (codePoints[i] * 0x10 + (duplicateSlot ? codePoints[i]
					: codePoints[++i])) / 255.;
		}
		return new Color(channels[0], channels[1], channels[2], channels[3]);
	}
}
