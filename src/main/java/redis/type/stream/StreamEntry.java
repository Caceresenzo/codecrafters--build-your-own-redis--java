package redis.type.stream;

import java.util.List;

import redis.type.stream.identifier.UniqueIdentifier;

public record StreamEntry(
	UniqueIdentifier identifier,
	List<Object> content
) {

	

}