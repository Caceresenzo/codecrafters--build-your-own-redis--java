package redis.store;

public record Expiry<T>(
	T value,
	long until
) {

	public boolean isExpired() {
		if (until == -1) {
			return false;
		}
		
		return System.currentTimeMillis() > until;
	}

	public static <T> Expiry<T> never(T value) {
		return new Expiry<T>(value, -1);
	}

	public static <T> Expiry<T> in(T value, long milliseconds) {
		final var until = System.currentTimeMillis() + milliseconds;

		return new Expiry<T>(value, until);
	}

}