package redis.command.builtin.sortedset;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RInteger;
import redis.type.RString;

public record ZAddCommand(
	RString key,
	double score,
	RString value
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		return new CommandResponse(
			RInteger.ONE
		);
	}

}