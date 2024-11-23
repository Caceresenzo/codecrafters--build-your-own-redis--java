package redis.command.builtin.transaction;

import redis.Redis;
import redis.client.Client;
import redis.client.SocketClient;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RError;
import redis.type.ROk;

public record DiscardCommand() implements Command {

	private static final RError WITHOUT_MULTI = new RError("ERR DISCARD without MULTI");

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var socketClient = SocketClient.cast(client);

		if (!socketClient.isInTransaction()) {
			throw WITHOUT_MULTI.asException();
		}

		socketClient.discardTransaction();

		return new CommandResponse(ROk.OK);
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

}