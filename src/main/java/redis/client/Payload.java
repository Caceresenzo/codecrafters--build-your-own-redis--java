package redis.client;

public record Payload(
	Object value,
	boolean ignorableByReplica
) {

	public Payload(Object value) {
		this(value, true);
	}

	public String toString() {
		return "{%s, ignorable?=%s}".formatted(value, ignorableByReplica);
	}

}