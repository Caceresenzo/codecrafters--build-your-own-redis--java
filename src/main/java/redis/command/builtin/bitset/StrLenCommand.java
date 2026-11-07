package redis.command.builtin.bitset;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RInteger;
import redis.type.RString;

public record StrLenCommand(
	RString key
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var value = redis.getStorage().get(key);

		if (value instanceof RString string) {
			return new CommandResponse(RInteger.of(string.length()));
		}

		return new CommandResponse(RInteger.ZERO);
	}

}