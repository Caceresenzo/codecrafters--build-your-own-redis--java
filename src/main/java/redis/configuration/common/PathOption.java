package redis.configuration.common;

import java.util.List;

import redis.configuration.Argument;
import redis.configuration.Option;

public class PathOption extends Option {

	public PathOption(String name) {
		super(name, List.of(
			new StringArgument("path")
		));
	}

	public Argument<String> pathArgument() {
		return argument(0, String.class);
	}

}