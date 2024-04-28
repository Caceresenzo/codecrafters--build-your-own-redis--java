package redis.configuration;

import java.util.ArrayList;
import java.util.List;

public class Option {

	private final String name;
	private final List<Argument<?>> arguments;

	public Option(String name, List<Argument<?>> arguments) {
		this.name = name;
		this.arguments = new ArrayList<>(arguments);
	}

	public String name() {
		return name;
	}

	public Argument<?> argument(int index) {
		return arguments.get(index);
	}

	@SuppressWarnings("unchecked")
	public <R> Argument<R> argument(int index, Class<R> type) {
		return (Argument<R>) arguments.get(index);
	}

	public List<Argument<?>> arguments() {
		return arguments;
	}

	public int argumentsCount() {
		return arguments.size();
	}

}