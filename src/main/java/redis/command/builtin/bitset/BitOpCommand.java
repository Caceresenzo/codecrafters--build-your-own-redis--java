package redis.command.builtin.bitset;

import java.nio.charset.StandardCharsets;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RInteger;
import redis.type.RString;

public record BitOpCommand(
	Operation operation,
	RString destinationKey,
	RArray<RString> sourceKeys
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var result = redis.getStorage().compute(destinationKey, (__) -> {
			final var sourceValues = sourceKeys.stream()
				.map(redis.getStorage()::get)
				.map((value) -> value instanceof RString string ? string : RString.empty(false))
				.toList();

			final var longestLength = sourceValues.stream()
				.mapToInt(RString::length)
				.max()
				.orElse(0);

			final var resultBytes = new byte[longestLength];

			final var xSourceValue = sourceValues.get(0);
			final var ySourceValues = sourceValues.subList(1, sourceValues.size());

			final var yBytes = new byte[ySourceValues.size()];

			for (var index = 0; index < longestLength; index++) {
				final var xByte = getValueAtIndex(xSourceValue, index);
				fillYBytes(yBytes, ySourceValues, index);

				resultBytes[index] = operation.apply(xByte, yBytes);
			}

			return RString.bulk(new String(resultBytes, StandardCharsets.ISO_8859_1));
		});

		return new CommandResponse(RInteger.of(result.length()));
	}

	private void fillYBytes(byte[] yBytes, List<RString> ySourceValues, int charIndex) {
		for (var jndex = 0; jndex < ySourceValues.size(); jndex++) {
			final var ySourceValue = ySourceValues.get(jndex);
			yBytes[jndex] = getValueAtIndex(ySourceValue, charIndex);
		}
	}

	private byte getValueAtIndex(RString sourceValue, int index) {
		return (byte) (index < sourceValue.length() ? sourceValue.content().charAt(index) : 0);
	}

	@Override
	public boolean isPropagatable() {
		return true;
	}

	@Override
	public boolean isWriting() {
		return true;
	}

	@RequiredArgsConstructor
	@Getter
	@Accessors(fluent = true)
	public enum Operation {

		AND(Integer.MAX_VALUE) {

			@Override
			public byte apply(byte x, byte[] y) {
				for (byte value : y) {
					x &= value;
				}

				return x;
			}

		},

		OR(Integer.MAX_VALUE) {

			@Override
			public byte apply(byte x, byte[] y) {
				for (byte value : y) {
					x |= value;
				}

				return x;
			}

		},

		XOR(Integer.MAX_VALUE) {

			@Override
			public byte apply(byte x, byte[] y) {
				for (byte value : y) {
					x ^= value;
				}

				return x;
			}

		},

		NOT(1) {

			@Override
			public byte apply(byte x, byte[] y) {
				return (byte) ~x;
			}

		};

		private final int acceptedParameterCount;

		public abstract byte apply(byte x, byte[] y);

	}

}