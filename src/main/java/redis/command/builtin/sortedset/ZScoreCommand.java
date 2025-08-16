package redis.command.builtin.sortedset;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RNil;
import redis.type.RString;

public record ZScoreCommand(
	RString key,
	RString value
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var sortedSet = redis.getStorage().getSortedSet(key.content());
		if (sortedSet == null) {
			return new CommandResponse(RNil.BULK);
		}

		final var score = sortedSet.getScore(value.content());
		if (score == null) {
			return new CommandResponse(RNil.BULK);
		}

		return new CommandResponse(
			RString.bulk(String.valueOf(score))
		);
	}

}