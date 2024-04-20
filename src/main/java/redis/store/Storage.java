package redis.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Storage {

	private final Map<String, Cell<Object>> map = new ConcurrentHashMap<>();

	public void set(String key, Object value) {
		map.put(key, Cell.with(value));
	}

	public void set(String key, Object value, long milliseconds) {
		map.put(key, Cell.expiry(value, milliseconds));
	}

	public void put(String key, Cell<Object> cell) {
		map.put(key, cell);
	}

	public Object get(String key) {
		final var cell = map.computeIfPresent(
			key,
			(key_, value) -> {
				if (value.isExpired()) {
					return null;
				}

				return value;
			}
		);

		if (cell != null) {
			return cell.value();
		}

		return null;
	}

	public List<String> keys() {
		return new ArrayList<>(map.keySet());
	}

}