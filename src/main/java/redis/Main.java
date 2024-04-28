package redis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import redis.configuration.Configuration;
import redis.configuration.common.PortArgument;
import redis.configuration.common.RemoteOption;
import redis.rdb.RdbLoader;
import redis.serial.Deserializer;
import redis.serial.Serializer;
import redis.store.Storage;
import redis.type.BulkString;

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

		if (isSlave) {
			connectToMaster(
				configuration.replicaOf(),
				configuration.port().argument(0, Integer.class).get()
			);
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
				final var client = new Client(socket, storage, configuration);

				final var thread = threadFactory.newThread(client);
				thread.start();
			}
		}
	}

	@SneakyThrows
	public static void connectToMaster(RemoteOption replicaOf, int selfPort) {
		final var host = replicaOf.hostArgument().get();
		final var port = replicaOf.portArgument().get();
		
		System.out.println("replica: connect to master %s:%s".formatted(host, port));
		
		try (
			final var socket = new Socket(host, port)
		) {
			final var inputStream = socket.getInputStream();
			final var outputStream = socket.getOutputStream();

			final var deserializer = new Deserializer(inputStream);
			final var serializer = new Serializer(outputStream);

			{
				System.out.println("replica: send PING");
				serializer.write(List.of(new BulkString("PING")));

				final var response = deserializer.read();
				System.out.println("replica: received %s".formatted(response));
			}

			{
				System.out.println("replica: send REPLCONF listening-port");
				serializer.write(List.of(
					new BulkString("REPLCONF"),
					new BulkString("listening-port"),
					new BulkString(String.valueOf(selfPort))
				));

				final var response = deserializer.read();
				System.out.println("replica: received %s".formatted(response));
			}

			{
				System.out.println("replica: send REPLCONF capa psync2");
				serializer.write(List.of(
					new BulkString("REPLCONF"),
					new BulkString("capa"),
					new BulkString("psync2")
				));

				final var response = deserializer.read();
				System.out.println("replica: received %s".formatted(response));
			}
		}
	}

}