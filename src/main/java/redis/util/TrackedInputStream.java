package redis.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TrackedInputStream extends InputStream {

	private final InputStream delegate;
	private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	private long read;

	@Override
	public int available() throws IOException {
		return delegate.available();
	}

	@Override
	public int read() throws IOException {
		final var value = delegate.read();

		if (value != -1) {
			++read;
			buffer.write(value);
		}

		return value;
	}

	@Override
	public int read(byte[] b) throws IOException {
		final var value = delegate.read(b);

		if (value != -1) {
			this.read += value;
			buffer.write(b, 0, value);
		}

		return value;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		final var value = delegate.read(b, off, len);

		if (read != -1) {
			this.read += value;
			buffer.write(b, off, value);
		}

		return value;
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}

	public void begin() {
		read = 0;
		buffer.reset();
	}

	public long count() {
		return read;
	}

	public byte[] buffer() {
		return buffer.toByteArray();
	}

}