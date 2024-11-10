package redis.configuration.common;

import java.util.List;

import redis.configuration.Argument;
import redis.configuration.Option;

public class RemoteOption extends Option {

	public RemoteOption(String name) {
		super(name, List.of(
			new StringArgument("host and port")
		));
	}

	public Argument<String> hostAndPortArgument() {
		return argument(0, String.class);
	}

	public String host() {
		return hostAndPortArgument().get().split(" ")[0];
	}

	public int port() {
		return Integer.parseInt(hostAndPortArgument().get().split(" ")[1]);
	}

}