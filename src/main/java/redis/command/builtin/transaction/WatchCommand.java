package redis.command.builtin.transaction;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.ROk;
import redis.type.RString;

public record WatchCommand(
	RArray<RString> keys
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		return new CommandResponse(ROk.OK);
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

}