package redis.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Locked;
import redis.type.RArray;
import redis.type.RString;
import redis.util.Range;

public class SortedSet {

	private final Map<String, EntryValue> entries = new HashMap<>();
	private final List<String> sortedValues = new ArrayList<>();

	@Locked
	public boolean add(String value, double score) {
		var entry = entries.get(value);
		if (entry != null) {
			if (entry.score == score) {
				return false;
			}

			entry.score = score;

			computeIndexes();
			return false;
		} else {
			sortedValues.add(value);

			entry = new EntryValue(score);
			entries.put(value, entry);

			computeIndexes();
			return true;
		}
	}

	@Locked
	public Integer getRank(String value) {
		final var entry = entries.get(value);
		if (entry == null) {
			return null;
		}

		return entry.index;
	}

	public Double getScore(String value) {
		final var entry = entries.get(value);
		if (entry == null) {
			return null;
		}

		return entry.score;
	}

	@Locked
	public RArray<RString> range(int startIndex, int endIndex) {
		final var size = sortedValues.size();
		final var range = new Range(size, startIndex, endIndex);

		if (range.isEmpty()) {
			return RArray.empty();
		}

		final var rValues = range.subList(sortedValues)
			.stream()
			.map(RString::detect)
			.toList();

		return RArray.view(rValues);
	}

	@Locked
	public int cardinality() {
		return entries.size();
	}

	private void computeIndexes() {
		sortedValues.sort((leftKey, rightKey) -> {
			final var leftScore = entries.get(leftKey).score;
			final var rightScore = entries.get(rightKey).score;

			final var compare = Double.compare(leftScore, rightScore);
			if (compare != 0) {
				return compare;
			}

			return leftKey.compareTo(rightKey);
		});

		/* TODO could only update index after the changed entry */
		for (var index = 0; index < sortedValues.size(); index++) {
			entries.get(sortedValues.get(index)).index = index;
		}

		System.out.println(entries);
		System.out.println(sortedValues);
	}

	class EntryValue {

		double score;
		int index;

		private EntryValue(double score) {
			this.score = score;
		}

	}

}