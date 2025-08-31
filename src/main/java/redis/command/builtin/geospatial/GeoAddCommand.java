package redis.command.builtin.geospatial;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.GeoCoordinate;
import redis.type.RInteger;
import redis.type.RString;

public record GeoAddCommand(
	RString key,
	GeoCoordinate coordinate,
	RString member
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var score = coordinate.encode();

		redis.getStorage().addToSet(
			key.content(),
			member,
			score
		);

		return new CommandResponse(RInteger.ONE);
	}

}