package redis;
import java.util.Collections;
import java.util.List;

import redis.type.BulkString;
import redis.type.Error;

public class Evaluator {

	public Object evaluate(Object value) {
		if (value instanceof List<?> list) {
			return evaluateList(list);
		}

		return null;
	}

	private Object evaluateList(List<?> list) {
		if (list.isEmpty()) {
			return null;
		}

		if (!(list.getFirst() instanceof String first)) {
			return null;
		}

		if ("COMMAND".equalsIgnoreCase(first)) {
			return evaluateCommand(list);
		}

		if ("PING".equalsIgnoreCase(first)) {
			return evaluatePing(list);
		}

		if ("ECHO".equalsIgnoreCase(first)) {
			return evaluateEcho(list);
		}

		return null;
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

}