package redis;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import redis.serial.Deserializer;
import redis.serial.Serializer;

public class Client implements Runnable {

	private static final AtomicInteger ID_INCREMENT = new AtomicInteger();

	private final int id;
	private final Socket socket;
	private final Storage storage;

	public Client(Socket socket, Storage storage) throws IOException {
		this.id = ID_INCREMENT.incrementAndGet();
		this.socket = socket;
		this.storage = storage;
	}

	@Override
	public void run() {
		System.out.println("%d: connected".formatted(id));

		try (socket) {
			final var inputStream = new BufferedInputStream(socket.getInputStream());
			final var outputStream = new BufferedOutputStream(socket.getOutputStream());

			final var deserializer = new Deserializer(inputStream);
			final var serializer = new Serializer(outputStream);
			final var evaluator = new Evaluator(storage);

			Object value;
			while ((value = deserializer.read()) != null) {
				//				System.out.println(value);

				value = evaluator.evaluate(value);
				//				System.out.println(value);

				serializer.write(value);
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