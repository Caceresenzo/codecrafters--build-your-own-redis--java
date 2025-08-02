package redis.command.builtin.pubsub;

import redis.Redis;
import redis.client.Client;
import redis.client.SocketClient;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RInteger;
import redis.type.RString;

public record SubscribeCommand(
	RString key
) implements Command {

	private static final RString SUBSCRIBE = RString.simple("subscribe");

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var count = redis.getPubSub().subscribe((SocketClient) client, key.content());

		return new CommandResponse(RArray.of(
			SUBSCRIBE,
			RString.bulk(key),
			RInteger.of(count)
		));
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

}