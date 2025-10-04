package redis.command.builtin.geospatial;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.GeoCoordinate;
import redis.type.RNil;
import redis.type.RString;
import redis.util.NumberUtils;

public record GeoDistCommand(
	RString key,
	RString leftMember,
	RString rightMember
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var sortedSet = redis.getStorage().getSortedSet(key.content());
		if (sortedSet == null) {
			return new CommandResponse(RNil.BULK);
		}

		final var leftScore = sortedSet.getScore(leftMember.content());
		if (leftScore == null) {
			return new CommandResponse(RNil.BULK);
		}

		final var rightScore = sortedSet.getScore(rightMember.content());
		if (rightScore == null) {
			return new CommandResponse(RNil.BULK);
		}

		final var leftCoordinate = GeoCoordinate.decode(leftScore.longValue());
		final var rightCoordinate = GeoCoordinate.decode(rightScore.longValue());

		final var distance = leftCoordinate.distanceTo(rightCoordinate);

		return new CommandResponse(RString.bulk(NumberUtils.formatDoubleNoScientific(distance)));
	}

}