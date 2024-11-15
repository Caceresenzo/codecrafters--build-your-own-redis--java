package redis.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import redis.Redis;
import redis.serial.Deserializer;
import redis.serial.Serializer;
import redis.type.RBlob;
import redis.util.TrackedInputStream;
import redis.util.TrackedOutputStream;

public class Client implements Runnable {

	private static final AtomicInteger ID_INCREMENT = new AtomicInteger();

	private final int id;
	private final Socket socket;
	private final Redis evaluator;
	private boolean connected;
	private Consumer<Client> disconnectListener;
	private @Setter boolean replicate;
	private @Getter @Setter long offset = 0;
	private final BlockingQueue<Payload> pendingCommands = new ArrayBlockingQueue<>(128, true);
	private @Setter Consumer<Object> replicateConsumer;

	public Client(Socket socket, Redis evaluator) throws IOException {
		this.id = ID_INCREMENT.incrementAndGet();
		this.socket = socket;
		this.evaluator = evaluator;
	}

	@SneakyThrows
	@Override
	public void run() {
		connected = true;
		Redis.log("%d: connected".formatted(id));

		try (socket) {
			final var inputStream = new TrackedInputStream(socket.getInputStream());
			final var outputStream = new TrackedOutputStream(socket.getOutputStream());

			final var deserializer = new Deserializer(inputStream);
			final var serializer = new Serializer(outputStream);

			while (!replicate) {
				inputStream.begin();

				final var request = deserializer.read();
				if (request == null) {
					break;
				}

				final var read = inputStream.count();

				Redis.log("%d: received (%d): %s".formatted(id, read, request));
				final var values = evaluator.evaluate(this, request, read);

				if (values == null) {
					Redis.log("%d: no answer".formatted(id));
					continue;
				}

				for (var answer : values) {
					Redis.log("%d: answering: %s".formatted(id, answer));
					serializer.write(answer.value());
				}

				outputStream.flush();
			}

			if (replicate) {
				Thread.ofVirtual().start(new Runnable() {

					@Override
					@SneakyThrows
					public void run() {
						while (socket.isConnected()) {
							final var request = deserializer.read();
							if (request == null) {
								break;
							}

							final var consumer = replicateConsumer;
							if (consumer != null) {
								consumer.accept(request);
							}
						}
					}

				});
			}

			while (replicate && socket.isConnected()) {
				final var command = pendingCommands.poll(1, TimeUnit.MINUTES);
				if (command == null) {
					continue;
				}

				Redis.log("%d: send command: %s".formatted(id, command));

				outputStream.begin();
				serializer.write(command.value());
				serializer.flush();

				if (command.value() instanceof RBlob) {
					offset = 0;
					Redis.log("%d: reset offset".formatted(id));
				} else {
					offset += outputStream.count();
					Redis.log("%d: offset: %d".formatted(id, offset));
				}
			}
		} catch (Exception exception) {
			Redis.error("%d: returned an error: %s".formatted(id, exception.getMessage()));

			final var writer = new StringWriter();
			exception.printStackTrace(new PrintWriter(writer));

			for (final var line : writer.getBuffer().toString().split("\n")) {
				Redis.error("%d:   %s".formatted(id, line.replace("\r", "")));
			}
		}

		Redis.log("%d: disconnected".formatted(id));

		synchronized (this) {
			connected = false;

			if (disconnectListener != null) {
				disconnectListener.accept(this);
			}
		}
	}

	public void command(Payload value) {
		final var inserted = pendingCommands.offer(value);
		Redis.log("%d: queue command: %s - inserted?=%s newSize=%s".formatted(id, value, inserted, pendingCommands.size()));

		if (!inserted) {
			Redis.log("%d: retry queue command: %s".formatted(id, value));
			pendingCommands.add(value);
		}
	}

	public boolean onDisconnect(Consumer<Client> listener) {
		synchronized (this) {
			if (!connected) {
				return false;
			}

			if (disconnectListener != null) {
				return false;
			}

			disconnectListener = listener;
			return true;
		}
	}

}