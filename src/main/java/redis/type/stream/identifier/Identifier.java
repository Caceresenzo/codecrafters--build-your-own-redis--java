package redis.type.stream.identifier;

import java.util.regex.Pattern;

public sealed interface Identifier permits MillisecondsIdentifier, UniqueIdentifier, WildcardIdentifier {

	public static final Pattern PATTERN = Pattern.compile("^(\\d+)-(\\d+|\\*)$");

	public static Identifier parse(String input) {
		if (WildcardIdentifier.INSTANCE.toString().equals(input)) {
			return WildcardIdentifier.INSTANCE;
		}

		final var matcher = PATTERN.matcher(input);
		if (!matcher.find()) {
			throw new IllegalArgumentException("not a valid identifier: %s".formatted(input));
		}

		final var milliseconds = Long.parseLong(matcher.group(1));

		final var sequenceNumber = matcher.group(2);
		if ("*".equals(sequenceNumber)) {
			return new MillisecondsIdentifier(milliseconds);
		}

		return new UniqueIdentifier(
			milliseconds,
			Long.parseLong(sequenceNumber)
		);
	}

}