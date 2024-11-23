package redis.command.builtin;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RError;
import redis.type.RInteger;
import redis.type.RString;

public record IncrCommand(
	RString key
) implements Command {

	private static final RError VALUE_OUT_OF_RANGE = new RError("ERR value is not an integer or out of range");

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var newValueBox = new Object() {

			int x;

		};

		redis.getStorage().compute(
			key,
			(previous) -> {
				var value = 0;

				if (previous instanceof RString string) {
					value = string.asInteger().orElseThrow(VALUE_OUT_OF_RANGE::asException);
				}

				final var newValue = newValueBox.x = value + 1;
				return RString.simple(String.valueOf(newValue));
			}
		);

		return new CommandResponse(RInteger.of(newValueBox.x));
	}

}