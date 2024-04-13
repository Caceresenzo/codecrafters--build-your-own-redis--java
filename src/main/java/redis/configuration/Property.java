package redis.configuration;

public class Property {

	private final String key;
	private String value;

	public Property(String key) {
		this.key = key;
	}

	public String key() {
		return key;
	}

	public String get() {
		return value;
	}

	public void set(String value) {
		this.value = value;
	}

}