package redis;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;

import redis.configuration.Configuration;
import redis.rdb.RdbLoader;
import redis.store.Storage;

public class Main {

	public static void main(String[] args) throws IOException {
		System.out.println("codecrafters build-your-own-redis");

		final var threadFactory = Thread.ofVirtual().factory();
		final var storage = new Storage();
		final var configuration = new Configuration();

		for (var index = 0; index < args.length; ++index) {
			final var key = args[index].substring(2);

			final var option = configuration.getOption(key);
			if (option == null) {
				System.err.println("unknown property: %s".formatted(key));
				continue;
			}

			final var argumentsCount = option.argumentsCount();
			for (var jndex = 0; jndex < argumentsCount; ++jndex) {
				final var argumentValue = args[index + 1 + jndex];
				final var argument = option.argument(jndex);

				argument.set(argumentValue);
			}

			index += argumentsCount;
		}

		final var directory = configuration.directory().pathArgument();
		final var databaseFilename = configuration.databaseFilename().pathArgument();
		if (directory.isSet() && databaseFilename.isSet()) {
			final var path = Paths.get(directory.get(), databaseFilename.get());

			if (Files.exists(path)) {
				RdbLoader.load(path, storage);
			}
		}

		final var port = configuration.port().argument(0, Integer.class).get();
		System.out.println("port: %s".formatted(port));

		try (final var serverSocket = new ServerSocket(port)) {
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