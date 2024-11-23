package redis.command.builtin.transaction;

import redis.Redis;
import redis.client.Client;
import redis.client.SocketClient;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RError;
import redis.type.ROk;

public record MultiCommand() implements Command {

	private static final RError ALREADY_IN_TRANSACTION = new RError("ERR already in transaction");

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var socketClient = SocketClient.cast(client);

		if (socketClient.isInTransaction()) {
			throw ALREADY_IN_TRANSACTION.asException();
		}

		socketClient.beginTransaction();

		return new CommandResponse(ROk.OK);
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

}