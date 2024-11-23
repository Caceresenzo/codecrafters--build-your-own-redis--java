package redis.command.builtin.stream;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RString;
import redis.type.stream.Stream;
import redis.type.stream.StreamEntry;
import redis.type.stream.identifier.Identifier;

public record XRangeCommand(
	RString key,
	Identifier fromId,
	Identifier toId
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var stream = (Stream) redis.getStorage().get(key);
		final var entries = stream.range(fromId, toId);

		return new CommandResponse(
			StreamEntry.collectContent(entries)
		);
	}

}