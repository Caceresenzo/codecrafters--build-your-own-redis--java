package redis.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class Configuration {

	private final @Getter Property<Integer> port = new Property<>("port", Integer::parseInt, 6379);
	private final @Getter Property<String> directory = new Property<>("dir", Function.identity());
	private final @Getter Property<String> databaseFilename = new Property<>("dbfilename", Function.identity());

	private final List<Property<?>> properties = Arrays.asList(
		port,
		directory,
		databaseFilename
	);

	public Property<?> getProperty(String key) {
		for (final var property : properties) {
			if (property.key().equalsIgnoreCase(key)) {
				return property;
			}
		}

		return null;
	}

}