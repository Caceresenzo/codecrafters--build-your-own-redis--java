package redis.type;

public enum ROk implements RValue {

	OK,
	QUEUED;

	@Override
	public String toString() {
		return switch (this) {
			case OK -> "OK";
			case QUEUED -> "QUEUED";
		};
	}

}