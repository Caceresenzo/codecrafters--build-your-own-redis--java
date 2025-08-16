package redis.command.builtin.sortedset;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RInteger;
import redis.type.RString;

public record ZCardCommand(
	RString key
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var sortedSet = redis.getStorage().getSortedSet(key.content());
		if (sortedSet == null) {
			return new CommandResponse(RInteger.ZERO);
		}

		final var cardinality = sortedSet.cardinality();
		return new CommandResponse(RInteger.of(cardinality));
	}

}