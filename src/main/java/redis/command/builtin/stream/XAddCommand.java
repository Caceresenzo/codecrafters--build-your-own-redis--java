package redis.command.builtin.stream;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RString;
import redis.type.RValue;
import redis.type.stream.Stream;
import redis.type.stream.identifier.Identifier;
import redis.type.stream.identifier.UniqueIdentifier;

public record XAddCommand(
	RString key,
	Identifier id,
	RArray<RValue> keyValues
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var newIdBox = new Object() {

			UniqueIdentifier x;

		};

		redis.getStorage().compute(
			key,
			(previous) -> {
				final var stream = previous instanceof Stream stream_
					? stream_
					: new Stream();

				newIdBox.x = stream.add(id, keyValues);
				return stream;
			}
		);

		return new CommandResponse(
			RString.bulk(newIdBox.x.toString())
		);
	}

}