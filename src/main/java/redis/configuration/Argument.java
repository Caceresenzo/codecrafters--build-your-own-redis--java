package redis.configuration;

import java.util.function.Function;

public class Argument<T> {

	private final String name;
	private final Function<String, T> converter;
	private T value;

	public Argument(String name, Function<String, T> converter) {
		this.name = name;
		this.converter = converter;
	}

	public Argument(String name, Function<String, T> converter, T defaultValue) {
		this.name = name;
		this.converter = converter;
		this.value = defaultValue;
	}

	public String name() {
		return name;
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