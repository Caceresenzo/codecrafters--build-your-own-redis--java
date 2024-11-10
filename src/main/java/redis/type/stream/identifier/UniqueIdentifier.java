package redis.type.stream.identifier;

public record UniqueIdentifier(
	long milliseconds,
	long sequenceNumber
) implements Identifier, Comparable<Identifier> {

	public static final UniqueIdentifier ZERO = new UniqueIdentifier(0, 0);
	public static final UniqueIdentifier MINIMUM = new UniqueIdentifier(0, 1);
	public static final UniqueIdentifier MAXIMUM = new UniqueIdentifier(Long.MAX_VALUE, Long.MAX_VALUE);

	@Override
	public int compareTo(Identifier other) {
		return switch (other) {
			case MillisecondsIdentifier right -> Long.compare(this.milliseconds(), right.milliseconds());
			case WildcardIdentifier right -> 0;
			case UniqueIdentifier right -> {
				var compare = Long.compare(this.milliseconds(), right.milliseconds());
				if (compare != 0) {
					yield compare;
				}

				yield Long.compare(this.sequenceNumber(), right.sequenceNumber());
			}
		};
	}

	@Override
	public String toString() {
		return "%d-%d".formatted(milliseconds, sequenceNumber);
	}

}