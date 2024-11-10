package redis.type;

public enum RNil implements RValue {

	SIMPLE,
	BULK;

	public boolean bulk() {
		return this == BULK;
	}

	@Override
	public String toString() {
		return "nil";
	}

}