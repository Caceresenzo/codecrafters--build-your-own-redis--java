package redis.command.builtin.bitset;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RInteger;
import redis.type.RString;

public record GetBitCommand(
	RString key,
	int offset
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var value = redis.getStorage().get(key);

		if (!(value instanceof RString string)) {
			return new CommandResponse(RInteger.ZERO);
		}

		final var byteOffset = offset / 8;
		if (byteOffset >= string.content().length()) {
			return new CommandResponse(RInteger.ZERO);
		}

		final var byteValue = (byte) string.content().charAt(byteOffset);

		final var bitOffset = offset % 8;
		final var bitValue = (byteValue >> (7 - bitOffset)) & 1;

		return new CommandResponse(RInteger.of(bitValue));
	}

}