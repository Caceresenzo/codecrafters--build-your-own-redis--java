package redis.type;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.OptionalInt;

import lombok.NonNull;
import lombok.experimental.Delegate;
import redis.serial.Protocol;

public record RString(
	@NonNull String content,
	boolean bulk
) implements RValue, CharSequence {

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

	public OptionalInt asInteger() {
		try {
			return OptionalInt.of(Integer.parseInt(content));
		} catch (NumberFormatException __) {
			return OptionalInt.empty();
		}
	}

	public long asLong() {
		return Long.parseLong(content);
	}

	public double asDouble() {
		try {
			return Double.parseDouble(content);
		} catch (NumberFormatException exception) {
			throw RError.valueNotAFloat().asException();
		}
	}

	public Duration asDuration(TemporalUnit temporalUnit) {
		return Duration.of(asLong(), temporalUnit);
	}

	public String toString() {
		return "%s\"%s\"".formatted(
			bulk ? "B" : "N",
			content
		);
	}

	public static RString simple(@NonNull CharSequence value) {
		if (value instanceof RString input) {
			if (!input.bulk()) {
				return input;
			}

			return new RString(input.content(), false);
		}

		return new RString(value.toString(), false);
	}

	public static RString bulk(@NonNull CharSequence value) {
		if (value instanceof RString input) {
			if (input.bulk()) {
				return input;
			}

			return new RString(input.content(), true);
		}

		return new RString(value.toString(), true);
	}

	public static RString detect(@NonNull String value) {
		final var bulk = value.contains(Protocol.CRLF);

		return new RString(value, bulk);
	}

	public static boolean equalsIgnoreCase(@NonNull RString left, @NonNull String right) {
		return left.content.equalsIgnoreCase(right);
	}

	public static RString empty(boolean bulk) {
		return bulk ? EMPTY_BULK : EMPTY_SIMPLE;
	}

}