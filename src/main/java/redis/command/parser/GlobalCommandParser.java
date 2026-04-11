package redis.command.parser;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BiFunction;

import redis.command.Command;
import redis.command.builtin.core.ConfigCommand;
import redis.command.builtin.core.EchoCommand;
import redis.command.builtin.core.GetCommand;
import redis.command.builtin.core.IncrCommand;
import redis.command.builtin.core.InfoCommand;
import redis.command.builtin.core.KeysCommand;
import redis.command.builtin.core.PingCommand;
import redis.command.builtin.core.SetCommand;
import redis.command.builtin.core.TypeCommand;
import redis.command.builtin.geospatial.GeoAddCommand;
import redis.command.builtin.geospatial.GeoDistCommand;
import redis.command.builtin.geospatial.GeoPosCommand;
import redis.command.builtin.geospatial.GeoSearchCommand;
import redis.command.builtin.list.BLPopCommand;
import redis.command.builtin.list.LLenCommand;
import redis.command.builtin.list.LPopCommand;
import redis.command.builtin.list.LPushCommand;
import redis.command.builtin.list.LRangeCommand;
import redis.command.builtin.list.RPushCommand;
import redis.command.builtin.pubsub.PublishCommand;
import redis.command.builtin.pubsub.SubscribeCommand;
import redis.command.builtin.pubsub.UnsubscribeCommand;
import redis.command.builtin.replication.PSyncCommand;
import redis.command.builtin.replication.ReplConfCommand;
import redis.command.builtin.replication.WaitCommand;
import redis.command.builtin.sortedset.ZAddCommand;
import redis.command.builtin.sortedset.ZCardCommand;
import redis.command.builtin.sortedset.ZRangeCommand;
import redis.command.builtin.sortedset.ZRankCommand;
import redis.command.builtin.sortedset.ZRemCommand;
import redis.command.builtin.sortedset.ZScoreCommand;
import redis.command.builtin.stream.XAddCommand;
import redis.command.builtin.stream.XRangeCommand;
import redis.command.builtin.stream.XReadCommand;
import redis.command.builtin.transaction.DiscardCommand;
import redis.command.builtin.transaction.ExecCommand;
import redis.command.builtin.transaction.MultiCommand;
import redis.type.GeoCoordinate;
import redis.type.RArray;
import redis.type.RError;
import redis.type.RString;
import redis.type.stream.identifier.Identifier;
import redis.util.TriFunction;

public class GlobalCommandParser extends CommandParser {

	public GlobalCommandParser() {
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
		register("LRANGE", rangeCommand(LRangeCommand::new));
		register("LLEN", singleArgumentCommand(LLenCommand::new));
		register("LPOP", this::parseLPop);
		register("BLPOP", this::parseBLPop);

		register("SUBSCRIBE", singleArgumentCommand(SubscribeCommand::new));
		register("PUBLISH", doubleArgumentCommand(PublishCommand::new));
		register("UNSUBSCRIBE", singleArgumentCommand(UnsubscribeCommand::new));

		register("ZADD", this::parseZAdd);
		register("ZRANK", doubleArgumentCommand(ZRankCommand::new));
		register("ZRANGE", rangeCommand(ZRangeCommand::new));
		register("ZCARD", singleArgumentCommand(ZCardCommand::new));
		register("ZSCORE", doubleArgumentCommand(ZScoreCommand::new));
		register("ZREM", doubleArgumentCommand(ZRemCommand::new));

		register("GEOADD", this::parseGeoAdd);
		register("GEOPOS", this::parseGeoPos);
		register("GEODIST", tripleArgumentCommand(GeoDistCommand::new));
		register("GEOSEARCH", this::parseGeoSearch);

		register("ACL", new AclCommandParser());
	}

	private BiFunction<String, RArray<RString>, Command> rangeCommand(TriFunction<RString, Integer, Integer, Command> constructor) {
		return (name, arguments) -> {
			if (arguments.size() != 3) {
				throw wrongNumberOfArguments(name).asException();
			}

			final var key = arguments.get(0);
			final var startIndex = arguments.get(1).asInteger().getAsInt();
			final var endIndex = arguments.get(2).asInteger().getAsInt();

			return constructor.apply(key, startIndex, endIndex);
		};
	}

	private WaitCommand parseWait(String name, RArray<RString> arguments) {
		final var numberOfReplicas = arguments.get(0).asInteger().getAsInt();
		final var timeout = arguments.get(1).asInteger().getAsInt();

		return new WaitCommand(
			numberOfReplicas,
			Duration.ofSeconds(timeout)
		);
	}

