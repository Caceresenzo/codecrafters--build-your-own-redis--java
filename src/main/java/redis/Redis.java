package redis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import redis.client.Client;
import redis.client.Payload;
import redis.configuration.Configuration;
import redis.store.Cell;
import redis.store.Storage;
import redis.type.ErrorException;
import redis.type.RArray;
import redis.type.RBlob;
import redis.type.RError;
import redis.type.RInteger;
import redis.type.RNil;
import redis.type.ROk;
import redis.type.RString;
import redis.type.RValue;
import redis.type.stream.Stream;
import redis.type.stream.StreamEntry;
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
			if (value instanceof RArray array) {
				try {
					return evaluate(client, array, read);
				} catch (ErrorException exception) {
					return List.of(new Payload(exception.getError()));
				}
			}

			return List.of(new Payload(new RError("ERR command be sent in an array")));
		} finally {
			final var offset = replicationOffset.addAndGet(read);
			log("offset: %s".formatted(offset));
		}
	}

	private List<Payload> evaluate(Client client, RArray<RValue> arguments, long read) {
		if (arguments.isEmpty()) {
			return List.of(new Payload(new RError("ERR command array is empty")));
		}

		final var command = ((RString) arguments.getFirst()).content();

		if ("PING".equalsIgnoreCase(command)) {
			return Collections.singletonList(new Payload(evaluatePing(arguments)));
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

		if ("WAIT".equalsIgnoreCase(command)) {
			return Collections.singletonList(new Payload(evaluateWait(arguments)));
		}

		if ("INCR".equalsIgnoreCase(command)) {
			return Collections.singletonList(new Payload(evaluateIncrement(arguments)));
		}

		return List.of(new Payload(new RError("ERR unknown '%s' command".formatted(command))));
	}

	private RValue evaluatePing(RArray<RValue> list) {
		return RString.pong();
	}

	private RValue evaluateEcho(RArray<RValue> list) {
		if (list.size() != 2) {
			return new RError("ERR wrong number of arguments for 'echo' command");
		}

		return RString.bulk((RString) list.get(1));
	}

	private RValue evaluateSet(RArray<RValue> list) {
		if (list.size() != 3 && list.size() != 5) {
			return new RError("ERR wrong number of arguments for 'set' command");
		}

		final var key = (RString) list.get(1);
		final var value = list.get(2);

		if (list.size() == 5) {
			final var px = (RString) list.get(3);
			if (!RString.equalsIgnoreCase(px, "px")) {
				return RError.syntax();
			}

			final var millisecondes = ((RString) list.get(4)).asLong();
			storage.set(key, value, millisecondes);
		} else {
			storage.set(key, value);
		}

		return ROk.INSTANCE;
	}

	private RValue evaluateGet(RArray<RValue> list) {
		if (list.size() != 2) {
			return new RError("ERR wrong number of arguments for 'get' command");
		}

		final var key = (RString) list.get(1);
		final var value = storage.get(key);

		if (value == null) {
			return RNil.BULK;
		}

		return (RValue) value;
	}

	private RValue evaluateXAdd(RArray<RValue> list) {
		final var key = (RString) list.get(1);
		final var id = Identifier.parse((RString) list.get(2));

		final var keyValues = RArray.view(list.subList(3, list.size()));

		final var newIdReference = new AtomicReference<UniqueIdentifier>();
		storage.append(
			key,
			Stream.class,
			() -> Cell.with(new Stream()),
			(stream) -> {
				final var newId = stream.add(id, keyValues);
				newIdReference.set(newId);
				return stream;
			}
		);

		return RString.bulk(newIdReference.get().toString());
	}

	private RValue evaluateXRange(RArray<RValue> list) {
		final var key = (RString) list.get(1);
		final var fromId = Identifier.parse((RString) list.get(2));
		final var toId = Identifier.parse((RString) list.get(3));

		final var stream = (Stream) storage.get(key);
		final var entries = stream.range(fromId, toId);

		return collectStreamContent(entries);
	}

	private RValue evaluateXRead(RArray<RValue> list) {
		record Query(
			RString key,
			Identifier identifier
		) {}

		final var queries = new ArrayList<Query>();
		Duration timeout = null;

		final var size = list.size();
		for (var index = 1; index < size; ++index) {
			var element = (RString) list.get(index);

			if (RString.equalsIgnoreCase(element, "block")) {
				++index;

				element = (RString) list.get(index);
				timeout = Duration.ofMillis(((RString) list.get(index)).asLong());

				continue;
			}

			if (RString.equalsIgnoreCase(element, "streams")) {
				++index;

				final var remaining = size - index;
				final var offset = remaining / 2;

				for (var jndex = 0; jndex < offset; ++jndex) {
					final var key = (RString) list.get(index + jndex);

					element = (RString) list.get(index + offset + jndex);
					final var identifier = timeout != null && RString.equalsIgnoreCase(element, "$")
						? null
						: Identifier.parse((RString) list.get(index + offset + jndex));

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
				return RNil.BULK;
			}

			return RArray.of(RArray.of(
				RString.bulk(key),
				collectStreamContent(entries)
			));
		}

		return RArray.view(
			queries.stream()
				.map((query) -> {
					final var key = query.key();
					final var stream = (Stream) storage.get(key);
					final var entries = stream.read(query.identifier());

					return RArray.of(
						RString.simple(key),
						collectStreamContent(entries)
					);
				})
				.toList()
		);
	}

	private RArray<RArray<RValue>> collectStreamContent(final List<StreamEntry> entries) {
		return RArray.view(
			entries.stream()
				.map((entry) -> RArray.<RValue>of(
					RString.bulk(entry.identifier().toString()),
					RArray.view(entry.content())
				))
				.toList()
		);
	}

	private RValue evaluateKeys(RArray<RValue> list) {
		if (list.size() != 2) {
			return new RError("ERR wrong number of arguments for 'keys' command");
		}

		// final var pattern = String.valueOf(list.get(1));

		return storage.keys();
	}

	private RValue evaluateType(RArray<RValue> list) {
		if (list.size() != 2) {
			return new RError("ERR wrong number of arguments for 'type' command");
		}

		final var key = (RString) list.get(1);
		final var value = storage.get(key);

		return RString.simple(switch (value) {
			case null -> "none";
			case RArray<?> __ -> "list";
			case RString __ -> "string";
			case Stream __ -> "stream";
			default -> throw new IllegalStateException("unknown type: %s".formatted(value.getClass()));
		});
	}

	private RValue evaluateConfig(RArray<RValue> list) {
		final var action = (RString) list.get(1);

		if (RString.equalsIgnoreCase(action, "GET")) {
			final var key = (RString) list.get(2);

			final var property = configuration.option(key.content());
			if (property == null) {
				return RArray.empty();
			}

			return RArray.of(
				key,
				RString.simple(String.valueOf(property.argument(0).get()))
			);
		}

		return new RError("ERR unknown subcommand '%s'. Try CONFIG HELP.".formatted(action));
	}

	private RValue evaluateInfo(RArray<RValue> list) {
		final var action = (RString) list.get(1);

		if (RString.equalsIgnoreCase(action, "REPLICATION")) {
			final var mode = configuration.isSlave()
				? "slave"
				: "master";

			return RString.bulk("""
				# Replication
				role:%s
				master_replid:%s
				master_repl_offset:%s
				""".formatted(
				mode,
				getMasterReplicationId(),
				replicationOffset
			));
		}

		return RString.empty(true);
	}

	private Payload evaluateReplicaConfig(RArray<RValue> list) {
		final var action = (RString) list.get(1);

		if (RString.equalsIgnoreCase(action, "GETACK")) {
			return new Payload(
				RArray.of(
					RString.bulk("REPLCONF"),
					RString.bulk("ACK"),
					RString.bulk(String.valueOf(replicationOffset))
				),
				false
			);
		}

		return new Payload(ROk.INSTANCE);
	}

	private List<Payload> evaluatePSync(Client client, RArray<RValue> list) {
		client.setReplicate(true);

		replicas.add(client);
		if (!client.onDisconnect(replicas::remove)) {
			replicas.remove(client);
			return List.of(new Payload(new RError("ERR could not enable replica")));
		}

		client.command(new Payload(RString.simple("FULLRESYNC %s 0".formatted(getMasterReplicationId()))));
		client.command(new Payload(RBlob.bulk(Base64.getDecoder().decode("UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog=="))));

		return null;
	}

	private RValue evaluateWait(RArray<RValue> list) {
		final var numberOfReplicas = ((RString) list.get(1)).asInteger();
		final var timeout = ((RString) list.get(2)).asInteger();

		if (replicationOffset.get() == 0) {
			return RInteger.of(replicas.size());
		}

		final var acks = new AtomicInteger();

		final var futures = new ArrayList<Map.Entry<Client, Future<Integer>>>(replicas.size());
		replicas.forEach((replica) -> {
			if (replica.getOffset() == 0) {
				acks.incrementAndGet();
				return;
			}

			final var future = new CompletableFuture<Integer>();

			replica.command(new Payload(
				RArray.of(
					RString.bulk("REPLCONF"),
					RString.bulk("GETACK"),
					RString.bulk("*")
				),
				false
			));

			replica.setReplicateConsumer((x) -> future.complete(1));
			futures.add(Map.entry(replica, future));
		});

		var remaining = (long) timeout;

		for (final var entry : futures) {
			if (acks.get() >= numberOfReplicas) {
				break;
			}

			final var future = entry.getValue();

			if (remaining <= 0) {
				final var isDone = future.isDone();
				if (isDone) {
					acks.incrementAndGet();
				}

				log("no time left isDone=%s acks=%d".formatted(isDone, acks.get()));
				continue;
			}

			final var start = System.currentTimeMillis();
			try {
				future.get(remaining, TimeUnit.MILLISECONDS);
				acks.incrementAndGet();

				final var took = System.currentTimeMillis() - start;
				remaining -= took;

				log("future ended took=%d remaining=%d acks=%d".formatted(took, remaining, acks.get()));
			} catch (TimeoutException exception) {
				final var took = System.currentTimeMillis() - start;

				remaining = 0;
				log("future timeout took=%d".formatted(took));
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}

		futures.forEach((entry) -> entry.getKey().setReplicateConsumer(null));

		return RInteger.of(acks.get());
	}

	private RValue evaluateIncrement(RArray<RValue> list) {
		final var key = (RString) list.get(1);

		return storage.append(
			key,
			RInteger.class,
			() -> Cell.with(RInteger.ZERO),
			RInteger::addOne
		);
	}

	public String getMasterReplicationId() {
		return configuration.masterReplicationId().argument(0, String.class).get();
	}

	public void progagate(RArray<RValue> command) {
		final var payload = new Payload(command);

		replicas.forEach((client) -> {
			client.command(payload);
		});
	}

	public static void log(String message) {
		System.out.println("[%d] %s".formatted(ProcessHandle.current().pid(), message));
	}

	public static void error(String message) {
		System.err.println("[%d] %s".formatted(ProcessHandle.current().pid(), message));
	}

}