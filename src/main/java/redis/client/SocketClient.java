package redis.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.Locked;
import lombok.Setter;
import lombok.SneakyThrows;
import redis.Redis;
import redis.command.CommandResponse;
import redis.command.ParsedCommand;
import redis.serial.Deserializer;
import redis.serial.Serializer;
import redis.type.RBlob;
import redis.type.RValue;
import redis.util.TrackedInputStream;
import redis.util.TrackedOutputStream;

public class SocketClient implements Client, Runnable {

	private static final AtomicInteger ID_INCREMENT = new AtomicInteger();

	private final int id;
	private final Socket socket;
	private final Redis redis;
	private boolean connected;
	private Consumer<SocketClient> disconnectListener;
	private @Setter boolean replicate;
	private @Getter @Setter long offset = 0;
	private final BlockingQueue<CommandResponse> pendingCommands = new ArrayBlockingQueue<>(128, true);
	private @Setter Consumer<Object> replicateConsumer;
	private @Getter @Setter List<ParsedCommand> queuedCommands;

	private final TrackedInputStream inputStream;
	private final TrackedOutputStream outputStream;
	private final Deserializer deserializer;
	private final Serializer serializer;

	public SocketClient(Socket socket, Redis redis) throws IOException {
		this.id = ID_INCREMENT.incrementAndGet();
		this.socket = socket;
		this.redis = redis;

		this.inputStream = new TrackedInputStream(socket.getInputStream());
		this.outputStream = new TrackedOutputStream(socket.getOutputStream());
		this.deserializer = new Deserializer(inputStream);
		this.serializer = new Serializer(outputStream);
	}

	@SneakyThrows
	@Override
	public void run() {
		connected = true;
		Redis.log("%d: connected".formatted(id));

		try (socket) {
			while (!replicate) {
				inputStream.begin();

				final var request = deserializer.read();
				if (request == null) {
					break;
				}

				final var read = inputStream.count();

				Redis.log("%d: received (%d): %s".formatted(id, read, request));
				final var response = redis.evaluate(this, request, read);

				if (response == null) {
					Redis.log("%d: no response".formatted(id));
					continue;
				} else {
					Redis.log("%d: responding: %s".formatted(id, response));
					serialize(response.value());
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
				serialize(command.value());
				serializer.flush();

				if (command.value() instanceof RBlob) {
					offset = 0;
					Redis.log("%d: reset offset".formatted(id));
				} else {
					offset += outputStream.count() - offset;
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

		redis.getPubSub().unsubscribeAll(this);
	}

	@SneakyThrows
	@Locked
	private void serialize(RValue value) {
		serializer.write(value);
	}

	public void command(CommandResponse value) {
		final var inserted = pendingCommands.offer(value);
		Redis.log("%d: queue command: %s - inserted?=%s newSize=%s".formatted(id, value, inserted, pendingCommands.size()));

		if (!inserted) {
			Redis.log("%d: retry queue command: %s".formatted(id, value));
			pendingCommands.add(value);
		}
	}

	public boolean onDisconnect(Consumer<SocketClient> listener) {
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

	public boolean isInTransaction() {
		return queuedCommands != null;
	}

	public void beginTransaction() {
		queuedCommands = new ArrayList<>();
	}

	public List<ParsedCommand> discardTransaction() {
		final var commands = queuedCommands;

		queuedCommands = null;

		return commands;
	}

	public boolean queueCommand(ParsedCommand command) {
		if (isInTransaction()) {
			queuedCommands.add(command);
			return true;
		}

		return false;
	}

	public void notifySubscription(RValue value) {
		Redis.log("%d: notifying subscription: %s".formatted(id, value));
		serialize(value);
	}

	public static SocketClient cast(Client client) {
		if (client instanceof SocketClient socketClient) {
			return socketClient;
		}

		throw new UnsupportedOperationException("client must be a SocketClient");
	}

}