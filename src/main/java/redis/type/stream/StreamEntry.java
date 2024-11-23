package redis.type.stream;

import java.util.List;

import redis.type.RArray;
import redis.type.RString;
import redis.type.RValue;
import redis.type.stream.identifier.UniqueIdentifier;

public record StreamEntry(
	UniqueIdentifier identifier,
	List<RValue> content
) {

	public static RArray<RArray<RValue>> collectContent(final List<StreamEntry> entries) {
		return RArray.view(
			entries.stream()
				.map((entry) -> RArray.<RValue>of(
					RString.bulk(entry.identifier().toString()),
					RArray.view(entry.content())
				))
				.toList()
		);
	}

}