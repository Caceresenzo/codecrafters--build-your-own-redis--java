package redis.type.stream;

import java.util.ArrayList;
import java.util.List;

import redis.type.Error;
import redis.type.ErrorException;
import redis.type.stream.identifier.Identifier;
import redis.type.stream.identifier.MillisecondsIdentifier;
import redis.type.stream.identifier.UniqueIdentifier;
import redis.type.stream.identifier.WildcardIdentifier;

public class Stream {

	private final List<StreamEntry> entries = new ArrayList<>();

	public UniqueIdentifier add(Identifier id, List<Object> content) {
		final var unique = switch (id) {
			case MillisecondsIdentifier identifier -> getIdentifier(identifier.milliseconds());
			case UniqueIdentifier identifier -> identifier;
			case WildcardIdentifier identifier -> getIdentifier(System.currentTimeMillis());
		};

		if (!isUnique(unique)) {
			throw new ErrorException(Error.xaddIdEqualOrSmaller());
		}

		entries.add(new StreamEntry(unique, content));

		return unique;
	}

	public UniqueIdentifier getIdentifier(long milliseconds) {
		if (!entries.isEmpty()) {
			final var last = entries.getLast().identifier();

			if (last.milliseconds() == milliseconds) {
				return new UniqueIdentifier(milliseconds, last.sequenceNumber() + 1);
			}
		}

		return new UniqueIdentifier(milliseconds, 0l);
	}

	public boolean isUnique(UniqueIdentifier identifier) {
		if (entries.isEmpty()) {
			return true;
		}

		final var last = entries.getLast().identifier();
		if (last.milliseconds() > identifier.milliseconds()) {
			return false;
		}

		if (last.milliseconds() == identifier.milliseconds()) {
			return last.sequenceNumber() < identifier.sequenceNumber();
		}

		return true;
	}

}