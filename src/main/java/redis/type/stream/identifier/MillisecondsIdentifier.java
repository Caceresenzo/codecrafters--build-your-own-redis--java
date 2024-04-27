package redis.type.stream.identifier;

public record MillisecondsIdentifier(
	long milliseconds
) implements Identifier {

	@Override
	public String toString() {
		return "%d-*".formatted(milliseconds);
	}

}