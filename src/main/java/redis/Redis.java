package redis;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import redis.client.Client;
import redis.client.SocketClient;
import redis.command.CommandParser;
import redis.command.CommandResponse;
import redis.command.ParsedCommand;
import redis.configuration.Configuration;
import redis.store.PubSub;
import redis.store.Storage;
import redis.type.RArray;
import redis.type.RError;
import redis.type.RErrorException;
import redis.type.ROk;
import redis.type.RString;
import redis.type.RValue;

@RequiredArgsConstructor
public class Redis {

	private final @Getter Configuration configuration;
	private final @Getter Storage storage;
	private final @Getter PubSub pubSub = new PubSub();
	private final @Getter List<SocketClient> replicas = Collections.synchronizedList(new ArrayList<>());
	private @Getter AtomicLong replicationOffset = new AtomicLong();
	private final CommandParser commandParser = new CommandParser();
	private final ReentrantLock lock = new ReentrantLock(true);
	private final Map<String, Condition> condititions = new ConcurrentHashMap<>();

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

		if (client instanceof SocketClient socketClient) {
			if (socketClient.isInTransaction() && command.isQueueable()) {
				socketClient.queueCommand(parsedCommand);
				return new CommandResponse(ROk.QUEUED);
			}

			if (pubSub.isSubscribed(socketClient) && !command.isPubSub()) {
				throw RError.invalidCommandInSubscribedContextFormat(command.getName()).asException();
			}
		}

		final var response = command.execute(this, client);
		System.out.printf("Redis.doExecute() response=%s command=%s %n", response, command);
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

	public RValue awaitKey(RString key, Optional<Duration> timeout) {
		final var condition = condititions.computeIfAbsent(key.content(), (__) -> lock.newCondition());

		try {
			lock.lock();

			if (timeout.isPresent()) {
				final var found = condition.await(timeout.get().toMillis(), TimeUnit.MILLISECONDS);

				if (!found) {
					return null;
				}
			} else {
				condition.await();
			}

			return (RValue) storage.get(key);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		} finally {
			lock.unlock();
		}

		return null;
	}

	public void notifyKey(RString key) {
		final var condition = condititions.get(key.content());
		if (condition == null) {
			return;
		}

		lock.lock();
		try {
			condition.signal();
		} finally {
			lock.unlock();
		}
	}

	public static void log(String message) {
		System.out.println("[%d] [%s] %s".formatted(ProcessHandle.current().pid(), LocalDateTime.now(), message));
	}

	public static void error(String message) {
		System.err.println("[%d] [%s] %s".formatted(ProcessHandle.current().pid(), LocalDateTime.now(), message));
	}

}