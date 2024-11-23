package redis.command.builtin;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RError;
import redis.type.RString;

public record ConfigCommand(
	RString action,
	RString key
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		if (RString.equalsIgnoreCase(action, "GET")) {
			final var property = redis.getConfiguration().option(key.content());
			if (property == null) {
				return new CommandResponse(RArray.empty());
			}

			return new CommandResponse(RArray.of(
				key,
				RString.simple(String.valueOf(property.argument(0).get()))
			));
		}

		throw new RError("ERR unknown subcommand '%s'. Try CONFIG HELP.".formatted(action)).asException();
	}

}