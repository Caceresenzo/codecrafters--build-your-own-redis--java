package redis.type;

import lombok.NonNull;

public record RBlob(
	byte[] content,
	boolean bulk
) implements RValue {

	public static RBlob simple(@NonNull byte[] content) {
		return new RBlob(content, false);
	}

	public static RBlob bulk(@NonNull byte[] content) {
		return new RBlob(content, true);
	}

}