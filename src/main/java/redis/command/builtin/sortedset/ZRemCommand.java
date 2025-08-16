package redis.command.builtin.sortedset;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RInteger;
import redis.type.RString;

public record ZRemCommand(
	RString key,
	RString value
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var sortedSet = redis.getStorage().getSortedSet(key.content());
		if (sortedSet == null) {
			return new CommandResponse(RInteger.ZERO);
		}

		final var removed = sortedSet.remove(value.content());

		return new CommandResponse(
			RInteger.of(removed)
		);
	}

}