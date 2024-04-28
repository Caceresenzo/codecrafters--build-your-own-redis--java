package redis.client;

public record Payload(
	Object value,
	boolean ignorableByReplica
) {

	public Payload(Object value) {
		this(value, true);
	}

}