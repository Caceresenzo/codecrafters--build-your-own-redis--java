package redis.command;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import redis.command.builtin.core.ConfigCommand;
import redis.command.builtin.core.EchoCommand;
import redis.command.builtin.core.GetCommand;
import redis.command.builtin.core.IncrCommand;
import redis.command.builtin.core.InfoCommand;
import redis.command.builtin.core.KeysCommand;
import redis.command.builtin.core.PingCommand;
import redis.command.builtin.core.SetCommand;
import redis.command.builtin.core.TypeCommand;
import redis.command.builtin.list.LLenCommand;
import redis.command.builtin.list.LPopCommand;
import redis.command.builtin.list.LPushCommand;
import redis.command.builtin.list.LRangeCommand;
import redis.command.builtin.list.RPushCommand;
import redis.command.builtin.replication.PSyncCommand;
import redis.command.builtin.replication.ReplConfCommand;
import redis.command.builtin.replication.WaitCommand;
import redis.command.builtin.stream.XAddCommand;
import redis.command.builtin.stream.XRangeCommand;
import redis.command.builtin.stream.XReadCommand;
import redis.command.builtin.transaction.DiscardCommand;
import redis.command.builtin.transaction.ExecCommand;
import redis.command.builtin.transaction.MultiCommand;
import redis.type.RArray;
import redis.type.RError;
import redis.type.RString;
import redis.type.RValue;
import redis.type.stream.identifier.Identifier;

public class CommandParser {

	private final Map<String, BiFunction<String, List<RString>, Command>> parsers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	public CommandParser() {
		register("PSYNC", (__, ___) -> new PSyncCommand());
		register("REPLCONF", doubleArgumentCommand(ReplConfCommand::new));
		register("WAIT", this::parseWait);

		register("XADD", this::parseXAdd);
		register("XRANGE", this::parseXRange);
		register("XREAD", this::parseXRead);

		register("DISCARD", noArgumentCommand(DiscardCommand::new));
		register("EXEC", noArgumentCommand(ExecCommand::new));
		register("MULTI", noArgumentCommand(MultiCommand::new));

		register("CONFIG", doubleArgumentCommand(ConfigCommand::new));
		register("ECHO", singleArgumentCommand(EchoCommand::new));
		register("GET", singleArgumentCommand(GetCommand::new));
		register("INCR", singleArgumentCommand(IncrCommand::new));
		register("INFO", singleArgumentCommand(InfoCommand::new));
		register("KEYS", singleArgumentCommand(KeysCommand::new));
		register("PING", noArgumentCommand(PingCommand::new));
		register("SET", this::parseSet);
		register("TYPE", singleArgumentCommand(TypeCommand::new));

		register("RPUSH", this::parseListPush);
		register("LPUSH", this::parseListPush);
		register("LRANGE", this::parseLRange);
		register("LLEN", singleArgumentCommand(LLenCommand::new));
		register("LPOP", this::parseLPop);
	}

	public void register(String name, BiFunction<String, List<RString>, Command> parser) {
		parsers.put(name, parser);
	}

	public ParsedCommand parse(RArray<RString> arguments) {
		if (arguments.isEmpty()) {
			throw new RError("ERR command array is empty").asException();
		}

		final var name = arguments.getFirst().toUpperCase();

		final var parser = parsers.get(name);
		if (parser == null) {
			throw new RError("ERR unknown '%s' command".formatted(name)).asException();
		}

		final var command = parser.apply(name, arguments.subList(1, arguments.size()));

		return new ParsedCommand(arguments, command);
	}

	private BiFunction<String, List<RString>, Command> noArgumentCommand(Supplier<Command> constructor) {
		return (name, arguments) -> {
			if (!arguments.isEmpty()) {
				throw wrongNumberOfArguments(name).asException();
			}

			return constructor.get();
		};
	}

	private BiFunction<String, List<RString>, Command> singleArgumentCommand(Function<RString, Command> constructor) {
		return (name, arguments) -> {
			if (arguments.size() != 1) {
				throw wrongNumberOfArguments(name).asException();
			}

			final var first = arguments.getFirst();
			return constructor.apply(first);
		};
	}

	private BiFunction<String, List<RString>, Command> doubleArgumentCommand(BiFunction<RString, RString, Command> constructor) {
		return (name, arguments) -> {
			if (arguments.size() != 2) {
				throw wrongNumberOfArguments(name).asException();
			}

			final var first = arguments.getFirst();
			final var second = arguments.get(1);

			return constructor.apply(first, second);
		};
	}

