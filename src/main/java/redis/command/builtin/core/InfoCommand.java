package redis.command.builtin.core;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RString;

public record InfoCommand(
	RString action
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		if (RString.equalsIgnoreCase(action, "REPLICATION")) {
			return new CommandResponse(RString.bulk(getReplicationContent(redis)));
		}

		return new CommandResponse(RString.empty(true));
	}

	public String getReplicationContent(Redis redis) {
		final var mode = redis.getConfiguration().isSlave()
			? "slave"
			: "master";

		return """
			# Replication
			role:%s
			master_replid:%s
			master_repl_offset:%s
			""".formatted(
			mode,
			redis.getMasterReplicationId(),
			redis.getReplicationOffset()
		);
	}

}