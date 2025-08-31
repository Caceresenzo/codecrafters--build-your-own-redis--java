package redis.command.builtin.geospatial;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.command.builtin.sortedset.ZAddCommand;
import redis.type.RString;

public record GeoAddCommand(
	RString key,
	double longitude,
	double latitude,
	RString member
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var delegate = new ZAddCommand(key, 0, key);

		return delegate.execute(redis, client);
	}

}