package redis.configuration;

import java.util.function.Function;

public class Property<T> {

	private final String key;
	private final Function<String, T> converter;
	private T value;

	public Property(String key, Function<String, T> converter) {
		this.key = key;
		this.converter = converter;
	}

	public Property(String key, Function<String, T> converter, T defaultValue) {
		this.key = key;
		this.converter = converter;
		this.value = defaultValue;
	}

	public String key() {
		return key;
	}

	public T get() {
		return value;
	}

	public boolean isSet() {
		return value != null;
	}

	public void set(String value) {
		this.value = converter.apply(value);
	}

}