	private XAddCommand parseXAdd(String name, RArray<RString> arguments) {
		if (arguments.size() <= 2) {
			throw wrongNumberOfArguments(name).asException();
		}

		final var key = arguments.get(0);
		final var id = Identifier.parse(arguments.get(1));

		final var keyValues = arguments.subList(2, arguments.size());

		return new XAddCommand(
			key,
			id,
			keyValues
		);
	}

	private XRangeCommand parseXRange(String name, RArray<RString> arguments) {
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

	private XReadCommand parseXRead(String name, RArray<RString> arguments) {
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

	private SetCommand parseSet(String name, RArray<RString> arguments) {
		if (arguments.size() != 2 && arguments.size() != 4) {
			throw wrongNumberOfArguments(name).asException();
		}

		final var key = arguments.get(0);
		final var value = arguments.get(1);
		var expiration = Optional.<Duration>empty();

		if (arguments.size() == 4) {
			final var unit = arguments.get(2);
			if (RString.equalsIgnoreCase(unit, "px")) {
				expiration = Optional.of(arguments.get(3).asDuration(ChronoUnit.MILLIS));
			} else if (RString.equalsIgnoreCase(unit, "ex")) {
				expiration = Optional.of(arguments.get(3).asDuration(ChronoUnit.SECONDS));
			} else {
				throw RError.syntax().asException();
			}
		}

		return new SetCommand(key, value, expiration);
	}

	private Command parseListPush(String name, RArray<RString> arguments) {
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

	private LPopCommand parseLPop(String name, RArray<RString> arguments) {
		final var argumentsSize = arguments.size();
		if (argumentsSize < 1 || argumentsSize > 2) {
			throw wrongNumberOfArguments(name).asException();
		}

		final var key = arguments.get(0);
		final var count = argumentsSize == 2
			? arguments.get(1).asInteger().getAsInt()
			: 1;

		return new LPopCommand(key, count);
	}

	private BLPopCommand parseBLPop(String name, RArray<RString> arguments) {
		final var argumentsSize = arguments.size();
		if (argumentsSize < 1 || argumentsSize > 2) {
			throw wrongNumberOfArguments(name).asException();
		}

		final var key = arguments.get(0);
		final var timeout = argumentsSize == 2
			? Duration.ofMillis((long) (arguments.get(1).asDouble() * 1000))
			: Duration.ZERO;

		if (timeout.isPositive()) {
			return new BLPopCommand(key, Optional.of(timeout));
		}

		return new BLPopCommand(key, Optional.empty());
	}

	private ZAddCommand parseZAdd(String name, RArray<RString> arguments) {
		final var argumentsSize = arguments.size();
		if (argumentsSize != 3) {
			throw wrongNumberOfArguments(name).asException();
		}

		final var key = arguments.get(0);
		final var score = arguments.get(1).asDouble();
		final var value = arguments.get(2);

		return new ZAddCommand(key, score, value);
	}

	private GeoAddCommand parseGeoAdd(String name, RArray<RString> arguments) {
		final var argumentsSize = arguments.size();
		if (argumentsSize != 4) {
			throw wrongNumberOfArguments(name).asException();
		}

		final var key = arguments.get(0);
		final var longitude = arguments.get(1).asDouble();
		final var latitude = arguments.get(2).asDouble();
		final var member = arguments.get(3);

		return new GeoAddCommand(key, new GeoCoordinate(longitude, latitude), member);
	}

	private GeoPosCommand parseGeoPos(String name, RArray<RString> arguments) {
		final var argumentsSize = arguments.size();
		if (argumentsSize < 2) {
			throw wrongNumberOfArguments(name).asException();
		}

		final var key = arguments.get(0);
		final var members = arguments.subList(1, argumentsSize);

		return new GeoPosCommand(key, members);
	}

	private GeoSearchCommand parseGeoSearch(String name, RArray<RString> arguments) {
		final var argumentsSize = arguments.size();
		if (argumentsSize < 7) {
			throw wrongNumberOfArguments(name).asException();
		}

		final var key = arguments.get(0);

		final var centerMode = arguments.get(1);
		if (!"FROMLONLAT".equals(centerMode.content())) {
			throw new RError("ERR only FROMLONLAT center mode is supported for GEOSEARCH command").asException();
		}

		final var longitude = arguments.get(2).asDouble();
		final var latitude = arguments.get(3).asDouble();

		final var regionMode = arguments.get(4);
		if (!"BYRADIUS".equals(regionMode.content())) {
			throw new RError("ERR only BYRADIUS region mode is supported for GEOSEARCH command").asException();
		}

		final var radius = arguments.get(5).asDouble();

		final var radiusUnit = arguments.get(6);
		if (!"m".equals(radiusUnit.content())) {
			throw new RError("ERR only meter (m) is supported for GEOSEARCH command").asException();
		}

		return new GeoSearchCommand(
			key,
			new GeoCoordinate(longitude, latitude),
			radius
		);
	}

}