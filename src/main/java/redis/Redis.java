package redis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import redis.client.Client;
import redis.client.Payload;
import redis.configuration.Configuration;
import redis.store.Cell;
import redis.store.Storage;
import redis.type.BulkBlob;
import redis.type.BulkString;
import redis.type.Error;
import redis.type.ErrorException;
import redis.type.Ok;
import redis.type.stream.Stream;
import redis.type.stream.identifier.Identifier;
import redis.type.stream.identifier.UniqueIdentifier;

@RequiredArgsConstructor
public class Redis {

	private final @Getter Storage storage;
	private final @Getter Configuration configuration;
	private final List<Client> replicas = Collections.synchronizedList(new ArrayList<>());
	private AtomicLong replicationOffset = new AtomicLong();

	@SuppressWarnings("unchecked")
	public List<Payload> evaluate(Client client, Object value, long read) {
		try {
			if (value instanceof List list) {
				try {
					return evaluate(client, list, read);
				} catch (ErrorException exception) {
					return List.of(new Payload(exception.getError()));
				}
			}

			return List.of(new Payload(new Error("ERR command be sent in an array")));
		} finally {
			final var offset = replicationOffset.addAndGet(read);
			System.out.println("offset: %s".formatted(offset));
		}
	}

	private List<Payload> evaluate(Client client, List<Object> arguments, long read) {
		if (arguments.isEmpty()) {
			return List.of(new Payload(new Error("ERR command array is empty")));
		}

		final var command = String.valueOf(arguments.getFirst());

		if ("COMMAND".equalsIgnoreCase(command)) {
			return Collections.singletonList(new Payload(evaluateCommand(arguments)));
		}

		if ("PING".equalsIgnoreCase(command)) {
			final var response = new Payload(evaluatePing(arguments));
			progagate(arguments);

			return Collections.singletonList(response);
		}

		if ("ECHO".equalsIgnoreCase(command)) {
			return Collections.singletonList(new Payload(evaluateEcho(arguments)));
		}

		if ("SET".equalsIgnoreCase(command)) {
			final var response = new Payload(evaluateSet(arguments));
			progagate(arguments);

			return Collections.singletonList(response);
		}

		if ("GET".equalsIgnoreCase(command)) {
			return Collections.singletonList(new Payload(evaluateGet(arguments)));
		}

		if ("XADD".equalsIgnoreCase(command)) {
			return Collections.singletonList(new Payload(evaluateXAdd(arguments)));
		}

		if ("XRANGE".equalsIgnoreCase(command)) {
			return Collections.singletonList(new Payload(evaluateXRange(arguments)));
		}

		if ("XREAD".equalsIgnoreCase(command)) {
			return Collections.singletonList(new Payload(evaluateXRead(arguments)));
		}

		if ("KEYS".equalsIgnoreCase(command)) {
			return Collections.singletonList(new Payload(evaluateKeys(arguments)));
		}

		if ("TYPE".equalsIgnoreCase(command)) {
			return Collections.singletonList(new Payload(evaluateType(arguments)));
		}

		if ("CONFIG".equalsIgnoreCase(command)) {
			return Collections.singletonList(new Payload(evaluateConfig(arguments)));
		}

		if ("INFO".equalsIgnoreCase(command)) {
			return Collections.singletonList(new Payload(evaluateInfo(arguments)));
		}

		if ("REPLCONF".equalsIgnoreCase(command)) {
			return Collections.singletonList(evaluateReplicaConfig(arguments));
		}

		if ("PSYNC".equalsIgnoreCase(command)) {
			return evaluatePSync(client, arguments);
		}

		return List.of(new Payload(new Error("ERR unknown '%s' command".formatted(command))));
	}

	private Object evaluateCommand(List<?> list) {
		return Collections.emptyList();
	}

	private Object evaluatePing(List<?> list) {
		return "PONG";
	}

	private Object evaluateEcho(List<?> list) {
		if (list.size() != 2) {
			return new Error("ERR wrong number of arguments for 'echo' command");
		}

		return new BulkString(String.valueOf(list.get(1)));
	}

	private Object evaluateSet(List<?> list) {
		if (list.size() != 3 && list.size() != 5) {
			return new Error("ERR wrong number of arguments for 'set' command");
		}

		final var key = String.valueOf(list.get(1));
		final var value = list.get(2);

		if (list.size() == 5) {
			final var px = String.valueOf(list.get(3));
			if (!"px".equalsIgnoreCase(px)) {
				return Error.syntax();
			}

			final var millisecondes = Long.parseLong(String.valueOf(list.get(4)));
			storage.set(key, value, millisecondes);
		} else {
			storage.set(key, value);
		}

		return Ok.INSTANCE;
	}

	private Object evaluateGet(List<?> list) {
		if (list.size() != 2) {
			return new Error("ERR wrong number of arguments for 'get' command");
		}

		final var key = String.valueOf(list.get(1));
		final var value = storage.get(key);

		if (value == null) {
			return new BulkString(null);
		}

		return value;
	}

	private Object evaluateXAdd(List<Object> list) {
		final var key = String.valueOf(list.get(1));
		final var id = Identifier.parse(String.valueOf(list.get(2)));

		final var keyValues = new ArrayList<>(list.subList(3, list.size()));

		final var newIdReference = new AtomicReference<UniqueIdentifier>();
		storage.append(
			key,
			Stream.class,
			() -> Cell.with(new Stream()),
			(stream) -> {
				final var newId = stream.add(id, keyValues);
				newIdReference.set(newId);
			}
		);

		return new BulkString(newIdReference.get().toString());
	}

