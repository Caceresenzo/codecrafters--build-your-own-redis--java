package redis.configuration.common;

import redis.configuration.Argument;

public class PortArgument extends Argument<Integer> {

	public PortArgument() {
		super("port", Integer::parseInt);
	}

	public PortArgument(int defaultValue) {
		super("port", Integer::parseInt, defaultValue);
	}

}