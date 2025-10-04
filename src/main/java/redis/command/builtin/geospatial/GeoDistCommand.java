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

	public static final double EARTH_RADIUS_IN_METERS = 6372797.560856;

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

		var distance = distance(leftCoordinate, rightCoordinate);
		
		final var precision = 1e4;
		distance = Math.round(distance * precision) / precision;

		return new CommandResponse(RString.bulk(NumberUtils.formatDoubleNoScientific(distance)));
	}

	public static double distance(GeoCoordinate left, GeoCoordinate right) {
		double lon1r = Math.toRadians(left.longitude());
		double lon2r = Math.toRadians(right.longitude());
		double v = Math.sin((lon2r - lon1r) / 2);

		/* if v == 0 we can avoid doing expensive math when lons are practically the same */
		if (v == 0.0) {
			return EARTH_RADIUS_IN_METERS * Math.abs(Math.toRadians(left.latitude()) - Math.toRadians(right.latitude()));
		}

		double lat1r = Math.toRadians(left.latitude());
		double lat2r = Math.toRadians(right.latitude());

		double u = Math.sin((lat2r - lat1r) / 2);
		double a = u * u + Math.cos(lat1r) * Math.cos(lat2r) * v * v;
		return 2.0 * EARTH_RADIUS_IN_METERS * Math.asin(Math.sqrt(a));
	}

}