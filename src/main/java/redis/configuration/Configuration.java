package redis.configuration;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class Configuration {

	private final @Getter Option port = new Option("port", List.of(new PortArgument(6379)));
	private final @Getter PathOption directory = new PathOption("dir");
	private final @Getter PathOption databaseFilename = new PathOption("dbfilename");

	private final List<Option> options = Arrays.asList(
		port,
		directory,
		databaseFilename
	);

	public Option getOption(String key) {
		for (final var property : options) {
			if (property.name().equalsIgnoreCase(key)) {
				return property;
			}
		}

		return null;
	}

}