package redis.type;

import lombok.NonNull;
import lombok.experimental.Delegate;
import redis.serial.Protocol;

public record RString(
	@NonNull String content,
	boolean bulk
) implements RValue, CharSequence {

	private static final RString PONG = new RString("PING", false);
	private static final RString EMPTY_SIMPLE = new RString("", false);
	private static final RString EMPTY_BULK = new RString("", true);

	public RString {
		if (!bulk && content.contains(Protocol.CRLF)) {
			throw new IllegalStateException("simple string cannot contains CRLF");
		}
	}

	@Delegate
	public String content() {
		return content;
	}

	public String toString() {
		return "\"%s\"".formatted(content);
	}

	public static RString simple(@NonNull CharSequence value) {
		return new RString(String.valueOf(value), false);
	}

	public static RString bulk(@NonNull CharSequence value) {
		return new RString(String.valueOf(value), true);
	}

	public static boolean equalsIgnoreCase(@NonNull RString left, @NonNull String right) {
		return left.content.equalsIgnoreCase(right);
	}

	public static RString pong() {
		return PONG;
	}

	public static RString empty(boolean bulk) {
		return bulk ? EMPTY_BULK : EMPTY_SIMPLE;
	}

}