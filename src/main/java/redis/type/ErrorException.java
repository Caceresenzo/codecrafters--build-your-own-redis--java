package redis.type;

import lombok.Getter;

@SuppressWarnings("serial")
public class ErrorException extends RuntimeException {

	@Getter
	private final RError error;

	public ErrorException(RError error) {
		this.error = error;
	}

}