package redis.serial;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

import lombok.RequiredArgsConstructor;
import redis.type.Error;

@RequiredArgsConstructor
public class Deserializer {

	private final InputStream inputStream;

	public Object read() throws IOException {
		return read(false);
	}

	public Object read(boolean likelyBlob) throws IOException {
		final var first = inputStream.read();
		if (first == -1) {
			return null;
		}

		return switch (first) {
			case Protocol.ARRAY -> parseArray();
			case Protocol.SIMPLE_STRING -> parseString();
			case Protocol.SIMPLE_ERROR -> new Error(parseString());
			case Protocol.BULK_STRING -> likelyBlob ? parseBulkBlob() : parseBulkString();

			default -> {
				//				System.out.print((char) first);
				//				while (true) {
				//					System.out.print((char) inputStream.read());
				//				}

				throw new IllegalArgumentException("Unexpected value: %s (%s)".formatted(first, (char) first));
			}
		};
	}

	private String parseString() throws IOException {
		final var builder = new StringBuilder();

		int value;
		while ((value = inputStream.read()) != -1) {
			if (value == '\r') {
				inputStream.read(); /* \n */
				break;
			}

			builder.append((char) value);
		}

		return builder.toString();
	}

	private String parseBulkString() throws IOException {
		final var length = parseUnsignedInteger();
		final var bytes = inputStream.readNBytes(length);

		inputStream.read();
		inputStream.read();
		// TODO validate

		return new String(bytes);
	}

	private byte[] parseBulkBlob() throws IOException {
		final var length = parseUnsignedInteger();

		return inputStream.readNBytes(length);
	}

	private Object parseArray() throws IOException {
		final var length = parseUnsignedInteger();

		if (length == -1) {
			return null;
		}

		if (length == 0) {
			return Collections.emptyList();
		}

		final var array = new ArrayList<Object>();
		for (int index = 0; index < length; index++) {
			array.add(read());
		}

		return array;
	}

	private int parseUnsignedInteger() throws IOException {
		final var line = parseUntilEndOfLine();

		if ("-1".equals(line)) {
			return -1;
		}

		return Integer.parseUnsignedInt(line);
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