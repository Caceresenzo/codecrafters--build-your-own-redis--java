package redis.type.stream.identifier;

public enum WildcardIdentifier implements Identifier {

	INSTANCE;

	@Override
	public String toString() {
		return "*";
	}

}