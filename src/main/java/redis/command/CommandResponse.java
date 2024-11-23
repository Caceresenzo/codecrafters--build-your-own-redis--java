package redis.command;

import redis.type.RValue;

public record CommandResponse(
	RValue value,
	boolean ignorableByReplica
) {

	public CommandResponse(RValue value) {
		this(value, true);
	}

	public String toString() {
		return "{%s, ignorable?=%s}".formatted(value, ignorableByReplica);
	}

}