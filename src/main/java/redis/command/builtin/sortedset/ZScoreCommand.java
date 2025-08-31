package redis.command.builtin.sortedset;

import java.text.DecimalFormat;

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

	private static final DecimalFormat NO_SCIENTIFIC = new DecimalFormat("0");

	static {
		NO_SCIENTIFIC.setMaximumFractionDigits(340);
	}

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

		final var stringValue = NO_SCIENTIFIC.format(score);
		return new CommandResponse(
			RString.bulk(stringValue)
		);
	}

}