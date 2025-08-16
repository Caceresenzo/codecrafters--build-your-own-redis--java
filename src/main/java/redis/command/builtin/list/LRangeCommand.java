package redis.command.builtin.list;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RString;
import redis.util.Range;

public record LRangeCommand(
	RString key,
	int startIndex,
	int endIndex
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var value = redis.getStorage().get(key);
		if (value == null || !(value instanceof RArray<?> array) || array.isEmpty()) {
			return new CommandResponse(RArray.empty());
		}

		final var size = array.size();
		final var range = new Range(size, startIndex, endIndex);

		if (range.isEmpty()) {
			return new CommandResponse(RArray.empty());
		}

		return new CommandResponse(range.subList(array));
	}

}