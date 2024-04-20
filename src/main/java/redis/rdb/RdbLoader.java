package redis.rdb;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import lombok.RequiredArgsConstructor;
import redis.store.Cell;
import redis.store.Storage;

@RequiredArgsConstructor
public class RdbLoader {

	public static final byte OPCODE_END_OF_FILE = (byte) 0xFF;
	public static final byte OPCODE_DATABASE_SELECTOR = (byte) 0xFE;
	public static final byte OPCODE_EXPIRE_TIME = (byte) 0xFD;
	public static final byte OPCODE_EXPIRE_TIME_MILLISECONDS = (byte) 0xFC;
	public static final byte OPCODE_RESIZE_DATABASE = (byte) 0xFB;
	public static final byte OPCODE_AUXILIARY_FIELDS = (byte) 0xFA;

	public static final byte LENGTH_6BIT = 0b00;
	public static final byte LENGTH_14BIT = 0b01;
	public static final byte LENGTH_32BIT = 0b10;
	public static final byte LENGTH_SPECIAL = 0b11;

	public static final byte STRING_INTEGER_8BIT = 0;
	public static final byte STRING_INTEGER_16BIT = 1;
	public static final byte STRING_INTEGER_32BIT = 2;

	public static final byte STRING_VALUE_TYPE = 0;

	private final DataInputStream inputStream;
	private final Storage storage;

	public void load() throws IOException {
		parseMagic();

		final var version = parseVersion();
		log("version", version);

		Integer databaseNumber = null;

		while (true) {
			final var opcode = inputStream.readByte();
			if (opcode == OPCODE_END_OF_FILE) {
				log("end of file");
				break;
			}

			switch (opcode) {
				case OPCODE_AUXILIARY_FIELDS: {
					final var key = readString();
					final var value = readString();
					log("metadata", key, value);

					break;
				}

				case OPCODE_DATABASE_SELECTOR: {
					databaseNumber = readUnsignedByte();
					log("databaseNumber", databaseNumber);

					break;
				}

				case OPCODE_RESIZE_DATABASE: {
					final var hashTableSize = readLength();
					log("hashTableSize", hashTableSize);

					final var expireHashTableSize = readLength();
					log("expireHashTableSize", expireHashTableSize);

					break;
				}

				case OPCODE_EXPIRE_TIME: {
					throw new UnsupportedOperationException("unsupported OPCODE_EXPIRE_TIME");
				}

				case OPCODE_EXPIRE_TIME_MILLISECONDS:
				default: {
					if (databaseNumber == null) {
						throw new IllegalArgumentException("unexpected value: %x".formatted(opcode));
					}

					int valueType = opcode;
					long expiration = -1;

					if (opcode == OPCODE_EXPIRE_TIME_MILLISECONDS) {
						expiration = readUnsignedLong();
						valueType = readUnsignedByte();

						log("expiration", expiration);
					}

					final var key = readString();
					log("key", key);

					final var value = readValue(valueType);
					log("value", value);

					final var cell = new Cell<>(value, expiration);
					storage.put(key, cell);
				}
			}
		}
	}

	private Object readValue(int valueType) throws IOException {
		return switch (valueType) {
			case STRING_VALUE_TYPE -> readString();
			default -> throw new IllegalStateException("unsupported value type: " + valueType);
		};
	}

	public void parseMagic() throws IOException {
		final var magic = new String(inputStream.readNBytes(5), StandardCharsets.US_ASCII);

		if (!magic.equals("REDIS")) {
			throw new IllegalStateException("invalid magic: " + magic);
		}
	}

	public int parseVersion() throws IOException {
		final var version = new String(inputStream.readNBytes(4), StandardCharsets.US_ASCII);

		try {
			return Integer.parseInt(version);
		} catch (NumberFormatException exception) {
			throw new IllegalStateException("invalid version: " + version, exception);
		}
	}

	public void log(Object... content) {
		System.out.println("rdb: " + Arrays.toString(content));
	}

	public int readUnsignedByte() throws IOException {
		return Byte.toUnsignedInt(inputStream.readByte());
	}

	public long readUnsignedInteger() throws IOException {
		return Integer.toUnsignedLong(Integer.reverseBytes(inputStream.readInt()));
	}

	public long readUnsignedLong() throws IOException {
		return Long.reverseBytes(inputStream.readLong());
	}

	public int readLength() throws IOException {
		final var first = readUnsignedByte();

		final var encoding = first >> 6;
		final var value = first & 0b0011_1111;

		return switch (encoding) {
			case LENGTH_6BIT -> value;

			case LENGTH_14BIT -> {
				final var second = readUnsignedByte();
				yield (value << 8) | second;
			}

			/* bad special "number" encoding */
			case LENGTH_SPECIAL -> -(value + 1);

			default -> throw new IllegalStateException("unexpected length encoding: " + Integer.toBinaryString(encoding));
		};
	}

	public String readString() throws IOException {
		final var length = readLength();

		if (length < 0) {
			/* bad special "number" encoding */
			final var type = (-length) - 1;

			// TODO ByteOrder?
			return switch (type) {
				case STRING_INTEGER_8BIT -> String.valueOf(Byte.toUnsignedInt(inputStream.readByte()));
				case STRING_INTEGER_16BIT -> String.valueOf(Short.toUnsignedInt(Short.reverseBytes(inputStream.readShort())));
				case STRING_INTEGER_32BIT -> Integer.toUnsignedString(Integer.reverseBytes(inputStream.readInt()));
				default -> throw new IllegalArgumentException("unexpected length type: " + Integer.toBinaryString(type));
			};
		}

		final var content = inputStream.readNBytes(length);
		return new String(content, StandardCharsets.US_ASCII);
	}

	public void skip(int length) throws IOException {
		inputStream.skip(length);
	}

	public static void load(Path path, Storage storage) throws IOException {
		try (
			final var fileInputStream = Files.newInputStream(path);
			final var dataInputStream = new DataInputStream(fileInputStream);
		) {
			final var loader = new RdbLoader(dataInputStream, storage);
			loader.load();
		}
	}

}