package redis.type;

public record RInteger(
	int value
) implements RValue {

	public static final RInteger ZERO = of(0);
	public static final RInteger ONE = of(1);

	public RInteger addOne() {
		return new RInteger(value + 1);
	}

	public static RInteger of(int value) {
		return new RInteger(value);
	}

	public static RInteger of(boolean value) {
		return value ? ONE : ZERO;
	}

}