package redis.type;

import redis.serial.Protocol;

public record Error(String message) {

	private static final Error SYNTAX = new Error("ERR syntax error");

	public Error {
		if (message.contains(Protocol.CRLF)) {
			throw new IllegalStateException("message cannot contains CRLF");
		}
	}

	public static Error syntax() {
		return SYNTAX;
	}

}