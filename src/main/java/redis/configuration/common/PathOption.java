package redis.configuration.common;

import java.util.List;

import redis.configuration.Argument;
import redis.configuration.Option;

public class PathOption extends Option {

	public PathOption(String name) {
		this(name, null);
	}

	public PathOption(String name, String defaultValue) {
		super(name, List.of(
			new StringArgument("path", defaultValue)
		));
	}

	public Argument<String> pathArgument() {
		return argument(0, String.class);
	}

}