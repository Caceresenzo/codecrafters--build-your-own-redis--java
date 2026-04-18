package redis.aof;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import lombok.SneakyThrows;
import redis.Redis;
import redis.serial.Deserializer;

public class AppendOnlyFileManager {

	private final Path directory;
	private final String baseFileName;

	private OutputStream currentWriter;

	public AppendOnlyFileManager(Path directory, String filename) {
		this.directory = directory;
		this.baseFileName = filename;
	}

	@SneakyThrows
	public void initialize() {
		Files.createDirectories(directory);

		final var firstAppendFileName = baseFileName + ".1.incr.aof";
		final var firstAppendFile = directory.resolve(firstAppendFileName);
		if (!Files.exists(firstAppendFile)) {
			Files.createFile(firstAppendFile);
		}

		final var manifestFile = getManifestFile();
		if (!Files.exists(manifestFile)) {
			final var firstRecord = new ManifestRecord(firstAppendFileName, 1, ManifestRecord.Type.INCREMENTAL);
			Files.writeString(manifestFile, firstRecord.toString() + "\n");
		}

		currentWriter = Files.newOutputStream(firstAppendFile, StandardOpenOption.APPEND);
	}

	public Path getManifestFile() {
		return directory.resolve(baseFileName + ".manifest");
	}

	@SneakyThrows
	public void load(Redis redis) {
		for (final var line : Files.readAllLines(getManifestFile())) {
			final var record = ManifestRecord.parse(line);

			if (record.type() == ManifestRecord.Type.INCREMENTAL) {
				loadIncrementalRecord(redis, record);
			}
		}
	}

	private void loadIncrementalRecord(Redis redis, ManifestRecord record) throws IOException {
		try (final var inputStream = Files.newInputStream(directory.resolve(record.fileName()))) {
			final var deserializer = new Deserializer(inputStream);

			while (true) {
				final var rawCommand = deserializer.read();
				if (rawCommand == null) {
					break;
				}

				redis.evaluate(null, rawCommand, 0, null);
			}
		}
	}

	@SneakyThrows
	public void log(byte[] command) {
		currentWriter.write(command);
		currentWriter.flush();
	}

}