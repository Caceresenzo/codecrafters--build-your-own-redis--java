package redis.command.builtin.list;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RNil;
import redis.type.RString;

public record LPopCommand(
	RString key
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var value = redis.getStorage().get(key);
		if (value == null || !(value instanceof RArray<?> array) || array.isEmpty()) {
			return new CommandResponse(RNil.BULK);
		}

		return new CommandResponse(array.removeFirst());
	}

	@Override
	public boolean isPropagatable() {
		return true;
	}

}