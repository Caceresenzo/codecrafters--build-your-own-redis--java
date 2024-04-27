package redis.type.stream;

import java.util.ArrayList;
import java.util.List;

import redis.type.Error;
import redis.type.stream.identifier.Identifier;
import redis.type.stream.identifier.MillisecondsIdentifier;
import redis.type.stream.identifier.UniqueIdentifier;
import redis.type.stream.identifier.WildcardIdentifier;

public class Stream {

	private final List<StreamEntry> entries = new ArrayList<>();

	public synchronized UniqueIdentifier add(Identifier id, List<Object> content) {
		final var unique = switch (id) {
			case MillisecondsIdentifier identifier -> getIdentifier(identifier.milliseconds());
			case UniqueIdentifier identifier -> identifier;
			case WildcardIdentifier identifier -> getIdentifier(System.currentTimeMillis());
		};

		if (!isUnique(unique)) {
			throw Error.xaddIdEqualOrSmaller().asException();
		}

		entries.add(new StreamEntry(unique, content));

		return unique;
	}

	public synchronized List<StreamEntry> range(Identifier from, Identifier to) {
		final var result = new ArrayList<StreamEntry>();

		var collecting = false;

		for (final var entry : entries) {
			final var identifier = entry.identifier();

			//			System.out.println("IN ?  identifier " + identifier + " from " + from + " compareTo " + identifier.compareTo(from));
			//			System.out.println("OUT?  identifier " + identifier + " to " + to + " compareTo " + identifier.compareTo(to));

			if (identifier.compareTo(to) > 0) {
				break;
			}

			if (collecting) {
				result.add(entry);
			} else if (identifier.compareTo(from) >= 0) {
				collecting = true;
				result.add(entry);
			}
		}

		return result;
	}

	public UniqueIdentifier getIdentifier(long milliseconds) {
		if (!entries.isEmpty()) {
			final var last = entries.getLast().identifier();

			if (last.milliseconds() == milliseconds) {
				return new UniqueIdentifier(milliseconds, last.sequenceNumber() + 1);
			}
		}

		final var sequenceNumber = milliseconds == 0
			? 1l
			: 0l;

		return new UniqueIdentifier(
			milliseconds,
			sequenceNumber
		);
	}

	public boolean isUnique(UniqueIdentifier identifier) {
		if (identifier.compareTo(UniqueIdentifier.MIN) < 0) {
			throw Error.xaddIdGreater00().asException();
		}

		if (entries.isEmpty()) {
			return true;
		}

		final var last = entries.getLast().identifier();
		return identifier.compareTo(last) > 0;
	}

}