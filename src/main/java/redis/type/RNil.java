package redis.type;

public enum RNil implements RValue {

	SIMPLE,
	BULK,
	ARRAY;

	@Override
	public String toString() {
		return "nil";
	}

}