package redis.type;

public record RInteger(
	int value
) implements RValue {

	public static RInteger of(int value) {
		return new RInteger(value);
	}

}