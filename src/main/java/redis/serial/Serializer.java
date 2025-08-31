package redis.serial;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import lombok.RequiredArgsConstructor;
import redis.type.RArray;
import redis.type.RBlob;
import redis.type.RError;
import redis.type.RInteger;
import redis.type.RNil;
import redis.type.ROk;
import redis.type.RString;
import redis.type.RValue;

@RequiredArgsConstructor
public class Serializer {

	private static final byte[] CRLF_BYTES = Protocol.CRLF.getBytes();
	private static final byte[] OK_BYTES = "OK".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] QUEUED_BYTES = "QUEUED".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] MINUS_ONE_BYTES = { '-', '1' };

	private final OutputStream outputStream;

	public void flush() throws IOException {
		outputStream.flush();
	}

	public void write(RValue value) throws IOException {
		switch (value) {
			case RArray<?> array -> writeArray(array.items());
			case RBlob blob -> {
				if (!blob.bulk()) {
					throw new UnsupportedOperationException("non bulk blob are not supported");

				}

				writeBulkBytes(blob.content());
			}
			case RError error -> writeError(error);
			case RInteger integer -> writeSimpleInteger(integer.value());
			case RNil.SIMPLE -> {
				writeNil();
			}
			case RNil.BULK -> {
				writeNullBulk();
			}
			case RNil.ARRAY -> {
				writeNullArray();
			}
			case ROk ok -> {
				switch (ok) {
					case OK -> writeOk();
					case QUEUED -> writeQueued();
				}
			}
			case RString string -> {
				if (string.bulk()) {
					writeBulkString(string.content());
				} else {
					writeSimpleString(string.content());
				}
			}
		}
	}

	private void writeSimpleString(String string) throws IOException {
		outputStream.write(Protocol.SIMPLE_STRING);
		outputStream.write(string.getBytes());
		outputStream.write(CRLF_BYTES);
	}

	private void writeSimpleInteger(Integer integer) throws IOException {
		outputStream.write(Protocol.INTEGER);
		outputStream.write(String.valueOf(integer).getBytes());
		outputStream.write(CRLF_BYTES);
	}

	private void writeBulkString(String string) throws IOException {
		writeBulkBytes(string.getBytes());
		outputStream.write(CRLF_BYTES);
	}

	private void writeBulkBytes(byte[] bytes) throws IOException {
		outputStream.write(Protocol.BULK_STRING);

		outputStream.write(String.valueOf(bytes.length).getBytes());
		outputStream.write(CRLF_BYTES);

		outputStream.write(bytes);
	}

	private void writeNullBulk() throws IOException {
		outputStream.write(Protocol.BULK_STRING);
		outputStream.write(MINUS_ONE_BYTES);
		outputStream.write(CRLF_BYTES);
	}
	
	private void writeNullArray() throws IOException {
		outputStream.write(Protocol.ARRAY);
		outputStream.write(MINUS_ONE_BYTES);
		outputStream.write(CRLF_BYTES);
	}

	private void writeArray(List<? extends RValue> list) throws IOException {
		outputStream.write(Protocol.ARRAY);
		outputStream.write(String.valueOf(list.size()).getBytes());
		outputStream.write(CRLF_BYTES);

		for (final var element : list) {
			write(element);
		}
	}

	private void writeError(RError error) throws IOException {
		final var message = error.message();

		outputStream.write(Protocol.SIMPLE_ERROR);
		outputStream.write(message.getBytes());
		outputStream.write(CRLF_BYTES);
	}

	private void writeOk() throws IOException {
		outputStream.write(Protocol.SIMPLE_STRING);
		outputStream.write(OK_BYTES);
		outputStream.write(CRLF_BYTES);
	}

	private void writeQueued() throws IOException {
		outputStream.write(Protocol.SIMPLE_STRING);
		outputStream.write(QUEUED_BYTES);
		outputStream.write(CRLF_BYTES);
	}

	private void writeNil() throws IOException {
		outputStream.write(Protocol.NULL);
		outputStream.write(CRLF_BYTES);
	}

}