package redis.util;

import java.io.IOException;
import java.io.InputStream;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TrackedInputStream extends InputStream {

	private final InputStream delegate;
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
		}

		return value;
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}

	public void begin() {
		read = 0;
	}

	public long count() {
		return read;
	}

}