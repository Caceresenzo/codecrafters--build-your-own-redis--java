package redis.client;

import java.util.function.Consumer;

public record Payload(
	Object value,
	boolean ignorableByReplica,
	Consumer<Object> callback
) {

	public Payload(Object value) {
		this(value, true, null);
	}

	public Payload(Object value, boolean ignorableByReplica) {
		this(value, ignorableByReplica, null);
	}

	public String toString() {
		return "{%s, ignorable?=%s}".formatted(value, ignorableByReplica);
	}

}