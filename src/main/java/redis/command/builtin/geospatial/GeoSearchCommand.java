package redis.command.builtin.geospatial;

import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.GeoCoordinate;
import redis.type.RArray;
import redis.type.RNil;
import redis.type.RString;

public record GeoSearchCommand(
	RString key,
	GeoCoordinate center,
	double radius
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var sortedSet = redis.getStorage().getSortedSet(key.content());
		if (sortedSet == null) {
			return new CommandResponse(RNil.BULK);
		}

		final var closeEntries = StreamSupport.stream(Spliterators.spliteratorUnknownSize(sortedSet.iterator(), Spliterator.ORDERED), false)
			.parallel()
			.filter((entry) -> GeoCoordinate.decode(entry.getValue().longValue()).distanceTo(center) <= radius)
			.map(Map.Entry::getKey)
			.map(RString::bulk)
			.toList();

		return new CommandResponse(RArray.view(closeEntries));
	}

}