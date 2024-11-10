package redis;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import redis.client.Client;
import redis.client.ReplicaClient;
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

			final var option = configuration.option(key);
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

		for (final var option : configuration.options()) {
			final var arguments = option.arguments()
				.stream()
				.map((argument) -> "%s=`%s`".formatted(argument.name(), argument.get()))
				.collect(Collectors.joining(", "));

			System.out.println("configuration: %s(%s)".formatted(option.name(), arguments));
		}

		final var isSlave = configuration.isSlave();
		System.out.println("configuration: isSlave=%s".formatted(isSlave));

		final var redis = new Redis(storage, configuration);

		if (isSlave) {
			connectToMaster(redis);
		} else {
			final var directory = configuration.directory().pathArgument();
			final var databaseFilename = configuration.databaseFilename().pathArgument();
			if (directory.isSet() && databaseFilename.isSet()) {
				final var path = Paths.get(directory.get(), databaseFilename.get());

				if (Files.exists(path)) {
					RdbLoader.load(path, storage);
				}
			}
		}

		final var port = configuration.port().argument(0, Integer.class).get();
		System.out.println("port: %s".formatted(port));

		try (final var serverSocket = new ServerSocket(port)) {
			serverSocket.setReuseAddress(true);

			while (true) {
				final var socket = serverSocket.accept();
				final var client = new Client(socket, redis);

				final var thread = threadFactory.newThread(client);
				thread.start();
			}
		}
	}

	@SneakyThrows
	public static void connectToMaster(Redis redis) {
		final var replicaOf = redis.getConfiguration().replicaOf();
		final var host = replicaOf.host();
		final var port = replicaOf.port();

		System.out.println("replica: connect to master %s:%s".formatted(host, port));

		final var socket = new Socket(host, port);
		Thread.ofVirtual().start(new ReplicaClient(socket, redis));
	}

}