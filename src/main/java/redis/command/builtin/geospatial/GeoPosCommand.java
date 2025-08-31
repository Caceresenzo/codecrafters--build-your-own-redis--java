package redis.command.builtin.geospatial;

import java.util.List;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RInteger;
import redis.type.RNil;
import redis.type.RString;

public record GeoPosCommand(
	RString key,
	List<RString> members
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var sortedSet = redis.getStorage().getSortedSet(key.content());
		if (sortedSet == null) {
			return new CommandResponse(RNil.BULK);
		}

		final var scores = members.stream()
			.map(RString::content)
			.map(sortedSet::getScore)
			.map((score) -> {
				if (score == null) {
					return RNil.ARRAY;
				}

				return RArray.of(RString.bulk("0"), RString.bulk("0"));
			})
			.toList();

		return new CommandResponse(RArray.view(scores));
	}

}