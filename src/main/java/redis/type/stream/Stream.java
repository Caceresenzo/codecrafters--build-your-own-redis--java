package redis.type.stream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import redis.type.Error;
import redis.type.stream.identifier.Identifier;
import redis.type.stream.identifier.MillisecondsIdentifier;
import redis.type.stream.identifier.UniqueIdentifier;
import redis.type.stream.identifier.WildcardIdentifier;

public class Stream {

	private final List<StreamEntry> entries = new ArrayList<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Condition newDataCondition = lock.writeLock().newCondition();
	private UniqueIdentifier lastIdentifier;

	public UniqueIdentifier add(Identifier id, List<Object> content) {
		lock.writeLock().lock();

		try {
			final var unique = switch (id) {
				case MillisecondsIdentifier identifier -> getIdentifier(identifier.milliseconds());
				case UniqueIdentifier identifier -> identifier;
				case WildcardIdentifier identifier -> getIdentifier(System.currentTimeMillis());
			};

			if (!isUnique(unique)) {
				throw Error.xaddIdEqualOrSmaller().asException();
			}

			entries.add(new StreamEntry(unique, content));
			lastIdentifier = unique;

			newDataCondition.signalAll();

			return unique;
		} finally {
			lock.writeLock().unlock();
		}
	}

	public List<StreamEntry> range(Identifier from, Identifier to) {
		lock.readLock().lock();

		try {
			final var result = new ArrayList<StreamEntry>();

			var collecting = false;

			for (final var entry : entries) {
				final var identifier = entry.identifier();

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
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<StreamEntry> read(Identifier fromExclusive) {
		lock.readLock().lock();

		try {
			final var result = new ArrayList<StreamEntry>();

			var collecting = false;

			for (final var entry : entries) {
				final var identifier = entry.identifier();

				if (collecting) {
					result.add(entry);
				} else if (identifier.compareTo(fromExclusive) > 0) {
					collecting = true;
					result.add(entry);
				}
			}

			return result;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<StreamEntry> read(Identifier fromExclusive, Duration timeout) {
		if (fromExclusive == null) {
			fromExclusive = lastIdentifier;
		} else {
			final var results = read(fromExclusive);
			if (!results.isEmpty()) {
				return results;
			}
		}

		if (awaitNewData(timeout)) {
			return read(fromExclusive);
		}

		return null;
	}

	public boolean awaitNewData(Duration timeout) {
		lock.writeLock().lock();
		try {
			if (Duration.ZERO.equals(timeout)) {
				newDataCondition.await();
				return true;
			} else {
				return newDataCondition.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
			}
		} catch (InterruptedException ignored) {
			;
		} finally {
			lock.writeLock().unlock();
		}

		return false;
	}

	public UniqueIdentifier getIdentifier(long milliseconds) {
		lock.readLock().lock();

		try {
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
		} finally {
			lock.readLock().unlock();
		}
	}

	private boolean isUnique(UniqueIdentifier identifier) {
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