package redis.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import redis.Redis;
import redis.serial.Deserializer;
import redis.serial.Serializer;
import redis.util.TrackedInputStream;

public class Client implements Runnable {

	private static final AtomicInteger ID_INCREMENT = new AtomicInteger();

	private final int id;
	private final Socket socket;
	private final Redis evaluator;
	private boolean connected;
	private Consumer<Client> disconnectListener;
	private @Setter boolean replicate;
	private @Getter AtomicLong replicationOffset = new AtomicLong();
	private final BlockingQueue<Payload> pendingCommands = new ArrayBlockingQueue<>(128, true);

	public Client(Socket socket, Redis evaluator) throws IOException {
		this.id = ID_INCREMENT.incrementAndGet();
		this.socket = socket;
		this.evaluator = evaluator;
	}

	@SneakyThrows
	@Override
	public void run() {
		connected = true;
		System.out.println("%d: connected".formatted(id));

		try (socket) {
			final var inputStream = new TrackedInputStream(new BufferedInputStream(socket.getInputStream()));
			final var outputStream = new BufferedOutputStream(socket.getOutputStream());

			final var deserializer = new Deserializer(inputStream);
			final var serializer = new Serializer(outputStream);

			while (!replicate) {
				inputStream.begin();

				final var request = deserializer.read();
				if (request == null) {
					break;
				}
				
				final var read = inputStream.count();

				System.out.println("%d: received (%d): %s".formatted(id, read, request));
				final var values = evaluator.evaluate(this, request, read);

				if (values == null) {
					System.out.println("%d: no answer".formatted(id));
					continue;
				}

				for (var answer : values) {
					System.out.println("%d: answering: %s".formatted(id, answer));
					serializer.write(answer.value());
				}

				outputStream.flush();
			}

			while (replicate && socket.isConnected()) {
				final var command = pendingCommands.poll(1, TimeUnit.MINUTES);
				if (command == null) {
					continue;
				}

				System.out.println("%d: send command: %s".formatted(id, command));

				if (socket.isConnected()) {
					serializer.write(command.value());
					outputStream.flush();
				}
			}
		} catch (IOException exception) {
			System.err.println("%d: returned an error: %s".formatted(id, exception.getMessage()));
			exception.printStackTrace();
		}

		System.out.println("%d: disconnected".formatted(id));

		synchronized (this) {
			connected = false;

			if (disconnectListener != null) {
				disconnectListener.accept(this);
			}
		}
	}

	public void command(Payload value) {
		System.out.println("%d: queue command: %s".formatted(id, value));
		pendingCommands.add(value);
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