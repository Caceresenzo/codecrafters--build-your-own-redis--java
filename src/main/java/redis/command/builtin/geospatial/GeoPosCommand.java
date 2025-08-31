package redis.command.builtin.geospatial;

import java.util.List;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.GeoCoordinate;
import redis.type.RArray;
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
			final var scores = members.stream()
				.map((__) -> RNil.ARRAY)
				.toList();

			return new CommandResponse(RArray.view(scores));
		}

		final var scores = members.stream()
			.map(RString::content)
			.map(sortedSet::getScore)
			.map((score) -> {
				if (score == null) {
					return RNil.ARRAY;
				}

				final var coordinates = GeoCoordinate.decode(score.longValue());
				return coordinates.toArray();
			})
			.toList();

		return new CommandResponse(RArray.view(scores));
	}

}