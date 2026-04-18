package redis.configuration;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.experimental.Accessors;
import redis.configuration.common.PathOption;
import redis.configuration.common.PortOption;
import redis.configuration.common.RemoteOption;
import redis.configuration.common.StringOption;
import redis.configuration.common.YesNoOption;

@Accessors(fluent = true)
public class Configuration {

	private final @Getter PortOption port = new PortOption("port", 6379);
	private final @Getter PathOption directory = PathOption.currentDirectory("dir");
	private final @Getter StringOption databaseFilename = new StringOption("dbfilename");
	private final @Getter RemoteOption replicaOf = new RemoteOption("replicaof");
	private final @Getter StringOption masterReplicationId = new StringOption("master-replid", "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb");
	private final @Getter YesNoOption appendOnly = new YesNoOption("appendonly", "no");
	private final @Getter StringOption appendDirectoryName = new StringOption("appenddirname", "appendonlydir");
	private final @Getter StringOption appendFileName = new StringOption("appendfilename", "appendonly.aof");
	private final @Getter StringOption appendFileSync = new StringOption("appendfsync", "everysec");

	@SuppressWarnings({ "rawtypes" })
	private final List<Option> options = Arrays.asList(
		port,
		directory,
		databaseFilename,
		replicaOf,
		appendOnly,
		appendDirectoryName,
		appendFileName,
		appendFileSync
	);

	@SuppressWarnings("rawtypes")
	public List<Option> options() {
		return options;
	}

	@SuppressWarnings("rawtypes")
	public Option option(String key) {
		for (final var property : options) {
			if (property.getName().equalsIgnoreCase(key)) {
				return property;
			}
		}

		return null;
	}

	public boolean isSlave() {
		return replicaOf.getValue() != null;
	}

}