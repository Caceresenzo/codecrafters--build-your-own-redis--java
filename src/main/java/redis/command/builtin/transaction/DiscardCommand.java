package redis.command.builtin.transaction;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RError;
import redis.type.ROk;

public record DiscardCommand() implements Command {

	private static final RError WITHOUT_MULTI = new RError("ERR DISCARD without MULTI");

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		if (!client.isInTransaction()) {
			throw WITHOUT_MULTI.asException();
		}

		client.discardTransaction();

		return new CommandResponse(ROk.OK);
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

}