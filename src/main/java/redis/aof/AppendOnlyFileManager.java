package redis.aof;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import lombok.SneakyThrows;

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

		final var manifestFile = directory.resolve(baseFileName + ".manifest");
		if (!Files.exists(manifestFile)) {
			Files.writeString(manifestFile, "file %s seq 1 type i\n".formatted(firstAppendFileName));
		}

		currentWriter = Files.newOutputStream(firstAppendFile, StandardOpenOption.APPEND);
	}

	@SneakyThrows
	public void log(byte[] command) {
		currentWriter.write(command);
		currentWriter.flush();
	}

}