package redis.configuration;

import java.util.Arrays;
import java.util.List;

public class Configuration {

	private final Property directory = new Property("dir");
	private final Property databaseFilename = new Property("dbfilename");

	private final List<Property> properties = Arrays.asList(
		directory,
		databaseFilename
	);

	public Property getProperty(String key) {
		for (final var property : properties) {
			if (property.key().equalsIgnoreCase(key)) {
				return property;
			}
		}

		return null;
	}

}