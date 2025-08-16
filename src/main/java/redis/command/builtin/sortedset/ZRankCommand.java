package redis.command.builtin.sortedset;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RInteger;
import redis.type.RNil;
import redis.type.RString;

public record ZRankCommand(
	RString key,
	RString value
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var index = redis.getStorage().rankInSet(
			key.content(),
			value
		);

		return new CommandResponse(
			index != null
				? RInteger.of(index)
				: RNil.BULK
		);
	}

}