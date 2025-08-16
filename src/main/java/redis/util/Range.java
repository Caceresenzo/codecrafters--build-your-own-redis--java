package redis.util;

import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import redis.type.RArray;
import redis.type.RValue;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Range {

	private final int startIndex;
	private final int endIndex;

	public Range(int arraySize, int startIndex, int endIndex) {
		this.startIndex = scope(startIndex, arraySize);
		this.endIndex = scope(endIndex, arraySize);
	}

	public boolean isEmpty() {
		return startIndex > endIndex;
	}

	public int getInclusiveStartIndex() {
		return startIndex;
	}

	public int getIncludeEndIndex() {
		return endIndex + 1;
	}

	public <T extends RValue> RArray<T> subList(RArray<T> list) {
		return RArray.view(subList(list.items()));
	}

	public <T> List<T> subList(List<T> list) {
		return list.subList(getInclusiveStartIndex(), getIncludeEndIndex());
	}

	private static int scope(int index, int size) {
		if (index < 0) {
			index += size;
		}

		return Math.clamp(index, 0, size - 1);
	}

}