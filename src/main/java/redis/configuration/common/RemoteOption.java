package redis.configuration.common;

import java.util.List;

import redis.configuration.Argument;
import redis.configuration.Option;

public class RemoteOption extends Option {

	public RemoteOption(String name) {
		super(name, List.of(
			new StringArgument("host"),
			new PortArgument()
		));
	}

	public Argument<String> hostArgument() {
		return argument(0, String.class);
	}

	public Argument<Integer> portArgument() {
		return argument(1, Integer.class);
	}

}