package redis;

import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import redis.store.Storage;
import redis.type.BulkString;
import redis.type.Error;
import redis.type.Ok;

@RequiredArgsConstructor
public class Evaluator {

	private final Storage storage;

	public Object evaluate(Object value) {
		if (value instanceof List<?> list) {
			return evaluate(list);
		}

		return new Error("ERR command be sent in an array");
	}

	private Object evaluate(List<?> arguments) {
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

}