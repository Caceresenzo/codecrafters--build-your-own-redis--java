package redis.command.builtin.acl;

import redis.Redis;
import redis.client.Client;
import redis.client.SocketClient;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RString;

public record AclWhoamiCommand() implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var socketClient = SocketClient.cast(client);
		final var user = socketClient.getUser();

		return new CommandResponse(RString.bulk(user.getName()));
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

}