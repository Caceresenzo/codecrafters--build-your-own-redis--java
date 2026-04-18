package redis.configuration.common;

import redis.configuration.Option;

public class PortOption extends Option<Integer> {

	public PortOption(String name, int defaultValue) {
		super(name, defaultValue);
	}

	@Override
	public Integer parse(String value) {
		return Integer.parseInt(value);
	}

}