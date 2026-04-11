package redis.command.builtin.transaction;

import redis.Redis;
import redis.client.Client;
import redis.client.SocketClient;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RError;
import redis.type.ROk;
import redis.type.RString;

public record WatchCommand(
	RArray<RString> keys
) implements Command {

	public static final RError IN_TRANSACTION = new RError("ERR WATCH inside MULTI is not allowed");

	@Override
	public CommandResponse execute(Redis redis, Client client) {

		final var socketClient = SocketClient.cast(client);

		if (socketClient.isInTransaction()) {
			throw IN_TRANSACTION.asException();
		}

		return new CommandResponse(ROk.OK);
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

}