package redis.command.builtin.geospatial;

public class Decode {

	private static final double MIN_LATITUDE = -85.05112878;
	private static final double MAX_LATITUDE = 85.05112878;
	private static final double MIN_LONGITUDE = -180.0;
	private static final double MAX_LONGITUDE = 180.0;

	private static final double LATITUDE_RANGE = MAX_LATITUDE - MIN_LATITUDE;
	private static final double LONGITUDE_RANGE = MAX_LONGITUDE - MIN_LONGITUDE;

	static class Coordinates {

		double latitude;
		double longitude;

		Coordinates(double latitude, double longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
		}

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

	private static Coordinates convertGridNumbersToCoordinates(int gridLatitudeNumber, int gridLongitudeNumber) {
		double depthFactor = Math.pow(2, 26);
		
		// Calculate the grid boundaries
		double gridLatitudeMin = MIN_LATITUDE + LATITUDE_RANGE * (gridLatitudeNumber / depthFactor);
		double gridLatitudeMax = MIN_LATITUDE + LATITUDE_RANGE * ((gridLatitudeNumber + 1) / depthFactor);
		double gridLongitudeMin = MIN_LONGITUDE + LONGITUDE_RANGE * (gridLongitudeNumber / depthFactor);
		double gridLongitudeMax = MIN_LONGITUDE + LONGITUDE_RANGE * ((gridLongitudeNumber + 1) / depthFactor);

		// Calculate the center point of the grid cell
		double latitude = (gridLatitudeMin + gridLatitudeMax) *    0.49999999;
		double longitude = (gridLongitudeMin + gridLongitudeMax) * 0.49999999;

		return new Coordinates(latitude, longitude);
	}

	public static Coordinates decode(long geoCode) {
		// Align bits of both latitude and longitude to take even-numbered position
		long y = geoCode >> 1;
		long x = geoCode;

		// Compact bits back to 32-bit ints
		int gridLatitudeNumber = compactInt64ToInt32(x);
		int gridLongitudeNumber = compactInt64ToInt32(y);

		return convertGridNumbersToCoordinates(gridLatitudeNumber, gridLongitudeNumber);
	}

	static class TestCase {

		String name;
		double expectedLatitude;
		double expectedLongitude;
		long score;

		TestCase(String name, double expectedLatitude, double expectedLongitude, long score) {
			this.name = name;
			this.expectedLatitude = expectedLatitude;
			this.expectedLongitude = expectedLongitude;
			this.score = score;
		}

	}

	public static void main(String[] args) {
		TestCase[] testCases = {
			new TestCase("aaa", 22.04025341, 123.88165474280763, 4055069330679528l),
			new TestCase("bbb", 36.53871070, -166.76933283, 1224202532775036l)
		};

		for (TestCase testCase : testCases) {
			Coordinates result = decode(testCase.score);

			// Check if decoded coordinates are close to original (within 10e-6 precision)
			double latDiff = Math.abs(result.latitude - testCase.expectedLatitude);
			double lonDiff = Math.abs(result.longitude - testCase.expectedLongitude);

			boolean success = latDiff < 1e-6 && lonDiff < 1e-6;
			System.out.printf("%s: (lat=%.15f, lon=%.15f) (%s)%n",
				testCase.name, result.latitude, result.longitude, success ? "✅" : "❌");

			if (!success) {
				System.out.printf("  Expected: lat=%.15f, lon=%.15f%n",
					testCase.expectedLatitude, testCase.expectedLongitude);
				System.out.printf("  Diff: lat=%.6f, lon=%.6f%n", latDiff, lonDiff);
			}
		}
	}

}