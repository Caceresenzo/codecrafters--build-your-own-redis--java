package redis.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;

import lombok.SneakyThrows;
import redis.Redis;
import redis.rdb.RdbLoader;
import redis.serial.Deserializer;
import redis.serial.Serializer;
import redis.type.RArray;
import redis.type.RBlob;
import redis.type.RString;
import redis.type.RValue;
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
		send(RArray.of(
			RString.bulk("PING")
		));

		final var port = redis.getConfiguration()
			.port()
			.argument(0, Integer.class)
			.get();

		send(RArray.of(
			RString.bulk("REPLCONF"),
			RString.bulk("listening-port"),
			RString.bulk(String.valueOf(port))
		));

		send(RArray.of(
			RString.bulk("REPLCONF"),
			RString.bulk("capa"),
			RString.bulk("psync2")
		));

		send(RArray.of(
			RString.bulk("PSYNC"),
			RString.bulk("?"),
			RString.bulk("-1")
		));

		final var rdb = (RBlob) deserializer.read(true);
		redis.getStorage().clear();
		RdbLoader.load(rdb.inputStream(), redis.getStorage());
	}

	@SneakyThrows
	public Object send(RArray<RValue> command) {
		Redis.log("replica: sending: %s".formatted(command));
		serializer.write(command);

		final var answer = deserializer.read();
		Redis.log("replica: received: %s".formatted(answer));

		return answer;
	}

}