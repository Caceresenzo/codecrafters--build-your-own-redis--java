package redis.configuration.common;

import java.util.List;

import redis.configuration.Argument;
import redis.configuration.Option;

public class StringOption extends Option {

	public StringOption(String name, String defaultValue) {
		super(name, List.of(
			new StringArgument("value", defaultValue)
		));
	}

	public Argument<String> valueArgument() {
		return argument(0, String.class);
	}

}