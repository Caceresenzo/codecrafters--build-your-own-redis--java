package redis;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import redis.client.Client;
import redis.command.CommandParser;
import redis.command.CommandResponse;
import redis.command.ParsedCommand;
import redis.configuration.Configuration;
import redis.store.Storage;
import redis.type.RArray;
import redis.type.RError;
import redis.type.RErrorException;
import redis.type.ROk;
import redis.type.RString;

@RequiredArgsConstructor
public class Redis {

	private final @Getter Storage storage;
	private final @Getter Configuration configuration;
	private final @Getter List<Client> replicas = Collections.synchronizedList(new ArrayList<>());
	private @Getter AtomicLong replicationOffset = new AtomicLong();
	private final CommandParser commandParser = new CommandParser();

	@SuppressWarnings("unchecked")
	public CommandResponse evaluate(Client client, Object value, long read) {
		try {
			if (value instanceof RArray array) {
				return execute(client, (RArray<RString>) array);
			}

			return new CommandResponse(new RError("ERR command be sent in an array"));
		} finally {
			final var offset = replicationOffset.addAndGet(read);
			log("offset: %s".formatted(offset));
		}
	}

	private CommandResponse execute(Client client, RArray<RString> arguments) {
		try {
			final var command = commandParser.parse(arguments);

			return doExecute(client, command);
		} catch (RErrorException exception) {
			return new CommandResponse(exception.getError());
		}
	}

	public CommandResponse execute(Client client, ParsedCommand command) {
		try {
			return doExecute(client, command);
		} catch (RErrorException exception) {
			return new CommandResponse(exception.getError());
		}
	}

	private CommandResponse doExecute(Client client, ParsedCommand parsedCommand) {
		final var command = parsedCommand.command();

		final var inTransaction = client.isInTransaction();
		if (inTransaction && command.isQueueable()) {
			client.queueCommand(parsedCommand);
			return new CommandResponse(ROk.QUEUED);
		}

		final var response = command.execute(this, client);
		if (command.isPropagatable()) {
			progagate(parsedCommand.raw());
		}

		return response;
	}

	public String getMasterReplicationId() {
		return configuration.masterReplicationId().argument(0, String.class).get();
	}

	public void progagate(RArray<RString> command) {
		final var payload = new CommandResponse(command);

		replicas.forEach((client) -> {
			client.command(payload);
		});
	}

	public static void log(String message) {
		System.out.println("[%d] [%s] %s".formatted(ProcessHandle.current().pid(), LocalDateTime.now(), message));
	}

	public static void error(String message) {
		System.err.println("[%d] [%s] %s".formatted(ProcessHandle.current().pid(), LocalDateTime.now(), message));
	}

}