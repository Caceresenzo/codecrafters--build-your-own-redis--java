package redis.configuration;

import lombok.Getter;

public abstract class Option<T> {

	private final @Getter String name;
	private @Getter T value;

	protected Option(String name, T defaultValue) {
		this.name = name;
		this.value = defaultValue;
		afterSet();
	}

	public void set(String value) {
		this.value = parse(value);
		afterSet();
	}

	protected void afterSet() {}

	public abstract T parse(String value);

}