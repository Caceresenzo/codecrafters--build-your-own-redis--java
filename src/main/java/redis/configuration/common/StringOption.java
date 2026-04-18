package redis.configuration.common;

import redis.configuration.Option;

public class StringOption extends Option<String> {

	public StringOption(String name) {
		super(name, null);
	}

	public StringOption(String name, String defaultValue) {
		super(name, defaultValue);
	}

	@Override
	public String parse(String value) {
		return value;
	}

}