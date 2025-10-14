package org.eclipse.lemminx.utils;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html2md.converter.*;
import com.vladsch.flexmark.util.data.MutableDataSet;

import static org.apache.commons.text.StringEscapeUtils.unescapeJava;
import static org.apache.commons.text.StringEscapeUtils.unescapeXml;

public class MarkdownConverter {

	public static String convert(String html) {

		if (!StringUtils.isTagOutsideOfBackticks(html)) {
			return unescapeXml(html);
		}
		MutableDataSet options = new MutableDataSet();
		options.set(FlexmarkHtmlConverter.SETEXT_HEADINGS, false);
		options.set(FlexmarkHtmlConverter.MAX_BLANK_LINES, 1);
		options.set(FlexmarkHtmlConverter.BR_AS_PARA_BREAKS, false);
//		options.set(HtmlRenderer.SOFT_BREAK, "\n");
//		options.set(FlexmarkHtmlConverter.BR_AS_EXTRA_BLANK_LINES, false);

		var converter = FlexmarkHtmlConverter.builder(options).build();
		return unescapeJava(converter.convert(html, -1));

	}
}