package redis.command.builtin.core;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RString;

public record EchoCommand(
	RString message
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		return new CommandResponse(message);
	}

}