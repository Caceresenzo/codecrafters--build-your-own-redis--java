package redis.command.builtin.acl;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RString;

public record AclGetUserCommand(
	RString username
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		return new CommandResponse(RArray.of(
			RString.bulk("flags"),
			RArray.of(
				RString.bulk("nopass")
			)
		));
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

}