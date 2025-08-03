package redis.command.builtin.core;

import redis.Redis;
import redis.client.Client;
import redis.client.SocketClient;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.store.PubSub;
import redis.type.RArray;
import redis.type.RString;

public record PingCommand() implements Command {

	private static final RString PONG = RString.simple("PONG");
	private static final RArray<RString> PONG_SUBSCRIPTION = RArray.of(PubSub.MessageKeys.PONG, RString.empty(false));

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		if (client instanceof SocketClient socketClient && redis.getPubSub().isSubscribed(socketClient)) {
			return new CommandResponse(PONG_SUBSCRIPTION);
		}

		return new CommandResponse(PONG);
	}

	@Override
	public boolean isPubSub() {
		return true;
	}

}