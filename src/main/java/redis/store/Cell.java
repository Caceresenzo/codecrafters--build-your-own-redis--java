package redis.store;

public record Cell<T>(
	T value,
	long until
) {

	public boolean isExpired() {
		if (until == -1) {
			return false;
		}

		return System.currentTimeMillis() > until;
	}
	
	public boolean isType(Class<?> type) {
		if (value == null) {
			return false;
		}
		
		return value.getClass().equals(type);
	}

	public static <T> Cell<T> with(T value) {
		return new Cell<>(value, -1);
	}

	public static <T> Cell<T> expiry(T value, long milliseconds) {
		final var until = System.currentTimeMillis() + milliseconds;

		return new Cell<>(value, until);
	}

}