package redis.configuration.common;

import lombok.Getter;

public class YesNoOption extends StringOption {

	private @Getter boolean yes;

	public YesNoOption(String name) {
		super(name, null);
	}

	public YesNoOption(String name, String defaultValue) {
		super(name, defaultValue);
	}

	@Override
	protected void afterSet() {
		yes = "yes".equalsIgnoreCase(getValue());
	}

}