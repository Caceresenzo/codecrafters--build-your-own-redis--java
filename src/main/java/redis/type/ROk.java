package redis.type;

public enum ROk implements RValue {

	INSTANCE;

	@Override
	public String toString() {
		return "OK";
	}

}