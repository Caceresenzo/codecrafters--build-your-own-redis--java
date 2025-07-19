package redis.command.builtin.core;

import java.time.Duration;
import java.util.Optional;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.ROk;
import redis.type.RString;
import redis.type.RValue;

public record SetCommand(
	RString key,
	RValue value,
	Optional<Duration> expiration
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		if (expiration.isPresent()) {
			redis.getStorage().set(key, value, expiration.get());
		} else {
			redis.getStorage().set(key, value);
		}

		return new CommandResponse(ROk.OK);
	}

	@Override
	public boolean isPropagatable() {
		return true;
	}

}