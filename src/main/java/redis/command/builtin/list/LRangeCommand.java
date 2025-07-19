package redis.command.builtin.list;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RString;

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
		final var start = scope(startIndex, size);
		final var end = scope(endIndex, size);

		if (start > end) {
			return new CommandResponse(RArray.empty());
		}

		return new CommandResponse(RArray.view(array.subList(start, end + 1)));
	}

	private Integer scope(int index, int size) {
		if (index < 0) {
			index += size;
		}

		return Math.clamp(index, 0, size - 1);
	}

}