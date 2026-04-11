package redis.command.builtin.acl;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RString;

public record AclWhoamiCommand() implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		return new CommandResponse(RString.bulk("default"));
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

}