package redis.configuration;

public class PortArgument extends Argument<Integer> {

	public PortArgument() {
		super("port", Integer::parseInt);
	}

	public PortArgument(int defaultValue) {
		super("port", Integer::parseInt, defaultValue);
	}

}