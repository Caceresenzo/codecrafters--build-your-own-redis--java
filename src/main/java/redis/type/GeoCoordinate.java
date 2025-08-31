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
		// Normalize to the range 0-2^26
		double normalizedLatitude = Math.pow(2, 26) * (latitude - MIN_LATITUDE) / LATITUDE_RANGE;
		double normalizedLongitude = Math.pow(2, 26) * (longitude - MIN_LONGITUDE) / LONGITUDE_RANGE;

		// Truncate to integers
		int latInt = (int) normalizedLatitude;
		int lonInt = (int) normalizedLongitude;

		return interleave(latInt, lonInt);
	}

	public RArray<RString> toArray() {
		return RArray.of(
			RString.bulk(String.format("%.15f", longitude)),
			RString.bulk(String.format("%.15f", latitude))
		);
	}

	private static long spreadInt32ToInt64(int v) {
		long result = v & 0xFFFFFFFFL;
		result = (result | (result << 16)) & 0x0000FFFF0000FFFFL;
		result = (result | (result << 8)) & 0x00FF00FF00FF00FFL;
		result = (result | (result << 4)) & 0x0F0F0F0F0F0F0F0FL;
		result = (result | (result << 2)) & 0x3333333333333333L;
		result = (result | (result << 1)) & 0x5555555555555555L;
		return result;
	}

	private static long interleave(int x, int y) {
		long xSpread = spreadInt32ToInt64(x);
		long ySpread = spreadInt32ToInt64(y);
		long yShifted = ySpread << 1;
		return xSpread | yShifted;
	}

	/* from https://github.com/codecrafters-io/redis-geocoding-algorithm/blob/0918076067b8458ae880c81e3d5678caccc31d5e/java/Decode.java#L44 */
	public static GeoCoordinate decode(long encoded) {
		// Align bits of both latitude and longitude to take even-numbered position
		long y = encoded >> 1;
		long x = encoded;

		// Compact bits back to 32-bit ints
		int gridLatitudeNumber = compactInt64ToInt32(x);
		int gridLongitudeNumber = compactInt64ToInt32(y);

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
		// Calculate the grid boundaries
		double gridLatitudeMin = MIN_LATITUDE + LATITUDE_RANGE * (gridLatitudeNumber / Math.pow(2, 26));
		double gridLatitudeMax = MIN_LATITUDE + LATITUDE_RANGE * ((gridLatitudeNumber + 1) / Math.pow(2, 26));
		double gridLongitudeMin = MIN_LONGITUDE + LONGITUDE_RANGE * (gridLongitudeNumber / Math.pow(2, 26));
		double gridLongitudeMax = MIN_LONGITUDE + LONGITUDE_RANGE * ((gridLongitudeNumber + 1) / Math.pow(2, 26));

		// Calculate the center point of the grid cell
		double latitude = (gridLatitudeMin + gridLatitudeMax) * 0.49999999;
		double longitude = (gridLongitudeMin + gridLongitudeMax) * 0.49999999;

		return new GeoCoordinate(longitude, latitude);
	}

}