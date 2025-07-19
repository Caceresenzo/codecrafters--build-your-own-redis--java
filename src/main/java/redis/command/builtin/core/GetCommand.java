package redis.command.builtin.core;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RNil;
import redis.type.RString;
import redis.type.RValue;

public record GetCommand(
	RString key
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var value = redis.getStorage().get(key);

		if (value == null) {
			return new CommandResponse(RNil.BULK);
		}

		return new CommandResponse((RValue) value);
	}

}