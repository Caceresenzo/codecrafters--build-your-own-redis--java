package redis.type;

public record GeoCoordinate(
	double longitude,
	double latitude
) {

	private static final double MIN_LATITUDE = -85.05112878;
	private static final double MAX_LATITUDE = 85.05112878;
	private static final double MIN_LONGITUDE = -180.0;
	private static final double MAX_LONGITUDE = 180.0;

	private static final double LATITUDE_RANGE = MAX_LATITUDE - MIN_LATITUDE;
	private static final double LONGITUDE_RANGE = MAX_LONGITUDE - MIN_LONGITUDE;

	public GeoCoordinate {
		if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE || latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
			throw new RError("ERR invalid longitude,latitude pair %.6f,%.6f".formatted(longitude, latitude)).asException();
		}
	}

	/* from https://github.com/codecrafters-io/redis-geocoding-algorithm/blob/0918076067b8458ae880c81e3d5678caccc31d5e/java/Encode.java#L27 */
	public long encode() {
		final var normalizedLatitude = Math.pow(2, 26) * (latitude - MIN_LATITUDE) / LATITUDE_RANGE;
		final var normalizedLongitude = Math.pow(2, 26) * (longitude - MIN_LONGITUDE) / LONGITUDE_RANGE;

		final var latInt = (int) normalizedLatitude;
		final var lonInt = (int) normalizedLongitude;

		return interleave(latInt, lonInt);
	}

	/**
	 * @return An array containing the latitude and longitude (yes order is reversed) as strings formatted to six decimal places.
	 */
	public RArray<RString> toArray() {
		return RArray.of(
			RString.bulk(String.format("%.6f", latitude)),
			RString.bulk(String.format("%.6f", longitude))
		);
	}

	private static long spreadInt32ToInt64(int v) {
		var result = v & 0xFFFFFFFFL;
		result = (result | (result << 16)) & 0x0000FFFF0000FFFFL;
		result = (result | (result << 8)) & 0x00FF00FF00FF00FFL;
		result = (result | (result << 4)) & 0x0F0F0F0F0F0F0F0FL;
		result = (result | (result << 2)) & 0x3333333333333333L;
		result = (result | (result << 1)) & 0x5555555555555555L;
		return result;
	}

	private static long interleave(int x, int y) {
		final var xSpread = spreadInt32ToInt64(x);
		final var ySpread = spreadInt32ToInt64(y);
		final var yShifted = ySpread << 1;
		return xSpread | yShifted;
	}

	/* from https://github.com/codecrafters-io/redis-geocoding-algorithm/blob/0918076067b8458ae880c81e3d5678caccc31d5e/java/Decode.java#L44 */
	public static GeoCoordinate decode(long encoded) {
		final var y = encoded >> 1;
		final var x = encoded;

		final var gridLatitudeNumber = compactInt64ToInt32(x);
		final var gridLongitudeNumber = compactInt64ToInt32(y);

		return convertGridNumbersToCoordinates(gridLatitudeNumber, gridLongitudeNumber);
	}

	private static int compactInt64ToInt32(long v) {
		v = v & 0x5555555555555555L;
		v = (v | (v >> 1)) & 0x3333333333333333L;
		v = (v | (v >> 2)) & 0x0F0F0F0F0F0F0F0FL;
		v = (v | (v >> 4)) & 0x00FF00FF00FF00FFL;
		v = (v | (v >> 8)) & 0x0000FFFF0000FFFFL;
		v = (v | (v >> 16)) & 0x00000000FFFFFFFFL;

		return (int) v;
	}

	private static GeoCoordinate convertGridNumbersToCoordinates(int gridLatitudeNumber, int gridLongitudeNumber) {
		final var gridLatitudeMin = MIN_LATITUDE + LATITUDE_RANGE * (gridLatitudeNumber / Math.pow(2, 26));
		final var gridLatitudeMax = MIN_LATITUDE + LATITUDE_RANGE * ((gridLatitudeNumber + 1) / Math.pow(2, 26));
		final var gridLongitudeMin = MIN_LONGITUDE + LONGITUDE_RANGE * (gridLongitudeNumber / Math.pow(2, 26));
		final var gridLongitudeMax = MIN_LONGITUDE + LONGITUDE_RANGE * ((gridLongitudeNumber + 1) / Math.pow(2, 26));

		final var latitude = (gridLatitudeMin + gridLatitudeMax) / 2;
		final var longitude = (gridLongitudeMin + gridLongitudeMax) / 2;

		return new GeoCoordinate(latitude, longitude);
	}

}