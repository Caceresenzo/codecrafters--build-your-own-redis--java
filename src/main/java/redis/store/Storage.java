package redis.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Storage {

	private final Map<String, Expiry<Object>> map = new ConcurrentHashMap<>();

	public void set(String key, Object value) {
		map.put(key, Expiry.never(value));
	}

	public void set(String key, Object value, long milliseconds) {
		map.put(key, Expiry.in(value, milliseconds));
	}

	public Object get(String key) {
		final var expiry = map.computeIfPresent(
			key,
			(key_, value) -> {
				if (value.isExpired()) {
					return null;
				}

				return value;
			}
		);

		if (expiry != null) {
			return expiry.value();
		}

		return null;
	}

}