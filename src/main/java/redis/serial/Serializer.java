package redis.serial;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import lombok.RequiredArgsConstructor;
import redis.type.BulkString;
import redis.type.Error;

@RequiredArgsConstructor
public class Serializer {

	private static final byte[] CRLF_BYTES = Protocol.CRLF.getBytes();

	private final OutputStream outputStream;

	public boolean write(Object value) throws IOException {
		if (value instanceof String string) {
			if (string.contains("\r\n")) {
				return writeBulkString(string);
			}

			return writeSimpleString(string);
		}

		if (value instanceof BulkString string) {
			return writeBulkString(string.message());
		}

		if (value instanceof List<?> list) {
			return writeArray(list);
		}

		if (value instanceof Error error) {
			return writeError(error);
		}

		throw new UnsupportedOperationException("type " + value.getClass().getSimpleName());
	}

	private boolean writeSimpleString(String string) throws IOException {
		outputStream.write(Protocol.SIMPLE_STRING);
		outputStream.write(string.getBytes());
		outputStream.write(CRLF_BYTES);

		return true;
	}

	private boolean writeBulkString(String string) throws IOException {
		outputStream.write(Protocol.BULK_STRING);
		outputStream.write(String.valueOf(string.length()).getBytes());
		outputStream.write(CRLF_BYTES);

		outputStream.write(string.getBytes());
		outputStream.write(CRLF_BYTES);

		return true;
	}

	private boolean writeArray(List<?> list) throws IOException {
		outputStream.write(Protocol.ARRAY);
		outputStream.write(String.valueOf(list.size()).getBytes());
		outputStream.write(CRLF_BYTES);

		for (final var element : list) {
			if (!write(element)) {
				return false;
			}
		}

		return true;
	}

	private boolean writeError(Error error) throws IOException {
		final var message = error.message();

		outputStream.write(Protocol.SIMPLE_ERROR);
		outputStream.write(message.getBytes());
		outputStream.write(CRLF_BYTES);

		return true;
	}

}