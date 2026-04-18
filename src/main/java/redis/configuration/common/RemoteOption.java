package redis.configuration.common;

import java.net.InetSocketAddress;

import redis.configuration.Option;

public class RemoteOption extends Option<InetSocketAddress> {

	public RemoteOption(String name) {
		super(name, null);
	}

	@Override
	public InetSocketAddress parse(String value) {
		final var parts = value.split(" ", 2);

		final var address = parts[0];
		final var port = Integer.parseInt(parts[1]);

		return new InetSocketAddress(address, port);
	}

}