package redis.type.stream;

import java.util.List;

import redis.type.RValue;
import redis.type.stream.identifier.UniqueIdentifier;

public record StreamEntry(
	UniqueIdentifier identifier,
	List<RValue> content
) {}