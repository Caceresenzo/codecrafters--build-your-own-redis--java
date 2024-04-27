package redis.type.stream.identifier;

public record UniqueIdentifier(
	long milliseconds,
	long sequenceNumber
) implements Identifier, Comparable<UniqueIdentifier> {

	public static final UniqueIdentifier MIN = new UniqueIdentifier(0, 1);

	@Override
	public int compareTo(UniqueIdentifier other) {
		var compare = Long.compare(this.milliseconds(), other.milliseconds());
		if (compare != 0) {
			return compare;
		}

		return Long.compare(this.sequenceNumber(), other.sequenceNumber());
	}

	@Override
	public String toString() {
		return "%d-%d".formatted(milliseconds, sequenceNumber);
	}

}