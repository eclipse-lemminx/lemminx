package org.eclipse.lemminx.settings;

public class XMLIncrementalParserSettings {

	private boolean enabled = false;
	
	private String generateTestWhen;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public String getGenerateTestWhen() {
		return generateTestWhen;
	}
}
