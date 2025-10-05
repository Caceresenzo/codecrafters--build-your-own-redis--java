package redis.command.builtin.stream;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RNil;
import redis.type.RString;
import redis.type.stream.Stream;
import redis.type.stream.StreamEntry;
import redis.type.stream.identifier.Identifier;

public record XReadCommand(
	List<Query> queries,
	Optional<Duration> timeout
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		if (timeout.isPresent()) {
			final var query = queries.getFirst();

			final var key = query.key();
			final var stream = (Stream) redis.getStorage().get(key);
			final var entries = stream.read(query.identifier(), timeout.get());

			if (entries == null) {
				return new CommandResponse(RNil.ARRAY);
			}

			return new CommandResponse(RArray.of(RArray.of(
				RString.bulk(key),
				StreamEntry.collectContent(entries)
			)));
		}

		return new CommandResponse(RArray.view(
			queries.stream()
				.map((query) -> {
					final var key = query.key();
					final var stream = (Stream) redis.getStorage().get(key);
					final var entries = stream.read(query.identifier());

					return RArray.of(
						RString.simple(key),
						StreamEntry.collectContent(entries)
					);
				})
				.toList()
		));
	}

	public static record Query(
		RString key,
		Identifier identifier
	) {}

}