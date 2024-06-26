package redis.serial;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import lombok.RequiredArgsConstructor;
import redis.type.BulkBlob;
import redis.type.BulkString;
import redis.type.Error;
import redis.type.Ok;

@RequiredArgsConstructor
public class Serializer {

	private static final byte[] CRLF_BYTES = Protocol.CRLF.getBytes();
	private static final byte[] OK_BYTES = { 'O', 'K' };
	private static final byte[] MINUS_ONE_BYTES = { '-', '1' };

	private final OutputStream outputStream;

	public void flush() throws IOException {
		outputStream.flush();
	}

	public boolean write(Object value) throws IOException {
		if (value instanceof String string) {
			if (string.contains("\r\n")) {
				return writeBulkString(string);
			}

			return writeSimpleString(string);
		}

		if (value instanceof Integer integer) {
			return writeSimpleInteger(integer);
		}

		if (value instanceof BulkString string) {
			return writeBulkString(string.message());
		}

		if (value instanceof BulkBlob string) {
			return writeBulkBytes(string.message());
		}

		if (value instanceof List<?> list) {
			return writeArray(list);
		}

		if (value instanceof Error error) {
			return writeError(error);
		}

		if (value instanceof Ok) {
			return writeOk();
		}

		if (value == null) {
			return writeNil();
		}

		throw new UnsupportedOperationException("type " + value.getClass().getSimpleName());
	}

	private boolean writeSimpleString(String string) throws IOException {
		outputStream.write(Protocol.SIMPLE_STRING);
		outputStream.write(string.getBytes());
		outputStream.write(CRLF_BYTES);

		return true;
	}

	private boolean writeSimpleInteger(Integer integer) throws IOException {
		outputStream.write(Protocol.INTEGER);
		outputStream.write(String.valueOf(integer).getBytes());
		outputStream.write(CRLF_BYTES);

		return true;
	}

	private boolean writeBulkString(String string) throws IOException {
		if (string == null) {
			return writeNullBulk();
		}

		writeBulkBytes(string.getBytes());
		outputStream.write(CRLF_BYTES);

		return true;
	}

	private boolean writeBulkBytes(byte[] bytes) throws IOException {
		if (bytes == null) {
			return writeNullBulk();
		}

		outputStream.write(Protocol.BULK_STRING);

		outputStream.write(String.valueOf(bytes.length).getBytes());
		outputStream.write(CRLF_BYTES);

		outputStream.write(bytes);

		return true;
	}

	private boolean writeNullBulk() throws IOException {
		outputStream.write(Protocol.BULK_STRING);
		outputStream.write(MINUS_ONE_BYTES);
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

	private boolean writeOk() throws IOException {
		outputStream.write(Protocol.SIMPLE_STRING);
		outputStream.write(OK_BYTES);
		outputStream.write(CRLF_BYTES);

		return true;
	}

	private boolean writeNil() throws IOException {
		outputStream.write(Protocol.NULL);
		outputStream.write(CRLF_BYTES);

		return true;
	}

}