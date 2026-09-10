package org.eclipse.lemminx.uriresolver;

public class CacheResourceFileNotFoundException extends CacheResourceException {

	public CacheResourceFileNotFoundException(String resourceURI, String message) {
		super(resourceURI, message);
	}
	
}
