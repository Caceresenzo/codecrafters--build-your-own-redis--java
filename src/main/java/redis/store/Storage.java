package redis.store;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import redis.type.RArray;
import redis.type.RString;
import redis.type.SortedSet;

public class Storage {

	private final Map<String, Cell<Object>> map = new ConcurrentHashMap<>();
	private final Map<String, SortedSet> sortedSets = new ConcurrentHashMap<>();

	public void clear() {
		map.clear();
	}

	public void set(RString key, Object value) {
		map.put(key.content(), Cell.with(value));
		System.out.println(map);
	}

	public void set(RString key, Object value, Duration milliseconds) {
		map.put(key.content(), Cell.expiry(value, milliseconds.toMillis()));
	}

	public void put(RString key, Cell<Object> cell) {
		put(key.content(), cell);
	}

	public void put(String key, Cell<Object> cell) {
		map.put(key, cell);
	}

	public boolean addToSet(String key, RString value, double score) {
		final var sortedSet = sortedSets.computeIfAbsent(key, (__) -> new SortedSet());

		return sortedSet.add(value.content(), score);
	}

	@SuppressWarnings("unchecked")
	public <T, R> R compute(RString key, Function<T, R> remappingFunction) {
		return ((Cell<R>) map.compute(
			key.content(),
			(key_, cell) -> {
				if (cell != null && cell.isExpired()) {
					cell = null;
				}

				final var value = cell != null ? (T) cell.value() : null;

				return Cell.with(remappingFunction.apply(value));
			}
		)).value();
	}

	public Object get(RString key) {
		return get(key.content());
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

	public RArray<RString> keys() {
		return RArray.view(
			map.keySet()
				.stream()
				.map(RString::simple)
				.toList()
		);
	}

	public SortedSet getSortedSet(String key) {
		return sortedSets.get(key);
	}

}