	private WaitCommand parseWait(String name, List<RString> arguments) {
		final var numberOfReplicas = arguments.get(0).asInteger().getAsInt();
		final var timeout = arguments.get(1).asInteger().getAsInt();

		return new WaitCommand(
			numberOfReplicas,
			Duration.ofSeconds(timeout)
		);
	}

	private XAddCommand parseXAdd(String name, List<RString> arguments) {
		if (arguments.size() <= 2) {
			throw wrongNumberOfArguments(name).asException();
		}

		final var key = arguments.get(0);
		final var id = Identifier.parse(arguments.get(1));

		@SuppressWarnings({ "rawtypes", "unchecked" })
		final var keyValues = (RArray<RValue>) (RArray) RArray.view(arguments.subList(2, arguments.size()));

		return new XAddCommand(
			key,
			id,
			keyValues
		);
	}

	private XRangeCommand parseXRange(String name, List<RString> arguments) {
		if (arguments.size() <= 2) {
			throw wrongNumberOfArguments(name).asException();
		}

		final var key = arguments.get(0);
		final var fromId = Identifier.parse(arguments.get(1));
		final var toId = Identifier.parse(arguments.get(2));

		return new XRangeCommand(
			key,
			fromId,
			toId
		);
	}

	private XReadCommand parseXRead(String name, List<RString> arguments) {
		if (arguments.size() <= 2) {
			throw wrongNumberOfArguments(name).asException();
		}

		final var queries = new ArrayList<XReadCommand.Query>();
		var timeout = Optional.<Duration>empty();

		final var size = arguments.size();
		for (var index = 0; index < size; ++index) {
			var element = arguments.get(index);

			if (RString.equalsIgnoreCase(element, "block")) {
				++index;

				element = arguments.get(index);
				timeout = Optional.of(Duration.ofMillis((arguments.get(index)).asLong()));

				continue;
			}

			if (RString.equalsIgnoreCase(element, "streams")) {
				++index;

				final var remaining = size - index;
				final var offset = remaining / 2;

				for (var jndex = 0; jndex < offset; ++jndex) {
					final var key = arguments.get(index + jndex);

					element = arguments.get(index + offset + jndex);
					final var identifier = timeout != null && RString.equalsIgnoreCase(element, "$")
						? null
						: Identifier.parse(arguments.get(index + offset + jndex));

					queries.add(new XReadCommand.Query(key, identifier));
				}

				break;
			}
		}

		return new XReadCommand(
			queries,
			timeout
		);
	}

	private SetCommand parseSet(String name, List<RString> arguments) {
		if (arguments.size() != 2 && arguments.size() != 4) {
			throw wrongNumberOfArguments(name).asException();
		}

		final var key = arguments.get(0);
		final var value = arguments.get(1);
		var expiration = Optional.<Duration>empty();

		if (arguments.size() == 4) {
			final var px = arguments.get(2);
			if (!RString.equalsIgnoreCase(px, "px")) {
				throw RError.syntax().asException();
			}

			expiration = Optional.of(arguments.get(3).asDuration(ChronoUnit.MILLIS));
		}

		return new SetCommand(key, value, expiration);
	}

	private Command parseListPush(String name, List<RString> arguments) {
		if (arguments.size() < 2) {
			throw wrongNumberOfArguments(name).asException();
		}

		final var key = arguments.get(0);
		final var values = RArray.copy(arguments.subList(1, arguments.size()));

		if (Character.toUpperCase(name.charAt(0)) == 'L') {
			return new LPushCommand(key, values);
		}

		return new RPushCommand(key, values);
	}

	private LRangeCommand parseLRange(String name, List<RString> arguments) {
		if (arguments.size() != 3) {
			throw wrongNumberOfArguments(name).asException();
		}

		final var key = arguments.get(0);
		final var startIndex = arguments.get(1).asInteger().getAsInt();
		final var endIndex = arguments.get(2).asInteger().getAsInt();

		return new LRangeCommand(key, startIndex, endIndex);
	}

	private LPopCommand parseLPop(String name, List<RString> arguments) {
		final var argumentsSize = arguments.size();
		if (argumentsSize < 1 || argumentsSize > 2) {
			throw wrongNumberOfArguments(name).asException();
		}

		final var key = arguments.get(0);

		return new LPopCommand(key);
	}

	private RError wrongNumberOfArguments(String name) {
		return new RError("ERR wrong number of arguments for '%s' command".formatted(name));
	}

}