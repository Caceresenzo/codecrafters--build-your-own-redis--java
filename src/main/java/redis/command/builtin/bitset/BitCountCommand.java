package redis.command.builtin.bitset;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RInteger;
import redis.type.RString;

public record BitCountCommand(
	RString key
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var value = redis.getStorage().get(key);

		if (!(value instanceof RString string)) {
			return new CommandResponse(RInteger.ZERO);
		}

		var bitCount = 0;

		final var length = string.length();
		for (var index = 0; index < length; index++) {
			final var byteValue = (byte) string.content().charAt(index);
			bitCount += Integer.bitCount(byteValue & 0xFF);
		}

		return new CommandResponse(RInteger.of(bitCount));
	}

}