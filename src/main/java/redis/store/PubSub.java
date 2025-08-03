package redis.store;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import lombok.Locked;
import lombok.experimental.UtilityClass;
import redis.client.SocketClient;
import redis.type.RArray;
import redis.type.RString;
import redis.type.RValue;

public class PubSub {

	private final Map<String, Set<SocketClient>> subscribers = new HashMap<>();
	private final Map<SocketClient, Set<String>> subscribedKeys = new IdentityHashMap<>();

	@Locked
	public int subscribe(SocketClient client, RString key) {
		final var clients = subscribers.computeIfAbsent(key.content(), (__) -> Collections.newSetFromMap(new IdentityHashMap<>()));
		clients.add(client);

		final var keys = subscribedKeys.computeIfAbsent(client, (__) -> new HashSet<>());
		keys.add(key.content());

		return keys.size();
	}

	@Locked
	public int unsubscribe(SocketClient client, RString key) {
		final var keys = subscribedKeys.get(client);
		if (keys == null) {
			return 0;
		}

		if (!keys.remove(key.content())) {
			return 0;
		}

		removeClient(client, key.content());

		return keys.size();
	}

	@Locked
	public void unsubscribeAll(SocketClient client) {
		final var keys = subscribedKeys.get(client);
		if (keys == null) {
			return;
		}

		for (final var key : keys) {
			removeClient(client, key);
		}
	}

	private void removeClient(SocketClient client, String key) {
		final var clients = subscribers.get(key);
		clients.remove(client);

		if (clients.isEmpty()) {
			subscribers.remove(key);
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
			MessageKeys.MESSAGE,
			RString.bulk(key),
			value
		);

		for (final var client : clients) {
			client.notifySubscription(payload);
		}

		return clients.size();
	}

	@UtilityClass
	public static class MessageKeys {

		public static final RString SUBSCRIBE = RString.bulk("subscribe");
		public static final RString UNSUBSCRIBE = RString.bulk("unsubscribe");
		public static final RString MESSAGE = RString.bulk("message");
		public static final RString PONG = RString.bulk("pong");

	}

}