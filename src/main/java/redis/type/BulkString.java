package redis.type;

public record BulkString(String message) {

	public String toString() {
		return "\"%s\"".formatted(message);
	}

}