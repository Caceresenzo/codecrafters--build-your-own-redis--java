package redis.command.builtin.transaction;

import redis.Redis;
import redis.client.Client;
import redis.client.SocketClient;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RError;

public record ExecCommand() implements Command {

	private static final RError WITHOUT_MULTI = new RError("ERR EXEC without MULTI");

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var socketClient = SocketClient.cast(client);

		if (!socketClient.isInTransaction()) {
			throw WITHOUT_MULTI.asException();
		}

		final var values = socketClient.discardTransaction()
			.stream()
			.map((command) -> redis.execute(client, command))
			.map(CommandResponse::value)
			.toList();

		return new CommandResponse(RArray.view(values));
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

}