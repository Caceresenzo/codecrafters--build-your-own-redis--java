package redis.type;
import redis.serial.Protocol;

public record Error(String message) {
	
	public Error {
		if (message.contains(Protocol.CRLF)) {
			throw new IllegalStateException("message cannot contains CRLF");
		}
	}
	
}