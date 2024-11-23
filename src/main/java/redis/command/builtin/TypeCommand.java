package redis.command.builtin;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RString;
import redis.type.stream.Stream;

public record TypeCommand(
	RString key
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var value = redis.getStorage().get(key);

		return new CommandResponse(RString.simple(switch (value) {
			case null -> "none";
			case RArray<?> __ -> "list";
			case RString __ -> "string";
			case Stream __ -> "stream";
			default -> throw new IllegalStateException("unknown type: %s".formatted(value.getClass()));
		}));
	}

}