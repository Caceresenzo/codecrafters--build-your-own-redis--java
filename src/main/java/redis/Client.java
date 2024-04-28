package redis;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import redis.configuration.Configuration;
import redis.serial.Deserializer;
import redis.serial.Serializer;
import redis.store.Storage;

public class Client implements Runnable {

	private static final AtomicInteger ID_INCREMENT = new AtomicInteger();

	private final int id;
	private final Socket socket;
	private final Storage storage;
	private final Configuration configurationStorage;

	public Client(Socket socket, Storage storage, Configuration configurationStorage) throws IOException {
		this.id = ID_INCREMENT.incrementAndGet();
		this.socket = socket;
		this.storage = storage;
		this.configurationStorage = configurationStorage;
	}

	@Override
	public void run() {
		System.out.println("%d: connected".formatted(id));

		try (socket) {
			final var inputStream = new BufferedInputStream(socket.getInputStream());
			final var outputStream = new BufferedOutputStream(socket.getOutputStream());

			final var deserializer = new Deserializer(inputStream);
			final var serializer = new Serializer(outputStream);
			final var evaluator = new Redis(storage, configurationStorage);

			Object request;
			while ((request = deserializer.read()) != null) {
				System.out.println("%d: received: %s".formatted(id, request));
				final var values = evaluator.evaluate(request);

				for (var answer : values) {
					System.out.println("%d: answering: %s".formatted(id, answer));
					serializer.write(answer);
				}

				outputStream.flush();
			}

			socket.close();
		} catch (IOException exception) {
			System.err.println("%d: returned an error: %s".formatted(id, exception.getMessage()));
			exception.printStackTrace();
		}

		System.out.println("%d: disconnected".formatted(id));
	}

}