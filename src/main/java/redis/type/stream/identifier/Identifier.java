package redis.type.stream.identifier;

import java.util.regex.Pattern;

import redis.type.RErrorException;
import redis.type.RError;
import redis.type.RString;

public sealed interface Identifier permits MillisecondsIdentifier, UniqueIdentifier, WildcardIdentifier {

	public static final Pattern PATTERN = Pattern.compile("^(\\d+)(?:-(\\d+|\\*))?$");

	public static Identifier parse(RString input) {
		if (RString.equalsIgnoreCase(input, "*")) {
			return WildcardIdentifier.INSTANCE;
		}

		if (RString.equalsIgnoreCase(input, "-")) {
			return UniqueIdentifier.MINIMUM;
		}

		if (RString.equalsIgnoreCase(input, "+")) {
			return UniqueIdentifier.MAXIMUM;
		}

		final var matcher = PATTERN.matcher(input);
		if (!matcher.find()) {
			throw new RErrorException(RError.streamIdInvalid());
		}

		final var milliseconds = Long.parseLong(matcher.group(1));

		final var sequenceNumber = matcher.group(2);
		if (sequenceNumber == null) {
			return new UniqueIdentifier(
				milliseconds,
				0
			);
		}

		if ("*".equals(sequenceNumber)) {
			return new MillisecondsIdentifier(milliseconds);
		}

		return new UniqueIdentifier(
			milliseconds,
			Long.parseLong(sequenceNumber)
		);
	}

}