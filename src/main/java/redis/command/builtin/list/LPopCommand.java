package redis.command.builtin.list;

import java.util.ArrayList;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RNil;
import redis.type.RString;

public record LPopCommand(
	RString key,
	int count
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var value = redis.getStorage().get(key);
		if (value == null || !(value instanceof RArray<?> array) || array.isEmpty()) {
			return new CommandResponse(RNil.BULK);
		}

		final var popped = new ArrayList<RString>(count);
		for (var index = 0; index < count && !array.isEmpty(); index++) {
			popped.add((RString) array.removeFirst());
		}

		if (popped.size() == 1) {
			return new CommandResponse(popped.getFirst());
		}

		return new CommandResponse(RArray.view(popped));
	}

	@Override
	public boolean isPropagatable() {
		return true;
	}

}