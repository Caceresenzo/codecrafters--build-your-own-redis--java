package redis.type;

import redis.serial.Protocol;

public record Error(String message) {

	private static final Error SYNTAX = new Error("ERR syntax error");
	private static final Error STREAM_ID_INVALID = new Error("ERR Invalid stream ID specified as stream command argument");
	private static final Error XADD_ID_EQUAL_OR_SMALLER = new Error("ERR The ID specified in XADD is equal or smaller than the target stream top item");

	public Error {
		if (message.contains(Protocol.CRLF)) {
			throw new IllegalStateException("message cannot contains CRLF");
		}
	}

	public static Error syntax() {
		return SYNTAX;
	}
	
	public static Error streamIdInvalid() {
		return STREAM_ID_INVALID;
	}
	
	public static Error xaddIdEqualOrSmaller() {
		return XADD_ID_EQUAL_OR_SMALLER;
	}

}