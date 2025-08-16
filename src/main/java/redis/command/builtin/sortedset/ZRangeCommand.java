package redis.command.builtin.sortedset;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RString;

public record ZRangeCommand(
	RString key,
	int startIndex,
	int endIndex
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var sortedSet = redis.getStorage().getSortedSet(key.content());
		if (sortedSet == null) {
			return new CommandResponse(
				RArray.empty()
			);
		}

		return new CommandResponse(
			sortedSet.range(startIndex, endIndex)
		);
	}

}