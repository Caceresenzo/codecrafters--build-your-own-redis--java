package redis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import lombok.RequiredArgsConstructor;
import redis.configuration.Configuration;
import redis.store.Cell;
import redis.store.Storage;
import redis.type.BulkString;
import redis.type.Error;
import redis.type.ErrorException;
import redis.type.Ok;
import redis.type.stream.Stream;
import redis.type.stream.identifier.Identifier;
import redis.type.stream.identifier.UniqueIdentifier;

@RequiredArgsConstructor
public class Evaluator {

	private final Storage storage;
	private final Configuration configurationStorage;

	@SuppressWarnings("unchecked")
	public Object evaluate(Object value) {
		if (value instanceof List list) {
			try {
				return evaluate(list);
			} catch (ErrorException exception) {
				return exception.getError();
			}
		}

		return new Error("ERR command be sent in an array");
	}

	private Object evaluate(List<Object> arguments) {
		if (arguments.isEmpty()) {
			return new Error("ERR command array is empty");
		}

		final var command = String.valueOf(arguments.getFirst());

		if ("COMMAND".equalsIgnoreCase(command)) {
			return evaluateCommand(arguments);
		}

		if ("PING".equalsIgnoreCase(command)) {
			return evaluatePing(arguments);
		}

		if ("ECHO".equalsIgnoreCase(command)) {
			return evaluateEcho(arguments);
		}

		if ("SET".equalsIgnoreCase(command)) {
			return evaluateSet(arguments);
		}

		if ("GET".equalsIgnoreCase(command)) {
			return evaluateGet(arguments);
		}

		if ("XADD".equalsIgnoreCase(command)) {
			return evaluateXAdd(arguments);
		}

		if ("XRANGE".equalsIgnoreCase(command)) {
			return evaluateXRange(arguments);
		}

		if ("XREAD".equalsIgnoreCase(command)) {
			return evaluateXRead(arguments);
		}

		if ("KEYS".equalsIgnoreCase(command)) {
			return evaluateKeys(arguments);
		}

		if ("TYPE".equalsIgnoreCase(command)) {
			return evaluateType(arguments);
		}

		if ("CONFIG".equalsIgnoreCase(command)) {
			return evaluateConfig(arguments);
		}

		return new Error("ERR unknown '%s' command".formatted(command));
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

			final var property = configurationStorage.getProperty(key);
			if (property == null) {
				return Collections.emptyList();
			}

			return Arrays.asList(
				key,
				property.get()
			);
		}

		return new Error("ERR unknown subcommand '%s'. Try CONFIG HELP.".formatted(action));
	}

}