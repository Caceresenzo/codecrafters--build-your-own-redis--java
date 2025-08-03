package redis.command.builtin.pubsub;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RInteger;
import redis.type.RString;

public record PublishCommand(
	RString key,
	RString value
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var count = redis.getPubSub().publish(key, value);

		return new CommandResponse(RInteger.of(count));
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

	@Override
	public boolean isPubSub() {
		return true;
	}

}