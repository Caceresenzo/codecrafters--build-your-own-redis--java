package redis.store;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import lombok.Locked;
import redis.client.SocketClient;
import redis.type.RArray;
import redis.type.RString;
import redis.type.RValue;

public class PubSub {

	private static final RString TYPE_MESSAGE = RString.bulk("message");

	private final Map<String, Set<SocketClient>> subscribers = new HashMap<>();
	private final Map<SocketClient, Set<String>> subscribedKeys = new IdentityHashMap<>();

	@Locked
	public int subscribe(SocketClient client, String key) {
		final var clients = subscribers.computeIfAbsent(key, (__) -> Collections.newSetFromMap(new IdentityHashMap<>()));
		clients.add(client);

		final var keys = subscribedKeys.computeIfAbsent(client, (__) -> new HashSet<>());
		keys.add(key);

		return keys.size();
	}

	@Locked
	public void unsubscribeAll(SocketClient client) {
		final var keys = subscribedKeys.get(client);
		if (keys == null) {
			return;
		}

		for (final var key : keys) {
			final var clients = subscribers.get(key);
			clients.remove(client);

			if (clients.isEmpty()) {
				subscribers.remove(key);
			}
		}
	}

	public boolean isSubscribed(SocketClient socketClient) {
		final var keys = subscribedKeys.get(socketClient);

		return keys != null && !keys.isEmpty();
	}

	@Locked
	public int publish(RString key, RValue value) {
		final var clients = subscribers.get(key.content());
		if (clients == null) {
			return 0;
		}

		final var payload = RArray.of(
			TYPE_MESSAGE,
			RString.bulk(key),
			value
		);

		for (final var client : clients) {
			client.notifySubscription(payload);
		}

		return clients.size();
	}

}