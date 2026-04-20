package org.eclipse.lemminx.commons.text;

import java.io.Reader;

public class CharSequenceUtils {

	public static Reader newReader(CharSequence charSequence) {
		return new CharSequenceReader(charSequence);
	}
	
	public static int indexOf(CharSequence text, CharSequence pattern) {
	    int textLength = text.length();
	    int patternLength = pattern.length();

	    if (patternLength == 0) return 0;
	    if (patternLength > textLength) return -1;

	    outer:
	    for (int i = 0; i <= textLength - patternLength; i++) {
	        for (int j = 0; j < patternLength; j++) {
	            if (text.charAt(i + j) != pattern.charAt(j)) {
	                continue outer;
	            }
	        }
	        return i;
	    }
	    return -1;
	}
	
	public static int indexOf(CharSequence text, CharSequence pattern, int fromIndex) {
	    int textLength = text.length();
	    int patternLength = pattern.length();

	    if (fromIndex < 0) fromIndex = 0;
	    if (patternLength == 0) return fromIndex;
	    if (patternLength > textLength) return -1;

	    outer:
	    for (int i = fromIndex; i <= textLength - patternLength; i++) {
	        for (int j = 0; j < patternLength; j++) {
	            if (text.charAt(i + j) != pattern.charAt(j)) {
	                continue outer;
	            }
	        }
	        return i;
	    }
	    return -1;
	}
}
