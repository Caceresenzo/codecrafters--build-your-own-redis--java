package redis.command.builtin;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RString;

public record PingCommand() implements Command {

	private static final RString PONG = new RString("PONG", false);

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		return new CommandResponse(PONG);
	}

}