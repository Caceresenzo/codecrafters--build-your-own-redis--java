package redis.command.builtin.geospatial;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RInteger;
import redis.type.RString;

public record GeoAddCommand(
	RString key,
	double longitude,
	double latitude,
	RString member
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		return new CommandResponse(RInteger.ONE);
	}

}