	private Object evaluateXRange(List<Object> list) {
		final var key = String.valueOf(list.get(1));
		final var fromId = Identifier.parse(String.valueOf(list.get(2)));
		final var toId = Identifier.parse(String.valueOf(list.get(3)));

		final var stream = (Stream) storage.get(key);
		final var entries = stream.range(fromId, toId);

		return entries.stream()
			.map((entry) -> List.of(
				new BulkString(entry.identifier().toString()),
				entry.content()
			))
			.toList();
	}

	private Object evaluateXRead(List<Object> list) {
		record Query(
			String key,
			Identifier identifier
		) {}

		final var queries = new ArrayList<Query>();
		Duration timeout = null;

		final var size = list.size();
		for (var index = 1; index < size; ++index) {
			var element = String.valueOf(list.get(index));

			if ("block".equalsIgnoreCase(element)) {
				++index;

				element = String.valueOf(list.get(index));
				timeout = Duration.ofMillis(Long.parseLong(element));

				continue;
			}

			if ("streams".equalsIgnoreCase(element)) {
				++index;

				final var remaining = size - index;
				final var offset = remaining / 2;

				for (var jndex = 0; jndex < offset; ++jndex) {
					final var key = String.valueOf(list.get(index + jndex));

					element = String.valueOf(list.get(index + offset + jndex));
					final var identifier = timeout != null && "$".equals(element)
						? null
						: Identifier.parse(String.valueOf(list.get(index + offset + jndex)));

					queries.add(new Query(key, identifier));
				}

				break;
			}
		}

		if (timeout != null) {
			final var query = queries.getFirst();

			final var key = query.key();
			final var stream = (Stream) storage.get(key);
			final var entries = stream.read(query.identifier(), timeout);

			if (entries == null) {
				return new BulkString(null);
			}

			return List.of(List.of(
				new BulkString(key),
				entries.stream()
					.map((entry) -> List.of(
						new BulkString(entry.identifier().toString()),
						entry.content()
					))
					.toList()
			));
		}

		return queries.stream()
			.map((query) -> {
				final var key = query.key();
				final var stream = (Stream) storage.get(key);
				final var entries = stream.read(query.identifier());

				return List.of(
					key,
					entries.stream()
						.map((entry) -> List.of(
							new BulkString(entry.identifier().toString()),
							entry.content()
						))
						.toList()
				);
			})
			.toList();
	}

	private Object evaluateKeys(List<?> list) {
		if (list.size() != 2) {
			return new Error("ERR wrong number of arguments for 'keys' command");
		}

		// final var pattern = String.valueOf(list.get(1));

		return storage.keys();
	}

	private Object evaluateType(List<?> list) {
		if (list.size() != 2) {
			return new Error("ERR wrong number of arguments for 'type' command");
		}

		final var key = String.valueOf(list.get(1));
		final var value = storage.get(key);

		if (value == null) {
			return "none";
		}

		if (value instanceof List) {
			return "list";
		}

		if (value instanceof String) {
			return "string";
		}

		if (value instanceof Stream) {
			return "stream";
		}

		throw new IllegalStateException("unknown type: %s".formatted(value.getClass()));
	}

	private Object evaluateConfig(List<?> list) {
		final var action = String.valueOf(list.get(1));

		if ("GET".equalsIgnoreCase(action)) {
			final var key = String.valueOf(list.get(2));

			final var property = configuration.option(key);
			if (property == null) {
				return Collections.emptyList();
			}

			return Arrays.asList(
				key,
				property.argument(0).get()
			);
		}

		return new Error("ERR unknown subcommand '%s'. Try CONFIG HELP.".formatted(action));
	}

	private Object evaluateInfo(List<?> list) {
		final var action = String.valueOf(list.get(1));

		if ("REPLICATION".equalsIgnoreCase(action)) {
			final var mode = configuration.isSlave()
				? "slave"
				: "master";

			return new BulkString("""
				# Replication
				role:%s
				master_replid:%s
				master_repl_offset:%s
				""".formatted(
				mode,
				getMasterReplicationId(),
				replicationOffset
			)
			);
		}

		return new BulkString("");
	}

	private Payload evaluateReplicaConfig(List<?> list) {
		final var action = String.valueOf(list.get(1));

		if ("GETACK".equalsIgnoreCase(action)) {
			return new Payload(
				List.of(
					new BulkString("REPLCONF"),
					new BulkString("ACK"),
					new BulkString(String.valueOf(replicationOffset))
				),
				false
			);
		}

		return new Payload(Ok.INSTANCE);
	}

	private List<Payload> evaluatePSync(Client client, List<?> list) {
		client.setReplicate(true);

		replicas.add(client);
		if (!client.onDisconnect(replicas::remove)) {
			replicas.remove(client);
			return List.of(new Payload(new Error("ERR could not enable replica")));
		}

		client.command(new Payload("FULLRESYNC %s 0".formatted(getMasterReplicationId())));
		client.command(new Payload(new BulkBlob(Base64.getDecoder().decode("UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog=="))));

		return null;
	}

	public String getMasterReplicationId() {
		return configuration.masterReplicationId().argument(0, String.class).get();
	}

	public void progagate(List<Object> command) {
		final var payload = new Payload(
			command.stream()
				.map(String::valueOf)
				.map(BulkString::new)
				.toList()
		);

		replicas.forEach((client) -> {
			client.command(payload);
		});
	}

}