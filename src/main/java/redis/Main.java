package redis;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Paths;

import redis.configuration.Configuration;
import redis.rdb.RdbLoader;
import redis.store.Storage;

public class Main {

	public static final int PORT = 6379;

	public static void main(String[] args) throws IOException {
		System.out.println("codecrafters build-your-own-redis");

		final var threadFactory = Thread.ofVirtual().factory();
		final var storage = new Storage();
		final var configuration = new Configuration();

		for (var index = 0; index < args.length; index += 2) {
			final var key = args[index].substring(2);
			final var value = args[index + 1];

			final var property = configuration.getProperty(key);
			if (property == null) {
				System.err.println("unknown property: %s".formatted(key));
			} else {
				property.set(value);
			}
		}

		final var directory = configuration.directory();
		final var databaseFilename = configuration.databaseFilename();
		if (directory.isSet() && databaseFilename.isSet()) {
			final var path = Paths.get(directory.get(), databaseFilename.get());
			RdbLoader.load(path, storage);
		}

		try (final var serverSocket = new ServerSocket(PORT)) {
			serverSocket.setReuseAddress(true);

			while (true) {
				final var socket = serverSocket.accept();
				final var client = new Client(socket, storage, configuration);

				final var thread = threadFactory.newThread(client);
				thread.start();
			}
		}
	}

}