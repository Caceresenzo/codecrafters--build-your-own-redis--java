package redis.command.builtin.list;

import java.util.function.Function;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RError;
import redis.type.RInteger;
import redis.type.RString;
import redis.type.RValue;

public record LPushCommand(
	RString key,
	RArray<RString> values
) implements Command, Function<RValue, RArray<RString>> {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final RArray<RString> array = redis.getStorage().compute(key, this);
		final var size = array.size();

		redis.notifyKey(key);

		return new CommandResponse(RInteger.of(size));
	}

	@SuppressWarnings("unchecked")
	@Override
	public RArray<RString> apply(RValue oldValue) {
		if (oldValue == null) {
			return RArray.copy(values.reversed());
		}

		if (oldValue instanceof RArray array) {
			array.addAll(0, values.reversed());
			return array;
		}

		throw RError.wrongtypeWrongKindOfValue().asException();
	}

	@Override
	public boolean isPropagatable() {
		return true;
	}

}