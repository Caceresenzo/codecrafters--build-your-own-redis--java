package redis.store;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import redis.client.SocketClient;
import redis.type.RArray;
import redis.type.RString;
import redis.type.SortedSet;

public class Storage {

	private final Map<String, Cell<Object>> map = new ConcurrentHashMap<>();
	private final Map<String, SortedSet> sortedSets = new ConcurrentHashMap<>();
	private final Map<String, Set<SocketClient>> watchedKeys = new ConcurrentHashMap<>();

	public void clear() {
		map.clear();
	}

	public void set(RString key, Object value) {
		final var keyString = key.content();

		map.put(keyString, Cell.with(value));
		// System.out.println(map);

		notifyWatchedKeys(keyString);
	}

	public void set(RString key, Object value, Duration expiration) {
		final var keyString = key.content();

		map.put(keyString, Cell.expiry(value, expiration.toMillis()));
		// System.out.println(map);

		notifyWatchedKeys(keyString);
	}

	public void put(RString key, Cell<Object> cell) {
		put(key.content(), cell);
	}

	public void put(String key, Cell<Object> cell) {
		map.put(key, cell);

		notifyWatchedKeys(key);
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

				final var newCell = Cell.<Object>with(remappingFunction.apply(value));
				notifyWatchedKeys(key_);

				return newCell;
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
				.map(RString::bulk)
				.toList()
		);
	}

	public SortedSet getSortedSet(String key) {
		return sortedSets.get(key);
	}

	public void watch(String key, SocketClient socketClient) {
		watchedKeys.computeIfAbsent(key, (__) -> ConcurrentHashMap.newKeySet()).add(socketClient);
	}

	public void unwatch(String key, SocketClient socketClient) {
		final var clients = watchedKeys.get(key);
		if (clients == null) {
			return;
		}

		clients.remove(socketClient);

		// TODO Potential race condition here
		if (clients.isEmpty()) {
			watchedKeys.remove(key);
		}
	}

	private void notifyWatchedKeys(String key) {
		final var clients = watchedKeys.get(key);
		if (clients == null) {
			return;
		}

		clients.forEach((client) -> client.notifyWatchedKeyChanged(key));
	}

}