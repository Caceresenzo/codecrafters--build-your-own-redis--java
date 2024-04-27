package redis.type.stream.identifier;

public record UniqueIdentifier(
	long milliseconds,
	long sequenceNumber
) implements Identifier {

	@Override
	public String toString() {
		return "%d-%d".formatted(milliseconds, sequenceNumber);
	}

}