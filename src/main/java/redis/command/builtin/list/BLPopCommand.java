package redis.command.builtin.list;

import java.time.Duration;
import java.util.Optional;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RError;
import redis.type.RNil;
import redis.type.RString;
import redis.type.RValue;

public record BLPopCommand(
	RString key,
	Optional<Duration> timeout
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		var value = redis.getStorage().get(key);

		if (value == null || !(value instanceof RArray<?> array) || array.isEmpty()) {
			value = redis.awaitKey(key, timeout);

			if (value == null) {
				return new CommandResponse(RNil.ARRAY);
			}

			if (!(value instanceof RArray<?>)) {
				throw RError.wrongtypeWrongKindOfValue().asException();
			}
		}

		@SuppressWarnings("unchecked")
		final var first = ((RArray<RValue>) value).removeFirst();

		return new CommandResponse(RArray.of(key, first));
	}

}