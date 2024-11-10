package redis.client;

import redis.type.RValue;

public record Payload(
	RValue value,
	boolean ignorableByReplica
) {

	public Payload(RValue value) {
		this(value, true);
	}

	public String toString() {
		return "{%s, ignorable?=%s}".formatted(value, ignorableByReplica);
	}

}