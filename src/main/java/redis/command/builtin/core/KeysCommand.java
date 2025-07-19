package redis.command.builtin.core;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RString;

public record KeysCommand(
	RString pattern
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var keys = redis.getStorage().keys();

		return new CommandResponse(keys);
	}

}