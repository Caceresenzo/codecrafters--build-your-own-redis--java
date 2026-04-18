package redis;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;

import lombok.SneakyThrows;
import redis.aof.AppendOnlyFileManager;
import redis.client.ReplicaClient;
import redis.client.SocketClient;
import redis.configuration.Configuration;
import redis.rdb.RdbLoader;
import redis.store.Storage;

public class Main {

	@SneakyThrows
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

			option.set(args[index + 1]);

			++index;
		}

		for (final var option : configuration.options()) {
			System.out.println("configuration: %s=%s".formatted(option.getName(), option.getValue()));
		}

		final var isSlave = configuration.isSlave();
		System.out.println("configuration: isSlave=%s".formatted(isSlave));

		final var redis = new Redis(configuration, storage);
		final var directory = configuration.directory().getValue();

		if (isSlave) {
			connectToMaster(redis);
		} else {
			final var databaseFilename = configuration.databaseFilename().getValue();

			if (databaseFilename != null) {
				final var path = directory.resolve(databaseFilename);

				if (Files.exists(path)) {
					RdbLoader.load(path, storage);
				}
			}

			if (configuration.appendOnly().isYes()) {
				final var manager = new AppendOnlyFileManager(
					directory.resolve(configuration.appendDirectoryName().getValue()),
					configuration.appendFileName().getValue()
				);

				manager.initialize();

				redis.setAppendOnlyFileManager(manager);
			}
		}

		final var port = configuration.port().getValue();
		System.out.println("port: %s".formatted(port));

		try (final var serverSocket = new ServerSocket(port)) {
			serverSocket.setReuseAddress(true);

			while (true) {
				final var socket = serverSocket.accept();
				final var client = new SocketClient(socket, redis);

				final var thread = threadFactory.newThread(client);
				thread.start();

				// FIXME codecrafters tester is failing because of out of order...
				// Thread.sleep(100l);
			}
		}
	}

	@SneakyThrows
	public static void connectToMaster(Redis redis) {
		final var replicaOf = redis.getConfiguration().replicaOf().getValue();

		System.out.println("replica: connect to master %s:%s".formatted(replicaOf.getAddress().getCanonicalHostName(), replicaOf.getPort()));

		final var socket = new Socket(replicaOf.getAddress(), replicaOf.getPort());
		Thread.ofVirtual().start(new ReplicaClient(socket, redis));
	}

}