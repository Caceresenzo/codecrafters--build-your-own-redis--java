package redis.type.stream.identifier;

import java.util.regex.Pattern;

import redis.type.Error;
import redis.type.ErrorException;

public sealed interface Identifier permits MillisecondsIdentifier, UniqueIdentifier, WildcardIdentifier {

	public static final Pattern PATTERN = Pattern.compile("^(\\d+)(?:-(\\d+|\\*))?$");

	public static Identifier parse(String input) {
		if (WildcardIdentifier.INSTANCE.toString().equals(input)) {
			return WildcardIdentifier.INSTANCE;
		}

		if ("-".equals(input)) {
			return UniqueIdentifier.MIN;
		}

		if ("+".equals(input)) {
			return UniqueIdentifier.MAX;
		}

		final var matcher = PATTERN.matcher(input);
		if (!matcher.find()) {
			throw new ErrorException(Error.streamIdInvalid());
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