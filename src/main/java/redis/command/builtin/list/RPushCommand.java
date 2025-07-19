package redis.command.builtin.list;

import java.util.function.Function;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RInteger;
import redis.type.RString;
import redis.type.RValue;

public record RPushCommand(
	RString key,
	RArray<RString> values
) implements Command, Function<RValue, RArray<RString>> {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final RArray<RString> array = redis.getStorage().compute(key, this);

		return new CommandResponse(RInteger.of(array.size()));
	}

	@Override
	public RArray<RString> apply(RValue oldValue) {
		if (oldValue == null) {
			return RArray.copy(values);
		}

		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isPropagatable() {
		return true;
	}

}