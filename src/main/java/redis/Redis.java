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
import java.util.function.Supplier;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import redis.aof.AppendOnlyFileManager;
import redis.client.Client;
import redis.client.SocketClient;
import redis.command.CommandResponse;
import redis.command.ParsedCommand;
import redis.command.parser.GlobalCommandParser;
import redis.configuration.Configuration;
import redis.store.PubSub;
import redis.store.Storage;
import redis.type.RArray;
import redis.type.RError;
import redis.type.RErrorException;
import redis.type.ROk;
import redis.type.RString;
import redis.type.RValue;
import redis.user.UserRepository;

@RequiredArgsConstructor
public class Redis {

	private final @Getter Configuration configuration;
	private final @Getter Storage storage;
	private final @Getter PubSub pubSub = new PubSub();
	private final @Getter List<SocketClient> replicas = Collections.synchronizedList(new ArrayList<>());
	private final @Getter AtomicLong replicationOffset = new AtomicLong();
	private final GlobalCommandParser commandParser = new GlobalCommandParser();
	private final ReentrantLock lock = new ReentrantLock(true);
	private final Map<String, Condition> condititions = new ConcurrentHashMap<>();
	private final @Getter UserRepository userRepository = new UserRepository();
	private @Setter AppendOnlyFileManager appendOnlyFileManager;
	private boolean running;

	public void start() {
		if (appendOnlyFileManager != null) {
			appendOnlyFileManager.load(this);
		}

		running = true;
	}

	@SuppressWarnings("unchecked")
	public CommandResponse evaluate(Client client, Object value, long read, Supplier<byte[]> commandBytes) {
		try {
			if (value instanceof RArray array) {
				return execute(client, array, commandBytes);
			}

			return new CommandResponse(new RError("ERR command be sent in an array"));
		} finally {
			final var offset = replicationOffset.addAndGet(read);
			log("offset: %s".formatted(offset));
		}
	}

	private CommandResponse execute(Client client, RArray<RString> arguments, Supplier<byte[]> commandBytes) {
		try {
			final var command = commandParser.parse(arguments);
			final var result = doExecute(client, command);

			if (running && command.isWriting() && appendOnlyFileManager != null) {
				appendOnlyFileManager.log(commandBytes.get());
			}

			return result;
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
			if (command.isAuthenticationRequired() && socketClient.getUser() == null) {
				throw RError.authenticationRequired().asException();
			}

			if (socketClient.isInTransaction() && command.isQueueable()) {
				socketClient.queueCommand(parsedCommand);
				return new CommandResponse(ROk.QUEUED);
			}

			if (pubSub.isSubscribed(socketClient) && !command.isPubSub()) {
				throw RError.invalidCommandInSubscribedContextFormat(command.getName()).asException();
			}
		}

		final var response = command.execute(this, client);
		// System.out.printf("Redis.doExecute() response=%s command=%s replicas=%s %n", response, command, replicas);
		if (command.isPropagatable()) {
			progagate(parsedCommand.raw());
		}

		return response;
	}

	public String getMasterReplicationId() {
		return configuration.masterReplicationId().getValue();
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