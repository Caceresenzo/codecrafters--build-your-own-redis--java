package redis.command.builtin.replication;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.ROk;
import redis.type.RString;

public record ReplConfCommand(
	RString action,
	RString key
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		if (RString.equalsIgnoreCase(action, "GETACK")) {
			return new CommandResponse(
				RArray.of(
					RString.bulk("REPLCONF"),
					RString.bulk("ACK"),
					RString.bulk(String.valueOf(redis.getReplicationOffset()))
				),
				false
			);
		}

		return new CommandResponse(ROk.OK);
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

}