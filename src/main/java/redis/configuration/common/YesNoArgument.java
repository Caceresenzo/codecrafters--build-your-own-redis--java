package redis.configuration.common;

import java.util.function.Function;

import redis.configuration.Argument;

public class YesNoArgument extends Argument<Boolean> {

	public YesNoArgument(String name) {
		super(name, new Parser(name));
	}

	public YesNoArgument(String name, boolean defaultValue) {
		super(name, new Parser(name), defaultValue);
	}

	public static record Parser(
		String name
	) implements Function<String, Boolean> {

		@Override
		public Boolean apply(String input) {
			if ("yes".equalsIgnoreCase(input)) {
				return true;
			}

			if ("no".equalsIgnoreCase(input)) {
				return false;
			}

			throw new IllegalArgumentException("Invalid value for argument '" + name() + "': " + input + ". Expected 'yes' or 'no'.");
		}

	}

}