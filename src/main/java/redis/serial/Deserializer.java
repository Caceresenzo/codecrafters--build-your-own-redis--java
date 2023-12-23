package redis.serial;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Deserializer {

	private final InputStream inputStream;

	public Object read() throws IOException {
		final var first = inputStream.read();
		if (first == -1) {
			return null;
		}

		return switch (first) {
			case Protocol.ARRAY -> parseArray();
			case Protocol.BULK_STRING -> parseBulkString();

			default -> throw new IllegalArgumentException("Unexpected value: " + first);
		};
	}

	private Object parseBulkString() throws IOException {
		final var length = parseUnsignedInteger();
		final var bytes = inputStream.readNBytes(length);

		inputStream.read();
		inputStream.read();
		// TODO validate

		return new String(bytes);
	}

	private Object parseArray() throws IOException {
		final var length = parseUnsignedInteger();

		if (length == 0) {
			return Collections.emptyList();
		}

		final var array = new ArrayList<Object>();
		for (int index = 0; index < length; index++) {
			final var child = read();
			if (child == null) {
				return null;
			}

			array.add(child);
		}

		return array;
	}

	private int parseUnsignedInteger() throws IOException {
		return Integer.parseUnsignedInt(parseUntilEndOfLine());
	}

	private String parseUntilEndOfLine() throws IOException {
		final var builder = new StringBuilder();

		var cariageReturn = false;

		int value;
		while ((value = inputStream.read()) != -1) {
			if ('\n' == value && cariageReturn) {
				break;
			} else if ('\r' == value) {
				cariageReturn = true;
			} else {
				if (cariageReturn) {
					builder.append('\r');
				}

				builder.append((char) value);
				cariageReturn = false;
			}
		}

		return builder.toString();
	}

}