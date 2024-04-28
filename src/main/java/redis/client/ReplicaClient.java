package redis.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

import lombok.SneakyThrows;
import redis.Redis;
import redis.rdb.RdbLoader;
import redis.serial.Deserializer;
import redis.serial.Serializer;
import redis.type.BulkString;

public class ReplicaClient implements Runnable {

	private final Socket socket;
	private final Redis redis;

	private final Deserializer deserializer;
	private final Serializer serializer;

	public ReplicaClient(Socket socket, Redis redis) throws IOException {
		this.socket = socket;
		this.redis = redis;

		final var inputStream = socket.getInputStream();
		final var outputStream = socket.getOutputStream();

		deserializer = new Deserializer(inputStream);
		serializer = new Serializer(outputStream);
	}

	@SneakyThrows
	@Override
	public void run() {
		try (socket) {
			handshake(deserializer, serializer);

			Object request;
			while ((request = deserializer.read()) != null) {
				System.out.println("replica: received: %s".formatted(request));
				final var values = redis.evaluate(null, request);

				if (values == null) {
					System.out.println("replica: no answer");
					continue;
				}

				for (var answer : values) {
					System.out.println("replica: answering: %s".formatted(answer));
					serializer.write(answer);
				}

				serializer.flush();
			}
		}
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
		System.out.println("replica: sending: %s".formatted(command));
		serializer.write(command);

		final var answer = deserializer.read();
		System.out.println("replica: received: %s".formatted(answer));

		return answer;
	}

}