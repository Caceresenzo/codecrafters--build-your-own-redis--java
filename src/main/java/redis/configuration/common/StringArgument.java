package redis.configuration.common;

import java.util.function.Function;

import redis.configuration.Argument;

public class StringArgument extends Argument<String> {

	public StringArgument(String name) {
		super(name, Function.identity());
	}

	public StringArgument(String name, String defaultValue) {
		super(name, Function.identity(), defaultValue);
	}

}