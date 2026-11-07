package redis.command.builtin.bitset;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RInteger;
import redis.type.RString;

public record SetBitCommand(
	RString key,
	int offset,
	boolean value
) implements Command {

	private static final byte[] EMPTY = new byte[0];

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		AtomicBoolean previousValue = new AtomicBoolean(false);
		redis.getStorage().compute(key, (currentValue) -> {
			var bitSet = EMPTY;
			if (currentValue instanceof RString string) {
				bitSet = string.content().getBytes(StandardCharsets.ISO_8859_1);
			}

			final var byteOffset = offset / 8;
			final var bitOffset = offset % 8;

			if (byteOffset >= bitSet.length) {
				final var newArray = new byte[byteOffset + 1];
				System.arraycopy(bitSet, 0, newArray, 0, bitSet.length);
				bitSet = newArray;
			}

			final var oldBitValue = (bitSet[byteOffset] >> (7 - bitOffset)) & 1;
			previousValue.set(oldBitValue == 1);

			if (value) {
				bitSet[byteOffset] |= (1 << (7 - bitOffset));
			} else {
				bitSet[byteOffset] &= ~(1 << (7 - bitOffset));
			}

			return RString.detect(new String(bitSet, StandardCharsets.ISO_8859_1));
		});

		return new CommandResponse(RInteger.of(previousValue.get()));
	}

}