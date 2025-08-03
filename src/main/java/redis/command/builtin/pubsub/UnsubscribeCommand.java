package redis.command.builtin.pubsub;

import redis.Redis;
import redis.client.Client;
import redis.client.SocketClient;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.store.PubSub;
import redis.type.RArray;
import redis.type.RInteger;
import redis.type.RString;

public record UnsubscribeCommand(
	RString key
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var count = redis.getPubSub().unsubscribe((SocketClient) client, key);

		return new CommandResponse(RArray.of(
			PubSub.MessageKeys.UNSUBSCRIBE,
			RString.bulk(key),
			RInteger.of(count)
		));
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