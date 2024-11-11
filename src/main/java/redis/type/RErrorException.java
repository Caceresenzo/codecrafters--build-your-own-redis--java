package redis.type;

import lombok.Getter;

@SuppressWarnings("serial")
public class RErrorException extends RuntimeException {

	@Getter
	private final RError error;

	public RErrorException(RError error) {
		this.error = error;
	}

}