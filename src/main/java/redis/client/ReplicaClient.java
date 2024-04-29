package redis.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.List;

import lombok.SneakyThrows;
import redis.Redis;
import redis.rdb.RdbLoader;
import redis.serial.Deserializer;
import redis.serial.Serializer;
import redis.type.BulkString;
import redis.util.TrackedInputStream;

public class ReplicaClient implements Runnable {

	private final Socket socket;
	private final Redis redis;

	private final TrackedInputStream inputStream;
	private final Deserializer deserializer;
	private final Serializer serializer;

	public ReplicaClient(Socket socket, Redis redis) throws IOException {
		this.socket = socket;
		this.redis = redis;

		inputStream = new TrackedInputStream(socket.getInputStream());
		final var outputStream = socket.getOutputStream();

		deserializer = new Deserializer(inputStream);
		serializer = new Serializer(outputStream);
	}

	@SneakyThrows
	@Override
	public void run() {
		try (socket) {
			handshake(deserializer, serializer);

			while (true) {
				inputStream.begin();
				
				final var request = deserializer.read();
				if (request == null) {
					break;
				}

				final var read = inputStream.count();

				Redis.log("replica: received (%s): %s".formatted(read, request));
				final var values = redis.evaluate(null, request, read);

				if (values == null) {
					Redis.log("replica: no answer");
					continue;
				}

				for (var answer : values) {
					Redis.log("replica: answering: %s".formatted(answer));

					if (!answer.ignorableByReplica()) {
						serializer.write(answer.value());
					}
				}

				serializer.flush();
			}
		} catch (Exception exception) {
			Redis.error("replica: returned an error: %s".formatted(exception.getMessage()));
			
			final var writer = new StringWriter();
			exception.printStackTrace(new PrintWriter(writer));

			for (final var line : writer.getBuffer().toString().split("\n")) {
				Redis.error("replica:   %s".formatted(line.replace("\r", "")));
			}
		}

		Redis.log("replica: disconnected");
	}

	@SneakyThrows
	private void handshake(Deserializer deserializer, Serializer serializer) {
		send(List.of(
			new BulkString("PING")
		));

		final var port = redis.getConfiguration()
			.port()
			.argument(0, Integer.class)
			.get();

		send(List.of(
			new BulkString("REPLCONF"),
			new BulkString("listening-port"),
			new BulkString(String.valueOf(port))
		));

		send(List.of(
			new BulkString("REPLCONF"),
			new BulkString("capa"),
			new BulkString("psync2")
		));

		send(List.of(
			new BulkString("PSYNC"),
			new BulkString("?"),
			new BulkString("-1")
		));

		final var rdb = (byte[]) deserializer.read(true);
		redis.getStorage().clear();
		RdbLoader.load(new ByteArrayInputStream(rdb), redis.getStorage());
	}

	@SneakyThrows
	public Object send(List<Object> command) {
		Redis.log("replica: sending: %s".formatted(command));
		serializer.write(command);

		final var answer = deserializer.read();
		Redis.log("replica: received: %s".formatted(answer));

		return answer;
	}

}