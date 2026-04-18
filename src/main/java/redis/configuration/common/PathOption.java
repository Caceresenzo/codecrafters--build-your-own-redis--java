package redis.configuration.common;

import java.nio.file.Path;

import redis.configuration.Option;

public class PathOption extends Option<Path> {

	public PathOption(String name) {
		super(name, null);
	}

	public PathOption(String name, Path defaultValue) {
		super(name, defaultValue);
	}

	@Override
	public Path parse(String value) {
		return Path.of(value);
	}

	public static PathOption currentDirectory(String name) {
		return new PathOption(name, Path.of(System.getProperty("user.dir")));
	}

}