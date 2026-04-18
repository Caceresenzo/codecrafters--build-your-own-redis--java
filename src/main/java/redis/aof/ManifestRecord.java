package redis.aof;

import java.util.regex.Pattern;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public record ManifestRecord(
	String fileName,
	long sequence,
	Type type
) {

	public static final Pattern PATTERN = Pattern.compile("file (\\S+) seq (\\d+) type (\\S+)");

	@Override
	public final String toString() {
		return "file %s seq %d type %s".formatted(fileName, sequence, type.code);
	}

	public static final ManifestRecord parse(String line) {
		final var matcher = PATTERN.matcher(line);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("invalid manifest record: %s".formatted(line));
		}

		final var fileName = matcher.group(1);
		final var sequence = Long.parseLong(matcher.group(2));
		final var type = Type.fromCode(matcher.group(3));

		return new ManifestRecord(fileName, sequence, type);
	}

	@Getter
	@RequiredArgsConstructor
	public enum Type {

		INCREMENTAL("i"),
		UNKNOWN("?");

		private final String code;

		public static Type fromCode(String code) {
			for (final var type : values()) {
				if (type.code.equals(code)) {
					return type;
				}
			}

			return UNKNOWN;
		}

	}

}