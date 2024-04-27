package redis.type;

import lombok.Getter;

@SuppressWarnings("serial")
public class ErrorException extends RuntimeException {

	@Getter
	private final Error error;

	public ErrorException(Error error) {
		this.error = error;
	}

}