package redis.command.parser;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import redis.command.Command;
import redis.command.ParsedCommand;
import redis.type.RArray;
import redis.type.RError;
import redis.type.RString;
import redis.util.TriFunction;

public abstract class CommandParser {

	private final Map<String, BiFunction<String, RArray<RString>, Command>> parsers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private final Map<String, CommandParser> subParsers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	public void register(String name, BiFunction<String, RArray<RString>, Command> parser) {
		parsers.put(name, parser);
	}

	public void register(String name, CommandParser subParser) {
		subParsers.put(name, subParser);
	}

	public ParsedCommand parse(RArray<RString> arguments) {
		if (arguments.isEmpty()) {
			throw new RError("ERR command array is empty").asException();
		}

		final var name = arguments.getFirst().content();

		final var parser = parsers.get(name);
		if (parser != null) {
			final var command = parser.apply(name, arguments.subList(1, arguments.size()));
			return new ParsedCommand(arguments, command);
		}

		final var subParser = subParsers.get(name);
		if (subParser != null) {
			return subParser.parse(arguments.subList(1, arguments.size()));
		}

		throw new RError("ERR unknown '%s' command".formatted(name)).asException();
	}

	protected BiFunction<String, RArray<RString>, Command> noArgumentCommand(Supplier<Command> constructor) {
		return (name, arguments) -> {
			if (!arguments.isEmpty()) {
				throw wrongNumberOfArguments(name).asException();
			}

			return constructor.get();
		};
	}

	protected BiFunction<String, RArray<RString>, Command> singleArgumentCommand(Function<RString, Command> constructor) {
		return (name, arguments) -> {
			if (arguments.size() != 1) {
				throw wrongNumberOfArguments(name).asException();
			}

			final var first = arguments.getFirst();
			return constructor.apply(first);
		};
	}

	protected BiFunction<String, RArray<RString>, Command> doubleArgumentCommand(BiFunction<RString, RString, Command> constructor) {
		return (name, arguments) -> {
			if (arguments.size() != 2) {
				throw wrongNumberOfArguments(name).asException();
			}

			final var first = arguments.getFirst();
			final var second = arguments.get(1);

			return constructor.apply(first, second);
		};
	}

	protected BiFunction<String, RArray<RString>, Command> tripleArgumentCommand(TriFunction<RString, RString, RString, Command> constructor) {
		return (name, arguments) -> {
			if (arguments.size() != 3) {
				throw wrongNumberOfArguments(name).asException();
			}

			final var first = arguments.getFirst();
			final var second = arguments.get(1);
			final var third = arguments.get(2);

			return constructor.apply(first, second, third);
		};
	}

	protected RError wrongNumberOfArguments(String name) {
		return new RError("ERR wrong number of arguments for '%s' command".formatted(name));
	}

}