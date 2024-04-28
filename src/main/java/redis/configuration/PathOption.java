package redis.configuration;

import java.util.List;
import java.util.function.Function;

public class PathOption extends Option {

	public PathOption(String name) {
		super(name, List.of(
			new Argument<String>("path", Function.identity())
		));
	}

	public Argument<String> pathArgument() {
		return argument(0, String.class);
	}

}